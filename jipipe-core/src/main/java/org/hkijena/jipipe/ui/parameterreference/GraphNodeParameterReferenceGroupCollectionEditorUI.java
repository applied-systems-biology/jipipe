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
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.pickers.PickNodeDialog;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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
    private final FormPanel editorPanel;
    private JIPipeParameterTree parameterTree;
    private boolean withRefresh;

    /**
     * @param workbench      the workbench
     * @param parameters     the parameters to edit
     * @param withRefresh    if the editor should refresh on changes
     */
    public GraphNodeParameterReferenceGroupCollectionEditorUI(JIPipeWorkbench workbench, GraphNodeParameterReferenceGroupCollection parameters, MarkdownDocument documentation, boolean withRefresh) {
        super(workbench);
        this.parameters = parameters;
        this.withRefresh = withRefresh;
        this.editorPanel = new FormPanel(documentation != null ? documentation : MarkdownDocument.fromPluginResource("documentation/parameter-reference-editor.md", Collections.emptyMap()), FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION | FormPanel.DOCUMENTATION_BELOW);
        parameters.getEventBus().register(this);
        parameters.getGraph().getEventBus().register(this);
        initialize();
        refreshContent();
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
        splitPane.setRightComponent(editorPanel);
    }

    private void initializeLeftPanel(AutoResizeSplitPane splitPane) {
        add(splitPane, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout());
        splitPane.setLeftComponent(leftPanel);
        initializeToolbar(leftPanel);

        groupJTree.setCellRenderer(new ParameterReferenceGroupCollectionTreeCellRenderer(this));
        leftPanel.add(new JScrollPane(groupJTree), BorderLayout.CENTER);
    }

    private void initializeToolbar(JPanel leftPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton refreshButton = new JButton(UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.setToolTipText("Refresh");
        refreshButton.addActionListener(e -> refreshContent());
        toolBar.add(refreshButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton addGroupButton = new JButton("Add group", UIUtils.getIconFromResources("actions/list-add.png"));
        JPopupMenu popupMenu = UIUtils.addPopupMenuToComponent(addGroupButton);
        popupMenu.add(UIUtils.createMenuItem("Empty group", "Adds an empty group", UIUtils.getIconFromResources("actions/list-add.png"), this::addEmptyGroup));
        popupMenu.add(UIUtils.createMenuItem("Node as group", "Add all parameters within a node as group", UIUtils.getIconFromResources("data-types/node.png"), this::addWholeNode));
        toolBar.add(addGroupButton);

        JButton removeButton = new JButton("Remove", UIUtils.getIconFromResources("actions/delete.png"));
        removeButton.addActionListener(e -> removeSelectedItems());
        toolBar.add(removeButton);

        leftPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void removeSelectedItems() {

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
            refreshContent();
        }
    }

    private void addEmptyGroup() {
        parameters.addNewGroup();
        if (!withRefresh) {
            refreshContent();
        }
    }

    public void refreshContent() {
        parameterTree = getParameters().getGraph().getParameterTree(false);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        for (GraphNodeParameterReferenceGroup referenceGroup : parameters.getParameterReferenceGroups()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(referenceGroup);
            for (GraphNodeParameterReference parameterReference : referenceGroup.getContent()) {
                   DefaultMutableTreeNode referenceNode = new DefaultMutableTreeNode(parameterReference);
                   groupNode.add(referenceNode);
            }
            rootNode.add(groupNode);
        }
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        groupJTree.setModel(model);
    }

    /**
     * Triggered when the references were changed
     *
     * @param event the event
     */
    @Subscribe
    public void onParameterReferenceChanged(ParameterReferencesChangedEvent event) {
        if (withRefresh)
            refreshContent();
    }

    /**
     * Triggered when the parameter structure was changed
     *
     * @param event the event
     */
    @Subscribe
    public void onParameterStructureChanged(JIPipeParameterCollection.ParameterStructureChangedEvent event) {
        if (withRefresh)
            refreshContent();
    }

    /**
     * Triggered when the algorithm graph was changed
     *
     * @param event the event
     */
    @Subscribe
    public void onGraphStructureChanged(JIPipeGraph.GraphChangedEvent event) {
        if (withRefresh)
            refreshContent();
    }

    /**
     * @return The current parameter tree that was generated for this refresh cycle
     */
    public JIPipeParameterTree getParameterTree() {
        return parameterTree;
    }
}
