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

package org.hkijena.jipipe.ui.parameterreference;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReference;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroup;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.pickers.PickNodeDialog;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Editor component for {@link GraphNodeParameterReferenceGroupCollection}
 */
public class GraphNodeParameterReferenceGroupCollectionEditorUI extends JIPipeWorkbenchPanel {

    private final GraphNodeParameterReferenceGroupCollection parameters;
    private final JTree groupJTree = new JTree();
    private final JPanel rightPanel = new JPanel(new BorderLayout());
    private final MarkdownDocument documentation;
    private final JLabel noGroupsLabel = UIUtils.createInfoLabel("No groups", "Click <i>Add &gt; Empty group</i> to begin editing parameters.");
    private JIPipeParameterTree parameterTree;
    private boolean withRefresh;

    /**
     * @param workbench   the workbench
     * @param parameters  the parameters to edit
     * @param withRefresh if the editor should refresh on changes
     */
    public GraphNodeParameterReferenceGroupCollectionEditorUI(JIPipeWorkbench workbench, GraphNodeParameterReferenceGroupCollection parameters, MarkdownDocument documentation, boolean withRefresh) {
        super(workbench);
        this.parameters = parameters;
        this.documentation = documentation != null ? documentation : MarkdownDocument.fromPluginResource("documentation/parameter-reference-editor.md", Collections.emptyMap());
        this.withRefresh = withRefresh;
        parameters.getEventBus().register(this);
        parameters.getGraph().getEventBus().register(this);
        initialize();
        refreshContent(true, null);
        onTreeNodeSelected();
    }

    public GraphNodeParameterReferenceGroupCollection getParameters() {
        return parameters;
    }

    public boolean isWithRefresh() {
        return withRefresh;
    }

    public void setWithRefresh(boolean withRefresh) {
        this.withRefresh = withRefresh;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(AutoResizeSplitPane.LEFT_RIGHT, AutoResizeSplitPane.RATIO_1_TO_3);
        initializeLeftPanel(splitPane);
        splitPane.setRightComponent(rightPanel);
    }

    private void initializeLeftPanel(AutoResizeSplitPane splitPane) {
        add(splitPane, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout());
        splitPane.setLeftComponent(leftPanel);
        initializeToolbar(leftPanel);

        groupJTree.setCellRenderer(new ParameterReferenceGroupCollectionTreeCellRenderer(this));
        groupJTree.addTreeSelectionListener(e -> {
            onTreeNodeSelected();
        });
        leftPanel.add(new JScrollPane(groupJTree), BorderLayout.CENTER);
    }

