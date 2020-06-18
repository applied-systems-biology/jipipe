package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Adds a delete button
 */
public class DeleteAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        ACAQGraphNode algorithm = ui.getAlgorithm();
        ACAQAlgorithmGraphCanvasUI graphUI = ui.getGraphUI();
        JMenuItem deleteButton = new JMenuItem("Delete algorithm", UIUtils.getIconFromResources("delete.png"));
        deleteButton.setEnabled(graphUI.getAlgorithmGraph().canUserDelete(algorithm));
        deleteButton.addActionListener(e -> removeAlgorithm(ui, algorithm, graphUI));
        contextMenu.add(deleteButton);
    }

    private void removeAlgorithm(ACAQAlgorithmUI ui, ACAQGraphNode algorithm, ACAQAlgorithmGraphCanvasUI graphUI) {
        if (JOptionPane.showConfirmDialog(ui,
                "Do you really want to remove the algorithm '" + algorithm.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            graphUI.getAlgorithmGraph().removeNode(algorithm);
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
