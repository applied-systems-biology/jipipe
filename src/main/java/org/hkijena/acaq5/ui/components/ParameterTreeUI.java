package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
     *
     * @param tree the tree
     */
    public ParameterTreeUI(ACAQParameterTree tree) {
        this.tree = tree;
        this.setCellRenderer(new Renderer());
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
     * Picks a node or parameter
     *
     * @param parent the parent component
     * @param tree   the tree
     * @param title  the title
     * @return selected object or null if not was selected
     */
    public static List<Object> showPickerDialog(Component parent, ACAQParameterTree tree, String title) {
        ParameterTreeUI ui = new ParameterTreeUI(tree);
        UIUtils.expandAllTree(ui);
        JPanel contentPanel = new JPanel(new BorderLayout());

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        dialog.setTitle(title);
        dialog.setContentPane(contentPanel);

        contentPanel.add(new JScrollPane(ui), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            ui.clearSelection();
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton pickButton = new JButton("Select", UIUtils.getIconFromResources("pick.png"));
        pickButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(pickButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        List<Object> selected = new ArrayList<>();
        if (ui.getSelectionPaths() != null) {
            for (TreePath selectionPath : ui.getSelectionPaths()) {
                if (selectionPath != null) {
                    Object lastPathComponent = selectionPath.getLastPathComponent();
                    if (lastPathComponent instanceof DefaultMutableTreeNode) {
                        selected.add(((DefaultMutableTreeNode) lastPathComponent).getUserObject());
                    }
                }
            }
        }
        return selected;
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

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
                Object userObject = treeNode.getUserObject();
                if (userObject instanceof ACAQParameterTree.Node) {
                    ACAQParameterTree.Node node = (ACAQParameterTree.Node) userObject;
                    Icon icon = null;
                    if (node.getCollection() instanceof ACAQGraphNode) {
                        ACAQAlgorithmDeclaration declaration = ((ACAQGraphNode) node.getCollection()).getDeclaration();
                        icon = ACAQUIAlgorithmRegistry.getInstance().getIconFor(declaration);
                    }
                    if (icon == null) {
                        UIUtils.getIconFromResources("object.png");
                    }
                    setIcon(icon);
                    String name = node.getName();
                    if (name == null)
                        name = node.getKey();
                    setText(name);
                } else if (userObject instanceof ACAQParameterAccess) {
                    ACAQParameterAccess access = (ACAQParameterAccess) userObject;
                    setIcon(UIUtils.getIconFromResources("parameters.png"));
                    String name = access.getName();
                    if (name == null)
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
