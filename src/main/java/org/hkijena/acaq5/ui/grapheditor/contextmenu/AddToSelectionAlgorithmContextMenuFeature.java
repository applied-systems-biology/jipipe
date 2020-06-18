package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Adds button "add to selection"
 */
public class AddToSelectionAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        JMenuItem addToSelectionButton = new JMenuItem("Add to selection", UIUtils.getIconFromResources("select.png"));
        addToSelectionButton.addActionListener(e -> ui.getEventBus().post(new AlgorithmSelectedEvent(ui, true)));
        contextMenu.add(addToSelectionButton);
    }

    @Override
    public void update(ACAQAlgorithmUI ui) {

    }

    @Override
    public boolean withSeparator() {
        return false;
    }
}