    private void onTreeNodeSelected() {
        if (groupJTree.getSelectionPath() != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) groupJTree.getSelectionPath().getLastPathComponent();
            if (node.getUserObject() instanceof GraphNodeParameterReferenceGroup) {
                selectGroup((GraphNodeParameterReferenceGroup) node.getUserObject());
            } else if (node.getUserObject() instanceof GraphNodeParameterReference) {
                selectReference((GraphNodeParameterReference) node.getUserObject());
            }
        } else {
            selectNone();
        }
    }

    private void selectNone() {
        rightPanel.removeAll();
        rightPanel.add(new MarkdownReader(false, documentation), BorderLayout.CENTER);
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void initializeToolbar(JPanel leftPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton refreshButton = new JButton(UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.setToolTipText("Refresh");
        refreshButton.addActionListener(e -> refreshContent(true, null));
        toolBar.add(refreshButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton addGroupButton = new JButton("Add", UIUtils.getIconFromResources("actions/list-add.png"));
        JPopupMenu popupMenu = UIUtils.addPopupMenuToComponent(addGroupButton);
        popupMenu.add(UIUtils.createMenuItem("Empty group", "Adds an empty group", UIUtils.getIconFromResources("actions/list-add.png"), this::addEmptyGroup));
        popupMenu.add(UIUtils.createMenuItem("Node as group", "Add all parameters within a node as group", UIUtils.getIconFromResources("data-types/node.png"), this::addWholeNode));
        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Parameter", "Adds a reference to a parameter", UIUtils.getIconFromResources("data-types/parameters.png"), this::addParameterReference));
        toolBar.add(addGroupButton);

        JButton removeButton = new JButton("Remove", UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.addActionListener(e -> removeSelectedItems());
        toolBar.add(removeButton);

        leftPanel.add(UIUtils.gridVertical(toolBar, noGroupsLabel), BorderLayout.NORTH);
    }

    private void addParameterReference() {
        if (!parameters.getParameterReferenceGroups().isEmpty()) {
            if (groupJTree.getSelectionPath() != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) groupJTree.getSelectionPath().getLastPathComponent();
                GraphNodeParameterReferenceGroup group;
                if (node.getUserObject() instanceof GraphNodeParameterReference) {
                    group = (GraphNodeParameterReferenceGroup) ((DefaultMutableTreeNode) node.getParent()).getUserObject();
                } else {
                    group = (GraphNodeParameterReferenceGroup) node.getUserObject();
                }
                addParameterReference(group);
            } else {
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) groupJTree.getModel().getRoot();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(0);
                GraphNodeParameterReferenceGroup group = (GraphNodeParameterReferenceGroup) node.getUserObject();
                groupJTree.setSelectionPath(new TreePath(node.getPath()));
                addParameterReference(group);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please add a parameter group first. To do this, click 'Add' and select 'Empty group'.", "Add parameter reference", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addParameterReference(GraphNodeParameterReferenceGroup group) {
        List<Object> selected = ParameterTreeUI.showPickerDialog(this, parameterTree, "Add parameter");
        List<GraphNodeParameterReference> referenceList = new ArrayList<>();
        for (Object parameter : selected) {
            if (parameter != null) {
                for (JIPipeParameterAccess child : parameterTree.getAllChildParameters(parameter)) {
                    referenceList.add(new GraphNodeParameterReference(child, parameterTree));
                }
            }
        }
        if (referenceList.isEmpty())
            return;
        group.addContent(referenceList);
        refreshContent(true, referenceList.get(0));
    }

    private void removeSelectedItems() {
        if (groupJTree.getSelectionPaths() != null) {
            for (TreePath path : groupJTree.getSelectionPaths()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof GraphNodeParameterReference) {
                    GraphNodeParameterReferenceGroup group = (GraphNodeParameterReferenceGroup) path.getParentPath().getLastPathComponent();
                    group.removeContent((GraphNodeParameterReference) node.getUserObject());
                }
            }
            for (TreePath path : groupJTree.getSelectionPaths()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.getUserObject() instanceof GraphNodeParameterReferenceGroup) {
                    parameters.removeGroup((GraphNodeParameterReferenceGroup) node.getUserObject());
                }
            }
        }
        refreshContent(true, null);
    }

    private void addWholeNode() {
        JIPipeGraphNode algorithm = PickNodeDialog.showDialog(this,
                parameters.getGraph().getGraphNodes(),
                null, "Add all parameters of node");
        if (algorithm != null) {
            List<GraphNodeParameterReferenceGroup> groupList = new ArrayList<>();
            Stack<JIPipeParameterCollection> collectionStack = new Stack<>();
            collectionStack.push(algorithm);
            while (!collectionStack.isEmpty()) {
                JIPipeParameterCollection top = collectionStack.pop();
                GraphNodeParameterReferenceGroup group = new GraphNodeParameterReferenceGroup();
                group.setName(algorithm.getName());
                group.setDescription(algorithm.getCustomDescription());

                JIPipeParameterTree.Node node = parameterTree.getSourceNode(top);
                for (JIPipeParameterAccess parameter : node.getParameters().values()) {
                    if (algorithm.isParameterUIVisible(parameterTree, parameter)) {
                        GraphNodeParameterReference reference = new GraphNodeParameterReference(parameter, parameterTree);
                        group.addContent(reference);
                    }
                }
                for (JIPipeParameterTree.Node childNode : node.getChildren().values()) {
                    collectionStack.push(childNode.getCollection());
                }

                groupList.add(group);
            }

            parameters.addGroups(groupList);
        }
        if (!withRefresh) {
            refreshContent(true, null);
        }
    }

    private void addEmptyGroup() {
        GraphNodeParameterReferenceGroup group = parameters.addNewGroup();
        if (!withRefresh) {
            refreshContent(true, group);
        }
    }

    public void refreshContent(boolean selectAfterwards, Object selectedObject) {
        if (selectedObject == null && groupJTree.getSelectionPath() != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) groupJTree.getSelectionPath().getLastPathComponent();
            selectedObject = node.getUserObject();
        }

        DefaultMutableTreeNode toSelect = null;

        parameterTree = getParameters().getGraph().getParameterTree(false, parameters.getUiRestrictToCompartments());
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        for (GraphNodeParameterReferenceGroup referenceGroup : parameters.getParameterReferenceGroups()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(referenceGroup);
            for (GraphNodeParameterReference parameterReference : referenceGroup.getContent()) {
                DefaultMutableTreeNode referenceNode = new DefaultMutableTreeNode(parameterReference);
                groupNode.add(referenceNode);

                if (parameterReference == selectedObject) {
                    toSelect = referenceNode;
                }
            }

            if (referenceGroup == selectedObject) {
                toSelect = groupNode;
            }
            rootNode.add(groupNode);
        }
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        groupJTree.setModel(model);
        noGroupsLabel.setVisible(parameters.getParameterReferenceGroups().isEmpty());
        UIUtils.expandAllTree(groupJTree);

        if (selectAfterwards && toSelect != null) {
            DefaultMutableTreeNode finalToSelect = toSelect;
            SwingUtilities.invokeLater(() -> {
                groupJTree.setSelectionPath(new TreePath(finalToSelect.getPath()));
            });
        }
    }


    private void selectReference(GraphNodeParameterReference reference) {
        rightPanel.removeAll();
        reference.getEventBus().register(this);
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), reference, documentation, ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW);
        rightPanel.add(parameterPanel, BorderLayout.CENTER);
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void selectGroup(GraphNodeParameterReferenceGroup group) {
        rightPanel.removeAll();
        group.getEventBus().register(this);
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), group, documentation, ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_BELOW);
        rightPanel.add(parameterPanel, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton addButton = new JButton("Add parameter reference ...", UIUtils.getIconFromResources("actions/add.png"));
        addButton.addActionListener(e -> addParameterReference(group));
        toolBar.add(addButton);

        rightPanel.add(toolBar, BorderLayout.NORTH);

        rightPanel.revalidate();
        rightPanel.repaint();
    }

    @Subscribe
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if (event.getSource() instanceof GraphNodeParameterReference || event.getSource() instanceof GraphNodeParameterReferenceGroup) {
            groupJTree.revalidate();
            groupJTree.repaint();
        }
    }

    /**
     * Triggered when the references were changed
     *
     * @param event the event
     */
    @Subscribe
    public void onParameterReferenceChanged(ParameterReferencesChangedEvent event) {
        if (withRefresh) {
            refreshContent(true, null);
        }
    }

    /**
     * Triggered when the parameter structure was changed
     *
     * @param event the event
     */
    @Subscribe
    public void onParameterStructureChanged(JIPipeParameterCollection.ParameterStructureChangedEvent event) {
        if (withRefresh) {
            refreshContent(true, null);
        }
    }

    /**
     * Triggered when the algorithm graph was changed
     *
     * @param event the event
     */
    @Subscribe
    public void onGraphStructureChanged(JIPipeGraph.GraphChangedEvent event) {
        if (withRefresh) {
            refreshContent(true, null);
        }
    }

    /**
     * @return The current parameter tree that was generated for this refresh cycle
     */
    public JIPipeParameterTree getParameterTree() {
        return parameterTree;
    }
}
