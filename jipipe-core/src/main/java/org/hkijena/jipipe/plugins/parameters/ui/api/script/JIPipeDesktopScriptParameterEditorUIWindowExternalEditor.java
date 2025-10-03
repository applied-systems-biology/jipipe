package org.hkijena.jipipe.plugins.parameters.ui.api.script;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.codeeditor.JIPipeDesktopCodeEditorUI;
import org.hkijena.jipipe.desktop.app.codeeditor.JIPipeDesktopParameterCodeEditorDocument;
import org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter;
import org.hkijena.jipipe.plugins.parameters.ui.api.JIPipeDesktopScriptParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.ref.WeakReference;

public class JIPipeDesktopScriptParameterEditorUIWindowExternalEditor extends JIPipeDesktopScriptParameterEditorUIExternalEditor {

    private JFrame frame;

    public JIPipeDesktopScriptParameterEditorUIWindowExternalEditor(JIPipeDesktopWorkbench workbench, WeakReference<JIPipeParameterCollection> parameterCollection, String parameterKey, JIPipeParameterTree parameterTree, JIPipeParameterAccess access) {
        super(workbench, parameterCollection, parameterKey, parameterTree, access);
        initialize();
    }

    private void initialize() {
        JIPipeDesktopCodeEditorUI editorUI = new JIPipeDesktopCodeEditorUI(getDesktopWorkbench(), new JIPipeDesktopParameterCodeEditorDocument(getParameterAccess()));
        JIPipeScriptParameter code = getParameter(JIPipeScriptParameter.class);
        frame = new JFrame();
        frame.setTitle("JIPipe - " + getParameterAccess().getKey() + " (" + code.getLanguageName() + ")");
        frame.setContentPane(editorUI);
        frame.setIconImage(UIUtils.getJIPipeIcon128());
        frame.pack();
        frame.setSize(1024, 768);
        frame.setLocationRelativeTo(getDesktopWorkbench().getWindow());
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                JIPipeDesktopScriptParameterEditorUIWindowExternalEditor.super.close();
            }
        });
    }

    @Override
    public void close() {
        super.close();
        frame.setVisible(false);
        frame = null;
    }
}
