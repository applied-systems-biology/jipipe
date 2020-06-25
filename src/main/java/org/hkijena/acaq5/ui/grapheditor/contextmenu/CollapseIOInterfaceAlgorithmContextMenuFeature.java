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
