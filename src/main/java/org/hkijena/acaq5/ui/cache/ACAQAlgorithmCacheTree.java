package org.hkijena.acaq5.ui.cache;

import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Map;

/**
 * Displays a tree that contains all cached items of a state
 */
public class ACAQAlgorithmCacheTree extends ACAQProjectWorkbenchPanel {
    private final ACAQGraphNode graphNode;
    private JScrollPane treeScollPane;
    private JTree tree;

    /**
     * @param workbenchUI Workbench ui
     * @param graphNode   the targeted algorithm
     */
    public ACAQAlgorithmCacheTree(ACAQProjectWorkbench workbenchUI, ACAQGraphNode graphNode) {
        super(workbenchUI);
        this.graphNode = graphNode;
        initialize();
        refreshTree();
    }

    /**
     * Rebuilds the tree
     */
    public void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        Map<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateMap = getProject().getCache().extract((ACAQAlgorithm) graphNode);
        if (stateMap != null) {
            for (Map.Entry<ACAQProjectCache.State, Map<String, ACAQDataSlot>> stateEntry : stateMap.entrySet()) {
                DefaultMutableTreeNode stateNode = new DefaultMutableTreeNode(stateEntry.getKey());

                for (ACAQDataSlot slot : stateEntry.getValue().values()) {
                    DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(slot);
                    stateNode.add(slotNode);
                }

                if (stateNode.getChildCount() > 0)
                    root.add(stateNode);
            }
        }

        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        UIUtils.expandAllTree(tree);

        tree.getSelectionModel().setSelectionPath(new TreePath(root));

        treeScollPane.getVerticalScrollBar().setValue(scrollPosition);
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
