package org.hkijena.jipipe.extensions.nodeexamples;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.parameters.library.references.IconRefParameterEditorUI;
import org.hkijena.jipipe.extensions.settings.NodeTemplateSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LoadExampleContextMenuAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        if(selection.size() == 1) {
            JIPipeNodeUI nodeUI = selection.iterator().next();
            if(!(nodeUI.getNode() instanceof JIPipeAlgorithm)) {
                return false;
            }
            return !getExamples(nodeUI).isEmpty();
        }
        return false;
    }

   private List<JIPipeNodeExample> getExamples(JIPipeNodeUI nodeUI) {
        List<JIPipeNodeExample> result;
        if(nodeUI.getGraphUI().getWorkbench() instanceof JIPipeProjectWorkbench) {
            result = ((JIPipeProjectWorkbench) nodeUI.getGraphUI().getWorkbench()).getProject().getNodeExamples(nodeUI.getNode().getInfo().getId());
        }
        else {
            result = new ArrayList<>(JIPipe.getNodes().getNodeExamples(nodeUI.getNode().getInfo().getId()));
        }
        return result;
   }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JIPipeNodeUI nodeUI = selection.iterator().next();
        JIPipeGraphNode node = nodeUI.getNode();
        JIPipeNodeExamplePickerDialog pickerDialog = new JIPipeNodeExamplePickerDialog(canvasUI.getWorkbench().getWindow());
        pickerDialog.setTitle("Load example");
        List<JIPipeNodeExample> nodeExamples = getExamples(nodeUI);
        pickerDialog.setAvailableItems(nodeExamples);
        JIPipeNodeExample picked = pickerDialog.showDialog();
        if(picked != null) {
            ((JIPipeAlgorithm)node).loadExample(picked);
        }
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }

    @Override
    public String getName() {
        return "Load example";
    }

    @Override
    public String getDescription() {
        return "Loads example parameters.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/graduation-cap.png");
    }

}
