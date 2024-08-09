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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.stream.Collectors;

public class DeleteCompartmentUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(canvasUI.getDesktopWorkbench()))
            return;
        if (!JIPipeGraphEditorUIApplicationSettings.getInstance().isAskOnDeleteCompartment() || JOptionPane.showConfirmDialog(canvasUI.getDesktopWorkbench().getWindow(),
                "Do you really want to remove the following compartments/annotations: " +
                        selection.stream().map(JIPipeDesktopGraphNodeUI::getNode).filter(node -> !node.isUiLocked()).map(JIPipeGraphNode::getName).collect(Collectors.joining(", ")), "Delete compartments",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (JIPipeDesktopGraphNodeUI ui : ImmutableList.copyOf(selection)) {
                if (ui.getNode().isUiLocked())
                    continue;
                if (ui.getNode() instanceof JIPipeProjectCompartment) {
                    JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) ui.getNode();
                    if (canvasUI.getHistoryJournal() != null) {
                        canvasUI.getHistoryJournal().snapshotBeforeRemoveCompartment(compartment);
                    }
                    compartment.getRuntimeProject().removeCompartment(compartment);
                } else {
                    canvasUI.getGraph().removeNode(ui.getNode(), true);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Delete";
    }

    @Override
    public String getDescription() {
        return "Deletes the selected compartments";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/delete.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true);
    }

    @Override
    public boolean showInToolbar() {
        return true;
    }
}
