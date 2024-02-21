package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class ReadonlyCopyableTextField extends JPanel {

    private final JTextField textField = new JTextField();
    private final JButton copyButton;

    public ReadonlyCopyableTextField(String text, boolean monospace) {
        setLayout(new BorderLayout(4, 0));
        setOpaque(true);
        setBackground(UIManager.getColor("TextField.background"));
        setBorder(UIUtils.createControlBorder());

        add(textField, BorderLayout.CENTER);
        textField.setBorder(null);
        textField.setText(text);
        textField.setEditable(false);
        textField.setBackground(UIManager.getColor("TextField.background"));
        if (monospace) {
            textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        }

        copyButton = new JButton(UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyButton.setToolTipText("Copy current value");
        copyButton.addActionListener(e -> copyCurrentValue());
        UIUtils.makeFlat25x25(copyButton);
        copyButton.setBorder(null);
        add(copyButton, BorderLayout.EAST);
    }

    private void copyCurrentValue() {
        StringSelection selection = new StringSelection(textField.getText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        copyButton.setIcon(UIUtils.getIconFromResources("emblems/checkmark.png"));
        Timer timer = new Timer(500, e -> {
            copyButton.setIcon(UIUtils.getIconFromResources("actions/edit-copy.png"));
        });
        timer.setRepeats(false);
        timer.start();
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public JTextField getTextField() {
        return textField;
    }
}
