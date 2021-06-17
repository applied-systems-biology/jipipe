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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.primitives.FilePathParameterSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PathEditor;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Parameter editor for {@link ScriptParameter}
 */
public class ScriptParameterEditorUI extends JIPipeParameterEditorUI {

    private EditorPane textArea;
    private JLabel collapseInfoLabel;
    private boolean isCollapsed;
    private JToggleButton externalCodeToggle;
    private Component pathEditorComponent;

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
                UIUtils.getIconFromResources("actions/eye-slash.png"),
                JLabel.LEFT);
        collapseInfoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        textArea = new EditorPane();
        UIUtils.applyThemeToCodeEditor(textArea);
        textArea.setBackground(UIManager.getColor("TextArea.background"));
        textArea.setHighlightCurrentLine(false);
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

        externalCodeToggle = new JToggleButton("External", UIUtils.getIconFromResources("actions/edit-link.png"));
        externalCodeToggle.setToolTipText("If enabled, the code is extracted from an external file.");
        externalCodeToggle.addActionListener(e -> toggleExternalCode());
        toolBar.add(externalCodeToggle);

        JToggleButton collapseButton = new JToggleButton("Collapse", UIUtils.getIconFromResources("actions/eye-slash.png"));
        collapseButton.setSelected(code.isCollapsed());
        collapseButton.addActionListener(e -> toggleCollapse());
        toolBar.add(collapseButton);

        JButton openIdeButton = new JButton("Large editor", UIUtils.getIconFromResources("actions/window_new.png"));
        openIdeButton.addActionListener(e -> openIDE());
        toolBar.add(openIdeButton);

        add(toolBar, BorderLayout.NORTH);

        setBorder(BorderFactory.createEtchedBorder());
    }

    private void toggleExternalCode() {
        ScriptParameter code = getParameter(ScriptParameter.class);
        code.getExternalScriptFile().setEnabled(!code.getExternalScriptFile().isEnabled());
        setParameter(code, true);
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
                UIUtils.getIconFromResources("actions/dialog-xml-editor.png"),
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
        if (!code.isCollapsed() || !isCollapsed) {
            remove(textArea);
            remove(collapseInfoLabel);
            if (pathEditorComponent != null)
                remove(pathEditorComponent);
            if (code.isCollapsed()) {
                add(collapseInfoLabel, BorderLayout.CENTER);
            } else {
                if (code.getExternalScriptFile().isEnabled()) {
                    JIPipeManualParameterAccess access = JIPipeManualParameterAccess.builder().setFieldClass(Path.class)
                            .setGetter(() -> code.getExternalScriptFile().getContent())
                            .setSetter((Object p) -> code.getExternalScriptFile().setContent((Path) p))
                            .setSource(new JIPipeDummyParameterCollection())
                            .setKey("external-path")
                            .addAnnotation(new FilePathParameterSettings() {
                                @Override
                                public Class<? extends Annotation> annotationType() {
                                    return FilePathParameterSettings.class;
                                }

                                @Override
                                public PathEditor.IOMode ioMode() {
                                    return PathEditor.IOMode.Open;
                                }

                                @Override
                                public PathEditor.PathMode pathMode() {
                                    return PathEditor.PathMode.FilesOnly;
                                }

                                @Override
                                public String[] extensions() {
                                    return new String[0];
                                }

                                @Override
                                public String key() {
                                    return FileChooserSettings.KEY_PROJECT;
                                }
                            }).build();
                    JIPipeParameterEditorUI pathEditor = JIPipe.getInstance().getParameterTypeRegistry().createEditorFor(getWorkbench(), access);
                    FormPanel formPanel = new FormPanel(null, FormPanel.NONE);
                    formPanel.addToForm(pathEditor, new JLabel("External script path"), null);
                    add(formPanel, BorderLayout.CENTER);
                    pathEditorComponent = formPanel;
                } else {
                    add(textArea, BorderLayout.CENTER);
                }
            }
            isCollapsed = code.isCollapsed();
            if (!code.getExternalScriptFile().isEnabled() && !Objects.equals(textArea.getText(), code.getCode()))
                textArea.setText(code.getCode());
            revalidate();
            repaint();
        }
    }
}
