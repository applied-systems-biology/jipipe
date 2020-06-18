package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Adds a "Open settings" button
 */
public class OpenSettingsAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        JMenuItem selectOnlyButton = new JMenuItem("Open settings", UIUtils.getIconFromResources("cog.png"));
        selectOnlyButton.addActionListener(e -> ui.getEventBus().post(new AlgorithmSelectedEvent(ui, false)));
        contextMenu.add(selectOnlyButton);
    }

    @Override
    public void update(ACAQAlgorithmUI ui) {

    }

    @Override
    public boolean withSeparator() {
        return false;
    }
}
