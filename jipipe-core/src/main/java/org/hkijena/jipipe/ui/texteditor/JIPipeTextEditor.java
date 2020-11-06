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

package org.hkijena.jipipe.ui.texteditor;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import java.awt.BorderLayout;

public class JIPipeTextEditor extends JIPipeWorkbenchPanel {
    private EditorPane textArea;

    /**
     * @param workbench the workbench
     */
    public JIPipeTextEditor(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
    }


    private void initialize() {
        setLayout(new BorderLayout());
        textArea = new EditorPane();
        textArea.setCodeFoldingEnabled(true);
        textArea.setTabSize(4);
        getWorkbench().getContext().inject(textArea);

        RTextScrollPane scrollPane = new RTextScrollPane(textArea, true);
        scrollPane.setFoldIndicatorEnabled(true);
        add(scrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton undoButton = new JButton("Undo", UIUtils.getIconFromResources("actions/undo.png"));
        undoButton.addActionListener(e -> textArea.undoLastAction());
        toolBar.add(undoButton);

        JButton redoButton = new JButton("Redo", UIUtils.getIconFromResources("actions/edit-redo.png"));
        redoButton.addActionListener(e -> textArea.redoLastAction());
        toolBar.add(redoButton);

        add(toolBar, BorderLayout.NORTH);
    }

    public String getMimeType() {
        return textArea.getSyntaxEditingStyle();
    }

    public void setMimeType(String mimeType) {
        textArea.setSyntaxEditingStyle(mimeType);
    }

    public void setText(String data) {
        textArea.setText(data);
    }

    public static JIPipeTextEditor openInNewTab(JIPipeWorkbench workbench, String name) {
        JIPipeTextEditor editor = new JIPipeTextEditor(workbench);
        workbench.getDocumentTabPane().addTab(name, UIUtils.getIconFromResources("actions/edit-select-text.png"),
                editor, DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        workbench.getDocumentTabPane().switchToLastTab();
        return editor;
    }
}
