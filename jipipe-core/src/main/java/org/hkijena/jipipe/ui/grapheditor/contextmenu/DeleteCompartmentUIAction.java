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

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.history.DeleteCompartmentGraphHistorySnapshot;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class DeleteCompartmentUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        if (!GraphEditorUISettings.getInstance().isAskOnDeleteCompartment() || JOptionPane.showConfirmDialog(canvasUI,
                "Do you really want to remove the following compartments: " +
                        selection.stream().map(JIPipeNodeUI::getNode).map(JIPipeGraphNode::getName).collect(Collectors.joining(", ")), "Delete compartments",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JIPipeProject project = ((JIPipeProjectWorkbench) canvasUI.getWorkbench()).getProject();
            for (JIPipeNodeUI ui : selection) {
                JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) ui.getNode();
                canvasUI.getGraphHistory().addSnapshotBefore(new DeleteCompartmentGraphHistorySnapshot(project, compartment));
                compartment.getProject().removeCompartment(compartment);
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
