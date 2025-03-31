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

package org.hkijena.jipipe.plugins.parameters.library.primitives;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyTextField;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Objects;

/**
 * Parameter editor for {@link String}
 */
public class StringDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private JTextComponent textComponent;

    public StringDesktopParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        boolean monospaced = false;
        boolean multiline = false;
        String iconURL = null;
        String prompt = "";
        boolean visible = true;
        StringParameterSettings settings = getParameterAccess().getAnnotationOfType(StringParameterSettings.class);
        if (settings != null) {
            monospaced = settings.monospace();
            multiline = settings.multiline();
            iconURL = settings.icon();
            prompt = settings.prompt();
            visible = settings.visible();
        }

        if (visible) {
            Object value = getParameterAccess().get(Object.class);
            String stringValue = "";
            if (value != null) {
                stringValue = "" + value;
            }

            if (multiline) {
                JTextArea textArea = new JTextArea(stringValue);
                textArea.setBorder(UIUtils.createControlBorder());
                textComponent = textArea;
                add(textArea, BorderLayout.CENTER);
            } else {
                JLabel iconLabel = null;
                if (!StringUtils.isNullOrEmpty(iconURL)) {
                    ImageIcon imageIcon = new ImageIcon(getClass().getResource(iconURL));
                    iconLabel = new JLabel(imageIcon);
                }

                JIPipeDesktopFancyTextField textField = new JIPipeDesktopFancyTextField(iconLabel, prompt, false);
                textComponent = textField.getTextField();
                add(textField, BorderLayout.CENTER);
            }
            if (monospaced)
                textComponent.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            textComponent.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    setParameter(textComponent.getText(), false);
                }
            });
        } else {
            JButton editButton = new JButton("Show/Edit", UIUtils.getIconFromResources("actions/document-edit.png"));
            editButton.addActionListener(e -> editText());
            add(editButton, BorderLayout.CENTER);
        }
    }

    private void editText() {
        boolean monospaced = false;
        boolean multiline = false;
        String iconURL = null;
        String prompt = "";
        StringParameterSettings settings = getParameterAccess().getAnnotationOfType(StringParameterSettings.class);
        if (settings != null) {
            monospaced = settings.monospace();
            multiline = settings.multiline();
            iconURL = settings.icon();
            prompt = settings.prompt();
        }

        Object value = getParameterAccess().get(Object.class);
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value;
        }

        JTextComponent textComponent;
        if (multiline) {
            JTextArea textArea = new JTextArea(stringValue);
            textArea.setBorder(UIUtils.createControlBorder());
            textComponent = textArea;
        } else {
            JLabel iconLabel = null;
            if (!StringUtils.isNullOrEmpty(iconURL)) {
                ImageIcon imageIcon = new ImageIcon(getClass().getResource(iconURL));
                iconLabel = new JLabel(imageIcon);
            }

            JIPipeDesktopFancyTextField textField = new JIPipeDesktopFancyTextField(iconLabel, prompt, true);
            textComponent = textField.getTextField();
        }
        if (monospaced)
            textComponent.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel contentPanel = new JPanel(new BorderLayout());
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getDesktopWorkbench().getWindow()));
        dialog.setContentPane(contentPanel);
        dialog.setTitle("Edit " + getParameterAccess().getName());
        dialog.setModal(true);
        UIUtils.addEscapeListener(dialog);

        contentPanel.add(new JScrollPane(textComponent), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Pick", UIUtils.getIconFromResources("actions/checkmark.png"));
        confirmButton.addActionListener(e -> {
            setParameter(textComponent.getText(), false);
            dialog.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setSize(new Dimension(500, 600));
        dialog.setLocationRelativeTo(getDesktopWorkbench().getWindow());
        dialog.setVisible(true);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (textComponent != null) {
            Object value = getParameterAccess().get(Object.class);
            String stringValue = "";
            if (value != null) {
                stringValue = "" + value;
            }
            if (!Objects.equals(stringValue, textComponent.getText()))
                textComponent.setText(stringValue);
        }
    }
}
