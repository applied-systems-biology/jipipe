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

package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQAlgorithmUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(ACAQAlgorithmGraphCanvasUI canvasUI, Set<ACAQAlgorithmUI> selection) {
        Set<ACAQGraphNode> algorithms = selection.stream().map(ACAQAlgorithmUI::getAlgorithm).collect(Collectors.toSet());
        ACAQGraph graph = canvasUI.getAlgorithmGraph();
        ACAQGraph subGraph = graph.extract(algorithms, false);
        NodeGroup group = new NodeGroup(subGraph, true);
        for (ACAQGraphNode algorithm : algorithms) {
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
        return UIUtils.getIconFromResources("group.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }
}
