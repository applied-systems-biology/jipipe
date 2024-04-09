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

package org.hkijena.jipipe.desktop.app.grapheditor.compartments.contextmenu.clipboard.clipboard;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.stream.Collectors;

public class AlgorithmGraphCutNodeUIContextAction extends AlgorithmGraphCopyNodeUIContextAction {

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        super.run(canvasUI, selection);
        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(canvasUI.getDesktopWorkbench()))
            return;
        Set<JIPipeGraphNode> nodes = selection.stream().map(JIPipeDesktopGraphNodeUI::getNode).filter(node -> !node.isUiLocked()).collect(Collectors.toSet());
        if (canvasUI.getHistoryJournal() != null) {
            canvasUI.getHistoryJournal().snapshotBeforeCutNodes(nodes, nodes.stream().map(JIPipeGraphNode::getCompartmentUUIDInParentGraph).findFirst().orElse(null));
        }
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
        return UIUtils.getIconFromResources("actions/edit-cut.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK, true);
    }

    @Override
    public boolean showInMultiSelectionPanel() {
        return false;
    }
}
