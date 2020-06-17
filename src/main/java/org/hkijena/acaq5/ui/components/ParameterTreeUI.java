package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Displays the contents of {@link ACAQParameterTree}.
 * Contains {@link javax.swing.tree.DefaultMutableTreeNode} that either reference the {@link org.hkijena.acaq5.api.parameters.ACAQParameterAccess}
 * or {@link ACAQParameterTree.Node}
 */
public class ParameterTreeUI extends JTree {
    private ACAQParameterTree tree;

    /**
     * Creates a new empty instance
     */
    public ParameterTreeUI() {
        this.setCellRenderer(new Renderer());
    }

    /**
     * Creates a new instance
     * @param tree the tree
     */
    public ParameterTreeUI(ACAQParameterTree tree) {
        this.tree = tree;
        rebuildModel();
    }

    public ACAQParameterTree getTree() {
        return tree;
    }

    public void setTree(ACAQParameterTree tree) {
        this.tree = tree;
        rebuildModel();
    }

    /**
     * Recreates the tree model
     */
    public void rebuildModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(tree.getRoot());
        DefaultTreeModel model = new DefaultTreeModel(root);
        traverse(root, tree.getRoot());
        setModel(model);
    }

    private void traverse(DefaultMutableTreeNode uiNode, ACAQParameterTree.Node node) {
        for (ACAQParameterAccess value : node.getParameters().values()) {
            DefaultMutableTreeNode parameterUINode = new DefaultMutableTreeNode(value);
            uiNode.add(parameterUINode);
        }
        for (ACAQParameterTree.Node child : node.getChildren().values()) {
            DefaultMutableTreeNode childUINode = new DefaultMutableTreeNode(child);
            uiNode.add(childUINode);
            traverse(childUINode, child);
        }
    }

    /**
     * Renders the entries
     */
    public static class Renderer extends JLabel implements TreeCellRenderer {

        /**
         * Creates a new instance
         */
        public Renderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            if(value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
                Object userObject = treeNode.getUserObject();
                if(userObject instanceof ACAQParameterTree.Node) {
                    ACAQParameterTree.Node node = (ACAQParameterTree.Node) userObject;
                    setIcon(UIUtils.getIconFromResources("object.png"));
                    String name = node.getName();
                    if(name == null)
                        name = node.getKey();
                    setText(name);
                }
                else if(userObject instanceof ACAQParameterAccess) {
                    ACAQParameterAccess access = (ACAQParameterAccess) userObject;
                    setIcon(UIUtils.getIconFromResources("edit.png"));
                    String name = access.getName();
                    if(name == null)
                        name = access.getKey();
                    setText(name);
                }
            }

            if (selected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }

            return this;
        }
    }
}
