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

package org.hkijena.acaq5.api.grouping.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.grouping.events.ParameterReferencesChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.PickAlgorithmDialog;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Editor component for {@link GraphNodeParameters}
 */
public class GraphNodeParametersUI extends ACAQWorkbenchPanel {

    private final GraphNodeParameters parameters;
    private FormPanel content;
    private ACAQParameterTree tree;

    /**
     * @param workbench  the workbench
     * @param parameters the parameters to edit
     */
    public GraphNodeParametersUI(ACAQWorkbench workbench, GraphNodeParameters parameters) {
        super(workbench);
        this.parameters = parameters;
        parameters.getEventBus().register(this);
        parameters.getGraph().getEventBus().register(this);
        initialize();
        refreshContent();
    }

    public GraphNodeParameters getParameters() {
        return parameters;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(Box.createHorizontalGlue());

        JButton autoAddAlgorithmButton = new JButton("Auto add algorithm", UIUtils.getIconFromResources("cog.png"));
        autoAddAlgorithmButton.setToolTipText("Adds a group based on an algorithm");
        autoAddAlgorithmButton.addActionListener(e -> autoAddAlgorithm());
        toolBar.add(autoAddAlgorithmButton);

        JButton addEmptyGroupButton = new JButton("Add group", UIUtils.getIconFromResources("add.png"));
        addEmptyGroupButton.setToolTipText("Adds a new group");
        addEmptyGroupButton.addActionListener(e -> addEmptyGroup());
        toolBar.add(addEmptyGroupButton);

        add(toolBar, BorderLayout.NORTH);

        content = new FormPanel(null, FormPanel.WITH_SCROLLING);
        content.setBorder(null);
        add(content, BorderLayout.CENTER);
    }

    private void autoAddAlgorithm() {
        ACAQGraphNode algorithm = PickAlgorithmDialog.showDialog(this,
                parameters.getGraph().getAlgorithmNodes().values(),
                "Add parameters of algorithm");
        if (algorithm != null) {
            List<GraphNodeParameterReferenceGroup> groupList = new ArrayList<>();
            Stack<ACAQParameterCollection> collectionStack = new Stack<>();
            collectionStack.push(algorithm);
            while (!collectionStack.isEmpty()) {
                ACAQParameterCollection top = collectionStack.pop();
                GraphNodeParameterReferenceGroup group = new GraphNodeParameterReferenceGroup();
                group.setName(algorithm.getName());
                group.setDescription(algorithm.getCustomDescription());

                ACAQParameterTree.Node node = tree.getSourceNode(top);
                for (ACAQParameterAccess parameter : node.getParameters().values()) {
                    if (parameter.getVisibility().isVisibleIn(ACAQParameterVisibility.TransitiveVisible)) {
                        GraphNodeParameterReference reference = new GraphNodeParameterReference(parameter, tree);
                        group.addContent(reference);
                    }
                }
                for (ACAQParameterTree.Node childNode : node.getChildren().values()) {
                    collectionStack.push(childNode.getCollection());
                }

                groupList.add(group);
            }

            parameters.addGroups(groupList);
        }
    }

    private void addEmptyGroup() {
        parameters.addNewGroup();
    }

    private void refreshContent() {
        tree = getParameters().getGraph().getParameterTree();
        int scrollValue = content.getScrollPane().getVerticalScrollBar().getValue();
        content.clear();
        for (GraphNodeParameterReferenceGroup referenceGroup : parameters.getParameterReferenceGroups()) {
            GraphNodeParameterReferenceGroupUI groupUI = new GraphNodeParameterReferenceGroupUI(this, referenceGroup);
            JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
            UIUtils.makeBorderlessWithoutMargin(removeButton);
            removeButton.addActionListener(e -> parameters.removeGroup(referenceGroup));
            content.addToForm(groupUI, removeButton, null);
        }
        content.addVerticalGlue();
        SwingUtilities.invokeLater(() -> content.getScrollPane().getVerticalScrollBar().setValue(scrollValue));
    }

    /**
     * Triggered when the references were changed
     *
     * @param event the event
     */
    @Subscribe
    public void onParameterReferenceChanged(ParameterReferencesChangedEvent event) {
        refreshContent();
    }

    /**
     * Triggered when the parameter structure was changed
     *
     * @param event the event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        refreshContent();
    }

    /**
     * Triggered when the algorithm graph was changed
     *
     * @param event the event
     */
    @Subscribe
    public void onGraphStructureChanged(AlgorithmGraphChangedEvent event) {
        refreshContent();
    }

    /**
     * @return The current parameter tree that was generated for this refresh cycle
     */
    public ACAQParameterTree getTree() {
        return tree;
    }
}
