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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays available parameters of existing or available nodes and allows to pick the parameter keys
 */
public class JIPipeDesktopParameterKeyPickerUI extends JPanel {

    private JIPipeDesktopSearchTextField nodeSearchField;
    private JList<Object> nodeJList;
    private JIPipeDesktopSearchTextField parameterSearchField;
    private JTree treeComponent;
    private Set<JIPipeGraphNode> nodeInstances = new HashSet<>();
    private JIPipeParameterTree lastTree;

    /**
     * Creates a new empty instance
     */
    public JIPipeDesktopParameterKeyPickerUI() {
        initialize();
        rebuildNodeList();
    }

    private void rebuildNodeList() {
        DefaultListModel<Object> model = new DefaultListModel<>();

        // Add all node instances
        Map<UUID, List<JIPipeGraphNode>> byCompartment = nodeInstances.stream().collect(Collectors.groupingBy(JIPipeGraphNode::getCompartmentUUIDInParentGraph));
        for (Map.Entry<UUID, List<JIPipeGraphNode>> entry : byCompartment.entrySet()) {
            entry.getValue().stream().sorted(Comparator.comparing(JIPipeGraphNode::getName))
                    .filter(node -> nodeSearchField.test(node.getName() + " " + node.getInfo().getDescription().getBody()))
                    .forEach(model::addElement);
        }

        // Add all known node types
        JIPipe.getNodes().getRegisteredNodeInfos().values().stream()
                .filter(info -> nodeSearchField.test(info.getName() + " " + info.getDescription().getBody()))
                .sorted(Comparator.comparing(JIPipeNodeInfo::getName)).forEach(model::addElement);

        nodeJList.setModel(model);

        if(!model.isEmpty()) {
            nodeJList.setSelectedIndex(0);
        }
    }

    public Set<JIPipeGraphNode> getNodeInstances() {
        return nodeInstances;
    }

    public void setNodeInstances(Set<JIPipeGraphNode> nodeInstances) {
        this.nodeInstances = nodeInstances;
        rebuildNodeList();
    }

    public void selectNode(Object node) {
        if(node != null) {
            nodeJList.setSelectedValue(node, true);
        }
    }

    /**
     * Picks a node or parameter
     *
     * @param parent the parent component
     * @param title  the title
     * @return list of selected objects. Either {@link JIPipeParameterAccess} or {@link JIPipeParameterTree.Node}
     */
    public static List<ParameterEntry> showPickerDialog(Component parent, String title, Set<JIPipeGraphNode> nodeInstances, Object preselected) {

        List<ParameterEntry> result = new ArrayList<>();

        JIPipeDesktopParameterKeyPickerUI ui = new JIPipeDesktopParameterKeyPickerUI();
        ui.setNodeInstances(nodeInstances);
        ui.selectNode(preselected);
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
        pickButton.addActionListener(e -> {
            result.addAll(ui.getSelectedEntries());
            dialog.setVisible(false);
        });
        buttonPanel.add(pickButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setSize(1024, 768);
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setVisible(true);


        return result;
    }

    private List<ParameterEntry> getSelectedEntries() {
        List<ParameterEntry> result = new ArrayList<>();
        if(treeComponent != null && treeComponent.getSelectionPaths() != null)  {
            for (TreePath selectionPath : treeComponent.getSelectionPaths()) {
                Object lastPathComponent = selectionPath.getLastPathComponent();
                if(lastPathComponent instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode) lastPathComponent).getUserObject();
                    if(userObject instanceof JIPipeParameterAccess) {
                        JIPipeParameterAccess parameterAccess = (JIPipeParameterAccess) userObject;
                        result.add(new ParameterEntry(parameterAccess.getName(),
                                parameterAccess.getDescription(),
                                lastTree.getUniqueKey(parameterAccess),
                                parameterAccess.getFieldClass(),
                                parameterAccess.get(Object.class)));
                    }
                    else if(userObject instanceof JIPipeParameterTree.Node) {
                        for (JIPipeParameterAccess parameterAccess : ((JIPipeParameterTree.Node) userObject).getParameters().values()) {
                            result.add(new ParameterEntry(parameterAccess.getName(),
                                    parameterAccess.getDescription(),
                                    lastTree.getUniqueKey(parameterAccess),
                                    parameterAccess.getFieldClass(),
                                    parameterAccess.get(Object.class)));
                        }
                    }
                }
            }
        }

        return result;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Node panel
        JPanel nodePanel = new JPanel(new BorderLayout());

        nodeSearchField = new JIPipeDesktopSearchTextField();
        nodeSearchField.addActionListener(e -> rebuildNodeList());
        nodePanel.add(nodeSearchField, BorderLayout.NORTH);

        nodeJList = new JList<>();
        nodeJList.setCellRenderer(new NodeListCellRenderer());
        nodeJList.addListSelectionListener(e -> rebuildTreeModel());
        nodePanel.add(new JScrollPane(nodeJList), BorderLayout.CENTER);

        // Tree panel
        JPanel treePanel = new JPanel(new BorderLayout());

        parameterSearchField = new JIPipeDesktopSearchTextField();
        treePanel.add(parameterSearchField, BorderLayout.NORTH);

