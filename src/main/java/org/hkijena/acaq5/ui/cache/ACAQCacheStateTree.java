package org.hkijena.acaq5.ui.cache;

import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultTreeCellRenderer;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Displays a tree that contains all cached items of a state
 */
public class ACAQCacheStateTree extends ACAQProjectWorkbenchPanel {
    private final ACAQProjectCache.State state;
    private JScrollPane treeScollPane;
    private JTree tree;

    /**
     * @param workbenchUI Workbench ui
     * @param state the state that is displayed
     */
    public ACAQCacheStateTree(ACAQProjectWorkbench workbenchUI, ACAQProjectCache.State state) {
        super(workbenchUI);
        this.state = state;
        initialize();
        refreshTree();
    }

    private void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        Map<String, Map<ACAQGraphNode, List<ACAQDataSlot>>> byCompartmentId = new HashMap<>();
        for (ACAQGraphNode node : getProject().getGraph().getAlgorithmNodes().values()) {
            Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap = getProject().getCache().extract((ACAQAlgorithm) node);
            if(stateMap == null)
                continue;
            Map<String, ACAQDataSlot> slots = stateMap.getOrDefault(state, null);
            if(slots == null)
                continue;
            String compartmentId = node.getCompartment();

            Map<ACAQGraphNode, List<ACAQDataSlot>> algorithmMap = byCompartmentId.getOrDefault(compartmentId, null);
            if(algorithmMap == null) {
                algorithmMap = new HashMap<>();
                byCompartmentId.put(compartmentId, algorithmMap);
            }

            algorithmMap.put(node, new ArrayList<>(slots.values()));
        }


        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        Set<String> coveredCompartments = new HashSet<>();
        for (ACAQProjectCompartment compartment : getProject().getCompartmentGraph().traverseAlgorithms()
                .stream().map(a -> (ACAQProjectCompartment) a).collect(Collectors.toList())) {
            createCompartmentNode(root, byCompartmentId.getOrDefault(compartment.getProjectCompartmentId(), Collections.emptyMap()), compartment.getName());
            coveredCompartments.add(compartment.getProjectCompartmentId());
        }
        for (Map.Entry<String, Map<ACAQGraphNode, List<ACAQDataSlot>>> entry : byCompartmentId.entrySet()) {
            if(!coveredCompartments.contains(entry.getKey())) {
                createCompartmentNode(root, byCompartmentId.getOrDefault(entry.getKey(), Collections.emptyMap()), entry.getKey());
            }
        }

        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        UIUtils.expandAllTree(tree);

        tree.getSelectionModel().setSelectionPath(new TreePath(root));

        treeScollPane.getVerticalScrollBar().setValue(scrollPosition);
    }

    private void createCompartmentNode(DefaultMutableTreeNode root, Map<ACAQGraphNode, List<ACAQDataSlot>> algorithms, String compartmentName) {
        DefaultMutableTreeNode compartmentNode = new DefaultMutableTreeNode(compartmentName);

        for (Map.Entry<ACAQGraphNode, List<ACAQDataSlot>> algorithmEntry : algorithms.entrySet()) {
            DefaultMutableTreeNode algorithmNode = new DefaultMutableTreeNode(algorithmEntry.getKey());

            for (ACAQDataSlot slot : algorithmEntry.getValue()) {
                DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(slot);
                algorithmNode.add(slotNode);
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
