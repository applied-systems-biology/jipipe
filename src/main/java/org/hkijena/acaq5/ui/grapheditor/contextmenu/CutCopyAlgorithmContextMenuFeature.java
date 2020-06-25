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
