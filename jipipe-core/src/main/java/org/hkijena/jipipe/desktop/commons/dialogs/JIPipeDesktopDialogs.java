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

package org.hkijena.jipipe.desktop.commons.dialogs;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.components.color.JIPipeDesktopColorChooserDialog;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The modern dialog API (work item #1341): a static facade over
 * {@link JIPipeDesktopDialog} in which the classic option-pane
 * confusion between "the user closed the window" (CLOSED_OPTION) and a real
 * choice is unrepresentable. Every dismissal - the Cancel button, the window
 * close button, or Escape - funnels into the dialog's single
 * {@link JIPipeDesktopDialog#dismiss()} path and maps to
 * {@code Optional.empty()} or {@code false} by construction.
 * <p>
 * All methods are application-modal and must be called on the
 * <em>event dispatch thread</em> (the same precondition as the pre-migration
 * dialog call sites). The facade does not marshal calls to the
 * EDT itself; doing so would be redundant for the existing call sites and
 * would change the threading semantics of the migration.
 * <p>
 * This class currently offers the notify/decide/choose/input methods
 * ({@link #message}, {@link #error}, {@link #confirm}, {@link #choose},
 * {@link #inputText}, {@link #inputUniqueText}, {@link #inputMultiLine},
 * {@link #inputInteger}, {@link #inputDouble}), the color chooser
 * delegation ({@link #chooseColor}), and the {@link Builder} escape hatch.
 */
public final class JIPipeDesktopDialogs {

    /**
     * Preferred dimensions of the scrollable stack-trace area in
     * {@link #error} dialogs: the width matches the wrapped message label,
     * and the height is bounded so long traces scroll instead of growing.
     */

    /**
     * Preferred dimensions of the scrollable multi-line input area in
     * {@link #inputMultiLine} dialogs (spec §4.2: ≈480×240, overridable only
     * via the builder).
     */
    private static final Dimension MULTILINE_PREFERRED_SIZE = new Dimension(480, 240);

    private JIPipeDesktopDialogs() {
    }

    /**
     * Re-exposes the dialog-level presenter seam for facade tests: installs
     * the {@link DialogPresenter} used by all facade methods (delegates to
     * {@link JIPipeDesktopDialog#setPresenter(DialogPresenter)}).
     * <p>
     * Package-private: {@link DialogPresenter} is package-private, so a
     * {@code public} method could not be implemented outside this package
     * anyway (tests are same-package).
     *
     * @param p the presenter to use
     */
    static void setPresenter(DialogPresenter p) {
        JIPipeDesktopDialog.setPresenter(p);
    }

    /**
     * Shows an OK-only notification dialog with the given message.
     * <p>
     * All dismissal paths (window close, Escape) are unrepresentable: the
     * dialog is acknowledged, not answered.
     *
     * @param parent  the parent component, or null for an ownerless dialog
     * @param title   the dialog title
     * @param message the message to show (may contain {@code <html>…</html>}), or null (no label)
     */
    public static void message(Component parent, String title, String message) {
        JIPipeDesktopDialog dialog = newDialog(parent, title);
        dialog.setMessageIcon(JIPipe.RESOURCES.getIcon32("actions/info.png"));
        dialog.addMessage(normalizeMessage(message));
        // Default-flagged OK: Enter acknowledges the dialog (the classic
        // option pane registered OK as the default button everywhere). The
        // result is unused (the dialog is acknowledged, not answered), so no
        // identity comparison is needed here.
        dialog.setButtons(Button.OK.asDefault());
        dialog.showAndWait();
    }

    /**
     * Shows an error dialog with the given message. When {@code cause} is
     * non-null, the dialog additionally shows the exception's stack trace in
     * a bounded (preferred height ≤150px), scrollable, read-only monospaced
     * text area below the message.
     *
     * @param parent  the parent component, or null for an ownerless dialog
     * @param title   the dialog title
     * @param message the error message (may contain {@code <html>…</html>}), or null (no label)
     * @param cause   the exception whose stack trace is shown, or null to omit it
     */
    public static void error(Component parent, String title, String message, Throwable cause) {
        JIPipeDesktopDialog dialog = newDialog(parent, title);
        dialog.setMessageIcon(JIPipe.RESOURCES.getIcon32("actions/error.png"));
        dialog.addMessage(normalizeMessage(message));
        if (cause != null) {
            dialog.addContent(tracePane(cause));
        }
        // Default-flagged OK: Enter acknowledges the dialog (see message()).
        dialog.setButtons(Button.OK.asDefault());
        dialog.showAndWait();
    }

    /**
     * Shows a Yes/No confirmation dialog.
     * <p>
     * Truth table (identical to the former {@code == YES_OPTION}
     * idiom): Yes → {@code true}; No, Cancel, window close, or Escape →
     * {@code false}. There is no way to represent "dismissed" separately
     * from "No" - by design.
     *
     * @param parent  the parent component, or null for an ownerless dialog
     * @param title   the dialog title
     * @param message the question (may contain {@code <html>…</html>}), or null (no label)
     * @return whether the user confirmed
     */
    public static boolean confirm(Component parent, String title, String message) {
        JIPipeDesktopDialog dialog = newDialog(parent, title);
        dialog.setMessageIcon(JIPipe.RESOURCES.getIcon32("actions/question.png"));
        dialog.addMessage(normalizeMessage(message));
        Button yes = Button.YES.asDefault();
        dialog.setCancelable(true);
        dialog.setButtons(yes, Button.NO);
        dialog.showAndWait();
        // "YES chosen" by identity: the dialog preserves the exact instance,
        // so a No click (or a dismissal, which leaves the result empty) is
        // false, and only the Yes instance itself is true.
        return dialog.getResult().orElse(null) == yes;
    }

    /**
     * Shows a choice dialog with the given options, left to right, plus an
     * implicit Cancel button appended last (skipped when the caller already
     * included {@link Button#CANCEL}).
     * <p>
     * The returned button is the <em>same instance</em> the caller passed,
     * so call sites can compare with {@code ==}. Dismissal (Cancel, window
     * close, Escape) yields {@link Optional#empty()}.
     *
     * @param parent  the parent component, or null for an ownerless dialog
     * @param title   the dialog title
     * @param message the question (may contain {@code <html>…</html>}), or null (no label)
     * @param options the choice buttons, in display order (at least one)
     * @return the chosen button instance, or empty if the dialog was dismissed
     * @throws IllegalArgumentException if no options are given
     */
    /**
     * The widest button row the choose dialog still renders as buttons.
     * Wider option sets switch to a combo box: five theme templates produced
     * a ~1000px-wide button row (found in the #1341 UX review). The threshold
     * is width-based rather than count-based, so a few long labels trigger
     * the switch just like many short ones.
     */
    private static final int MAX_CHOOSE_BUTTON_ROW_WIDTH = 480;

    public static Optional<Button> choose(Component parent, String title, String message, Button... options) {
        if (options.length == 0) {
            throw new IllegalArgumentException("choose() requires at least one option");
        }
        if (buttonRowWidth(options) > MAX_CHOOSE_BUTTON_ROW_WIDTH) {
            return chooseViaComboBox(parent, title, message, options);
        }
        JIPipeDesktopDialog dialog = newDialog(parent, title);
        dialog.addMessage(normalizeMessage(message));
        dialog.setCancelable(true);
        dialog.setButtons(options);
        dialog.showAndWait();
        return dialog.getResult();
    }

    /**
     * @return the width the button row for the given options would occupy
     * (option buttons plus the implicit Cancel and the row gaps)
     */
    private static int buttonRowWidth(Button[] options) {
        int width = 0;
        for (Button option : options) {
            width += new JButton(option.getLabel(), option.getIcon()).getPreferredSize().width;
        }
        width += new JButton(Button.CANCEL.getLabel(), Button.CANCEL.getIcon()).getPreferredSize().width;
        width += 8 * (options.length + 1); // the FlowLayout gaps
        return width;
    }

    /**
     * The combo-box rendering of {@link #choose(Component, String, String, Button...)}
     * for option sets too wide for a button row. The combo lists the options
     * (the default-flagged one preselected); OK submits the selected option
     * and Cancel/dismiss still yields empty, so the result contract of
     * {@code choose()} is unchanged.
     */
    private static Optional<Button> chooseViaComboBox(Component parent, String title,
                                                      String message, Button... options) {
        JComboBox<Button> comboBox = new JComboBox<>(options);
        for (Button option : options) {
            if (option.isDefault()) {
                comboBox.setSelectedItem(option);
                break;
            }
        }
        Button ok = Button.OK.asDefault();
        JIPipeDesktopDialog dialog = newInputDialog(parent, title, message, comboBox, ok);
        dialog.showAndWait();
        if (dialog.getResult().orElse(null) != ok) {
            return Optional.empty();
        }
        return Optional.ofNullable((Button) comboBox.getSelectedItem());
    }

    /**
     * Shows a single-line text input dialog: a {@link JTextField} pre-filled
     * with {@code initialValue} (null → empty) whose content is selected.
     * Enter inside the field and the OK button both submit.
     * <p>
     * Deliberate behavior change vs. the classic {@code showInputDialog}
     * (spec §4.2): submitting with <em>empty or whitespace-only</em> text
     * counts as dismiss - the result is {@link Optional#empty()}, never an
     * empty string. A caller that genuinely needs an empty value uses the
     * {@code Builder} escape hatch (a later task of #1341).
     *
     * @param parent       the parent component, or null for an ownerless dialog
     * @param title        the dialog title
     * @param message      the message to show (may contain {@code <html>…</html>}), or null (no label)
     * @param initialValue the initial field text, or null for an empty field
     * @return the submitted text, or empty if the dialog was dismissed or the submission was empty/whitespace-only
     */
    public static Optional<String> inputText(Component parent, String title, String message, String initialValue) {
        JTextField field = new JTextField(initialValue != null ? initialValue : "", 24);
        field.selectAll();
        // Default-flagged OK: Enter (outside the field) and the initial
        // focus behavior match the classic option pane. The flagged copy
        // must be kept: the chrome records it as the result, and okChosen
        // compares against this exact instance (never the Button.OK constant).
        Button ok = Button.OK.asDefault();
        JIPipeDesktopDialog dialog = newInputDialog(parent, title, message, field, ok);
        // Enter inside the field submits the OK action (spec §4.2): the
        // field's action event (fired on Enter) clicks the OK button - the
        // same code path as an OK click. Index 0 is the OK button: it is the
        // only caller-provided button (the implicit Cancel is appended last).
        JButton okButton = dialog.getActionButtons().get(0);
        field.addActionListener(e -> okButton.doClick());
        dialog.showAndWait();
        if (!okChosen(dialog, ok)) {
            return Optional.empty();
        }
        String text = field.getText();
        if (text.isBlank()) {
            // Empty-submit rule (spec §4.2): an empty/whitespace-only
            // submission counts as dismiss, not as an empty-string value.
            return Optional.empty();
        }
        return Optional.of(text);
    }

    /**
     * Shows a multi-line text input dialog: a {@link JTextArea} in a scroll
     * pane (≈480×240), pre-filled with {@code initialValue} (null → empty).
     * Enter inserts a newline - submission is via the OK button only.
     * <p>
     * Same empty-submit rule as {@link #inputText} (spec §4.2): submitting
     * with empty or whitespace-only text counts as dismiss.
     *
     * @param parent       the parent component, or null for an ownerless dialog
     * @param title        the dialog title
     * @param message      the message to show (may contain {@code <html>…</html>}), or null (no label)
     * @param initialValue the initial area text, or null for an empty area
     * @return the submitted text, or empty if the dialog was dismissed or the submission was empty/whitespace-only
     */
    public static Optional<String> inputMultiLine(Component parent, String title, String message, String initialValue) {
        JTextArea area = new JTextArea(initialValue != null ? initialValue : "");
        JScrollPane pane = new JScrollPane(area);
        pane.setPreferredSize(MULTILINE_PREFERRED_SIZE);
        Button ok = Button.OK.asDefault();
        JIPipeDesktopDialog dialog = newInputDialog(parent, title, message, pane, ok);
        // No Enter binding here: inside a JTextArea, Enter inserts a newline;
        // submission is via the OK button only (spec §4.2).
        dialog.showAndWait();
        if (!okChosen(dialog, ok)) {
            return Optional.empty();
        }
        String text = area.getText();
        if (text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    /**
     * Shows an integer input dialog: a plain {@link JSpinner} with a
     * {@link SpinnerNumberModel} (step 1) bounded by {@code min}/{@code max}.
     * A null {@code initialValue} starts the spinner at {@code min}.
     *
     * @param parent       the parent component, or null for an ownerless dialog
     * @param title        the dialog title
     * @param message      the message to show (may contain {@code <html>…</html>}), or null (no label)
     * @param initialValue the initial value, or null to start at {@code min}
     * @param min          the minimum (inclusive)
     * @param max          the maximum (inclusive)
     * @return the submitted value, or empty if the dialog was dismissed
     * @throws IllegalArgumentException if {@code min > max} (thrown before any dialog is built)
     */
    public static Optional<Integer> inputInteger(Component parent, String title, String message, Integer initialValue, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not be greater than max (" + max + ")");
        }
        SpinnerNumberModel model = new SpinnerNumberModel(initialValue != null ? initialValue : min, min, max, 1);
        JSpinner spinner = new JSpinner(model);
        Button ok = Button.OK.asDefault();
        JIPipeDesktopDialog dialog = newInputDialog(parent, title, message, spinner, ok);
        dialog.showAndWait();
        if (!okChosen(dialog, ok)) {
            return Optional.empty();
        }
        return Optional.of(model.getNumber().intValue());
    }

    /**
     * Shows a floating-point input dialog: a plain {@link JSpinner} with a
     * {@link SpinnerNumberModel} (step 1) bounded by {@code min}/{@code max}.
     * A null {@code initialValue} starts the spinner at {@code min}.
     *
     * @param parent       the parent component, or null for an ownerless dialog
     * @param title        the dialog title
     * @param message      the message to show (may contain {@code <html>…</html>}), or null (no label)
     * @param initialValue the initial value, or null to start at {@code min}
     * @param min          the minimum (inclusive)
     * @param max          the maximum (inclusive)
     * @return the submitted value, or empty if the dialog was dismissed
     * @throws IllegalArgumentException if {@code min > max} (thrown before any dialog is built)
     */
    public static Optional<Double> inputDouble(Component parent, String title, String message, Double initialValue, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not be greater than max (" + max + ")");
        }
        SpinnerNumberModel model = new SpinnerNumberModel(initialValue != null ? initialValue : min, min, max, 1.0);
        JSpinner spinner = new JSpinner(model);
        Button ok = Button.OK.asDefault();
        JIPipeDesktopDialog dialog = newInputDialog(parent, title, message, spinner, ok);
        dialog.showAndWait();
        if (!okChosen(dialog, ok)) {
            return Optional.empty();
        }
        return Optional.of(model.getNumber().doubleValue());
    }

    /**
     * Shows a single-line text input dialog whose submitted value must be
     * free: {@code exists} tests whether a submitted value is already taken
     * (e.g. used by another element). Submitting a taken value keeps the
     * dialog open for editing: an inline error label below the field shows
     * {@code errorTemplate} with {@code {value}} replaced by the submitted
     * text, and the field content is reselected.
     * <p>
     * Because a taken submission cannot keep the original dialog open (an OK
     * click always hides the dialog and records its result), the facade
     * re-presents a fresh dialog each round, carrying over the submitted
     * text (reselected) and the error label; the loop ends on a free value
     * or a dismissal. Each round is one modal {@code showAndWait()}
     * round-trip, and - like every facade method - the whole loop runs on
     * the event dispatch thread; the facade does not marshal anything.
     * <p>
     * Same empty-submit rule as {@link #inputText} (spec §4.2): submitting
     * empty or whitespace-only text counts as dismiss.
     *
     * @param parent        the parent component, or null for an ownerless dialog
     * @param title         the dialog title
     * @param message       the message to show (may contain {@code <html>…</html>}), or null (no label)
     * @param initialValue  the initial field text, or null for an empty field
     * @param exists        whether a submitted value is already taken
     * @param errorTemplate the error text shown on a taken submission ({@code {value}} is replaced by the submission), or null for no text
     * @return the submitted free value, or empty if the dialog was dismissed or the submission was empty/whitespace-only
     */
    public static Optional<String> inputUniqueText(Component parent, String title, String message,
            String initialValue, Predicate<String> exists, String errorTemplate) {
        String template = errorTemplate != null ? errorTemplate : "";
        // State carried into the next presentation round after a taken
        // submission: the text to re-present (selected) and the error to show.
        String lastValue = initialValue != null ? initialValue : "";
        String error = null;
        // Default-flagged OK, shared by every re-presentation round (a fresh
        // copy per round would be equivalent; the instance only matters for
        // the okChosen identity comparison of its own round).
        Button ok = Button.OK.asDefault();
        while (true) {
            JTextField field = new JTextField(lastValue, 24);
            field.selectAll();
            JLabel errorLabel = new JLabel(error != null ? error : "");
            JPanel content = new JPanel(new BorderLayout(0, 4));
            content.add(field, BorderLayout.NORTH);
            content.add(errorLabel, BorderLayout.SOUTH);
            JIPipeDesktopDialog dialog = newInputDialog(parent, title, message, content, ok);
            // Enter inside the field submits the OK action (spec §4.2): the
            // same wiring as inputText. Index 0 is the OK button: it is the
            // only caller-provided button (the implicit Cancel is appended
            // last).
            JButton okButton = dialog.getActionButtons().get(0);
            field.addActionListener(e -> okButton.doClick());
            dialog.showAndWait();
            if (!okChosen(dialog, ok)) {
                return Optional.empty();
            }
            String submitted = field.getText();
            if (submitted.isBlank()) {
                // Empty-submit rule (spec §4.2): an empty/whitespace-only
                // submission counts as dismiss, not as an empty-string value.
                return Optional.empty();
            }
            if (!exists.test(submitted)) {
                return Optional.of(submitted);
            }
            // Taken: show the substituted error and re-present for editing.
            // A fresh dialog also starts with an empty result, so a round
            // that ends without a decision (window close) is a dismissal -
            // never the previous round's OK.
            error = template.replace("{value}", submitted);
            lastValue = submitted;
        }
    }

    /**
     * Shows the color chooser dialog ({@link JIPipeDesktopColorChooserDialog},
     * #1339) and returns the chosen color. Pure delegation: the chooser's
     * own contract (null on cancel) maps to {@link Optional#empty()}.
     *
     * @param parent    the parent component, or null for an ownerless dialog
     * @param title     the dialog title
     * @param initial   the initial color, or null for the chooser's default
     * @param withAlpha whether the alpha rail and checkerboard are shown
     * @return the chosen color, or empty if the dialog was cancelled
     */
    public static Optional<Color> chooseColor(Component parent, String title, Color initial, boolean withAlpha) {
        return Optional.ofNullable(JIPipeDesktopColorChooserDialog.show(parent, title, initial, withAlpha));
    }

    /**
     * Creates a builder for a fully customized dialog - the escape hatch for
     * dialogs that need arbitrary content, button sets, or sizes beyond the
     * facade's canned methods (spec §4.4).
     *
     * @param parent the parent component, or null for an ownerless dialog
     * @param title  the dialog title
     * @return a new builder
     */
    public static Builder builder(Component parent, String title) {
        return new Builder(parent, title);
    }

    /**
     * Builder for a fully customized {@link JIPipeDesktopDialog} (the escape
     * hatch of the modern dialog API, #1341, spec §4.4).
     * <p>
     * Button rules (spec §4.4): {@link #buttons(Button...)} replaces the
     * default set (OK) with the given buttons, in order;
     * {@link #cancelable(boolean) cancelable(false)} suppresses the Cancel
     * button entirely; otherwise (the default {@code cancelable == true}) an
     * implicit {@link Button#CANCEL} is appended last unless the effective
     * set already contains one. The append logic lives in the dialog chrome
     * - the Builder never duplicates it.
     * <p>
     * {@link #build()} only constructs the dialog; {@link #show()} builds
     * and presents it via the same presenter seam as the facade methods and
     * returns the chosen button (empty on dismissal).
     */
    public static final class Builder {

        private final Component parent;
        private final String title;
        private String message;
        private JComponent content;
        private Button[] buttons;
        private boolean cancelable = true;
        private Dimension size;

        private Builder(Component parent, String title) {
            this.parent = parent;
            this.title = title;
        }

        /**
         * Sets the message shown above the dialog content.
         *
         * @param message the message (may contain {@code <html>…</html>}), or null (no text)
         * @return this builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the content shown in the center of the dialog, below the
         * message.
         *
         * @param content the content component, or null for no content
         * @return this builder
         */
        public Builder content(JComponent content) {
            this.content = content;
            return this;
        }

        /**
         * Replaces the default button set (OK) with the given buttons, in
         * display order.
         *
         * @param buttons the action buttons, left to right
         * @return this builder
         */
        public Builder buttons(Button... buttons) {
            this.buttons = buttons;
            return this;
        }

        /**
         * Configures whether a Cancel button is offered. With the default
         * {@code true}, the dialog chrome appends the implicit
         * {@link Button#CANCEL} unless the button set already contains one;
         * {@code false} suppresses the Cancel button entirely.
         *
         * @param cancelable whether the dialog can be cancelled
         * @return this builder
         */
        public Builder cancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        /**
         * Sets the dialog size.
         *
         * @param width  the dialog width
         * @param height the dialog height
         * @return this builder
         */
        public Builder size(int width, int height) {
            this.size = new Dimension(width, height);
            return this;
        }

        /**
         * Builds the dialog without presenting it.
         * <p>
         * Package-private: the built {@link JIPipeDesktopDialog} is
         * package-private, so a {@code public} method would leak a
         * non-exportable type. All uses are same-package {@link #show()}
         * chains (which stays public).
         *
         * @return the configured dialog
         */
        JIPipeDesktopDialog build() {
            JIPipeDesktopDialog dialog = newDialog(parent, title);
            dialog.addMessage(normalizeMessage(message));
            if (content != null) {
                dialog.addContent(content);
            }
            dialog.setCancelable(cancelable);
            // The chrome appends the implicit Cancel (when cancelable and not
            // already present) - the Builder never duplicates that logic.
            dialog.setButtons(buttons != null ? buttons : new Button[]{Button.OK});
            if (size != null) {
                dialog.setSize(size);
            }
            return dialog;
        }

        /**
         * Builds the dialog (see {@link #build()}) and presents it via the
         * same presenter seam as the facade methods.
         *
         * @return the chosen button, or empty if the dialog was dismissed
         */
        public Optional<Button> show() {
            JIPipeDesktopDialog dialog = build();
            dialog.showAndWait();
            return dialog.getResult();
        }
    }

    /**
     * Resolves the owner window from the given parent component, null-safe
     * (same pattern as {@code JIPipeDesktopColorChooserDialog.show}).
     */
    private static Window resolveOwner(Component parent) {
        if (parent instanceof Window window) {
            return window;
        }
        return parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
    }

    /**
     * Creates a new dialog owned by the given parent component's window.
     */
    private static JIPipeDesktopDialog newDialog(Component parent, String title) {
        return new JIPipeDesktopDialog(resolveOwner(parent), title);
    }

    /**
     * A null message renders as an empty label (spec §8).
     */
    private static String normalizeMessage(String message) {
        return message != null ? message : "";
    }

    /**
     * The stack-trace content of an {@link #error} dialog: a bounded,
     * scrollable, read-only monospaced text area below the message.
     */
    private static JComponent tracePane(Throwable cause) {
        StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));
        JTextArea area = new JTextArea(writer.toString());
        area.setEditable(false);
        area.setLineWrap(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
        // Size to the trace's actual content: width capped (long lines scroll
        // inside the box - a 1000px-wide dialog for one long stack frame is
        // worse than a scrollbar), height capped at 150px (short traces must
        // not leave a half-empty gray box; found in the #1341 UX review).
        Dimension preferred = area.getPreferredSize();
        area.setPreferredSize(new Dimension(Math.min(Math.max(preferred.width, 420), 680),
                Math.min(preferred.height + 10, 150)));
        // A delimited secondary "code" region: the trace sits on the window
        // background inside a thin theme border, clearly separated from the
        // message (which lives on the island panel's surface).
        area.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        JScrollPane pane = new JScrollPane(area,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeUtils.getCurrentStyle().getBorderColor()),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        return pane;
    }

    /**
     * Creates an input dialog: message on top, the given content in the
     * center, cancelable with a default-flagged OK button (the implicit
     * Cancel is appended by the dialog chrome - always the
     * {@link Button#CANCEL} constant, never a copy). The {@code ok} instance
     * must be reused for the caller's result comparison: the chrome records
     * the exact instance it was given ({@code asDefault()} returns a copy -
     * comparing against the {@link Button#OK} constant would never match).
     */
    private static JIPipeDesktopDialog newInputDialog(Component parent, String title, String message, JComponent content, Button ok) {
        JIPipeDesktopDialog dialog = newDialog(parent, title);
        dialog.addMessage(normalizeMessage(message));
        dialog.addContent(content);
        dialog.setCancelable(true);
        dialog.setButtons(ok);
        return dialog;
    }

    /**
     * Whether the OK button of an input dialog was chosen (by identity: the
     * dialog preserves the exact instance, so a Cancel click or a dismissal -
     * which leaves the result empty - is not an OK choice). The compared
     * {@code ok} must be the default-flagged instance the dialog was given
     * (see {@link #newInputDialog}).
     */
    private static boolean okChosen(JIPipeDesktopDialog dialog, Button ok) {
        return dialog.getResult().orElse(null) == ok;
    }
}
