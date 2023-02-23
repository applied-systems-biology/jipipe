package org.hkijena.jipipe.extensions.nodeexamples;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LoadExampleContextMenuAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        if (selection.size() == 1) {
            JIPipeNodeUI nodeUI = selection.iterator().next();
            if (!(nodeUI.getNode() instanceof JIPipeAlgorithm)) {
                return false;
            }
            return !getExamples(nodeUI).isEmpty();
        }
        return false;
    }

    private List<JIPipeNodeExample> getExamples(JIPipeNodeUI nodeUI) {
        List<JIPipeNodeExample> result;
        if (nodeUI.getGraphCanvasUI().getWorkbench() instanceof JIPipeProjectWorkbench) {
            result = ((JIPipeProjectWorkbench) nodeUI.getGraphCanvasUI().getWorkbench()).getProject().getNodeExamples(nodeUI.getNode().getInfo().getId());
        } else {
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
        JIPipeNodeExample example = pickerDialog.showDialog();
        if (example != null) {
            ((JIPipeAlgorithm) node).loadExample(example);
            canvasUI.getWorkbench().sendStatusBarText("Loaded example '" + example.getNodeTemplate().getName() + "' into " + node.getDisplayName());
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
