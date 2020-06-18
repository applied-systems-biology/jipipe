package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Adds a delete button
 */
public class DeleteCompartmentContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        ACAQGraphNode algorithm = ui.getAlgorithm();
        ACAQAlgorithmGraphCanvasUI graphUI = ui.getGraphUI();
        JMenuItem deleteButton = new JMenuItem("Delete compartment", UIUtils.getIconFromResources("delete.png"));
        deleteButton.setEnabled(graphUI.getAlgorithmGraph().canUserDelete(algorithm));
        deleteButton.addActionListener(e -> removeCompartment(ui, algorithm));
        contextMenu.add(deleteButton);
    }

    private void removeCompartment(ACAQAlgorithmUI ui, ACAQGraphNode algorithm) {
        ACAQProjectCompartment compartment = (ACAQProjectCompartment) algorithm;
        if (JOptionPane.showConfirmDialog(ui, "Do you really want to delete the compartment '" + compartment.getName() + "'?\n" +
                "You will lose all nodes stored in this compartment.", "Delete compartment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            compartment.getProject().removeCompartment(compartment);
        }
    }

    @Override
    public void update(ACAQAlgorithmUI ui) {

    }

    @Override
    public boolean withSeparator() {
        return false;
    }
}
