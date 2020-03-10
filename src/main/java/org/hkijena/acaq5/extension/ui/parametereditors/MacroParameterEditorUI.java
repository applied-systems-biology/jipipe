package org.hkijena.acaq5.extension.ui.parametereditors;

import ij.plugin.frame.Editor;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extension.api.macro.MacroCode;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.jdesktop.swingx.JXTextArea;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Font;

public class MacroParameterEditorUI extends ACAQParameterEditorUI {

    public MacroParameterEditorUI(ACAQParameterAccess parameterAccess) {
        super(parameterAccess);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MacroCode code = getParameterAccess().get();
        JTextArea textArea = new JTextArea("" + code.getCode());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBorder(BorderFactory.createEtchedBorder());
        textArea.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                code.setCode(textArea.getText());
                getParameterAccess().set(code);
            }
        });
        add(textArea, BorderLayout.CENTER);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
