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

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grouping.JsonAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JsonAlgorithmToGroupNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return JIPipe.getNodes().hasNodeInfoWithId("node-group") && selection.stream().map(JIPipeGraphNodeUI::getNode).anyMatch(a -> a instanceof JsonAlgorithm);
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvasUI.getWorkbench()))
            return;
        if (canvasUI.getHistoryJournal() != null) {
            Set<JIPipeGraphNode> nodes = selection.stream().map(JIPipeGraphNodeUI::getNode).collect(Collectors.toSet());
            UUID compartment = nodes.stream().map(JIPipeGraphNode::getUUIDInParentGraph).findFirst().orElse(null);
            canvasUI.getHistoryJournal().snapshot("Convert to group", "Converted nodes into a group", compartment, UIUtils.getIconFromResources("actions/extract-archive.png"));
        }
        for (JIPipeGraphNodeUI ui : selection) {
            if (ui.getNode() instanceof JsonAlgorithm) {
                JsonAlgorithm.unpackToNodeGroup((JsonAlgorithm) ui.getNode());
            }
        }
    }

    @Override
    public String getName() {
        return "Convert to group";
    }

    @Override
    public String getDescription() {
        return "Converts selected JSON algorithms into distinct group nodes";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/extract-archive.png");
    }

}
