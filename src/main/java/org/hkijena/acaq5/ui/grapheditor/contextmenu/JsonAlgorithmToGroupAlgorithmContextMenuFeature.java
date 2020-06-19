package org.hkijena.acaq5.ui.grapheditor.contextmenu;


import org.hkijena.acaq5.api.grouping.JsonAlgorithm;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Allows the user to convert {@link org.hkijena.acaq5.api.grouping.JsonAlgorithm} to {@link org.hkijena.acaq5.api.grouping.NodeGroup}
 */
public class JsonAlgorithmToGroupAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        if(ui.getAlgorithm() instanceof JsonAlgorithm) {
            JMenuItem unpackItem = new JMenuItem("Convert to group", UIUtils.getIconFromResources("archive-extract.png"));
            unpackItem.addActionListener(e -> JsonAlgorithm.unpackToNodeGroup((JsonAlgorithm) ui.getAlgorithm()));;
            contextMenu.add(unpackItem);
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
