package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.api.ACAQProjectSample;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Renders a sample in the {@link org.hkijena.acaq5.ui.ACAQSampleManagerUI}
 */
public class ACAQSampleTreeCellRenderer extends JLabel implements TreeCellRenderer {

    public ACAQSampleTreeCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if(tree.getFont() != null) {
            setFont(tree.getFont());
        }

        Object o = ((DefaultMutableTreeNode)value).getUserObject();
        if(o instanceof ACAQProjectSample) {
            ACAQProjectSample sample = (ACAQProjectSample)o;
            setText(sample.getName());
            setIcon(UIUtils.getIconFromResources("sample.png"));
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
