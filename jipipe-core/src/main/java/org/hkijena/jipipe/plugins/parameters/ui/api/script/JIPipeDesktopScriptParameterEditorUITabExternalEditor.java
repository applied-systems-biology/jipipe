package org.hkijena.jipipe.plugins.parameters.ui.api.script;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.codeeditor.JIPipeDesktopCodeEditorUI;
import org.hkijena.jipipe.desktop.app.codeeditor.JIPipeDesktopParameterCodeEditorDocument;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter;

import java.lang.ref.WeakReference;

public class JIPipeDesktopScriptParameterEditorUITabExternalEditor extends JIPipeDesktopScriptParameterEditorUIExternalEditor {
    private JIPipeDesktopCodeEditorUI editorUI;

    public JIPipeDesktopScriptParameterEditorUITabExternalEditor(JIPipeDesktopWorkbench workbench, WeakReference<JIPipeParameterCollection> parameterCollection, String parameterKey, JIPipeParameterTree parameterTree, JIPipeParameterAccess access) {
        super(workbench, parameterCollection, parameterKey, parameterTree, access);
        initialize();
    }

    private void initialize() {
        editorUI = new JIPipeDesktopCodeEditorUI(getDesktopWorkbench(), new JIPipeDesktopParameterCodeEditorDocument(getParameterAccess()));
        JIPipeScriptParameter code = getParameter(JIPipeScriptParameter.class);
        getDesktopWorkbench().getDocumentTabPane().addTab(getParameterAccess().getName() + " (" + code.getLanguageName() + ")",
                JIPipe.RESOURCES.getIcon16("actions/dialog-xml-editor.png"),
                editorUI,
                JIPipeDesktopTabPane.CloseMode.withSilentCloseButton,
                true);
        getDesktopWorkbench().getDocumentTabPane().switchToLastTab();
    }

    @Override
    public void close() {
        super.close();
        JIPipeDesktopTabPane.DocumentTab tab = getDesktopWorkbench().getDocumentTabPane().getTabContainingContent(editorUI);
        if (tab != null) {
            getDesktopWorkbench().getDocumentTabPane().closeTab(tab);
        }
        editorUI = null;
    }
}
