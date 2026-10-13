package org.hkijena.jipipe.desktop.app.grapheditor.nodefinder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;

class FunctionTreeCellRenderer implements TreeCellRenderer {

    private static final Icon GENERIC_NODE_ICON = JIPipe.RESOURCES.getIcon16("actions/tag.png");
    private final JIPipeDesktopNodeFinderDialogUI jiPipeDesktopNodeFinderDialogUI;

    public FunctionTreeCellRenderer(JIPipeDesktopNodeFinderDialogUI jiPipeDesktopNodeFinderDialogUI) {
        this.jiPipeDesktopNodeFinderDialogUI = jiPipeDesktopNodeFinderDialogUI;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        if (node.getUserObject() instanceof JIPipeNodeTaxonomyFunction function) {
            label.setText(function.getName());
            Icon icon = function.getIcon();
            label.setIcon(icon != null ? icon : GENERIC_NODE_ICON);
            label.setToolTipText(function.getDescription());
            // Search highlight: the search matched a node filed under this
            // leaf (or a descendant leaf)
            if (jiPipeDesktopNodeFinderDialogUI.searchHighlightedLeafPaths.contains(new TreePath(node.getPath()))) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
        } else {
            // The root node
            label.setText("All functions");
            label.setIcon(GENERIC_NODE_ICON);
        }
        Color selectionBackground = UIManager.getColor("Tree.selectionBackground");
        Color selectionForeground = UIManager.getColor("Tree.selectionForeground");
        label.setForeground(selected && selectionForeground != null ? selectionForeground : tree.getForeground());
        label.setBackground(selected && selectionBackground != null ? selectionBackground : tree.getBackground());
        return label;
    }
}
