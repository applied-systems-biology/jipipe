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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.actions;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeAndEdgesUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGraphEditorUIApplicationSettings;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Set;

public class DeleteCompartmentNodesAndEdgesContextAction implements NodeAndEdgesUIContextAction {
    @Override
    public boolean matchesNodes(Set<JIPipeDesktopGraphNodeUI> selection) {
        return !selection.isEmpty();
    }


    @Override
    public String getName() {
        return "Delete";
    }

    @Override
    public boolean matchesEdges(Set<JIPipeDesktopGraphEdgeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void runNodesAndEdges(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> nodeSelection, Set<JIPipeDesktopGraphEdgeUI> edgeSelection) {
        boolean allowDeleteNodes = true;
        if (!nodeSelection.isEmpty()) {
            if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(canvasUI.getDesktopWorkbench())) {
                allowDeleteNodes = false;
            }
        }

        String subject = "";
        if (allowDeleteNodes && !nodeSelection.isEmpty()) {
            subject = "compartments/nodes";
        }
        if (!edgeSelection.isEmpty()) {
            if (!subject.isEmpty()) {
                subject += "/";
            }
            subject += "edges";
        }

        if (!JIPipeGraphEditorUIApplicationSettings.getInstance().isAskOnDeleteCompartment() || JOptionPane.showConfirmDialog(canvasUI.getDesktopWorkbench().getWindow(),
                "Do you really want to remove the selected " + subject + "?", "Delete " + subject,
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (!edgeSelection.isEmpty()) {
                for (JIPipeDesktopGraphEdgeUI edgeUI : edgeSelection) {
                    canvasUI.getGraph().disconnect(edgeUI.getSource(), edgeUI.getTarget(), true);
                }
            }
            if (!nodeSelection.isEmpty()) {
                for (JIPipeDesktopGraphNodeUI ui : ImmutableList.copyOf(nodeSelection)) {
                    if (ui.getNode().isUiLocked())
                        continue;
                    if (ui.getNode() instanceof JIPipeProjectCompartment compartment) {
                        if (canvasUI.getHistoryJournal() != null) {
                            canvasUI.getHistoryJournal().snapshotBeforeRemoveCompartment(compartment);
                        }
                        compartment.getRuntimeProject().removeCompartment(compartment);
                    } else {
                        canvasUI.getGraph().removeNode(ui.getNode(), true);
                    }
                }
            }
            canvasUI.repaintLowLag();
        }
    }

    @Override
    public String getDescription() {
        return "Deletes the selected compartments/edges";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/delete.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true);
    }

    @Override
    public boolean isDisplayedInToolbar() {
        return true;
    }
}
