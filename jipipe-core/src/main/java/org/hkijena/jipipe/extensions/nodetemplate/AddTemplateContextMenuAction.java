package org.hkijena.jipipe.extensions.nodetemplate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.NodeTemplateSettings;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.parameters.ParameterExplorerWindow;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class AddTemplateContextMenuAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return selection.size() >= 1;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        Set<JIPipeGraphNode> algorithms = selection.stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet());
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        JIPipeGraph graph = canvasUI.getGraph();
        JIPipeGraph subGraph = graph.extract(algorithms, false);
        template.setData(JsonUtils.toPrettyJsonString(subGraph));

        int result = JOptionPane.YES_OPTION;
        if(canvasUI.getGraph().getProject() != null) {
            result = JOptionPane.showOptionDialog(canvasUI.getWorkbench().getWindow(),
                    "Node templates can be stored globally or inside the project. Where should the template be stored?",
                    "Create node template",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Globally", "Inside project", "Cancel"},
                    "Globally");
        }
        if(result == JOptionPane.CANCEL_OPTION)
            return;

        if(ParameterPanel.showDialog(canvasUI.getWorkbench(), template, new MarkdownDocument("# Node templates\n\nUse this user interface to modify node templates."), "Create template",
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_DOCUMENTATION)) {
            if(result == JOptionPane.YES_OPTION) {
                // Store globally
                NodeTemplateSettings.getInstance().getNodeTemplates().add(template);
                NodeTemplateSettings.getInstance().triggerParameterChange("node-templates");
                JIPipe.getSettings().save();
            }
            else {
                // Store locally
                canvasUI.getGraph().getProject().getMetadata().getNodeTemplates().add(template);
                canvasUI.getGraph().getProject().getMetadata().triggerParameterChange("node-templates");
            }
            NodeTemplateSettings.triggerRefreshedEvent();
        }
    }

    @Override
    public String getName() {
        return "Create node template";
    }

    @Override
    public String getDescription() {
        return "Converts the selection into a node template.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/favorite.png");
    }

}
