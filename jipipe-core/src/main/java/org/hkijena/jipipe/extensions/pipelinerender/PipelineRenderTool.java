package org.hkijena.jipipe.extensions.pipelinerender;

import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

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
        if(JOptionPane.showConfirmDialog(getWorkbench().getWindow(), "This will create a high-resolution render of the whole pipeline, organized by their compartment.\n" +
                "This might take a while.\nContinue?", getText(), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeRunnerQueue.getInstance().enqueue(new RenderPipelineRun(((JIPipeProjectWorkbench)getWorkbench()).getProject()));
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
