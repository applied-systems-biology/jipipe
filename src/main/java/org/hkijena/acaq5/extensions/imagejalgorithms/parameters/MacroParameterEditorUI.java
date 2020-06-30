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

package org.hkijena.acaq5.extensions.imagejalgorithms.parameters;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;

/**
 * Parameter editor for {@link MacroCode}
 */
public class MacroParameterEditorUI extends ACAQParameterEditorUI {

    private boolean isReloading = false;
    private EditorPane textArea;

    /**
     * @param workbench       workbench
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
