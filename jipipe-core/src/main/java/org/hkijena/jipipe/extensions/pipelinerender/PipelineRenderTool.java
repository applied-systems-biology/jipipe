package org.hkijena.jipipe.extensions.pipelinerender;

import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

public class PipelineRenderTool extends JIPipeMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public PipelineRenderTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Export whole pipeline as *.png");
        setToolTipText("Rebuilds the node alias IDs for all nodes. This can help if the " +
                "generated alias IDs are too long.");
        setIcon(UIUtils.getIconFromResources("actions/camera.png"));
        addActionListener(e -> runRenderTool());
    }

    private void runRenderTool() {
        JOptionPane.showMessageDialog(getWorkbench().getWindow(),
                "Please check if you organized your compartments as compact as possible, to minimize computational load of generating a full resolution pipeline.",
                getText(),
                JOptionPane.WARNING_MESSAGE);
        Path path = FileChooserSettings.saveFile(getWorkbench().getWindow(), FileChooserSettings.LastDirectoryKey.External, getText(), UIUtils.EXTENSION_FILTER_PNG);
        if(path != null) {
            JIPipeRunExecuterUI.runInDialog(getWorkbench().getWindow(), new RenderPipelineRun(((JIPipeProjectWorkbench)getWorkbench()).getProject(), path));
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Project";
    }
}
