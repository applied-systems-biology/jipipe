package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

public class StringParameterEditorUI extends ACAQParameterEditorUI {

    public StringParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initialize();
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
            textArea.getDocument().addDocumentListener(new DocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    getParameterAccess().set(textArea.getText());
                }
            });
            add(textArea, BorderLayout.CENTER);
        } else {
            JTextField textField = new JTextField(stringValue);
            textField.getDocument().addDocumentListener(new DocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    getParameterAccess().set(textField.getText());
                }
            });
            add(textField, BorderLayout.CENTER);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
