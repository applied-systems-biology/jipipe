package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.api.ACAQProjectSample;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class ACAQDataSlotTreeCellRenderer extends JLabel implements TreeCellRenderer {

    public ACAQDataSlotTreeCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if(tree.getFont() != null) {
            setFont(tree.getFont());
        }

        Object o = ((DefaultMutableTreeNode)value).getUserObject();
        if(o instanceof ACAQDataSlot<?>) {
            ACAQDataSlot<?> slot = (ACAQDataSlot<?>)o;
            setText(slot.getName());
            setIcon(ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(slot.getAcceptedDataType()));
        }
        else {
            setText(o.toString());
            setIcon(null);
        }

        // Update status
        // Update status
        if(selected) {
            setBackground(new Color(184, 207, 229));
        }
        else {
            setBackground(new Color(255,255,255));
        }

        return this;
    }
}
