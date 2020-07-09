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

package org.hkijena.jipipe.extensions.parameters.scripts;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Objects;

/**
 * Parameter editor for {@link ScriptParameter}
 */
public class ScriptParameterEditorUI extends JIPipeParameterEditorUI {

    private EditorPane textArea;
    private JLabel collapseInfoLabel;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public ScriptParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ScriptParameter code = getParameter(ScriptParameter.class);
        collapseInfoLabel = new JLabel("The code is hidden. Click the 'Collapse' button to show it",
                UIUtils.getIconFromResources("eye-slash.png"),
                JLabel.LEFT);
        collapseInfoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        textArea = new EditorPane();
        if (code.getLanguage() != null) {
            ReflectionUtils.invokeMethod(textArea, "setLanguage", code.getLanguage());
            textArea.setAutoCompletionEnabled(true);
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

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new JLabel(code.getLanguageName()));

        toolBar.add(Box.createHorizontalGlue());

        JToggleButton collapseButton = new JToggleButton("Collapse", UIUtils.getIconFromResources("eye-slash.png"));
        collapseButton.setSelected(code.isCollapsed());
        collapseButton.addActionListener(e -> toggleCollapse());
        toolBar.add(collapseButton);

        JButton openIdeButton = new JButton("IDE", UIUtils.getIconFromResources("window-new.png"));
        openIdeButton.addActionListener(e -> openIDE());
        toolBar.add(openIdeButton);

        add(toolBar, BorderLayout.NORTH);

        setBorder(BorderFactory.createEtchedBorder());
    }

    private void toggleCollapse() {
        ScriptParameter code = getParameter(ScriptParameter.class);
        code.setCollapsed(!code.isCollapsed());
        setParameter(code, true);
    }

    private void openIDE() {
        for (DocumentTabPane.DocumentTab documentTab : getWorkbench().getDocumentTabPane().getTabsContaining(LargeScriptParameterEditorUI.class)) {
            LargeScriptParameterEditorUI editorUI = (LargeScriptParameterEditorUI) documentTab.getContent();
            if (editorUI.getParameterAccess() == getParameterAccess()) {
                getWorkbench().getDocumentTabPane().switchToContent(editorUI);
                return;
            }
        }
        ScriptParameter code = getParameter(ScriptParameter.class);
        getWorkbench().getDocumentTabPane().addTab(getParameterAccess().getName() + " (" + code.getLanguageName() + ")",
                UIUtils.getIconFromResources("algorithms/dialog-xml-editor.png"),
                new LargeScriptParameterEditorUI(getWorkbench(), getParameterAccess()),
                DocumentTabPane.CloseMode.withSilentCloseButton,
                true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        ScriptParameter code = getParameter(ScriptParameter.class);
        remove(textArea);
        remove(collapseInfoLabel);
        if (code.isCollapsed()) {
            add(collapseInfoLabel, BorderLayout.CENTER);
        } else {
            add(textArea, BorderLayout.CENTER);
        }
        if (!Objects.equals(textArea.getText(), code.getCode()))
            textArea.setText(code.getCode());
        revalidate();
        repaint();
    }
}
