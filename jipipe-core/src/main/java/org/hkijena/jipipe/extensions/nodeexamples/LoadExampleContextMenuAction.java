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

package org.hkijena.jipipe.extensions.nodeexamples;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LoadExampleContextMenuAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        if (selection.size() == 1) {
            JIPipeDesktopGraphNodeUI nodeUI = selection.iterator().next();
            if (!(nodeUI.getNode() instanceof JIPipeAlgorithm)) {
                return false;
            }
            return !getExamples(nodeUI).isEmpty();
        }
        return false;
    }

    private List<JIPipeNodeExample> getExamples(JIPipeDesktopGraphNodeUI nodeUI) {
        List<JIPipeNodeExample> result;
        if (nodeUI.getGraphCanvasUI().getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            result = ((JIPipeDesktopProjectWorkbench) nodeUI.getGraphCanvasUI().getDesktopWorkbench()).getProject().getNodeExamples(nodeUI.getNode().getInfo().getId());
        } else {
            result = new ArrayList<>(JIPipe.getNodes().getNodeExamples(nodeUI.getNode().getInfo().getId()));
        }
        return result;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        JIPipeDesktopGraphNodeUI nodeUI = selection.iterator().next();
        JIPipeGraphNode node = nodeUI.getNode();
        JIPipeNodeExamplePickerDialog pickerDialog = new JIPipeNodeExamplePickerDialog(canvasUI.getDesktopWorkbench().getWindow());
        pickerDialog.setTitle("Load example");
        List<JIPipeNodeExample> nodeExamples = getExamples(nodeUI);
        pickerDialog.setAvailableItems(nodeExamples);
        JIPipeNodeExample example = pickerDialog.showDialog();
        if (example != null) {
            ((JIPipeAlgorithm) node).loadExample(example);
            canvasUI.getDesktopWorkbench().sendStatusBarText("Loaded example '" + example.getNodeTemplate().getName() + "' into " + node.getDisplayName());
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
