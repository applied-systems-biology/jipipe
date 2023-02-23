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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.stream.Collectors;

public class DeleteCompartmentUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvasUI.getWorkbench()))
            return;
        if (!GraphEditorUISettings.getInstance().isAskOnDeleteCompartment() || JOptionPane.showConfirmDialog(canvasUI.getWorkbench().getWindow(),
                "Do you really want to remove the following compartments: " +
                        selection.stream().map(JIPipeNodeUI::getNode).map(JIPipeGraphNode::getName).collect(Collectors.joining(", ")), "Delete compartments",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (JIPipeNodeUI ui : ImmutableList.copyOf(selection)) {
                if (ui.getNode() instanceof JIPipeProjectCompartment) {
                    JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) ui.getNode();
                    if (canvasUI.getHistoryJournal() != null) {
                        canvasUI.getHistoryJournal().snapshotBeforeRemoveCompartment(compartment);
                    }
                    compartment.getProject().removeCompartment(compartment);
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
    public boolean disableOnNonMatch() {
        return false;
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true);
    }
}
