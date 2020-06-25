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
import org.hkijena.acaq5.api.grouping.JsonAlgorithm;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

/**
 * Adds "convert to group" for {@link org.hkijena.acaq5.api.grouping.JsonAlgorithm}
 */
public class ConvertToGroupAlgorithmContextMenuFeature implements ACAQAlgorithmUIContextMenuFeature {
    @Override
    public void install(ACAQAlgorithmUI ui, JPopupMenu contextMenu) {
        ACAQGraphNode algorithm = ui.getAlgorithm();
        if (algorithm instanceof JsonAlgorithm) {
            JMenuItem unpackItem = new JMenuItem("Convert to group", UIUtils.getIconFromResources("archive-extract.png"));
            unpackItem.setToolTipText("Converts the algorithm into a group that can be edited");
            unpackItem.addActionListener(e -> JsonAlgorithm.unpackToNodeGroup((JsonAlgorithm) algorithm));
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
