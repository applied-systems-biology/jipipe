/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.contextmenu;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.history.GraphChangedHistorySnapshot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return JIPipe.getNodes().hasNodeInfoWithId("node-group") && !selection.isEmpty();
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvasUI.getWorkbench()))
            return;
        canvasUI.getGraphHistory().addSnapshotBefore(new GraphChangedHistorySnapshot(canvasUI.getGraph(), "Group"));
        Set<JIPipeGraphNode> algorithms = selection.stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet());
        JIPipeGraph graph = canvasUI.getGraph();
        JIPipeGraph subGraph = graph.extract(algorithms, false);
        NodeGroup group = new NodeGroup(subGraph, true);
        for (JIPipeGraphNode algorithm : algorithms) {
            graph.removeNode(algorithm, true);
        }
        graph.insertNode(group, canvasUI.getCompartment());
    }

    @Override
    public String getName() {
        return "Group";
    }

    @Override
    public String getDescription() {
        return "Moves the selected nodes into a group node.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/object-group.png");
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
