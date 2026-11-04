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

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
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

    private final JLabel messageLabel;
    private final JPanel buttonPanel = new JPanel(new BorderLayout());
    private final List<JButton> actionButtons = new ArrayList<>();
    private boolean cancelable;
    private Button result;

    /**
     * Creates a new desktop dialog with the given title.
     *
     * @param owner the owner window, or null for an ownerless dialog
     * @param title the dialog title
     */
    JIPipeDesktopDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getJIPipeIcon128());
        if (owner != null) {
            setLocationRelativeTo(owner);
        }

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        messageLabel = new JLabel(" ");
        messageLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        contentPane.add(messageLabel, BorderLayout.NORTH);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        // Escape funnels into dismiss() (empty result). Registered as a plain
        // action (same key stroke and scope as UIUtils.addEscapeListener) so
        // the binding is inspectable and clearable.
        getRootPane().registerKeyboardAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dismiss();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
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
     * Sets the message displayed above the dialog content. The message is
     * HTML, so it is word-wrapped.
     *
     * @param message the message text, e.g. {@code "Hello <b>world</b>"}
     */
    void addMessage(String message) {
        messageLabel.setText("<html><div style=\"width: 400px\"> " + message + " </div></html>");
    }

    /**
     * Adds content to the center of the dialog, below the message.
     *
     * @param content the content component to show
     */
    void addContent(JComponent content) {
        getContentPane().add(content, BorderLayout.CENTER);
    }

    /**
     * Installs the action buttons at the bottom of the dialog. If the dialog
     * is {@linkplain #setCancelable(boolean) cancelable} and no
     * {@link Button#CANCEL} is given, a Cancel button is appended last.
     * <p>
     * The first button flagged with {@link Button#asDefault()} is registered
     * as the root pane's default button, so Enter activates it while focus
     * is not in a component that consumes Enter (e.g. a text field). If no
     * button is flagged, any previously registered default button is cleared.
     *
     * @param buttons the action buttons, left to right
     */
    void setButtons(Button... buttons) {
        buttonPanel.removeAll();
        actionButtons.clear();

        List<Button> effective = new ArrayList<>(List.of(buttons));
        if (cancelable && effective.stream().noneMatch(b -> b == Button.CANCEL)) {
            effective.add(Button.CANCEL);
        }

        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton defaultButton = null;
        for (Button b : effective) {
            JButton jButton = new JButton(b.getLabel(), b.getIcon());
            jButton.addActionListener(e -> onActionClicked(b));
            if (defaultButton == null && b.isDefault()) {
                defaultButton = jButton;
            }
            actionButtons.add(jButton);
            row.add(jButton);
        }
        buttonPanel.add(row, BorderLayout.CENTER);
        buttonPanel.revalidate();
        buttonPanel.repaint();
        getRootPane().setDefaultButton(defaultButton);
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
     * Shows the dialog and blocks until the user has decided.
     *
     * @return the dialog, with the result set by {@link #getResult()}
     * @see #getResult()
     */
    JIPipeDesktopDialog showAndWait() {
        return presenter.show(this);
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
    List<JButton> getActionButtons() {
        return actionButtons;
    }

    /**
     * A non-cancel action was clicked: record the button and close the dialog.
     */
    private void onActionClicked(Button b) {
        if (b == Button.CANCEL) {
            dismiss();
        } else {
            result = b;
            setVisible(false);
        }
    }
}
