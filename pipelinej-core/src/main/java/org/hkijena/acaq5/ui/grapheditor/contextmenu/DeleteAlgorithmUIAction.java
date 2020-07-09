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

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.history.RemoveNodeGraphHistorySnapshot;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQNodeUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class DeleteAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection) {
        if (JOptionPane.showConfirmDialog(canvasUI,
                "Do you really want to remove the following algorithms: " +
                        selection.stream().map(ACAQNodeUI::getNode).map(ACAQGraphNode::getName).collect(Collectors.joining(", ")), "Delete algorithms",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Set<ACAQGraphNode> nodes = selection.stream().map(ACAQNodeUI::getNode).collect(Collectors.toSet());
            canvasUI.getGraphHistory().addSnapshotBefore(new RemoveNodeGraphHistorySnapshot(canvasUI.getGraph(), nodes));
            canvasUI.getGraph().removeNodes(nodes, true);
        }
    }

    @Override
    public String getName() {
        return "Delete";
    }

    @Override
    public String getDescription() {
        return "Deletes the selected nodes";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("delete.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
