/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.utils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

public class OKCancelDialog extends JDialog {
    private final String title;
    private final Component editor;
    private final String okLabel;
    private boolean cancelled = true;

    public OKCancelDialog(Component parent, String title, Component editor, String okLabel) {
        super(SwingUtilities.getWindowAncestor(parent));
        this.title = title;
        this.editor = editor;
        this.okLabel = okLabel;
        initialize();
    }

    private void initialize() {
        setTitle(title);
        setModal(true);
        setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        UIUtils.addEscapeListener(this);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(editor, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            cancelled = true;
            setVisible(false);
        });

        JButton okButton = new JButton(okLabel, UIUtils.getIconFromResources("actions/checkmark.png"));
        okButton.addActionListener(e -> {
            cancelled = false;
            setVisible(false);
        });

        JPanel buttonPanel = UIUtils.boxHorizontal(Box.createHorizontalGlue(), cancelButton, okButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public static boolean showDialog(Component parent, String title, Component editor, String okLabel, Dimension size) {
        OKCancelDialog dialog = new OKCancelDialog(parent, title, editor, okLabel);
        dialog.pack();
        dialog.setSize(size);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return !dialog.isCancelled();
    }
}
