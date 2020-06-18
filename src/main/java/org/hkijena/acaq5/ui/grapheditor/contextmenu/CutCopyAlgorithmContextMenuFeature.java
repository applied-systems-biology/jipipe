package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Collections;

/**
 * Implements cut/copy
 */
public class CutCopyAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    private JMenuItem cutContextMenuButton;
    private JMenuItem copyContextMenuButton;

    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        ACAQAlgorithmGraphCanvasUI graphUI = ui.getGraphUI();
        cutContextMenuButton = new JMenuItem("Cut", UIUtils.getIconFromResources("cut.png"));
        cutContextMenuButton.addActionListener(e -> graphUI.getCopyPasteBehavior().cut(Collections.singleton(ui.getAlgorithm())));
        contextMenu.add(cutContextMenuButton);

        copyContextMenuButton = new JMenuItem("Copy", UIUtils.getIconFromResources("copy.png"));
        copyContextMenuButton.addActionListener(e -> graphUI.getCopyPasteBehavior().copy(Collections.singleton(ui.getAlgorithm())));
        contextMenu.add(copyContextMenuButton);
    }

    @Override
    public void update(ACAQAlgorithmUI ui) {
        ACAQGraphNode algorithm = ui.getAlgorithm();
        ACAQAlgorithmGraphCanvasUI graphUI = ui.getGraphUI();
        if (cutContextMenuButton != null)
            cutContextMenuButton.setEnabled(algorithm.canUserDelete() && graphUI.getCopyPasteBehavior() != null);
        if (copyContextMenuButton != null)
            copyContextMenuButton.setEnabled(algorithm.canUserDelete() && graphUI.getCopyPasteBehavior() != null);
    }

    @Override
    public boolean withSeparator() {
        return false;
    }
}
