package org.hkijena.acaq5.ui.cache;

import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays a tree that contains all cached items of a state
 */
public class ACAQCacheTree extends ACAQProjectWorkbenchPanel {
    private JScrollPane treeScollPane;
    private JTree tree;

    /**
     * @param workbenchUI Workbench ui
     */
    public ACAQCacheTree(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        refreshTree();
    }

    /**
     * Rebuilds the tree
     */
    public void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        Map<String, Map<ACAQGraphNode, Map<ACAQProjectCache.State, List<ACAQDataSlot>>>> byCompartmentId = new HashMap<>();
        for (ACAQGraphNode node : getProject().getGraph().getAlgorithmNodes().values()) {
            Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap = getProject().getCache().extract((ACAQAlgorithm) node);
            if (stateMap == null)
                continue;

            for (Map.Entry<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateEntry : stateMap.entrySet()) {
                Map<String, ACAQDataSlot> slots = stateMap.getOrDefault(stateEntry.getKey(), null);
                if (slots == null)
                    continue;
                String compartmentId = node.getCompartment();
                Map<ACAQGraphNode, Map<ACAQProjectCache.State, List<ACAQDataSlot>>> algorithmMap = byCompartmentId.getOrDefault(compartmentId, null);
                if (algorithmMap == null) {
                    algorithmMap = new HashMap<>();
                    byCompartmentId.put(compartmentId, algorithmMap);
                }

                Map<ACAQProjectCache.State, List<ACAQDataSlot>> stateMap2 = algorithmMap.getOrDefault(node, null);
                if (stateMap2 == null) {
                    stateMap2 = new HashMap<>();
                    algorithmMap.put(node, stateMap2);
                }

                stateMap2.put(stateEntry.getKey(), new ArrayList<>(slots.values()));
            }

        }


        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        Set<String> coveredCompartments = new HashSet<>();
        for (ACAQProjectCompartment compartment : getProject().getCompartmentGraph().traverseAlgorithms()
                .stream().map(a -> (ACAQProjectCompartment) a).collect(Collectors.toList())) {
            createCompartmentNode(root, byCompartmentId.getOrDefault(compartment.getProjectCompartmentId(), Collections.emptyMap()), compartment.getName());
            coveredCompartments.add(compartment.getProjectCompartmentId());
        }
        for (Map.Entry<String, Map<ACAQGraphNode, Map<ACAQProjectCache.State, List<ACAQDataSlot>>>> entry : byCompartmentId.entrySet()) {
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
    public void selectDataSlot(ACAQProjectCache.State state, ACAQDataSlot dataSlot) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration enumeration = root.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if (node.getUserObject() instanceof ACAQDataSlot) {
                ACAQDataSlot cacheSlot = (ACAQDataSlot) node.getUserObject();
                if (cacheSlot.getAlgorithm() == dataSlot.getAlgorithm() && Objects.equals(cacheSlot.getName(), dataSlot.getName())) {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    ACAQProjectCache.State nodeState = (ACAQProjectCache.State) parent.getUserObject();
                    if (Objects.equals(nodeState, state)) {
                        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                        tree.setSelectionPath(new TreePath(model.getPathToRoot(node)));
                        return;
                    }
                }
            }
        }
    }

    private void createCompartmentNode(DefaultMutableTreeNode root, Map<ACAQGraphNode, Map<ACAQProjectCache.State, List<ACAQDataSlot>>> algorithms, String compartmentName) {
        DefaultMutableTreeNode compartmentNode = new DefaultMutableTreeNode(compartmentName);

        for (Map.Entry<ACAQGraphNode, Map<ACAQProjectCache.State, List<ACAQDataSlot>>> algorithmEntry : algorithms.entrySet()) {
            DefaultMutableTreeNode algorithmNode = new DefaultMutableTreeNode(algorithmEntry.getKey());

            for (ACAQProjectCache.State state : algorithmEntry.getValue().keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                DefaultMutableTreeNode stateNode = new DefaultMutableTreeNode(state);

                for (ACAQDataSlot slot : algorithmEntry.getValue().get(state)) {
                    DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(slot);
                    stateNode.add(slotNode);
                }

                if (stateNode.getChildCount() > 0)
                    algorithmNode.add(stateNode);
            }

            if (algorithmNode.getChildCount() > 0)
                compartmentNode.add(algorithmNode);
        }

        if (compartmentNode.getChildCount() > 0)
            root.add(compartmentNode);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JTree();
        tree.setCellRenderer(new ACAQCacheStateTreeCellRenderer());
        treeScollPane = new JScrollPane(tree);
        add(treeScollPane, BorderLayout.CENTER);
    }

    /**
     * @return The tree component
     */
    public JTree getTree() {
        return tree;
    }
}
