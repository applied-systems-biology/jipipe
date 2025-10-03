package org.hkijena.jipipe.desktop.app.codeeditor;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

public class JIPipeDesktopCodeEditorRSyntaxTextEditorUI extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeDesktopCodeEditorUI editorUI;

    public JIPipeDesktopCodeEditorRSyntaxTextEditorUI(JIPipeDesktopCodeEditorUI editorUI) {
        super(editorUI.getDesktopWorkbench());
        this.editorUI = editorUI;
        initialize();
    }

    private void initialize() {

    }
}