        treeComponent = new JTree();
        treeComponent.setCellRenderer(new ParameterTreeCellRenderer());
        treePanel.add(new JScrollPane(treeComponent), BorderLayout.CENTER);

        add(new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT,
                nodePanel,
                treePanel,
                new AutoResizeSplitPane.DynamicSidebarRatio(300, true)));
    }

    /**
     * Recreates the tree model
     */
    public void rebuildTreeModel() {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultTreeModel model = new DefaultTreeModel(root);

        if(nodeJList.getSelectedValue() != null) {
            lastTree = null;
            if(nodeJList.getSelectedValue() instanceof JIPipeGraphNode) {
                lastTree = new JIPipeParameterTree((JIPipeParameterCollection) nodeJList.getSelectedValue());
            }
            else if(nodeJList.getSelectedValue() instanceof JIPipeNodeInfo) {
                lastTree = new JIPipeParameterTree(((JIPipeNodeInfo) nodeJList.getSelectedValue()).newInstance());
            }

            if(lastTree != null) {
                root.setUserObject(lastTree.getRoot());
                traverse(root, lastTree.getRoot(), Collections.emptySet(), false);
            }
        }

        treeComponent.setModel(model);
        UIUtils.expandAllTree(treeComponent);
    }

    private void traverse(DefaultMutableTreeNode uiNode, JIPipeParameterTree.Node node, Set<JIPipeParameterTree.Node> ignored, boolean noSearch) {
        if (ignored.contains(node)) {
            return;
        }
        for (JIPipeParameterAccess value : node.getParameters().values().stream().sorted(Comparator.comparing(JIPipeParameterAccess::getUIOrder).thenComparing(JIPipeParameterAccess::getName)).collect(Collectors.toList())) {
            if (noSearch || parameterSearchField.test(value.getName())) {
                DefaultMutableTreeNode parameterUINode = new DefaultMutableTreeNode(value);
                uiNode.add(parameterUINode);
            }
        }
        for (JIPipeParameterTree.Node child : node.getChildren().values().stream().sorted(Comparator.comparing(JIPipeParameterTree.Node::getName)).collect(Collectors.toList())) {
            DefaultMutableTreeNode childUINode = new DefaultMutableTreeNode(child);
            traverse(childUINode, child, ignored, noSearch || parameterSearchField.test(child.getName()));
            if (childUINode.getChildCount() > 0)
                uiNode.add(childUINode);
        }
    }

    public static class ParameterEntry {
        private final String name;
        private final String description;
        private final String key;
        private final Class<?> fieldClass;
        private final Object value;

        public ParameterEntry(String name, String description, String key, Class<?> fieldClass, Object value) {
            this.name = name;
            this.description = description;
            this.key = key;
            this.fieldClass = fieldClass;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Object getValue() {
            return value;
        }

        public String getKey() {
            return key;
        }

        public Class<?> getFieldClass() {
            return fieldClass;
        }

        public Object getInitialValue() {
            JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(fieldClass);
            if(value != null) {
                return info.duplicate(value);
            }
            else {
                return info.newInstance();
            }
        }
    }

    public static class NodeListCellRenderer extends JPanel implements ListCellRenderer<Object> {

        private final JLabel iconLabel = new JLabel();
        private final JLabel mainLabel = new JLabel();
        private final JLabel secondaryLabel = new JLabel();

        public NodeListCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            secondaryLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
            setLayout(new GridBagLayout());
            Insets insets = new Insets(2,2,2,2);
            add(iconLabel, new GridBagConstraints(0,
                    0,
                    1,
                    1,
                    0,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.NONE,
                    insets,
                    0,
                    0));
            add(mainLabel, new GridBagConstraints(1,
                    0,
                    1,
                    1,
                    1,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL,
                    insets,
                    0,
                    0));
            add(secondaryLabel, new GridBagConstraints(1,
                    1,
                    1,
                    1,
                    1,
                    0,
                    GridBagConstraints.NORTHWEST,
                    GridBagConstraints.HORIZONTAL,
                    insets,
                    0,
                    0));
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            if(value instanceof JIPipeNodeInfo) {
                iconLabel.setIcon(((JIPipeNodeInfo) value).getIcon());
                mainLabel.setText(((JIPipeNodeInfo) value).getName());
                secondaryLabel.setText("");
            }
            else if(value instanceof JIPipeGraphNode) {
                iconLabel.setIcon(((JIPipeGraphNode) value).getInfo().getIcon());
                mainLabel.setText(((JIPipeGraphNode) value).getName());
                UUID compartmentUUID = ((JIPipeGraphNode) value).getCompartmentUUIDInParentGraph();
                JIPipeProject project = ((JIPipeGraphNode) value).getParentGraph().getProject();
                if(compartmentUUID != null && project != null) {
                    secondaryLabel.setText(project.getCompartments().get(compartmentUUID).getName());
                }
            }

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }

    /**
     * Renders the entries
     */
    public static class ParameterTreeCellRenderer extends JLabel implements TreeCellRenderer {

        /**
         * Creates a new instance
         */
        public ParameterTreeCellRenderer() {
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
                    setIcon(   UIUtils.getIconFromResources("actions/folder.png"));
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
