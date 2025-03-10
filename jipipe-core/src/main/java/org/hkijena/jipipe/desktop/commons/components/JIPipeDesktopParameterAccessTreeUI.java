/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.commons.components;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Displays the contents of {@link JIPipeParameterTree}.
 * Contains {@link javax.swing.tree.DefaultMutableTreeNode} that either reference the {@link org.hkijena.jipipe.api.parameters.JIPipeParameterAccess}
 * or {@link JIPipeParameterTree.Node}
 */
public class JIPipeDesktopParameterAccessTreeUI extends JPanel {
    private JTree treeComponent;
    private JIPipeDesktopSearchTextField searchTextField;
    private JIPipeParameterTree tree;

    /**
     * Creates a new empty instance
     */
    public JIPipeDesktopParameterAccessTreeUI() {
        initialize();
    }

    /**
     * Creates a new instance
     *
     * @param tree the tree
     */
    public JIPipeDesktopParameterAccessTreeUI(JIPipeParameterTree tree) {
        this.tree = tree;
        initialize();
        rebuildModel();
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
        JIPipeDesktopParameterAccessTreeUI ui = new JIPipeDesktopParameterAccessTreeUI(tree);
        JPanel contentPanel = new JPanel(new BorderLayout());

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        dialog.setTitle(title);
        dialog.setContentPane(contentPanel);

        contentPanel.add(ui, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        JButton collapseAllButton = new JButton("Collapse all", UIUtils.getIconFromResources("actions/collapse-all.png"));
        collapseAllButton.addActionListener(e -> UIUtils.setTreeExpandedState(ui.treeComponent, false));
        buttonPanel.add(collapseAllButton);

        JButton expandAllButton = new JButton("Expand all", UIUtils.getIconFromResources("actions/expand-all.png"));
        expandAllButton.addActionListener(e -> UIUtils.setTreeExpandedState(ui.treeComponent, true));
        buttonPanel.add(expandAllButton);

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
        dialog.setSize(500, 768);
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

    private void initialize() {
        setLayout(new BorderLayout());
        treeComponent = new JTree();
        treeComponent.setCellRenderer(new Renderer());
        JScrollPane treeScrollPane = new JScrollPane(treeComponent);
        add(treeScrollPane, BorderLayout.CENTER);
        searchTextField = new JIPipeDesktopSearchTextField();
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

    public JTree getTreeComponent() {
        return treeComponent;
    }

    /**
     * Recreates the tree model
     */
    public void rebuildModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(tree.getRoot());
        DefaultTreeModel model = new DefaultTreeModel(root);

        // Attempt to select for compartments
        Set<JIPipeParameterTree.Node> ignored = new HashSet<>();
        Multimap<JIPipeProjectCompartment, JIPipeParameterTree.Node> perCompartment = HashMultimap.create();
        for (JIPipeParameterTree.Node node : tree.getRoot().getChildren().values()) {
            if (node.getCollection() instanceof JIPipeGraphNode) {
                JIPipeGraphNode graphNode = (JIPipeGraphNode) node.getCollection();
                if (graphNode.getCompartmentUUIDInParentGraph() != null && graphNode.getParentGraph().getProject() != null) {
                    JIPipeProjectCompartment compartment = graphNode.getParentGraph().getProject().getCompartments().getOrDefault(graphNode.getCompartmentUUIDInParentGraph(), null);
                    perCompartment.put(compartment, node);
                    ignored.add(node);
                }
            }
        }

        // Compartment-based nodes
        for (JIPipeProjectCompartment compartment : perCompartment.keySet()) {
            DefaultMutableTreeNode compartmentNode = new DefaultMutableTreeNode(compartment);
            for (JIPipeParameterTree.Node node : perCompartment.get(compartment)) {
                DefaultMutableTreeNode uiNode = new DefaultMutableTreeNode(node);
                traverse(uiNode, node, Collections.emptySet(), false);
                compartmentNode.add(uiNode);
            }
            root.add(compartmentNode);
        }

        traverse(root, tree.getRoot(), ignored, false);
        treeComponent.setModel(model);
//        UIUtils.expandAllTree(treeComponent);
    }

    private void traverse(DefaultMutableTreeNode uiNode, JIPipeParameterTree.Node node, Set<JIPipeParameterTree.Node> ignored, boolean noSearch) {
        if (ignored.contains(node)) {
            return;
        }
        for (JIPipeParameterAccess value : node.getParameters().values()) {
            if (noSearch || searchTextField.test(value.getName())) {
                DefaultMutableTreeNode parameterUINode = new DefaultMutableTreeNode(value);
                uiNode.add(parameterUINode);
            }
        }
        for (JIPipeParameterTree.Node child : node.getChildren().values()) {
            DefaultMutableTreeNode childUINode = new DefaultMutableTreeNode(child);
            traverse(childUINode, child, ignored, noSearch || searchTextField.test(child.getName()));
            if (childUINode.getChildCount() > 0)
                uiNode.add(childUINode);
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

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
                Object userObject = treeNode.getUserObject();
                if (userObject instanceof JIPipeParameterTree.Node) {
                    JIPipeParameterTree.Node node = (JIPipeParameterTree.Node) userObject;
                    Icon icon = null;
                    if (node.getCollection() instanceof JIPipeGraphNode) {
                        JIPipeNodeInfo info = ((JIPipeGraphNode) node.getCollection()).getInfo();
                        icon = JIPipe.getNodes().getIconFor(info);
                    }
                    if (icon == null) {
                        UIUtils.getIconFromResources("actions/object-group.png");
                    }
                    setIcon(icon);
                    String name = node.getName();
                    if (name == null)
                        name = node.getKey();
                    setText(name);
                } else if (userObject instanceof JIPipeProjectCompartment) {
                    setIcon(UIUtils.getIconFromResources("data-types/graph-compartment.png"));
                    setText(((JIPipeProjectCompartment) userObject).getName());
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
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }

            return this;
        }
    }
}
