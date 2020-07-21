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

import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.history.RemoveNodeGraphHistorySnapshot;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.stream.Collectors;

public class DeleteNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        if (selection.isEmpty())
            return false;
        for (JIPipeNodeUI ui : selection) {
            if (ui.getNode().getCategory() == JIPipeNodeCategory.Internal)
                return false;
        }
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        if (!GraphEditorUISettings.getInstance().isAskOnDeleteNode() || JOptionPane.showConfirmDialog(canvasUI,
                "Do you really want to remove the following algorithms: " +
                        selection.stream().map(JIPipeNodeUI::getNode).map(JIPipeGraphNode::getName).collect(Collectors.joining(", ")), "Delete algorithms",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Set<JIPipeGraphNode> nodes = selection.stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet());
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
        return UIUtils.getIconFromResources("actions/delete.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true);
    }
}
