/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.desktop.app.shortcuts.JIPipeDesktopShortcutDispatcher;
import org.hkijena.jipipe.desktop.commons.dialogs.Button;
import org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The modal "record a shortcut" dialog of the keyboard settings page (issue
 * #1346, Task 9).
 * <p>
 * Built on the modern dialog API's {@link JIPipeDesktopDialogs.Builder
 * Builder} escape hatch (#1341): the recorder area is the builder's content,
 * and the chrome provides the default-flagged OK button, the Cancel button,
 * and the Escape-to-dismiss behavior. All dismissal paths funnel into the
 * dialog's single dismiss path and map to {@link Optional#empty()}; only an
 * explicit OK click commits the displayed chord.
 * <p>
 * <b>Recording rule.</b> While the dialog is showing, a {@code KeyEventDispatcher}
 * local to this method observes every {@code KEY_PRESSED} of the focused
 * window (the dialog is application-modal, so that is the dialog itself):
 * <ul>
 * <li>A pure modifier press (Ctrl, Shift, Alt or Meta alone, including the
 *     left/right variants) never completes a stroke: it is ignored, so the
 *     user can hold a modifier down before the key.</li>
 * <li>A bare Enter or Space (no modifier) is not a stroke: it passes through
 *     so it confirms the dialog or activates the focused button like any
 *     other key. With any modifier it is recorded (e.g. Ctrl+Enter).</li>
 * <li>The first recorded stroke is displayed as the primary chord
 *     ({@link JIPipeShortcutRegistry#formatChord(List)} of the single
 *     stroke).</li>
 * <li>A second recorded stroke while one is displayed forms the chord
 *     ({@code first then second}).</li>
 * <li>A third recorded stroke <em>replaces the chord's second stroke</em>
 *     (a chord never grows beyond two strokes; the first stroke stays).</li>
 * </ul>
 * A recorded stroke is consumed, so it cannot trigger buttons or text fields
 * of the dialog. The application-wide
 * {@link JIPipeDesktopShortcutDispatcher} is suspended for the dialog's
 * lifetime ({@link JIPipeDesktopShortcutDispatcher#setCaptureActive(boolean)}),
 * because the recorded strokes are often exactly the shortcuts currently bound
 * to them (re-assigning an action to its own default is the common case), and
 * the dispatcher would otherwise fire those shortcuts and consume the chord
 * prefix.
 * <p>
 * <b>Result.</b> OK commits the displayed chord: a non-empty chord is the new
 * single-variant binding, an empty chord clears the action's override
 * ({@code Optional.of(List.of())}). Cancel, Escape or the window close button
 * return {@link Optional#empty()}.
 * <p>
 * Must be called on the event dispatch thread (the dialog is modal).
 */
public final class JIPipeDesktopKeyboardCaptureDialog {

    /**
     * The placeholder text of the recorder area while nothing is recorded.
     */
    private static final String HINT = "Press a key combination...";
    /**
     * The worst case of a two-stroke chord display: two strokes each carrying
     * all four modifier names and a long key name. Only used to measure the
     * recorder area's preferred width, so a realistic chord never clips. A
     * chord never grows beyond two strokes, so this bounds the width.
     */
    private static final String WORST_CASE_CHORD = "Shift+Ctrl+Meta+Alt+Back Slash then Shift+Ctrl+Meta+Alt+Back Slash";
    private static final int MODIFIER_MASK = InputEvent.SHIFT_DOWN_MASK
            | InputEvent.CTRL_DOWN_MASK
            | InputEvent.ALT_DOWN_MASK
            | InputEvent.META_DOWN_MASK;

    private JIPipeDesktopKeyboardCaptureDialog() {
    }

    /**
     * Records a new shortcut for the given action.
     *
     * @param parent           the parent component (the owning window is derived from it)
     * @param actionName       the display name of the action being (re)bound
     * @param currentVariants  the action's current effective variants, shown for reference
     * @return the recorded chord (a list of one or two strokes), or an empty
     *         chord list when the user cleared the recording, or empty when
     *         the dialog was dismissed
     */
    public static Optional<List<KeyStroke>> capture(Component parent, String actionName, List<List<KeyStroke>> currentVariants) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("capture() must be called on the event dispatch thread");
        }
        List<List<KeyStroke>> variants = currentVariants == null ? List.of() : currentVariants;
        List<KeyStroke> chord = new ArrayList<>();

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // The current binding of the action (all variants, or "-" when unassigned).
        JPanel currentRow = new JPanel(new GridBagLayout());
        currentRow.setOpaque(false);
        JLabel currentTitle = new JLabel("Current:");
        currentTitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        String currentText = variants.isEmpty() ? "-" : JIPipeShortcutRegistry.formatVariants(variants);
        JLabel currentValue = new JLabel(currentText, JIPipe.RESOURCES.getIcon16("actions/keyboard.png"), JLabel.LEFT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 6);
        currentRow.add(currentTitle, gbc);
        gbc.insets = new Insets(2, 0, 2, 0);
        currentRow.add(currentValue, gbc);
        content.add(currentRow, BorderLayout.NORTH);

        // The live recorder area: the displayed chord, measured so a realistic
        // chord never clips (no assumed text sizes).
        JLabel chordLabel = new JLabel(HINT);
        chordLabel.setFont(chordLabel.getFont().deriveFont(Font.BOLD));
        chordLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        JPanel chip = new JPanel(new GridBagLayout());
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.getCurrentStyle().getBorderColor()),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        chip.add(chordLabel);
        Dimension natural = chip.getPreferredSize();
        FontMetrics metrics = chordLabel.getFontMetrics(chordLabel.getFont());
        int gapPadding = 32; // pure gap, not a text size
        chip.setPreferredSize(new Dimension(
                Math.max(natural.width, metrics.stringWidth(WORST_CASE_CHORD) + gapPadding),
                natural.height));
        chip.setMinimumSize(new Dimension(
                Math.min(chip.getPreferredSize().width, metrics.stringWidth(HINT) + gapPadding),
                natural.height));
        content.add(chip, BorderLayout.CENTER);

        Runnable updateChip = () -> {
            if (chord.isEmpty()) {
                chordLabel.setText(HINT);
                chordLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            } else {
                chordLabel.setText(JIPipeShortcutRegistry.formatChord(chord));
                chordLabel.setForeground(UIManager.getColor("Label.foreground"));
            }
            chip.revalidate();
        };

        JPanel bottomRow = new JPanel(new BorderLayout(8, 0));
        bottomRow.setOpaque(false);
        JButton clearButton = UIUtils.createButton("Clear",
                "Remove the recorded combination",
                JIPipe.RESOURCES.getIcon16("actions/edit-clear.png"),
                () -> {
                    chord.clear();
                    updateChip.run();
                });
        bottomRow.add(clearButton, BorderLayout.WEST);
        JLabel bottomHint = new JLabel("Esc cancels. Click OK to keep the combination.");
        bottomHint.setForeground(UIManager.getColor("Label.disabledForeground"));
        bottomRow.add(bottomHint, BorderLayout.EAST);
        content.add(bottomRow, BorderLayout.SOUTH);

        // Suspend the app-wide shortcut dispatch (the recorded strokes may be
        // bound shortcuts) and observe every KEY_PRESSED of the focused window
        // (the modal dialog) with a dispatcher local to this call.
        JIPipeDesktopShortcutDispatcher dispatcher = JIPipeDesktopShortcutDispatcher.getInstance();
        dispatcher.setCaptureActive(true);
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        KeyEventDispatcher recorder = e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                // Escape never records: the dialog chrome maps it to dismiss.
                return false;
            }
            KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
            if (stroke == null || isModifierKeyCode(stroke.getKeyCode())) {
                return false;
            }
            boolean noModifiers = (stroke.getModifiers() & MODIFIER_MASK) == 0;
            if (noModifiers && (stroke.getKeyCode() == KeyEvent.VK_ENTER
                    || stroke.getKeyCode() == KeyEvent.VK_SPACE)) {
                // A bare Enter (both the top-row and the numpad one use
                // VK_ENTER) or Space confirms the dialog (default button) or
                // activates the focused button, as in any other dialog.
                return false;
            }
            if (chord.size() >= 2) {
                // A third press replaces the chord's second stroke; the first
                // stroke stays (the chord never grows beyond two strokes).
                chord.set(1, stroke);
            } else {
                chord.add(stroke);
            }
            updateChip.run();
            // The recorder owns this press: do not let it reach buttons or
            // text fields of the dialog.
            return true;
        };
        kfm.addKeyEventDispatcher(recorder);
        try {
            // Default-flagged OK: the chrome records the exact instance, so the
            // result is compared by identity. The implicit Cancel is appended
            // by the chrome (cancelable by default).
            Button ok = Button.OK.asDefault();
            Optional<Button> result = JIPipeDesktopDialogs.builder(parent, "Record shortcut")
                    .message(actionName)
                    .content(content)
                    .buttons(ok)
                    .show();
            return result.filter(b -> b == ok).map(b -> List.copyOf(chord));
        } finally {
            kfm.removeKeyEventDispatcher(recorder);
            dispatcher.setCaptureActive(false);
        }
    }

    /**
     * @param keyCode a key code
     * @return whether the key is a pure modifier key (Ctrl, Shift, Alt, Alt
     *         Graph or Meta; the key codes of the left/right variants are the
     *         same as the plain ones in AWT)
     */
    private static boolean isModifierKeyCode(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT
                || keyCode == KeyEvent.VK_CONTROL
                || keyCode == KeyEvent.VK_ALT
                || keyCode == KeyEvent.VK_META
                || keyCode == KeyEvent.VK_ALT_GRAPH;
    }
}
