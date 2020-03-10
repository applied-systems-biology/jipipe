package org.hkijena.acaq5.extension.ui.parametereditors;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.extension.api.macro.MacroCode;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;

public class MacroParameterEditorUI extends ACAQParameterEditorUI {

    public MacroParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        registerIJMacroLanguage();
        initialize();
    }

    private void registerIJMacroLanguage() {
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/ijm", "org.scijava.ui.swing.script.highliters.ImageJMacroTokenMaker");
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MacroCode code = getParameterAccess().get();
//        JTextArea textArea = new JTextArea("" + code.getCode());
//        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
//        textArea.setBorder(BorderFactory.createEtchedBorder());
//        textArea.getDocument().addDocumentListener(new DocumentChangeListener() {
//            @Override
//            public void changed(DocumentEvent documentEvent) {
//                code.setCode(textArea.getText());
//                getParameterAccess().set(code);
//            }
//        });
        EditorPane textArea = new EditorPane();
        textArea.setBorder(BorderFactory.createEtchedBorder());
        getWorkbenchUI().getContext().inject(textArea);
        textArea.setText(code.getCode());
        textArea.setSyntaxEditingStyle("text/ijm");
        textArea.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                code.setCode(textArea.getText());
                getParameterAccess().set(code);
            }
        });
        add(textArea, BorderLayout.CENTER);

//        TextEditor textEditor = new TextEditor(getWorkbenchUI().getContext());
//        textEditor.setVisible(true);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
