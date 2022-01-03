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

package org.hkijena.jipipe.api.grouping.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PickNodeDialog;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Editor component for {@link GraphNodeParameters}
 */
public class GraphNodeParametersUI extends JIPipeWorkbenchPanel {

    private final GraphNodeParameters parameters;
    private final int formPanelFlags;
    private FormPanel content;
    private JIPipeParameterTree tree;
    private boolean withRefresh;

    /**
     * @param workbench      the workbench
     * @param parameters     the parameters to edit
     * @param formPanelFlags flags for the form panel
     * @param withRefresh    if the editor should refresh on changes
     */
    public GraphNodeParametersUI(JIPipeWorkbench workbench, GraphNodeParameters parameters, int formPanelFlags, boolean withRefresh) {
        super(workbench);
        this.parameters = parameters;
        this.formPanelFlags = formPanelFlags;
        this.withRefresh = withRefresh;
        parameters.getEventBus().register(this);
        parameters.getGraph().getEventBus().register(this);
        initialize();
        refreshContent();
    }

    public GraphNodeParameters getParameters() {
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

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton refreshButton = new JButton(UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.setToolTipText("Refresh");
        refreshButton.addActionListener(e -> refreshContent());
        toolBar.add(refreshButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton autoAddAlgorithmButton = new JButton("Auto add algorithm", UIUtils.getIconFromResources("actions/configure.png"));
        autoAddAlgorithmButton.setToolTipText("Adds a group based on an algorithm");
        autoAddAlgorithmButton.addActionListener(e -> autoAddAlgorithm());
        toolBar.add(autoAddAlgorithmButton);

        JButton addEmptyGroupButton = new JButton("Add group", UIUtils.getIconFromResources("actions/list-add.png"));
        addEmptyGroupButton.setToolTipText("Adds a new group");
        addEmptyGroupButton.addActionListener(e -> addEmptyGroup());
        toolBar.add(addEmptyGroupButton);

        add(toolBar, BorderLayout.NORTH);

        content = new FormPanel(null, formPanelFlags);
        content.setBorder(null);
        add(content, BorderLayout.CENTER);
    }

    private void autoAddAlgorithm() {
        JIPipeGraphNode algorithm = PickNodeDialog.showDialog(this,
                parameters.getGraph().getGraphNodes(),
                null, "Add parameters of algorithm");
        if (algorithm != null) {
            List<GraphNodeParameterReferenceGroup> groupList = new ArrayList<>();
            Stack<JIPipeParameterCollection> collectionStack = new Stack<>();
            collectionStack.push(algorithm);
            while (!collectionStack.isEmpty()) {
                JIPipeParameterCollection top = collectionStack.pop();
                GraphNodeParameterReferenceGroup group = new GraphNodeParameterReferenceGroup();
                group.setName(algorithm.getName());
                group.setDescription(algorithm.getCustomDescription());

                JIPipeParameterTree.Node node = tree.getSourceNode(top);
                for (JIPipeParameterAccess parameter : node.getParameters().values()) {
                    if (algorithm.isParameterUIVisible(tree, parameter)) {
                        GraphNodeParameterReference reference = new GraphNodeParameterReference(parameter, tree);
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
        tree = getParameters().getGraph().getParameterTree(false);
        int scrollValue = 0;
        if (content.getScrollPane() != null) {
            scrollValue = content.getScrollPane().getVerticalScrollBar().getValue();
        }
        content.clear();
        for (GraphNodeParameterReferenceGroup referenceGroup : parameters.getParameterReferenceGroups()) {
            GraphNodeParameterReferenceGroupUI groupUI = new GraphNodeParameterReferenceGroupUI(this, referenceGroup);
            JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> {
                parameters.removeGroup(referenceGroup);
                if (!isWithRefresh()) {
                    refreshContent();
                }
            });
            content.addToForm(groupUI, removeButton, null);
        }
        content.addVerticalGlue();
        if (content.getScrollPane() != null) {
            int finalScrollValue = scrollValue;
            SwingUtilities.invokeLater(() -> content.getScrollPane().getVerticalScrollBar().setValue(finalScrollValue));
        }
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
    public JIPipeParameterTree getTree() {
        return tree;
    }
}
