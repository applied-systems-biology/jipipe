package org.hkijena.acaq5.extensions.imagejalgorithms.parameters;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
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

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private EditorPane textArea;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public MacroParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        registerIJMacroLanguage();
        initialize();
    }

    private void registerIJMacroLanguage() {
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/ijm", "org.scijava.ui.swing.script.highliters.ImageJMacroTokenMaker");
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MacroCode code = getParameterAccess().get(MacroCode.class);
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
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        MacroCode code = getParameterAccess().get(MacroCode.class);
        textArea.setText(code.getCode());
        isReloading = false;
    }
}
