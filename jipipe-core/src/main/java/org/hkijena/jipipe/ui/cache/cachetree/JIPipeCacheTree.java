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

package org.hkijena.jipipe.ui.cache.cachetree;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
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
public class JIPipeCacheTree extends JIPipeProjectWorkbenchPanel {
    private JScrollPane treeScollPane;
    private JTree tree;
    private SearchTextField searchTextField;

    /**
     * @param workbenchUI Workbench ui
     */
    public JIPipeCacheTree(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        refreshTree();
    }

    /**
     * Rebuilds the tree
     */
    public void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        Map<UUID, Map<JIPipeGraphNode, Map<JIPipeProjectCacheState, List<JIPipeDataSlot>>>> byCompartmentId = new HashMap<>();
        for (JIPipeGraphNode node : getProject().getGraph().getGraphNodes()) {
            if (!(node instanceof JIPipeAlgorithm))
                continue;
            Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(node.getUUIDInParentGraph());
            if (stateMap == null)
                continue;

            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                Map<String, JIPipeDataSlot> slots = stateMap.getOrDefault(stateEntry.getKey(), null);
                if (slots == null)
                    continue;
                UUID compartmentId = node.getCompartmentUUIDInParentGraph();
                Map<JIPipeGraphNode, Map<JIPipeProjectCacheState, List<JIPipeDataSlot>>> algorithmMap = byCompartmentId.getOrDefault(compartmentId, null);
                if (algorithmMap == null) {
                    algorithmMap = new HashMap<>();
                    byCompartmentId.put(compartmentId, algorithmMap);
                }

                Map<JIPipeProjectCacheState, List<JIPipeDataSlot>> stateMap2 = algorithmMap.getOrDefault(node, null);
                if (stateMap2 == null) {
                    stateMap2 = new HashMap<>();
                    algorithmMap.put(node, stateMap2);
                }

                stateMap2.put(stateEntry.getKey(), new ArrayList<>(slots.values()));
            }

        }


        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        Set<UUID> coveredCompartments = new HashSet<>();
        for (JIPipeProjectCompartment compartment : getProject().getCompartmentGraph().traverse()
                .stream().filter(a -> a instanceof JIPipeProjectCompartment).map(a -> (JIPipeProjectCompartment) a).collect(Collectors.toList())) {
            createCompartmentNode(root, byCompartmentId.getOrDefault(compartment.getProjectCompartmentUUID(), Collections.emptyMap()), compartment.getProjectCompartmentUUID());
            coveredCompartments.add(compartment.getProjectCompartmentUUID());
        }
        for (Map.Entry<UUID, Map<JIPipeGraphNode, Map<JIPipeProjectCacheState, List<JIPipeDataSlot>>>> entry : byCompartmentId.entrySet()) {
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
     * @param state    the state
     * @param dataSlot the slot
     */
    public void selectDataSlot(JIPipeProjectCacheState state, JIPipeDataSlot dataSlot) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if (node.getUserObject() instanceof JIPipeDataSlot) {
                JIPipeDataSlot cacheSlot = (JIPipeDataSlot) node.getUserObject();
                if (cacheSlot.getNode() == dataSlot.getNode() && Objects.equals(cacheSlot.getName(), dataSlot.getName())) {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    JIPipeProjectCacheState nodeState = (JIPipeProjectCacheState) parent.getUserObject();
                    if (Objects.equals(nodeState, state)) {
                        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                        tree.setSelectionPath(new TreePath(model.getPathToRoot(node)));
                        return;
                    }
                }
            }
        }
    }

    private void createCompartmentNode(DefaultMutableTreeNode root, Map<JIPipeGraphNode, Map<JIPipeProjectCacheState, List<JIPipeDataSlot>>> algorithms, UUID compartmentUUID) {
        JIPipeProjectCompartment projectCompartment = getProject().getCompartments().getOrDefault(compartmentUUID, null);
        if (projectCompartment == null)
            return;
        DefaultMutableTreeNode compartmentNode = new DefaultMutableTreeNode(projectCompartment.getName());

        boolean compartmentMatches = searchTextField.test(projectCompartment.getName());
        boolean compartmentHasMatchingChild = false;

        for (Map.Entry<JIPipeGraphNode, Map<JIPipeProjectCacheState, List<JIPipeDataSlot>>> algorithmEntry : algorithms.entrySet()) {
            DefaultMutableTreeNode algorithmNode = new DefaultMutableTreeNode(algorithmEntry.getKey());

            boolean algorithmMatches = compartmentMatches || searchTextField.test(algorithmEntry.getKey().getName());
            compartmentHasMatchingChild |= algorithmMatches;
            boolean algorithmHasMatchingChild = false;

            for (JIPipeProjectCacheState state : algorithmEntry.getValue().keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                DefaultMutableTreeNode stateNode = new DefaultMutableTreeNode(state);

                boolean stateMatches = algorithmMatches || searchTextField.test(state.renderGenerationTime());
                boolean stateHasMatchingChild = false;
                compartmentHasMatchingChild |= stateMatches;
                algorithmHasMatchingChild |= stateMatches;

                for (JIPipeDataSlot slot : algorithmEntry.getValue().get(state)) {
                    DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(slot);
                    if (searchTextField.test(slot.getName())) {
                        stateHasMatchingChild = true;
                        algorithmHasMatchingChild = true;
                        compartmentHasMatchingChild = true;
                        stateNode.add(slotNode);
                    }
                }

                if (stateNode.getChildCount() > 0 && (stateMatches || stateHasMatchingChild))
                    algorithmNode.add(stateNode);
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
