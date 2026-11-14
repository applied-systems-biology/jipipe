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

package org.hkijena.jipipe.desktop.app.commandbar;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings;
import org.hkijena.jipipe.api.shortcuts.KeymapSettings;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopKeyboardCaptureDialog;
import org.hkijena.jipipe.desktop.app.shortcuts.JIPipeDesktopShortcutDispatcher;
import org.hkijena.jipipe.desktop.app.shortcuts.ShortcutScope;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.desktop.commons.dialogs.Button;
import org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The per-window command bar (issue #1346, Task 4): a search overlay shown
 * top-center in the host window that lists the registered shortcut commands
 * (built by {@link JIPipeDesktopCommandBarModel}), the ephemeral window and
 * tab switching commands (built by
 * {@link JIPipeDesktopCommandBarSwitching}), and - in chord mode - the
 * commands whose chord variant continues the pending prefix.
 * <p>
 * <b>Lifecycle.</b> The host window creates the bar once and calls
 * {@link #install(javax.swing.JFrame)} (typically right after the window is
 * realized) and {@link #uninstall()} on dispose. Install adds the bar
 * (invisible) to the window root pane's layered pane, registers a
 * {@link ShortcutScope} rooted at the root pane that answers only the
 * {@link JIPipeShortcutActions#COMMAND_BAR_OPEN} trigger, and installs the
 * dispatcher's chord-prefix hook when no other bar owns it (first bar wins,
 * like the status text sink). Uninstall mirrors both registrations; the hook
 * is cleared only when this bar still owns it (identity check).
 * <p>
 * <b>The captureActive invariant (read this before touching close()).</b>
 * While the bar is open, the application-wide shortcut dispatch is suspended
 * ({@link JIPipeDesktopShortcutDispatcher#setCaptureActive(boolean)}): the
 * trigger keystroke itself must reach the bar instead of re-resolving, and
 * typed text must never fire a shortcut. There is no timeout: if an exit
 * path ever skipped {@link #close()}, every keyboard shortcut in the
 * application would stay dead until the app is restarted. Therefore
 * <em>every</em> exit path funnels through {@link #close()}, which is the
 * only method that clears the flag and hides the overlay:
 * <ul>
 * <li>Escape (normal mode, via the search field input map)</li>
 * <li>the trigger keystroke pressed again (a toggle)</li>
 * <li>Enter firing the selected entry</li>
 * <li>focus loss of the search field (e.g. a click anywhere outside)</li>
 * <li>the configure affordance (before the capture dialog opens)</li>
 * <li>any key press while chord mode is open (fired chord, Escape, or an
 * unlisted non-modifier key, via the local {@link KeyEventDispatcher}; a
 * bare modifier press passes through and leaves chord mode open)</li>
 * <li>{@link #uninstall()} (defensively, so a disposed bar can never leave
 * the flag set)</li>
 * </ul>
 * The flag has a second owner: the capture dialog sets it for its lifetime.
 * The configure path never nests the two owners (it fully closes the bar
 * first), and {@link #close()} clears the flag only while the bar itself set
 * it (the {@code suspendOwned} ownership flag set by the open methods), so a
 * close can never resume the dispatch out from under the dialog's
 * recording.
 * <p>
 * <b>Why configure closes first.</b> The keybinding capture dialog
 * ({@link JIPipeDesktopKeyboardCaptureDialog}) owns the same suspend flag for
 * its lifetime (the recorded chord is often itself a bound shortcut). Nesting
 * the two owners would resume dispatch on the inner close and leave the outer
 * open state untracked, so the bar is fully closed (flag cleared, overlay
 * hidden, focus restored) <em>before</em> the dialog opens. The bar does not
 * reopen afterwards; the conflict warning and the override write mirror the
 * Keyboard settings page's commit path.
 */
public class JIPipeDesktopCommandBar extends JPanel {

    /**
     * The overlay width as a fraction of the root pane width. A layout
     * bound, deliberately not text-derived.
     */
    private static final double OVERLAY_WIDTH_FRACTION = 0.5;
    /**
     * The upper bound of the overlay height as a fraction of the root pane
     * height. A layout bound; a taller list scrolls.
     */
    private static final double OVERLAY_MAX_HEIGHT_FRACTION = 0.6;
    /**
     * The gap between the overlay's top edge and the root pane's top edge.
     * A pure gap, not a text-derived size.
     */
    private static final int OVERLAY_TOP_GAP = 8;
    /**
     * The size of the flat icon button convention of
     * {@link UIUtils#makeButtonFlat25x25}: an icon-button size, not a
     * text-derived size.
     */
    private static final int CONFIGURE_BUTTON_SIZE = 25;
    /**
     * The right padding of an entry row (the renderer's border inset).
     */
    private static final int ROW_RIGHT_PADDING = 4;
    /**
     * The width of the clickable configure affordance: the flat icon button
     * plus the row's right padding, i.e. exactly the area the renderer
     * paints the button into.
     */
    private static final int CONFIGURE_HIT_WIDTH = CONFIGURE_BUTTON_SIZE + ROW_RIGHT_PADDING;

    private static final String ACTION_KEY_UP = "command-bar-up";
    private static final String ACTION_KEY_DOWN = "command-bar-down";
    private static final String ACTION_KEY_FIRE = "command-bar-fire";
    private static final String ACTION_KEY_ESCAPE = "command-bar-escape";
    private static final String ACTION_KEY_TRIGGER = "command-bar-trigger";

    /**
     * The "Assign anyway" button of the conflict warning
     * (identity-compared, like the Keyboard settings page's button).
     */
    private static final Button ASSIGN_ANYWAY = Button.custom("Assign anyway");

    private final JIPipeDesktopSearchTextField searchField = new JIPipeDesktopSearchTextField();
    private final DefaultListModel<JIPipeDesktopCommandBarModel.Entry> listModel = new DefaultListModel<>();
    private final JList<JIPipeDesktopCommandBarModel.Entry> list = new JList<>(listModel);
    private final JIPipeDesktopShortcutDispatcher dispatcher = JIPipeDesktopShortcutDispatcher.getInstance();
    /**
     * The trigger strokes currently installed in the search field's input
     * map, so a rebind can replace exactly the strokes this bar installed.
     */
    private final List<KeyStroke> installedTriggerStrokes = new ArrayList<>();
    /**
     * Repositions the overlay when the root pane is resized (added in
     * {@link #install}, removed in {@link #uninstall}).
     */
    private final ComponentListener repositionListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            repositionOverlay();
        }
    };

    /**
     * The host window, or {@code null} while not installed.
     */
    private JFrame window;
    /**
     * The host window's root pane (the scope root and the size reference of
     * the overlay), or {@code null} while not installed.
     */
    private JRootPane rootPane;
    /**
     * The constraints the bar was added to the layered pane with (remembered
     * for a clean uninstall).
     */
    private Object layerConstraints;
    /**
     * The trigger scope registered with the dispatcher, or {@code null}
     * while not installed.
     */
    private ShortcutScope scope;
    /**
     * The chord-prefix hook this bar installed, or {@code null}. Kept so
     * {@link #uninstall()} can remove it only while this bar still owns it.
     */
    private Predicate<KeyStroke> chordPrefixHandler;
    /**
     * The local key event dispatcher of chord mode, or {@code null} while
     * chord mode is closed. Removed in every {@link #close()}.
     */
    private KeyEventDispatcher chordDispatcher;
    /**
     * The entries the list currently displays (the same rows the chord
     * dispatcher of chord mode matches against).
     */
    private List<JIPipeDesktopCommandBarModel.Entry> entries = List.of();
    /**
     * The pending chord prefix while chord mode is open.
     */
    private KeyStroke chordPrefix;
    /**
     * Whether the overlay is in chord mode (openChordMode) rather than
     * normal mode (openNormalMode).
     */
    private boolean chordMode;
    /**
     * The focus owner captured when the bar was opened; restored by
     * {@link #close()}.
     */
    private Component previousFocusOwner;
    /**
     * Whether the bar itself set {@code captureActive} (in
     * {@link #openNormalMode()} or {@link #openChordMode(KeyStroke)}) and
     * therefore owns clearing it: {@link #close()} resumes the dispatch
     * only while this flag is set. Visibility is not an ownership
     * guarantee, so a close of a bar that did not arm the suspension (or
     * whose owner was already handed over, e.g. to the capture dialog)
     * must leave the flag alone.
     */
    private boolean suspendOwned;

    /**
     * Creates the bar. Use {@link #install(JFrame)} to attach it to a window.
     */
    public JIPipeDesktopCommandBar() {
        super(new BorderLayout(0, 4));
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(UIUtils.createControlBorder(),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        searchField.setToolTipText("Type to filter commands");
        add(searchField, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // The list must never take the focus: the search field keeps it for
        // the whole open lifetime (its focus loss closes the bar), while
        // mouse clicks still select rows.
        list.setFocusable(false);
        list.setCellRenderer(new EntryCellRenderer());
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        installSearchFieldBehavior();
        installKeyBindings();
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onListClicked(e);
            }
        });

        setVisible(false);
    }

    /**
     * The list mode the bar renders with. Package-private indirection so the
     * read can be changed without touching the call sites.
     * <p>
     * Issue #1346, Task 6: reads the persisted list mode setting
     * ({@link CommandBarSettings#currentMode()}, null/unknown-safe with the
     * {@link JIPipeDesktopCommandBarModel.ListMode#GRAYED} fallback).
     */
    static JIPipeDesktopCommandBarModel.ListMode currentListMode() {
        return CommandBarSettings.currentMode();
    }

    // ------------------------------------------------------------------
    // Lifecycle: install / uninstall
    // ------------------------------------------------------------------

    /**
     * Installs the bar into the given window: adds it (invisible) to the
     * root pane's layered pane, registers the trigger shortcut scope rooted
     * at the root pane, and installs the dispatcher's chord-prefix hook when
     * no other bar owns it. Installing twice into the same window is a
     * no-op; installing into another window uninstalls first.
     *
     * @param window the host window
     */
    public void install(JFrame window) {
        Objects.requireNonNull(window, "window");
        if (this.window == window) {
            return;
        }
        if (this.window != null) {
            uninstall();
        }
        this.window = window;
        this.rootPane = window.getRootPane();
        // Above the content pane and menu bar, below popups (tooltips and
        // combobox lists of the bar itself stay visible).
        this.layerConstraints = JLayeredPane.MODAL_LAYER;
        rootPane.getLayeredPane().add(this, layerConstraints);
        setVisible(false);
        rootPane.addComponentListener(repositionListener);

        // The trigger scope: rooted at the window's root pane (the same root
        // resolution as JIPipeDesktopDockPanel) and answering only the open
        // command bar action. openNormalMode ignores a re-open of an already
        // open bar (toggle semantics; the dispatch is suspended while open,
        // so the trigger cannot fire through the scope in that state anyway).
        scope = new ShortcutScope(rootPane, actionId -> {
            if (JIPipeShortcutActions.COMMAND_BAR_OPEN.id().equals(actionId)) {
                return this::openNormalMode;
            }
            return null;
        });
        dispatcher.registerScope(scope);

        // The chord-prefix hook (the dispatcher's PREFIX_STARTED branch is
        // wired in Task 5). First bar wins, like the status text sink.
        if (JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler() == null) {
            chordPrefixHandler = this::openChordMode;
            JIPipeDesktopShortcutDispatcher.setChordPrefixHandler(chordPrefixHandler);
        }
    }

    /**
     * Uninstalls the bar: closes it (defensively - a disposed bar must never
     * leave the dispatch suspended), removes the resize listener, unregisters
     * the scope, and removes the chord-prefix hook only when this bar still
     * owns it. Uninstalling a non-installed bar is a no-op.
     */
    public void uninstall() {
        if (window == null) {
            return;
        }
        close();
        rootPane.removeComponentListener(repositionListener);
        rootPane.getLayeredPane().remove(this);
        if (scope != null) {
            dispatcher.unregisterScope(scope);
            scope = null;
        }
        if (chordPrefixHandler != null
                && JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler() == chordPrefixHandler) {
            JIPipeDesktopShortcutDispatcher.setChordPrefixHandler(null);
        }
        chordPrefixHandler = null;
        layerConstraints = null;
        rootPane = null;
        window = null;
    }

    // ------------------------------------------------------------------
    // Open / close
    // ------------------------------------------------------------------

    /**
     * Opens the overlay in normal mode: top-center of the layered pane, the
     * width a fraction of the root pane and the height the content's
     * preferred size (clamped to a fraction of the root pane height),
     * focused into the search field, with the app-wide shortcut dispatch
     * suspended for the open lifetime. See the class javadoc for the
     * captureActive invariant.
     */
    public void openNormalMode() {
        if (window == null || rootPane == null) {
            return;
        }
        if (isVisible()) {
            // Re-entry guard (toggle semantics, symmetric with
            // openChordMode's focus-owner guard): a second open must not
            // overwrite previousFocusOwner, or the focus restore of the
            // original open would be destroyed.
            return;
        }
        chordMode = false;
        chordPrefix = null;
        if (previousFocusOwner == null) {
            previousFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        }
        // A fresh open shows the unfiltered list.
        searchField.setText("");
        rebuildList();
        setVisible(true);
        repositionOverlay();
        JIPipeDesktopShortcutDispatcher.getInstance().setCaptureActive(true);
        suspendOwned = true;
        searchField.getTextField().requestFocusInWindow();
    }

    /**
     * Opens the overlay in chord mode: the list shows the registry commands
     * whose chord variant continues {@code prefix} (available first, no
     * ephemeral switching entries). Called by the dispatcher's chord-prefix
     * hook while a chord prefix is pending; returns {@code false} when the
     * bar's window does not own the keyboard focus (the chord belongs to
     * another window) or the bar is not installed.
     * <p>
     * While open, a local {@link KeyEventDispatcher} observes every key
     * press (the capture dialog's lifecycle pattern): the second key of a
     * listed chord variant fires that command and closes; Escape closes; a
     * bare modifier press passes through untouched and keeps chord mode
     * open (keyboards emit the modifier before the letter of a shifted
     * second key); any other key cancels and closes (the non-modifier
     * presses are all consumed, matching the dispatcher's swallow
     * semantics for a pending chord).
     *
     * @param prefix the first stroke of the pending chord
     * @return whether the bar opened (and consumed the chord)
     */
    public boolean openChordMode(KeyStroke prefix) {
        if (window == null || rootPane == null) {
            return false;
        }
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (kfm.getFocusedWindow() != window) {
            // The chord was pressed in another window: not ours to handle.
            return false;
        }
        chordMode = true;
        chordPrefix = prefix;
        if (previousFocusOwner == null) {
            previousFocusOwner = kfm.getFocusOwner();
        }
        // Chord mode is about bound commands; the search filter is not
        // applied and the field starts empty.
        searchField.setText("");
        rebuildList();
        setVisible(true);
        repositionOverlay();
        // Harmless if the dispatcher handoff already set it (Task 5 wires
        // that handoff).
        JIPipeDesktopShortcutDispatcher.getInstance().setCaptureActive(true);
        suspendOwned = true;
        installChordDispatcher();
        return true;
    }

    /**
     * The single exit path of the overlay: removes the local chord key
     * dispatcher, hides the overlay, restores the focus owner captured at
     * open time, and clears the app-wide dispatch suspension - but only
     * when the bar itself armed it ({@code suspendOwned}): visibility is
     * not an ownership guarantee, so a close that reaches a bar whose
     * suspension belongs to the capture dialog must leave the flag alone
     * (hiding an already hidden bar, in contrast, is harmless and stays
     * guarded by visibility alone). Every exit path funnels here; see the
     * class javadoc for why this is the most important invariant of the
     * class.
     */
    public void close() {
        if (chordDispatcher != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(chordDispatcher);
            chordDispatcher = null;
        }
        chordMode = false;
        chordPrefix = null;
        if (isVisible()) {
            setVisible(false);
            restorePreviousFocus();
        }
        if (suspendOwned) {
            JIPipeDesktopShortcutDispatcher.getInstance().setCaptureActive(false);
            suspendOwned = false;
        }
        previousFocusOwner = null;
    }

    private void restorePreviousFocus() {
        Component owner = previousFocusOwner;
        previousFocusOwner = null;
        if (owner != null) {
            owner.requestFocusInWindow();
        }
    }

    // ------------------------------------------------------------------
    // Entry list
    // ------------------------------------------------------------------

    /**
     * Rebuilds the displayed entries: collects the registry commands plus
     * (in normal mode) the ephemeral switching commands, then applies the
     * chord filter (chord mode) or the search query (normal mode), and
     * refreshes the trigger keystroke bindings. Must run on the EDT.
     */
    private void rebuildList() {
        List<JIPipeDesktopCommandBarModel.Entry> registryEntries = JIPipeDesktopCommandBarModel.collect(
                JIPipeShortcutRegistry.getInstance().all(),
                JIPipeShortcutSettings.current(),
                currentListMode(),
                // The bar itself is the focus reference: resolveAction walks
                // the scope chain, and while the bar is open the text-input
                // guard of a focused search field must not apply (dispatch
                // is suspended; see resolveAction's javadoc).
                id -> dispatcher.resolveAction(id, JIPipeDesktopCommandBar.this),
                chordMode ? List.of() : JIPipeDesktopCommandBarSwitching.all(window));
        List<JIPipeDesktopCommandBarModel.Entry> display;
        if (chordMode) {
            display = JIPipeDesktopCommandBarModel.chordEntries(registryEntries, chordPrefix);
        } else {
            display = filterByQuery(registryEntries, searchField.getText());
        }
        entries = display;
        listModel.clear();
        for (JIPipeDesktopCommandBarModel.Entry entry : display) {
            listModel.addElement(entry);
        }
        updateTriggerBindings();
        selectFirstSelectable();
    }

    /**
     * The search filter of the normal mode: an entry is kept when the query
     * (case insensitive) is contained in the entry name or in its category;
     * a category header is kept only while at least one of its entries
     * matched. One small method so Task 7 (fuzzy scoring) touches exactly
     * this place.
     */
    private static List<JIPipeDesktopCommandBarModel.Entry> filterByQuery(
            List<JIPipeDesktopCommandBarModel.Entry> entries, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return entries;
        }
        List<JIPipeDesktopCommandBarModel.Entry> result = new ArrayList<>(entries.size());
        JIPipeDesktopCommandBarModel.Entry pendingHeader = null;
        for (JIPipeDesktopCommandBarModel.Entry entry : entries) {
            if (entry.isCategoryHeader()) {
                pendingHeader = entry;
                continue;
            }
            if (containsIgnoreCase(entry.name(), q) || containsIgnoreCase(entry.category(), q)) {
                if (pendingHeader != null) {
                    result.add(pendingHeader);
                    pendingHeader = null;
                }
                result.add(entry);
            }
        }
        return result;
    }

    private static boolean containsIgnoreCase(String text, String query) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    // ------------------------------------------------------------------
    // Selection and firing
    // ------------------------------------------------------------------

    /**
     * @param entry a row, may be {@code null}
     * @return whether the row can hold the selection (category headers are
     *         skipped in selection movement)
     */
    private static boolean isSelectableRow(JIPipeDesktopCommandBarModel.Entry entry) {
        return entry != null && !entry.isCategoryHeader();
    }

    private void selectFirstSelectable() {
        for (int i = 0; i < listModel.size(); i++) {
            if (isSelectableRow(listModel.get(i))) {
                list.setSelectedIndex(i);
                list.ensureIndexIsVisible(i);
                return;
            }
        }
        list.clearSelection();
    }

    /**
     * Moves the selection by one row in the given direction, skipping
     * category headers, clamping at the list ends (no wrap).
     *
     * @param delta -1 for up, +1 for down
     */
    private void moveSelection(int delta) {
        int size = listModel.size();
        if (size == 0) {
            return;
        }
        int index = list.getSelectedIndex();
        if (index < 0) {
            // No selection yet: the first step lands on the nearest end.
            index = delta > 0 ? -1 : size;
        }
        int next = index;
        while (true) {
            next += delta;
            if (next < 0 || next >= size) {
                return;
            }
            if (isSelectableRow(listModel.get(next))) {
                list.setSelectedIndex(next);
                list.ensureIndexIsVisible(next);
                return;
            }
        }
    }

    /**
     * Fires the selected entry: only available entries with a command fire.
     * Registry commands are re-resolved through the dispatcher at fire time
     * (the availability may have changed since the list was built); a
     * re-resolve that comes up empty leaves the bar open (nothing fired),
     * otherwise the command runs on the EDT and the bar closes (the exit
     * path funnels through close()).
     */
    private void fireSelected() {
        JIPipeDesktopCommandBarModel.Entry entry = list.getSelectedValue();
        if (entry == null || entry.isCategoryHeader() || !entry.isAvailable()
                || entry.command() == null) {
            return;
        }
        Runnable command;
        if (entry.actionId() != null) {
            command = dispatcher.resolveAction(entry.actionId(), this);
            if (command == null) {
                // No longer resolvable: do not fire (and do not close).
                return;
            }
        } else {
            command = entry.command();
        }
        SwingUtilities.invokeLater(command);
        close();
    }

    // ------------------------------------------------------------------
    // Chord mode key handling
    // ------------------------------------------------------------------

    /**
     * Installs the local chord-mode key event dispatcher (the capture
     * dialog's lifecycle pattern: added before the interaction, removed in
     * {@link #close()}). Removing a dispatcher from inside its own callback
     * is safe: the keyboard focus manager iterates a copy of its dispatcher
     * list.
     */
    private void installChordDispatcher() {
        if (chordDispatcher != null) {
            return;
        }
        KeyEventDispatcher localDispatcher = e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
            if (stroke == null) {
                return false;
            }
            if (chordPrefix != null) {
                for (JIPipeDesktopCommandBarModel.Entry entry : entries) {
                    if (!entry.isAvailable() || entry.command() == null) {
                        continue;
                    }
                    for (List<KeyStroke> variant : entry.variants()) {
                        if (variant.size() == 2 && variant.get(0).equals(chordPrefix)
                                && variant.get(1).equals(stroke)) {
                            Runnable command = entry.command();
                            SwingUtilities.invokeLater(command);
                            close();
                            return true;
                        }
                    }
                }
            }
            if (stroke.getKeyCode() == KeyEvent.VK_ESCAPE && stroke.getModifiers() == 0) {
                // Escape: a clean close.
                close();
                return true;
            }
            if (isModifierKeyCode(stroke.getKeyCode())) {
                // A bare modifier press never completes a stroke: pass it
                // through and keep chord mode open (the capture dialog's
                // recorder follows the same rule). Keyboards emit the
                // modifier press BEFORE the letter, so closing here would
                // make every shifted second key (e.g. Ctrl+T then Shift+P)
                // unreachable: the Shift would swallow the chord, and the
                // P would arrive in normal dispatch.
                return false;
            }
            // An unlisted key: cancel and close, matching the dispatcher's
            // swallow semantics for a chord that resolves to nothing.
            close();
            return true;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(localDispatcher);
        chordDispatcher = localDispatcher;
    }

    /**
     * @param keyCode a key code
     * @return whether the key is a pure modifier key (Ctrl, Shift, Alt,
     *         Alt Graph or Meta; the key codes of the left/right variants
     *         are the same as the plain ones in AWT)
     */
    private static boolean isModifierKeyCode(int keyCode) {
        return keyCode == KeyEvent.VK_SHIFT
                || keyCode == KeyEvent.VK_CONTROL
                || keyCode == KeyEvent.VK_ALT
                || keyCode == KeyEvent.VK_META
                || keyCode == KeyEvent.VK_ALT_GRAPH;
    }

    // ------------------------------------------------------------------
    // Configure keybinding affordance
    // ------------------------------------------------------------------

    /**
     * The click handler of the list: a click into the configure affordance
     * (the rightmost {@link #CONFIGURE_HIT_WIDTH} pixels of a configurable,
     * available row) opens the capture flow for that row.
     */
    private void onListClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index < 0) {
            return;
        }
        Rectangle bounds = list.getCellBounds(index, index);
        if (bounds == null || !bounds.contains(e.getPoint())) {
            return;
        }
        JIPipeDesktopCommandBarModel.Entry entry = listModel.get(index);
        if (entry == null || entry.isCategoryHeader() || !entry.isConfigurable()
                || !entry.isAvailable() || entry.actionId() == null) {
            return;
        }
        if (e.getX() < bounds.x + bounds.width - CONFIGURE_HIT_WIDTH) {
            return;
        }
        configureEntry(entry);
    }

    /**
     * Runs the Keyboard settings page's commit path for the given entry:
     * FIRST closes the bar (the capture dialog owns the dispatch suspension
     * - see the class javadoc), then records a chord with
     * {@link JIPipeDesktopKeyboardCaptureDialog#capture}, resolves conflicts
     * (skipped for a cleared chord) with an identity-checked "Assign anyway"
     * warning, and writes the override through the keymap binding. The bar
     * does not reopen afterwards.
     */
    private void configureEntry(JIPipeDesktopCommandBarModel.Entry entry) {
        JIPipeShortcutAction action = JIPipeShortcutRegistry.getInstance().byId(entry.actionId());
        if (action == null || window == null) {
            return;
        }
        // The capture dialog owns the captureActive switch: the bar must be
        // fully closed before the dialog opens, so the two suspend owners
        // never nest.
        close();
        KeymapSettings current = JIPipeShortcutSettings.current();
        List<List<KeyStroke>> currentVariants = JIPipeShortcutRegistry.getInstance()
                .bindingFor(action.id(), current).variants();
        Optional<List<KeyStroke>> captured =
                JIPipeDesktopKeyboardCaptureDialog.capture(window, action.name(), currentVariants);
        if (captured.isEmpty()) {
            return;
        }
        List<KeyStroke> chord = captured.get();
        if (!chord.isEmpty()) {
            KeymapSettings settings = JIPipeShortcutSettings.current();
            List<String> conflictIds = JIPipeShortcutRegistry.getInstance()
                    .findConflicts(action.id(), List.of(chord), settings);
            if (!conflictIds.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (String id : conflictIds) {
                    JIPipeShortcutAction other = JIPipeShortcutRegistry.getInstance().byId(id);
                    names.add(escapeHtml(other != null ? other.name() : id));
                }
                // An <html> message passes through the dialog chrome verbatim,
                // so the (already escaped) names are safe to embed.
                String message = "<html>The shortcut you recorded is already assigned to:<br>"
                        + String.join(", ", names) + "<br><br>"
                        + "Assigning it here too would bind the same keys to several actions. "
                        + "Assign it anyway?";
                // asDefault() returns a copy; keep that instance for the
                // identity comparison (the chrome records the exact instance).
                Button assignAnyway = ASSIGN_ANYWAY.asDefault();
                Optional<Button> choice = JIPipeDesktopDialogs.choose(window, "Shortcut conflict", message,
                        assignAnyway, Button.CANCEL);
                if (choice.filter(b -> b == assignAnyway).isEmpty()) {
                    return;
                }
            }
        }
        KeymapSettings settings = JIPipeShortcutSettings.current();
        settings.setOverride(action.id(), List.of(chord));
        JIPipeShortcutSettings.binding().set(settings);
    }

    /**
     * Escapes the HTML-significant characters of a plain string, for
     * embedding in the (verbatim-passed) {@code <html>} conflict message.
     */
    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ------------------------------------------------------------------
    // Overlay geometry
    // ------------------------------------------------------------------

    /**
     * Positions the overlay top-center in the layered pane: the width is a
     * fraction of the root pane width (never text-derived); the height is
     * the content's preferred size clamped to a fraction of the root pane
     * height (a clamped list scrolls).
     */
    private void repositionOverlay() {
        if (rootPane == null || !isVisible()) {
            return;
        }
        int rootWidth = rootPane.getWidth();
        int rootHeight = rootPane.getHeight();
        if (rootWidth <= 0 || rootHeight <= 0) {
            return;
        }
        int width = (int) (rootWidth * OVERLAY_WIDTH_FRACTION);
        int maxHeight = (int) (rootHeight * OVERLAY_MAX_HEIGHT_FRACTION);
        int height = Math.min(getPreferredSize().height, maxHeight);
        int x = (rootWidth - width) / 2;
        setBounds(x, OVERLAY_TOP_GAP, width, height);
        doLayout();
    }

    // ------------------------------------------------------------------
    // Search field behavior and key bindings
    // ------------------------------------------------------------------

    /**
     * Wires the search field: typed text re-runs the list build (the query
     * filter), and any focus loss closes the bar (with dispatch suspended,
     * an unnoticed open state would silently kill every shortcut; see the
     * class javadoc).
     */
    private void installSearchFieldBehavior() {
        searchField.getTextField().getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (isVisible() && !chordMode) {
                    rebuildList();
                }
            }
        });
        searchField.getTextField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (isVisible()) {
                    close();
                }
            }
        });
    }

    /**
     * Installs the normal-mode key handling on the search field
     * (WHEN_FOCUSED): up/down move the selection, Enter fires, Escape
     * closes, and the trigger variants close the bar (a toggle).
     */
    private void installKeyBindings() {
        InputMap inputMap = searchField.getTextField().getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = searchField.getTextField().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("pressed UP"), ACTION_KEY_UP);
        inputMap.put(KeyStroke.getKeyStroke("pressed DOWN"), ACTION_KEY_DOWN);
        inputMap.put(KeyStroke.getKeyStroke("pressed ENTER"), ACTION_KEY_FIRE);
        inputMap.put(KeyStroke.getKeyStroke("pressed ESCAPE"), ACTION_KEY_ESCAPE);
        actionMap.put(ACTION_KEY_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelection(-1);
            }
        });
        actionMap.put(ACTION_KEY_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveSelection(1);
            }
        });
        actionMap.put(ACTION_KEY_FIRE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireSelected();
            }
        });
        actionMap.put(ACTION_KEY_ESCAPE, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        actionMap.put(ACTION_KEY_TRIGGER, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // The trigger is a toggle: pressing it while the bar is open
                // closes it.
                close();
            }
        });
    }

    /**
     * Refreshes the trigger keystroke bindings from the current keymap
     * (resolved on every rebuild, so a rebind works without reinstalling).
     * Only length-1 variants can be typed into the focused search field; a
     * chord trigger reaches the bar through the dispatcher instead.
     */
    private void updateTriggerBindings() {
        InputMap inputMap = searchField.getTextField().getInputMap(JComponent.WHEN_FOCUSED);
        for (KeyStroke stroke : installedTriggerStrokes) {
            inputMap.remove(stroke);
        }
        installedTriggerStrokes.clear();
        List<List<KeyStroke>> variants = JIPipeShortcutRegistry.getInstance()
                .bindingFor(JIPipeShortcutActions.COMMAND_BAR_OPEN.id(), JIPipeShortcutSettings.current())
                .variants();
        for (List<KeyStroke> variant : variants) {
            if (variant.size() == 1) {
                KeyStroke stroke = variant.get(0);
                inputMap.put(stroke, ACTION_KEY_TRIGGER);
                installedTriggerStrokes.add(stroke);
            }
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /**
     * The list renderer: category header rows (bold, muted foreground) and
     * entry rows (name left with the row icon, binding text right in gray,
     * unavailable rows fully muted, the configure affordance on the selected
     * available configurable row). Row heights come from the renderer's own
     * preferred sizes (no fixed cell height, no assumed text sizes); the
     * reused row panels are force-revalidated after every configure so the
     * freshly set texts are measured synchronously (the
     * {@code JIPipeDesktopFindPanel.FindTreeCellRenderer} pattern).
     */
    private final class EntryCellRenderer implements ListCellRenderer<JIPipeDesktopCommandBarModel.Entry> {
        private final JPanel headerPanel = new JPanel(new BorderLayout());
        private final JLabel headerLabel = new JLabel();
        private final JPanel entryPanel = new JPanel(new BorderLayout());
        private final JPanel entryEndPanel = new JPanel(new BorderLayout());
        private final JLabel nameLabel = new JLabel();
        private final JLabel bindingLabel = new JLabel();
        private final JButton configureButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit.png"));

        EntryCellRenderer() {
            headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
            headerPanel.setOpaque(true);
            headerPanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
            headerPanel.add(headerLabel, BorderLayout.CENTER);

            configureButton.setToolTipText("Change the key binding of this command");
            configureButton.setFocusable(false);
            configureButton.setFocusPainted(false);
            configureButton.setContentAreaFilled(false);
            UIUtils.makeButtonFlat25x25(configureButton);

            nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 0));
            bindingLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 0));
            entryEndPanel.setOpaque(false);
            entryEndPanel.add(bindingLabel, BorderLayout.CENTER);
            entryEndPanel.add(configureButton, BorderLayout.EAST);
            entryPanel.setOpaque(true);
            entryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, ROW_RIGHT_PADDING));
            entryPanel.add(nameLabel, BorderLayout.CENTER);
            entryPanel.add(entryEndPanel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeDesktopCommandBarModel.Entry> list,
                                                      JIPipeDesktopCommandBarModel.Entry value, int index,
                                                      boolean selected, boolean cellHasFocus) {
            if (value.isCategoryHeader()) {
                applyRowColors(headerPanel, selected);
                headerLabel.setText(value.name());
                headerLabel.setForeground(selected
                        ? headerPanel.getForeground()
                        : UIManager.getColor("Label.disabledForeground"));
                headerPanel.invalidate();
                headerPanel.validate();
                return headerPanel;
            }
            applyRowColors(entryPanel, selected);
            Color foreground = entryPanel.getForeground();
            Color mutedForeground = selected
                    ? foreground
                    : UIManager.getColor("Label.disabledForeground");
            nameLabel.setText(value.name());
            nameLabel.setIcon(value.icon());
            nameLabel.setForeground(value.isAvailable() ? foreground : mutedForeground);
            String bindingText = value.bindingText();
            bindingLabel.setText(bindingText == null ? "" : bindingText);
            bindingLabel.setForeground(mutedForeground);
            configureButton.setVisible(selected && value.isConfigurable()
                    && value.isAvailable() && value.actionId() != null);
            entryPanel.invalidate();
            entryPanel.validate();
            return entryPanel;
        }

        private void applyRowColors(JPanel panel, boolean selected) {
            if (selected) {
                panel.setBackground(JIPipeDesktopCommandBar.this.list.getSelectionBackground());
                panel.setForeground(JIPipeDesktopCommandBar.this.list.getSelectionForeground());
            } else {
                panel.setBackground(JIPipeDesktopCommandBar.this.list.getBackground());
                panel.setForeground(JIPipeDesktopCommandBar.this.list.getForeground());
            }
        }
    }
}
