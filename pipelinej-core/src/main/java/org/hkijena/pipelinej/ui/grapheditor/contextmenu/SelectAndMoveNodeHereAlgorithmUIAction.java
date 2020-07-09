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

package org.hkijena.pipelinej.ui.grapheditor.contextmenu;

import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.history.MoveNodesGraphHistorySnapshot;
import org.hkijena.pipelinej.ui.components.PickAlgorithmDialog;
import org.hkijena.pipelinej.ui.events.AlgorithmEvent;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQNodeUI;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class SelectAndMoveNodeHereAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQNodeUI> selection) {
        return selection.isEmpty();
    }

    @Override
    public void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection) {
        ACAQGraphNode algorithm = PickAlgorithmDialog.showDialog(canvasUI, canvasUI.getNodeUIs().keySet(), "Move node");
        if (algorithm != null) {
            ACAQNodeUI ui = canvasUI.getNodeUIs().getOrDefault(algorithm, null);
            if (ui != null) {
                canvasUI.getGraphHistory().addSnapshotBefore(new MoveNodesGraphHistorySnapshot(canvasUI.getGraph(), "Move node here ..."));
                ui.trySetLocationInGrid(canvasUI.getGraphEditorCursor().x, canvasUI.getGraphEditorCursor().y);
                canvasUI.repaint();
                canvasUI.getEventBus().post(new AlgorithmEvent(ui));
            }
        }
    }

    @Override
    public String getName() {
        return "Move node here ...";
    }

    @Override
    public String getDescription() {
        return "Shows a window to select a node and move it to the current cursor.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("move.png");
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
