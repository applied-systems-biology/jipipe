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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.pickers.JIPipeDesktopPickNodeDialog;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Set;

public class SelectAndMoveNodeHereNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return selection.size() <= 1;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        JIPipeGraphNode preSelected = selection.isEmpty() ? null : selection.iterator().next().getNode();
        JIPipeGraphNode algorithm = JIPipeDesktopPickNodeDialog.showDialog(canvasUI.getDesktopWorkbench().getWindow(),
                canvasUI.getNodeUIs().keySet(),
                preSelected,
                "Move node");
        if (algorithm != null) {
            JIPipeDesktopGraphNodeUI ui = canvasUI.getNodeUIs().getOrDefault(algorithm, null);
            if (ui != null) {
                if (canvasUI.getHistoryJournal() != null) {
                    canvasUI.getHistoryJournal().snapshotBeforeMoveNodes(Collections.singleton(ui.getNode()), ui.getNode().getCompartmentUUIDInParentGraph());
                }
                ui.moveToClosestGridPoint(canvasUI.getGraphEditorCursor(), false, true);
                canvasUI.repaint();
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
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK, true);
    }
}
