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

import org.hkijena.jipipe.desktop.commons.theme.ui.JIPipeDesktopRoundedButtonUI;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The shared chrome for all dialogs of the modern dialog API (#1341).
 * <p>
 * Layout: a word-wrapped message at the top, arbitrary content in the center,
 * and a right-aligned row of plain {@link JButton}s at the bottom. The dialog
 * is application-modal and closes (window button) or is dismissed (Escape)
 * with an empty result; clicking a button records the corresponding
 * {@link Button} descriptor as the result (identity is preserved, so the
 * caller can compare against the instance it passed).
 * <p>
 * Showing is delegated to a {@link DialogPresenter}, which tests replace to
 * construct and drive dialogs without ever showing them.
 */
class JIPipeDesktopDialog extends JDialog {

    /**
     * The presenter used by {@link #showAndWait()}; defaults to modal.
     */
    private static DialogPresenter presenter = new ModalPresenter();

    private final Window owner;
    private final JLabel messageLabel;
    private final JLabel messageIconLabel = new JLabel();
    private final JPanel bodyPanel = new JPanel(new BorderLayout(0, 12));
    private final JPanel buttonPanel = new JPanel(new BorderLayout());
    private final List<JButton> actionButtons = new ArrayList<>();
    private final List<Button> buttonDescriptors = new ArrayList<>();
    private JComponent content;
    private boolean cancelable;
    private boolean explicitSize;
    private boolean contentFocusRedirectArmed;
    private boolean contentFocusRequested;
    private Button result;

    /**
     * The narrowest dialog the pack path may produce. Empty or very short
     * messages would otherwise collapse the window to the button row's width
     * (reported as a pill-shaped dialog during #1341 testing).
     */
    private static final int MINIMUM_WIDTH = 320;

    /**
     * Creates a new desktop dialog with the given title.
     *
     * @param owner the owner window, or null for an ownerless dialog
     * @param title the dialog title
     */
    JIPipeDesktopDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.owner = owner;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getJIPipeIcon128());
        if (owner != null) {
            setLocationRelativeTo(owner);
        }

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // The message area: optional leading severity icon + word-wrapped text
        messageLabel = new JLabel(" ");
        messageLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        messageIconLabel.setVisible(false);
        messageIconLabel.setVerticalAlignment(SwingConstants.TOP);
        JPanel messageArea = new JPanel(new BorderLayout(12, 0));
        messageArea.setOpaque(false);
        messageArea.add(messageIconLabel, BorderLayout.WEST);
        messageArea.add(messageLabel, BorderLayout.CENTER);
        messageLabel.setVerticalAlignment(SwingConstants.TOP);

        // The body (message + optional content) is added to the content pane
        // at presentation time (applyDialogSize): it is wrapped in a rounded
        // island panel under the modern theme only when the dialog carries
        // content (a trace, an input field, a custom component) - for a bare
        // message the island would be redundant decoration.
        bodyPanel.setOpaque(false);
        bodyPanel.add(messageArea, BorderLayout.NORTH);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        // Escape funnels into dismiss() (empty result). Bound via the
        // input/action maps (same key stroke and scope as
        // UIUtils.addEscapeListener) so the binding is inspectable and
        // clearable.
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dismissDialog");
        getRootPane().getActionMap().put("dismissDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dismiss();
            }
        });
    }

    /**
     * Installs the presenter used by {@link #showAndWait()}.
     *
     * @param p the presenter to use
     */
    static void setPresenter(DialogPresenter p) {
        presenter = p;
    }

    /**
     * @return the presenter used by {@link #showAndWait()}
     */
    static DialogPresenter getPresenter() {
        return presenter;
    }

    /**
     * Sets the message displayed above the dialog content. Plain messages are
     * wrapped in a word-wrapping HTML container, with the HTML-significant
     * characters ({@code &}, {@code <}, {@code >}) escaped so they render
     * verbatim (the classic option pane showed plain strings verbatim); messages
     * that already are a full {@code <html>} document pass through verbatim
     * (the spec's {@code <html>} pass-through rule - nesting them in the
     * wrapper div would mangle their rendering, and their markup is
     * intentional).
     *
     * @param message the message text, e.g. {@code "Hello <b>world</b>"} or
     *                {@code "<html><b>Hello</b></html>"}
     */
    void addMessage(String message) {
        String trimmed = message != null ? message.trim() : "";
        if (trimmed.regionMatches(true, 0, "<html", 0, 5)) {
            messageLabel.setText(message);
            return;
        }
        // The classic option pane rendered multi-line strings on separate
        // lines, so plain messages turn their line breaks into <br> (after
        // escaping - <br> must survive as markup).
        String plain = escapeHtml(message)
                .replace("\r\n", "<br>")
                .replace("\r", "<br>")
                .replace("\n", "<br>");
        messageLabel.setText("<html><div style=\"width: 400px\"> " + plain + " </div></html>");
    }

    /**
     * Escapes the HTML-significant characters of a plain message so they
     * render verbatim inside the word-wrapping HTML container. The ampersand
     * must be replaced first - escaping it after the angle brackets would
     * double-escape the entities produced for them.
     */
    private static String escapeHtml(String message) {
        if (message == null) {
            return "";
        }
        return message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Adds content to the center of the dialog, below the message.
     *
     * @param content the content component to show
     */
    void addContent(JComponent content) {
        this.content = content;
        bodyPanel.add(content, BorderLayout.CENTER);
        // The body must be part of the content pane from here on (tests build
        // and inspect the dialog before it is presented); the presentation
        // step ({@link #applyDialogSize()}) decides the island wrap.
        getContentPane().add(bodyPanel, BorderLayout.CENTER);
    }

    /**
     * Assembles the body into the content pane right before sizing: the
     * island wrap is only applied when the dialog carries content, so a
     * message-only dialog stays plain (the user rejected the redundant
     * island for those during the #1341 UX review).
     */
    private void addBodyToContentPane() {
        JPanel contentPane = (JPanel) getContentPane();
        JComponent body = content != null
                ? UIUtils.wrapInBackgroundIslandPanelIfNeeded(bodyPanel)
                : bodyPanel;
        contentPane.remove(bodyPanel);
        contentPane.add(body, BorderLayout.CENTER);
    }

    /**
     * Sets the leading severity icon shown next to the message (modern dialog
     * convention: {@code error()} shows an error icon, {@code message()} an
     * info icon, {@code confirm()} a question icon; input and choose dialogs
     * stay iconless). {@code null} hides the icon.
     *
     * @param icon the icon, or null for no icon
     */
    void setMessageIcon(Icon icon) {
        messageIconLabel.setIcon(icon);
        messageIconLabel.setVisible(icon != null);
    }

    /**
     * Installs the action buttons at the bottom of the dialog. If the dialog
     * is {@linkplain #setCancelable(boolean) cancelable} and no
     * {@link Button#CANCEL} is given, a Cancel button is added.
     * <p>
     * Button order contract (the #1341 UX review): the Cancel button is
     * always ordered left-most and the default-flagged action always
     * right-most; the remaining buttons keep their given order in between.
     * <p>
     * The first button flagged with {@link Button#asDefault()} is registered
     * as the root pane's default button, so Enter activates it while focus
     * is not in a component that consumes Enter (e.g. a text field). If no
     * button is flagged, any previously registered default button is cleared.
     * A default-flagged {@link Button#CANCEL} is registered for Enter but
     * never receives the primary fill: Cancel is a dismissal, not an
     * affirmation.
     *
     * @param buttons the action buttons in the caller's preferred order
     */
    void setButtons(Button... buttons) {
        buttonPanel.removeAll();
        actionButtons.clear();
        buttonDescriptors.clear();

        List<Button> effective = new ArrayList<>(List.of(buttons));
        if (cancelable && effective.stream().noneMatch(b -> b == Button.CANCEL)) {
            effective.add(Button.CANCEL);
        }
        // The order contract: Cancel first, the default-flagged action last,
        // everything else in between. List.sort is stable, so every class
        // keeps its relative order.
        effective.sort(Comparator.comparingInt((Button b) -> b == Button.CANCEL ? 0 : b.isDefault() ? 2 : 1));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton defaultButton = null;
        for (Button b : effective) {
            JButton jButton = new JButton(b.getLabel(), b.getIcon());
            jButton.addActionListener(e -> onActionClicked(b));
            if (defaultButton == null && b.isDefault()) {
                defaultButton = jButton;
                // Cancel never renders as the primary action, even when it is
                // the default-flagged button (the #1341 UX review): Cancel is
                // a dismissal, not an affirmation. The label check covers a
                // default-flagged copy of CANCEL too, which loses the
                // constant's identity via asDefault().
                if (!Button.CANCEL.getLabel().equals(b.getLabel())) {
                    styleAsPrimary(jButton);
                }
            }
            buttonDescriptors.add(b);
            actionButtons.add(jButton);
            row.add(jButton);
        }
        // Equal-width action buttons (uniform dialog footers, per the #1341
        // UX review): every button takes the width of the widest one (after
        // the primary's fill styling, which changes its outline padding),
        // floored so short labels do not produce cramped buttons.
        int width = 42;
        for (JButton jButton : actionButtons) {
            width = Math.max(width, jButton.getPreferredSize().width);
        }
        for (JButton jButton : actionButtons) {
            Dimension d = new Dimension(width, UIUtils.DIALOG_BUTTON_HEIGHT);
            jButton.setPreferredSize(d);
            jButton.setMinimumSize(d);
        }
        buttonPanel.add(row, BorderLayout.CENTER);
        buttonPanel.revalidate();
        buttonPanel.repaint();
        getRootPane().setDefaultButton(defaultButton);
    }

    /**
     * Styles the affirmative (default-flagged) button as the dialog's filled
     * primary action (the Fluent-style "Done"): a rounded button filled with
     * the theme's accent color - the same emphasized fill the app's primary
     * parameter actions use - while the remaining actions stay standard theme
     * buttons.
     */
    private void styleAsPrimary(JButton button) {
        Color fill = ThemeUtils.getCurrentStyle().getParameterActionHighlightedBackground();
        button.setOpaque(true);
        button.setContentAreaFilled(false);
        button.setBackground(fill);
        button.setForeground(isLight(fill) ? Color.BLACK : Color.WHITE);
        button.setUI(new JIPipeDesktopRoundedButtonUI(5, fill, fill.darker()));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(1, 1, 1, 1),
                BorderFactory.createCompoundBorder(
                        new RoundedLineBorder(fill, 1, 5),
                        BorderFactory.createEmptyBorder(1, 12, 1, 12))));
    }

    /**
     * @return whether the color is light enough to carry dark text
     */
    private static boolean isLight(Color color) {
        return (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0 > 0.6;
    }

    /**
     * Configures whether a Cancel button is offered for this dialog.
     * Should be called before {@link #setButtons(Button...)}; the implicit
     * Cancel button is appended when the buttons are (re-)installed.
     *
     * @param cancelable whether the dialog can be cancelled
     * @see #setButtons(Button...)
     */
    void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    /**
     * @return the button the user selected, or empty until a button decides
     * (and always empty after the dialog was dismissed)
     */
    Optional<Button> getResult() {
        return Optional.ofNullable(result);
    }

    /**
     * Sizes the dialog for presentation: dialogs without an explicitly set
     * size are packed to their content (and grown to {@link #MINIMUM_WIDTH}
     * when the content is too short to be readable), and every dialog is then
     * centered on its owner. Called by {@link #showAndWait()} right before
     * presenting - the content and buttons are only fully assembled at that
     * point, and sizing in the constructor would compute from an empty
     * content pane (every dialog opened as a 1x56 pixel sliver before this
     * existed; found during #1341 testing).
     */
    void applyDialogSize() {
        addBodyToContentPane();
        if (!explicitSize) {
            pack();
            if (getWidth() < MINIMUM_WIDTH) {
                setSize(MINIMUM_WIDTH, getHeight());
            }
        }
        setLocationRelativeTo(owner);
    }

    @Override
    public void setSize(int width, int height) {
        explicitSize = true;
        super.setSize(width, height);
    }

    @Override
    public void setSize(Dimension d) {
        explicitSize = true;
        super.setSize(d);
    }

    /**
     * Shows the dialog and blocks until the user has decided. When a content
     * component was set, the initial focus is redirected to it: the button
     * panel is added before the content (container order), so without the
     * redirect, input dialogs would open with the OK button focused instead
     * of their text field. Without content (message/confirm/choose dialogs),
     * nothing is done - the default button receives focus naturally.
     * <p>
     * The redirect is armed at {@code windowOpened} (and deferred one more
     * event-loop hop): a plain {@code invokeLater} queued before presenting
     * could run before the dialog is realized when {@code showAndWait()} is
     * called off the event dispatch thread, and a focus request on a
     * not-yet-showing component is silently dropped.
     *
     * @return the dialog, with the result set by {@link #getResult()}
     * @see #getResult()
     */
    JIPipeDesktopDialog showAndWait() {
        applyDialogSize();
        if (content != null && !contentFocusRedirectArmed) {
            contentFocusRedirectArmed = true;
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    SwingUtilities.invokeLater(JIPipeDesktopDialog.this::requestInitialContentFocus);
                }
            });
        }
        return presenter.show(this);
    }

    /**
     * Redirects the initial focus to the content component. Records the
     * attempt (test seam): whether the window manager grants the request is
     * environment-dependent, but the redirect itself must always happen.
     */
    void requestInitialContentFocus() {
        contentFocusRequested = true;
        content.requestFocusInWindow();
    }

    /**
     * @return whether {@link #requestInitialContentFocus()} was invoked
     * (test seam, see {@link #showAndWait()})
     */
    boolean isContentFocusRequested() {
        return contentFocusRequested;
    }

    /**
     * Dismisses the dialog: records an empty result and hides/disposes it.
     * <p>
     * Package-private: used by the Escape binding and by the implicit Cancel
     * button, never by facade code directly.
     */
    void dismiss() {
        result = null;
        setVisible(false);
        dispose();
    }

    /**
     * @return the message label, for tests
     */
    JLabel getMessageLabel() {
        return messageLabel;
    }

    /**
     * @return the action buttons in display order, for tests
     */
    /**
     * Resolves a button descriptor to the {@link JButton} installed for it.
     * Identity comparison: the facade hands its default-flagged copies to
     * {@link #setButtons(Button...)} and resolves them back this way, so the
     * Enter wiring works regardless of the button order contract.
     *
     * @param descriptor the descriptor instance that was passed to
     *                   {@link #setButtons(Button...)}
     * @return the installed button, or null when the descriptor is not present
     */
    JButton getActionButton(Button descriptor) {
        for (int i = 0; i < buttonDescriptors.size(); i++) {
            if (buttonDescriptors.get(i) == descriptor) {
                return actionButtons.get(i);
            }
        }
        return null;
    }

    List<JButton> getActionButtons() {
        return actionButtons;
    }

    /**
     * A non-cancel action was clicked: record the button and close the
     * dialog. Disposes symmetrically to {@link #dismiss()} - otherwise every
     * decided dialog would keep its native window alive until GC.
     */
    private void onActionClicked(Button b) {
        if (b == Button.CANCEL) {
            dismiss();
        } else {
            result = b;
            setVisible(false);
            dispose();
        }
    }
}
