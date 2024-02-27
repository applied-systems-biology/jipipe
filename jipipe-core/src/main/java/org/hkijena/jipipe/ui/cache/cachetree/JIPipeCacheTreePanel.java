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

package org.hkijena.jipipe.ui.cache.cachetree;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays a tree that contains all cached items of a state
 */
public class JIPipeCacheTreePanel extends JIPipeProjectWorkbenchPanel {
    private JScrollPane treeScollPane;
    private JTree tree;
    private SearchTextField searchTextField;

    /**
     * @param workbenchUI Workbench ui
     */
    public JIPipeCacheTreePanel(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        refreshTree();
    }

    /**
     * Rebuilds the tree
     */
    public void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        Map<UUID, Map<JIPipeGraphNode, List<JIPipeDataSlot>>> byCompartmentId = new HashMap<>();
        for (JIPipeGraphNode node : getProject().getGraph().getGraphNodes()) {
            if (!(node instanceof JIPipeAlgorithm))
                continue;

            Map<String, JIPipeDataTable> slotMap = getProject().getCache().query(node, node.getUUIDInParentGraph(), new JIPipeProgressInfo());

            if (slotMap == null || slotMap.isEmpty())
                continue;

            UUID compartmentId = node.getCompartmentUUIDInParentGraph();
            Map<JIPipeGraphNode, List<JIPipeDataSlot>> algorithmMap = byCompartmentId.getOrDefault(compartmentId, null);
            if (algorithmMap == null) {
                algorithmMap = new HashMap<>();
                byCompartmentId.put(compartmentId, algorithmMap);
            }

            // We need to convert them to slots (required by the renderer)
            List<JIPipeDataSlot> slots = new ArrayList<>();
            for (Map.Entry<String, JIPipeDataTable> slotEntry : slotMap.entrySet()) {
                JIPipeDataSlot slot = new JIPipeOutputDataSlot(new JIPipeDataSlotInfo(slotEntry.getValue().getAcceptedDataType(), JIPipeSlotType.Output, slotEntry.getKey(), ""), null);
                slot.addDataFromTable(slotEntry.getValue(), new JIPipeProgressInfo());
                slots.add(slot);
            }

            algorithmMap.put(node, slots);

        }


        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        Set<UUID> coveredCompartments = new HashSet<>();
        for (JIPipeProjectCompartment compartment : getProject().getCompartmentGraph().traverse()
                .stream().filter(a -> a instanceof JIPipeProjectCompartment).map(a -> (JIPipeProjectCompartment) a).collect(Collectors.toList())) {
            createCompartmentNode(root, byCompartmentId.getOrDefault(compartment.getProjectCompartmentUUID(), Collections.emptyMap()), compartment.getProjectCompartmentUUID());
            coveredCompartments.add(compartment.getProjectCompartmentUUID());
        }
        for (Map.Entry<UUID, Map<JIPipeGraphNode, List<JIPipeDataSlot>>> entry : byCompartmentId.entrySet()) {
            if (!coveredCompartments.contains(entry.getKey())) {
                createCompartmentNode(root, byCompartmentId.getOrDefault(entry.getKey(), Collections.emptyMap()), entry.getKey());
            }
        }

        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        UIUtils.expandAllTree(tree);

        tree.getSelectionModel().setSelectionPath(new TreePath(root));

        treeScollPane.getVerticalScrollBar().setValue(scrollPosition);
    }

    /**
     * Selects a node by state and slot
     *
     * @param dataSlot the slot
     */
    public void selectDataSlot(JIPipeDataSlot dataSlot) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if (node.getUserObject() instanceof JIPipeDataSlot) {
                JIPipeDataSlot cacheSlot = (JIPipeDataSlot) node.getUserObject();
                if (cacheSlot.getNode() == dataSlot.getNode() && Objects.equals(cacheSlot.getName(), dataSlot.getName())) {
                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                    tree.setSelectionPath(new TreePath(model.getPathToRoot(node)));
                    return;
                }
            }
        }
    }

    private void createCompartmentNode(DefaultMutableTreeNode root, Map<JIPipeGraphNode, List<JIPipeDataSlot>> algorithms, UUID compartmentUUID) {
        JIPipeProjectCompartment projectCompartment = getProject().getCompartments().getOrDefault(compartmentUUID, null);
        if (projectCompartment == null)
            return;
        DefaultMutableTreeNode compartmentNode = new DefaultMutableTreeNode(projectCompartment.getName());

        boolean compartmentMatches = searchTextField.test(projectCompartment.getName());
        boolean compartmentHasMatchingChild = false;

        for (Map.Entry<JIPipeGraphNode, List<JIPipeDataSlot>> algorithmEntry : algorithms.entrySet()) {
            DefaultMutableTreeNode algorithmNode = new DefaultMutableTreeNode(algorithmEntry.getKey());

            boolean algorithmMatches = compartmentMatches || searchTextField.test(algorithmEntry.getKey().getName());
            compartmentHasMatchingChild |= algorithmMatches;
            boolean algorithmHasMatchingChild = false;

            for (JIPipeDataSlot dataSlot : algorithmEntry.getValue()) {
                DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(dataSlot);
                if (searchTextField.test(dataSlot.getName())) {
                    algorithmHasMatchingChild = true;
                    compartmentHasMatchingChild = true;
                    algorithmNode.add(slotNode);
                }
            }

            if (algorithmNode.getChildCount() > 0 && (algorithmMatches || algorithmHasMatchingChild))
                compartmentNode.add(algorithmNode);
        }

        if (compartmentNode.getChildCount() > 0 && (compartmentMatches || compartmentHasMatchingChild))
            root.add(compartmentNode);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JTree();
        tree.setCellRenderer(new JIPipeCacheStateTreeCellRenderer());
        treeScollPane = new JScrollPane(tree);
        add(treeScollPane, BorderLayout.CENTER);

        searchTextField = new SearchTextField();
        searchTextField.addActionListener(e -> refreshTree());
        add(searchTextField, BorderLayout.NORTH);
    }

    /**
     * @return The tree component
     */
    public JTree getTree() {
        return tree;
    }
}
