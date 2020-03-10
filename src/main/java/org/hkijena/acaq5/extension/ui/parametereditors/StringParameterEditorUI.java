package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.jdesktop.swingx.JXTextArea;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;

public class StringParameterEditorUI extends ACAQParameterEditorUI {

    public StringParameterEditorUI(ACAQParameterAccess parameterAccess) {
        super(parameterAccess);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        StringParameterSettings settings = getParameterAccess().getAnnotationOfType(StringParameterSettings.class);
        if(settings != null && settings.multiline()) {
            JTextArea textArea = new JTextArea("" + getParameterAccess().get());
            textArea.setBorder(BorderFactory.createEtchedBorder());
            textArea.getDocument().addDocumentListener(new DocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    getParameterAccess().set(textArea.getText());
                }
            });
            add(textArea, BorderLayout.CENTER);
        }
        else {
            JTextField textField = new JTextField("" + getParameterAccess().get());
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
