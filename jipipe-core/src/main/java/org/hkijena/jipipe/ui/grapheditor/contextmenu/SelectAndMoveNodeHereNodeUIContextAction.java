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

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.components.PickNodeDialog;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class SelectAndMoveNodeHereNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return selection.size() <= 1;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JIPipeGraphNode preSelected = selection.isEmpty() ? null : selection.iterator().next().getNode();
        JIPipeGraphNode algorithm = PickNodeDialog.showDialog(canvasUI.getWorkbench().getWindow(),
                canvasUI.getNodeUIs().keySet(),
                preSelected,
                "Move node");
        if (algorithm != null) {
            JIPipeNodeUI ui = canvasUI.getNodeUIs().getOrDefault(algorithm, null);
            if (ui != null) {
                canvasUI.getGraphHistory().addSnapshotBefore(new MoveNodesGraphHistorySnapshot(canvasUI.getGraph(), "Move node here ..."));
                ui.moveToClosestGridPoint(canvasUI.getGraphEditorCursor(), false, true);
                canvasUI.repaint();
                canvasUI.getEventBus().post(new JIPipeNodeUI.AlgorithmEvent(ui));
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
        return UIUtils.getIconFromResources("actions/transform-move.png");
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK, true);
    }
}
