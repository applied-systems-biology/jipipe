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
 */

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A {@link JXTextField} with some fancy additions
 */
public class FancyPasswordField extends JPanel {

    private final JPasswordField textField = new JPasswordField();

    /**
     * Creates a new instance
     *
     * @param icon Optional icon displayed to the left
     */
    public FancyPasswordField(JLabel icon) {
        setLayout(new BorderLayout(4, 0));
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(BorderFactory.createEtchedBorder());

        textField.setBorder(null);
        add(textField, BorderLayout.CENTER);

        JButton clearButton = new JButton(UIUtils.getIconFromResources("actions/edit-clear.png"));
        clearButton.setOpaque(false);
        clearButton.setToolTipText("Clear");
        clearButton.addActionListener(e -> setText(""));
        UIUtils.makeFlat25x25(clearButton);
        clearButton.setBorder(null);
        add(clearButton, BorderLayout.EAST);

        if (icon != null) {
            add(icon, BorderLayout.WEST);
        }

    }

    public String getText() {
        return new String(textField.getPassword());
    }

    public void setText(String text) {
        textField.setText(text);
    }


    /**
     * Adds a listener for when the search text changes
     *
     * @param listener the listener
     */
    public void addActionListener(ActionListener listener) {
        textField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                listener.actionPerformed(new ActionEvent(this, 1, "search-text-changed"));
            }
        });
    }

    public void styleText(boolean monospace, boolean italic, boolean bold) {
        int style = Font.PLAIN;
        if (italic)
            style |= Font.ITALIC;
        if (bold)
            style |= Font.BOLD;
        textField.setFont(new Font(monospace ? Font.MONOSPACED : Font.DIALOG, style, 12));
    }
}
