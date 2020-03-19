package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class StringParameterEditorUI extends ACAQParameterEditorUI {

    private JTextComponent textComponent;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    public StringParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        StringParameterSettings settings = getParameterAccess().getAnnotationOfType(StringParameterSettings.class);
        Object value = getParameterAccess().get();
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value;
        }
        if (settings != null && settings.multiline()) {
            JTextArea textArea = new JTextArea(stringValue);
            textArea.setBorder(BorderFactory.createEtchedBorder());
            textComponent = textArea;
            add(textArea, BorderLayout.CENTER);
        } else {
            JTextField textField = new JTextField(stringValue);
            textComponent = textField;
            add(textField, BorderLayout.CENTER);
        }

        textComponent.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isReloading) {
                    skipNextReload = true;
                    getParameterAccess().set(textComponent.getText());
                }
            }
        });
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        Object value = getParameterAccess().get();
        String stringValue = "";
        if (value != null) {
            stringValue = "" + value;
        }
        textComponent.setText(stringValue);
        isReloading = false;
    }
}
