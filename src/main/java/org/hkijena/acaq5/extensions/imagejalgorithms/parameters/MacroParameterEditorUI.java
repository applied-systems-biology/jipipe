package org.hkijena.acaq5.extensions.imagejalgorithms.parameters;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.scijava.Context;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Parameter editor for {@link MacroCode}
 */
public class MacroParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isReloading = false;
    private EditorPane textArea;

    /**
     * @param workbench        workbench
     * @param parameterAccess the parameter
     */
    public MacroParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        registerIJMacroLanguage();
        initialize();
    }

    private void registerIJMacroLanguage() {
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/ijm", "org.scijava.ui.swing.script.highliters.ImageJMacroTokenMaker");
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MacroCode code = getParameter(MacroCode.class);
        textArea = new EditorPane();
        textArea.setBorder(BorderFactory.createEtchedBorder());
        getContext().inject(textArea);
        textArea.setText(code.getCode());
        textArea.setSyntaxEditingStyle("text/ijm");
        textArea.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                if (!isReloading) {
                    code.setCode(textArea.getText());
                    getParameterAccess().set(code);
                }
            }
        });
        add(textArea, BorderLayout.CENTER);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        isReloading = true;
        MacroCode code = getParameter(MacroCode.class);
        textArea.setText(code.getCode());
        isReloading = false;
    }
}
