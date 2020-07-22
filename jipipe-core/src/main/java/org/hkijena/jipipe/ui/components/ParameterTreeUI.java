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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.registries.JIPipeUINodeRegistry;
import org.hkijena.jipipe.utils.CustomScrollPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the contents of {@link JIPipeParameterTree}.
 * Contains {@link javax.swing.tree.DefaultMutableTreeNode} that either reference the {@link org.hkijena.jipipe.api.parameters.JIPipeParameterAccess}
 * or {@link JIPipeParameterTree.Node}
 */
public class ParameterTreeUI extends JPanel {
    private JTree treeComponent;
    private SearchTextField searchTextField;
    private JScrollPane treeScrollPane;
    private JIPipeParameterTree tree;

    /**
     * Creates a new empty instance
     */
    public ParameterTreeUI() {
        initialize();
    }

    /**
     * Creates a new instance
     *
     * @param tree the tree
     */
    public ParameterTreeUI(JIPipeParameterTree tree) {
        this.tree = tree;
        initialize();
        rebuildModel();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        treeComponent = new JTree();
        treeComponent.setCellRenderer(new Renderer());
        treeScrollPane = new CustomScrollPane(treeComponent);
        add(treeScrollPane, BorderLayout.CENTER);
        searchTextField = new SearchTextField();
        searchTextField.addActionListener(e -> rebuildModel());
        add(searchTextField, BorderLayout.NORTH);
    }

    public JIPipeParameterTree getTree() {
        return tree;
    }

    public void setTree(JIPipeParameterTree tree) {
        this.tree = tree;
        rebuildModel();
    }

    /**
     * Recreates the tree model
     */
    public void rebuildModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(tree.getRoot());
        DefaultTreeModel model = new DefaultTreeModel(root);
        traverse(root, tree.getRoot(), false);
        treeComponent.setModel(model);
        UIUtils.expandAllTree(treeComponent);
    }

    private void traverse(DefaultMutableTreeNode uiNode, JIPipeParameterTree.Node node, boolean noSearch) {
        for (JIPipeParameterAccess value : node.getParameters().values()) {
            if (noSearch || searchTextField.test(value.getName())) {
                DefaultMutableTreeNode parameterUINode = new DefaultMutableTreeNode(value);
                uiNode.add(parameterUINode);
            }
        }
        for (JIPipeParameterTree.Node child : node.getChildren().values()) {
            DefaultMutableTreeNode childUINode = new DefaultMutableTreeNode(child);
            traverse(childUINode, child, noSearch || searchTextField.test(child.getName()));
            if (childUINode.getChildCount() > 0)
                uiNode.add(childUINode);
        }
    }

    /**
     * Picks a node or parameter
     *
     * @param parent the parent component
     * @param tree   the tree
     * @param title  the title
     * @return list of selected objects. Either {@link JIPipeParameterAccess} or {@link JIPipeParameterTree.Node}
     */
    public static List<Object> showPickerDialog(Component parent, JIPipeParameterTree tree, String title) {
        ParameterTreeUI ui = new ParameterTreeUI(tree);
        JPanel contentPanel = new JPanel(new BorderLayout());

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        dialog.setTitle(title);
        dialog.setContentPane(contentPanel);

        contentPanel.add(ui, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            ui.treeComponent.clearSelection();
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton pickButton = new JButton("Select", UIUtils.getIconFromResources("actions/color-select.png"));
        pickButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(pickButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);

        List<Object> selected = new ArrayList<>();
        if (ui.treeComponent.getSelectionPaths() != null) {
            for (TreePath selectionPath : ui.treeComponent.getSelectionPaths()) {
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
                if (userObject instanceof JIPipeParameterTree.Node) {
                    JIPipeParameterTree.Node node = (JIPipeParameterTree.Node) userObject;
                    Icon icon = null;
                    if (node.getCollection() instanceof JIPipeGraphNode) {
                        JIPipeNodeInfo info = ((JIPipeGraphNode) node.getCollection()).getInfo();
                        icon = JIPipeUINodeRegistry.getInstance().getIconFor(info);
                    }
                    if (icon == null) {
                        UIUtils.getIconFromResources("actions/object-group.png");
                    }
                    setIcon(icon);
                    String name = node.getName();
                    if (name == null)
                        name = node.getKey();
                    setText(name);
                } else if (userObject instanceof JIPipeParameterAccess) {
                    JIPipeParameterAccess access = (JIPipeParameterAccess) userObject;
                    setIcon(UIUtils.getIconFromResources("data-types/parameters.png"));
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
