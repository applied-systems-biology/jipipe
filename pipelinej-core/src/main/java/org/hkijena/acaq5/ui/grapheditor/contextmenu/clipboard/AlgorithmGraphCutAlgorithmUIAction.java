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

package org.hkijena.acaq5.ui.grapheditor.contextmenu.clipboard;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.history.CutNodeGraphHistorySnapshot;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQNodeUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class AlgorithmGraphCutAlgorithmUIAction extends AlgorithmGraphCopyAlgorithmUIAction {

    @Override
    public void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection) {
        super.run(canvasUI, selection);
        Set<ACAQGraphNode> nodes = selection.stream().map(ACAQNodeUI::getNode).collect(Collectors.toSet());
        canvasUI.getGraphHistory().addSnapshotBefore(new CutNodeGraphHistorySnapshot(canvasUI.getGraph(), nodes));
        canvasUI.getGraph().removeNodes(nodes, true);
    }

    @Override
    public String getName() {
        return "Cut";
    }

    @Override
    public String getDescription() {
        return "Cuts the selection into the clipboard";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("cut.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }
}
