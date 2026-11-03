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

import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * The modern dialog API (work item #1341): a static facade over
 * {@link JIPipeDesktopDialog} in which the classic {@code JOptionPane}
 * confusion between "the user closed the window" (CLOSED_OPTION) and a real
 * choice is unrepresentable. Every dismissal — the Cancel button, the window
 * close button, or Escape — funnels into the dialog's single
 * {@link JIPipeDesktopDialog#dismiss()} path and maps to
 * {@code Optional.empty()} or {@code false} by construction.
 * <p>
 * All methods are application-modal and must be called on the
 * <em>event dispatch thread</em> (the same precondition as today's
 * {@code JOptionPane} call sites). The facade does not marshal calls to the
 * EDT itself; doing so would be redundant for the existing call sites and
 * would change the threading semantics of the migration.
 * <p>
 * This class currently offers the notify/decide/choose methods
 * ({@link #message}, {@link #error}, {@link #confirm}, {@link #choose});
 * the typed input methods and the {@code Builder} escape hatch follow in
 * later tasks of #1341.
 */
public final class JIPipeDesktopDialogs {

    /**
     * Preferred dimensions of the scrollable stack-trace area in
     * {@link #error} dialogs: the width matches the wrapped message label,
     * and the height is bounded so long traces scroll instead of growing.
     */
    private static final Dimension ERROR_TRACE_PREFERRED_SIZE = new Dimension(420, 150);

    private JIPipeDesktopDialogs() {
    }

    /**
     * Re-exposes the dialog-level presenter seam for facade tests: installs
     * the {@link DialogPresenter} used by all facade methods (delegates to
     * {@link JIPipeDesktopDialog#setPresenter(DialogPresenter)}).
     *
     * @param p the presenter to use
     */
    public static void setPresenter(DialogPresenter p) {
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
        dialog.addMessage(normalizeMessage(message));
        dialog.setButtons(Button.OK);
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
        dialog.addMessage(normalizeMessage(message));
        if (cause != null) {
            dialog.addContent(tracePane(cause));
        }
        dialog.setButtons(Button.OK);
        dialog.showAndWait();
    }

    /**
     * Shows a Yes/No confirmation dialog.
     * <p>
     * Truth table (identical to the former {@code == JOptionPane.YES_OPTION}
     * idiom): Yes → {@code true}; No, Cancel, window close, or Escape →
     * {@code false}. There is no way to represent "dismissed" separately
     * from "No" — by design.
     *
     * @param parent  the parent component, or null for an ownerless dialog
     * @param title   the dialog title
     * @param message the question (may contain {@code <html>…</html>}), or null (no label)
     * @return whether the user confirmed
     */
    public static boolean confirm(Component parent, String title, String message) {
        JIPipeDesktopDialog dialog = newDialog(parent, title);
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
    public static Optional<Button> choose(Component parent, String title, String message, Button... options) {
        if (options.length == 0) {
            throw new IllegalArgumentException("choose() requires at least one option");
        }
        JIPipeDesktopDialog dialog = newDialog(parent, title);
        dialog.addMessage(normalizeMessage(message));
        dialog.setCancelable(true);
        dialog.setButtons(options);
        dialog.showAndWait();
        return dialog.getResult();
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
        area.setPreferredSize(ERROR_TRACE_PREFERRED_SIZE);
        JScrollPane pane = new JScrollPane(area,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setBorder(BorderFactory.createEmptyBorder());
        return pane;
    }
}
