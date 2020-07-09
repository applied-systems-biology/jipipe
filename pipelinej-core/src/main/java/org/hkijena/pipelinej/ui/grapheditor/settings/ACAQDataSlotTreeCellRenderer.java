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

package org.hkijena.pipelinej.ui.grapheditor.settings;

import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.pipelinej.utils.StringUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Renders an {@link ACAQDataSlot} in a {@link JTree}
 */
public class ACAQDataSlotTreeCellRenderer extends JLabel implements TreeCellRenderer {


    /**
     * Creates a new renderer
     */
    public ACAQDataSlotTreeCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (tree.getFont() != null) {
            setFont(tree.getFont());
        }

        Object o = ((DefaultMutableTreeNode) value).getUserObject();
        if (o instanceof ACAQDataSlot) {
            ACAQDataSlot slot = (ACAQDataSlot) o;
            if (!StringUtils.isNullOrEmpty(slot.getDefinition().getCustomName())) {
                setText("<html><i>" + slot.getDefinition().getCustomName() + "</i> [" + slot.getName() + "]</html>");
            } else {
                setText(slot.getName());
            }
            setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        } else {
            setText(o.toString());
            setIcon(null);
        }

        // Update status
        // Update status
        if (selected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
