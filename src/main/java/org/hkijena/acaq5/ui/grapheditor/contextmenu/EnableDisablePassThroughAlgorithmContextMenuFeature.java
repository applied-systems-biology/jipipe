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

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Adds enable/disable/pass-through
 */
public class EnableDisablePassThroughAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    private JMenuItem passThroughContextMenuButton;
    private JMenuItem enableDisableContextMenuButton;

    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        ACAQGraphNode algorithm = ui.getAlgorithm();
        if (algorithm instanceof ACAQAlgorithm) {
            ACAQAlgorithm a = (ACAQAlgorithm) algorithm;
            passThroughContextMenuButton = new JMenuItem("Enable/Disable pass through", UIUtils.getIconFromResources("pass-through.png"));
            passThroughContextMenuButton.addActionListener(e -> a.setPassThrough(!a.isPassThrough()));
            contextMenu.add(passThroughContextMenuButton);
        }

        if (algorithm instanceof ACAQAlgorithm) {
            ACAQAlgorithm a = (ACAQAlgorithm) algorithm;
            enableDisableContextMenuButton = new JMenuItem("Enable/Disable algorithm", UIUtils.getIconFromResources("block.png"));
            enableDisableContextMenuButton.addActionListener(e -> a.setEnabled(!a.isEnabled()));
            contextMenu.add(enableDisableContextMenuButton);
        }
    }

    @Override
    public void update(ACAQAlgorithmUI ui) {
        ACAQGraphNode algorithm = ui.getAlgorithm();
        if (algorithm instanceof ACAQAlgorithm && enableDisableContextMenuButton != null) {
            ACAQAlgorithm a = (ACAQAlgorithm) algorithm;
            if (a.isEnabled()) {
                enableDisableContextMenuButton.setText("Disable algorithm");
            } else {
                enableDisableContextMenuButton.setText("Enable algorithm");
            }
            if (a.isPassThrough()) {
                passThroughContextMenuButton.setText("Disable pass through");
            } else {
                passThroughContextMenuButton.setText("Enable pass through");
            }
        }
    }

    @Override
    public boolean withSeparator() {
        return false;
    }
}
