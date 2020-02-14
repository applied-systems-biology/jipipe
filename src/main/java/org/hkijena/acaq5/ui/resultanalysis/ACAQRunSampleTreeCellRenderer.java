package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRunSample;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Renders a sample in the {@link ACAQResultSampleManagerUI}
 */
public class ACAQRunSampleTreeCellRenderer extends JLabel implements TreeCellRenderer {

    public ACAQRunSampleTreeCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if(tree.getFont() != null) {
            setFont(tree.getFont());
        }

        Object o = ((DefaultMutableTreeNode)value).getUserObject();
        if(o instanceof ACAQRunSample) {
            ACAQRunSample sample = (ACAQRunSample)o;
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
