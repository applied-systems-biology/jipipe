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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.utils.StringUtils;
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
public class JIPipeDesktopFancyReadOnlyTextField extends JPanel {

    private final JXTextField textField = new JXTextField();

    public JIPipeDesktopFancyReadOnlyTextField(String text, boolean monospace) {
        setLayout(new BorderLayout(4, 0));
        setOpaque(true);
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());

        textField.setText(text);
        textField.setEditable(false);
        textField.setOpaque(false);
        textField.setBorder(null);
        if(monospace) {
            textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        }
        add(textField, BorderLayout.CENTER);

        {
            JButton clearButton = new JButton(UIUtils.getIconFromResources("actions/copy.png"));
            clearButton.setOpaque(false);
            clearButton.setToolTipText("Copy to clipboard");
            clearButton.addActionListener(e -> UIUtils.copyToClipboard(StringUtils.nullToEmpty(textField.getText())));
            UIUtils.makeButtonFlat25x25(clearButton);
            clearButton.setBorder(null);
            add(clearButton, BorderLayout.EAST);
        }
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public JXTextField getTextField() {
        return textField;
    }

    /**
     * Adds a listener for when the search text changes
     *
     * @param listener the listener
     */
    public void addActionListener(ActionListener listener) {
        textField.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
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
