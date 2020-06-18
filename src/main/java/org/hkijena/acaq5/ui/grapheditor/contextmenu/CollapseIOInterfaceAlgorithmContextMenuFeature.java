package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Removes an {@link org.hkijena.acaq5.api.compartments.algorithms.IOInterfaceAlgorithm}
 */
public class CollapseIOInterfaceAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        if (ui.getAlgorithm() instanceof IOInterfaceAlgorithm) {
            JMenuItem collapseItem = new JMenuItem("Delete (keep connections)", UIUtils.getIconFromResources("delete.png"));
            collapseItem.addActionListener(e -> IOInterfaceAlgorithm.collapse((IOInterfaceAlgorithm) ui.getAlgorithm()));
            contextMenu.add(collapseItem);
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
