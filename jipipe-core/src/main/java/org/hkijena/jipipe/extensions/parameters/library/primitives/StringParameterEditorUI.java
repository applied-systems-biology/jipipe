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

package org.hkijena.jipipe.extensions.parameters.library.primitives;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FancyTextField;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Objects;

/**
 * Parameter editor for {@link String}
 */
public class StringParameterEditorUI extends JIPipeParameterEditorUI {

    private JTextComponent textComponent;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public StringParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

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

        if (multiline) {
            JTextArea textArea = new JTextArea(stringValue);
            textArea.setBorder(BorderFactory.createEtchedBorder());
            textComponent = textArea;
            add(textArea, BorderLayout.CENTER);
        } else {
            JLabel iconLabel = null;
            if (!StringUtils.isNullOrEmpty(iconURL)) {
                ImageIcon imageIcon = new ImageIcon(getClass().getResource(iconURL));
                iconLabel = new JLabel(imageIcon);
            }

            FancyTextField textField = new FancyTextField(iconLabel, prompt, true);
            textComponent = textField.getTextField();
            add(textField, BorderLayout.CENTER);
        }
        if (monospaced)
            textComponent.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        textComponent.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                setParameter(textComponent.getText(), false);
            }
        });
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        Object value = getParameterAccess().get(Object.class);
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value;
        }
        if (!Objects.equals(stringValue, textComponent.getText()))
            textComponent.setText(stringValue);
    }
}
