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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeScriptAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.codeeditor.JIPipeDesktopCodeEditorUI;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter;
import org.hkijena.jipipe.plugins.parameters.ui.api.script.JIPipeDesktopScriptParameterEditorUIExternalEditor;
import org.hkijena.jipipe.plugins.parameters.ui.api.script.JIPipeDesktopScriptParameterEditorUIExternalFileExternalEditor;
import org.hkijena.jipipe.plugins.parameters.ui.api.script.JIPipeDesktopScriptParameterEditorUITabExternalEditor;
import org.hkijena.jipipe.plugins.parameters.ui.api.script.JIPipeDesktopScriptParameterEditorUIWindowExternalEditor;
import org.hkijena.jipipe.utils.CustomEditorPane;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Parameter editor for {@link JIPipeScriptParameter}
 */
public class JIPipeDesktopScriptParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeScriptParameter> {

    /**
     * The script editor will look for a specific code editor dock and use that one if
     */
    public static final String DOCK_CODE = "CODE_EDITOR";

    public static final List<JIPipeDesktopScriptParameterEditorUIExternalEditor> OPENED_EXTERNAL_EDITORS = new ArrayList<>();
    private CustomEditorPane textArea;
    private JLabel collapseInfoLabel;
    private boolean isCollapsed;
    private JButton closeExternalEditorsButton;
    private JToggleButton collapseButton;
    private JButton openIdeButton;
    private JButton openCodePanelButton;
    private JIPipeDesktopCodeEditorUI codeEditorDock;
    private boolean withDock;

    public JIPipeDesktopScriptParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeScriptParameter.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeScriptParameter code = getParameter();
        collapseInfoLabel = new JLabel("The code is hidden",
                JIPipe.RESOURCES.getIcon16("actions/eye-slash.png"),
                JLabel.LEFT);
        collapseInfoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
        initializeTextEditor(code);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new JLabel(code.getLanguageName()));

        toolBar.add(Box.createHorizontalGlue());

        collapseButton = new JToggleButton("", JIPipe.RESOURCES.getIcon16("actions/eye-slash.png"));
        collapseButton.setToolTipText("Collapse/show");
        collapseButton.setSelected(code.isCollapsed());
        collapseButton.addActionListener(e -> toggleCollapse());
        toolBar.add(collapseButton);

        openIdeButton = new JButton("", JIPipe.RESOURCES.getIcon16("actions/open-in-new-window.png"));
        openIdeButton.setToolTipText("Open in ...");
        JPopupMenu popupMenu = UIUtils.addPopupMenuToButton(openIdeButton);
        popupMenu.add(UIUtils.createMenuItem("New tab", "Opens the editor in a new tab", JIPipe.RESOURCES.getIcon16("actions/tab-new.png"), this::openIDEInTab));
        popupMenu.add(UIUtils.createMenuItem("New window", "Opens the editor in a new window", JIPipe.RESOURCES.getIcon16("actions/window_new.png"), this::openIdeInNewWindow));
        popupMenu.add(UIUtils.createMenuItem("External editor", "Opens the editor in an external application", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::openExternalIde));
        toolBar.add(openIdeButton);

        add(toolBar, BorderLayout.NORTH);

        setBorder(UIUtils.createControlBorder());

        closeExternalEditorsButton = new JButton("<html><strong>External editors are currently open</strong><br>Click this button to re-enable editing",
                JIPipe.RESOURCES.getIcon16("actions/unlock.png"));
        closeExternalEditorsButton.addActionListener(e -> closeExistingExternalEditors());

        openCodePanelButton = new JButton("<html><strong>Please use the code panel to edit this script</strong><br>Click this button to show the code panel",
                JIPipe.RESOURCES.getIcon16("actions/edit.png"));
        openCodePanelButton.addActionListener(e -> showCodeDock());
    }

    private void initializeTextEditor(JIPipeScriptParameter code) {
        textArea = new CustomEditorPane();
        ThemeUtils.applyThemeToCodeEditor(textArea);
        textArea.setBackground(UIManager.getColor("TextArea.background"));
        textArea.setHighlightCurrentLine(false);
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
    }

    private boolean isScriptAlgorithmScriptParameter() {
        JIPipeParameterCollection rootCollection = getParameterTree().getRoot().getCollection();
        if (rootCollection instanceof JIPipeScriptAlgorithm scriptAlgorithm) {
            return getParameterCollection() == rootCollection && Objects.equals(getParameterAccess().getKey(), scriptAlgorithm.getScriptParameterAccess().getKey());
        }
        return false;
    }

    private JIPipeDesktopDockPanel getDockPanel() {
        Container ancestor = SwingUtilities.getAncestorOfClass(JIPipeDesktopDockPanel.class, this);
        if (ancestor instanceof JIPipeDesktopDockPanel) {
            return (JIPipeDesktopDockPanel) ancestor;
        }
        return null;
    }

    private JIPipeDesktopCodeEditorUI getCodeEditorDock() {
        JIPipeDesktopDockPanel dockPanel = getDockPanel();
        if (dockPanel != null) {
            JComponent panelComponent = dockPanel.getPanelComponent(DOCK_CODE, JComponent.class);
            if (panelComponent instanceof JIPipeDesktopCodeEditorUI) {
                return (JIPipeDesktopCodeEditorUI) panelComponent;
            }
        }
        return null;
    }

    private void showCodeDock() {
        JIPipeDesktopCodeEditorUI editorDock = getCodeEditorDock();
        JIPipeDesktopDockPanel dockPanel = getDockPanel();
        if (editorDock != null && dockPanel != null) {
            dockPanel.activatePanel(DOCK_CODE, false);
        }
    }

    private void closeExistingExternalEditors() {
        for (JIPipeDesktopScriptParameterEditorUIExternalEditor editor : getExternalEditors()) {
            editor.close();
        }
        reload();
    }

    private List<JIPipeDesktopScriptParameterEditorUIExternalEditor> getExternalEditors() {
        return OPENED_EXTERNAL_EDITORS.stream().filter(editor -> editor.accessEquals(getParameterAccess())).collect(Collectors.toList());
    }

    private void openExternalIde() {
        closeExistingExternalEditors();
        JIPipeDesktopScriptParameterEditorUIExternalFileExternalEditor externalEditor = new JIPipeDesktopScriptParameterEditorUIExternalFileExternalEditor(getDesktopWorkbench(),
                new WeakReference<>(getParameterCollection()),
                getParameterAccess().getKey(),
                getParameterTree(), getParameterAccess());
        OPENED_EXTERNAL_EDITORS.add(externalEditor);
        reload();
    }

    private void openIdeInNewWindow() {
        closeExistingExternalEditors();
        JIPipeDesktopScriptParameterEditorUIWindowExternalEditor externalEditor = new JIPipeDesktopScriptParameterEditorUIWindowExternalEditor(getDesktopWorkbench(),
                new WeakReference<>(getParameterCollection()),
                getParameterAccess().getKey(),
                getParameterTree(), getParameterAccess());
        OPENED_EXTERNAL_EDITORS.add(externalEditor);
        reload();
    }

    private void toggleCollapse() {
        JIPipeScriptParameter code = getParameter();
        code.setCollapsed(!code.isCollapsed());
        setParameter(code, true);
    }

    private void openIDEInTab() {
        closeExistingExternalEditors();
        JIPipeDesktopScriptParameterEditorUITabExternalEditor externalEditor = new JIPipeDesktopScriptParameterEditorUITabExternalEditor(getDesktopWorkbench(),
                new WeakReference<>(getParameterCollection()),
                getParameterAccess().getKey(),
                getParameterTree(), getParameterAccess());
        OPENED_EXTERNAL_EDITORS.add(externalEditor);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public boolean reloadOnShownFirstTime() {
        return true;
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (!withDock) {
            // If we are with a dock, we don't want to keep reloading
            super.onParameterChanged(event);
        }
    }

    @Override
    public void onShownFirstTime() {
        codeEditorDock = getCodeEditorDock();
        withDock = isScriptAlgorithmScriptParameter() && codeEditorDock != null;

        super.onShownFirstTime();

        // We let the code dock do this itself
//        if(withDock) {
//            SwingUtilities.invokeLater(this::loadParameterIntoDock);
//        }
    }

//    private void loadParameterIntoDock() {
//        codeEditorDock.setDocument(new JIPipeDesktopParameterCodeEditorDocument(getParameterAccess()));
//        showCodeDock();
//    }

    @Override
    public void reload() {
        JIPipeScriptParameter code = getParameter();

        remove(textArea);
        remove(collapseInfoLabel);
        remove(closeExternalEditorsButton);
        remove(openCodePanelButton);

        collapseButton.setVisible(!withDock);
        openIdeButton.setVisible(!withDock);

        if (withDock) {
            add(openCodePanelButton, BorderLayout.CENTER);
        } else if (!code.isCollapsed() || !isCollapsed) {
            if (code.isCollapsed()) {
                add(collapseInfoLabel, BorderLayout.CENTER);
            } else {
                if (getExternalEditors().isEmpty()) {
                    add(textArea, BorderLayout.CENTER);
                } else {
                    add(closeExternalEditorsButton, BorderLayout.CENTER);
                }
            }
            isCollapsed = code.isCollapsed();
            if (!Objects.equals(textArea.getText(), code.getCode())) {
                textArea.setText(code.getCode());
            }
            revalidate();
            repaint();
        }
    }

}
