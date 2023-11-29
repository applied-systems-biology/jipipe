package org.hkijena.jipipe.extensions.dev;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.pipelinerender.RenderPipelineRun;
import org.hkijena.jipipe.extensions.pipelinerender.RenderPipelineRunSettings;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Map;

public class ExportNodeJsonsTool extends JIPipeMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ExportNodeJsonsTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Export all nodes as graph JSON");
        setToolTipText("Exports all available nodes as copy-able JSON into a directory.");
        setIcon(UIUtils.getIconFromResources("actions/bug.png"));
        addActionListener(e -> runExportTool());
    }

    private void runExportTool() {
        Path outputDirectory = FileChooserSettings.saveDirectory(getWorkbench().getWindow(), FileChooserSettings.LastDirectoryKey.External, "Output directory");
        if(outputDirectory != null) {
            for (Map.Entry<String, JIPipeNodeInfo> entry : JIPipe.getNodes().getRegisteredNodeInfos().entrySet()) {
                System.out.println("Export " + entry.getKey());
                String fileName = StringUtils.makeFilesystemCompatible(entry.getKey()) + ".json";
                JIPipeGraphNode node = entry.getValue().newInstance();
                JIPipeGraph graph = new JIPipeGraph();
                graph.insertNode(node);
                JsonUtils.saveToFile(graph, outputDirectory.resolve(fileName));
            }
            JOptionPane.showMessageDialog(getWorkbench().getWindow(), "OK", getText(), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Development";
    }
}
