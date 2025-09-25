/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter;
import org.hkijena.jipipe.utils.CustomEditorPane;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Objects;

/**
 * An editor for {@link JIPipeScriptParameter}
 */
public class JIPipeDesktopLargeScriptParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private CustomEditorPane textArea;

    public JIPipeDesktopLargeScriptParameterEditorUI(InitializationParameters parameters) {
        super(parameters);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeScriptParameter code = getParameter(JIPipeScriptParameter.class);
        textArea = new CustomEditorPane();
        ThemeUtils.applyThemeToCodeEditor(textArea);
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
        textArea.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
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

        JButton undoButton = new JButton("Undo", JIPipe.RESOURCES.getIcon16("actions/undo.png"));
        undoButton.addActionListener(e -> textArea.undoLastAction());
        toolBar.add(undoButton);

        JButton redoButton = new JButton("Redo", JIPipe.RESOURCES.getIcon16("actions/edit-redo.png"));
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
        JIPipeScriptParameter code = getParameter(JIPipeScriptParameter.class);
        if (!Objects.equals(textArea.getText(), code.getCode()))
            textArea.setText(code.getCode());
    }
}
