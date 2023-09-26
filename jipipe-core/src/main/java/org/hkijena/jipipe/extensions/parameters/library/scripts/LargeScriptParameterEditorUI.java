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

package org.hkijena.jipipe.extensions.parameters.library.scripts;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.api.scripts.ScriptParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.CustomEditorPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Objects;

/**
 * An editor for {@link ScriptParameter}
 */
public class LargeScriptParameterEditorUI extends JIPipeParameterEditorUI {

    private CustomEditorPane textArea;

    /**
     * Creates new instance
     *
     * @param workbench       the workbech
     * @param parameterAccess Parameter
     */
    public LargeScriptParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ScriptParameter code = getParameter(ScriptParameter.class);
        textArea = new CustomEditorPane();
        UIUtils.applyThemeToCodeEditor(textArea);
        textArea.setHighlightCurrentLine(false);
        textArea.setBackground(UIManager.getColor("TextArea.background"));
        textArea.setCodeFoldingEnabled(true);
        if (code.getLanguage() != null) {
            textArea.setLanguage(code.getLanguage());
            // Temporarily removed for backwards compatibility
//            textArea.setAutoCompletionEnabled(true);
        }
        textArea.setTabSize(4);
        getContext().inject(textArea);
        textArea.setText(code.getCode());
        textArea.setSyntaxEditingStyle(code.getMimeType());
        textArea.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                code.setCode(textArea.getText());
                setParameter(code, false);
            }
        });


        RTextScrollPane scrollPane = new RTextScrollPane(textArea, true);
        scrollPane.setFoldIndicatorEnabled(true);
        add(scrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new JLabel(code.getLanguageName()));

        toolBar.add(Box.createHorizontalGlue());

        JButton undoButton = new JButton("Undo", UIUtils.getIconFromResources("actions/undo.png"));
        undoButton.addActionListener(e -> textArea.undoLastAction());
        toolBar.add(undoButton);

        JButton redoButton = new JButton("Redo", UIUtils.getIconFromResources("actions/edit-redo.png"));
        redoButton.addActionListener(e -> textArea.redoLastAction());
        toolBar.add(redoButton);

        add(toolBar, BorderLayout.NORTH);
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        ScriptParameter code = getParameter(ScriptParameter.class);
        if (!Objects.equals(textArea.getText(), code.getCode()))
            textArea.setText(code.getCode());
    }
}
