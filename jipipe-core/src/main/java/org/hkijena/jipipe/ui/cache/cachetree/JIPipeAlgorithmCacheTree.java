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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.JIPipeProjectCacheState;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.theme.ArrowLessScrollBarUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;

/**
 * Displays a tree that contains all cached items of a state
 */
public class JIPipeAlgorithmCacheTree extends JIPipeProjectWorkbenchPanel {
    private final JIPipeGraphNode graphNode;
    private JScrollPane treeScollPane;
    private JTree tree;
    private BiMap<JIPipeProjectCacheState, DefaultMutableTreeNode> stateTreeNodeMap = HashBiMap.create();

    /**
     * @param workbenchUI Workbench ui
     * @param graphNode   the targeted algorithm
     */
    public JIPipeAlgorithmCacheTree(JIPipeProjectWorkbench workbenchUI, JIPipeGraphNode graphNode) {
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

        stateTreeNodeMap.clear();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        Map<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract(graphNode.getUUIDInParentGraph());
        if (stateMap != null) {
            for (Map.Entry<JIPipeProjectCacheState, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                DefaultMutableTreeNode stateNode = new DefaultMutableTreeNode(stateEntry.getKey());
                stateTreeNodeMap.put(stateEntry.getKey(), stateNode);

                for (JIPipeDataSlot slot : stateEntry.getValue().values()) {
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

//        tree.getSelectionModel().setSelectionPath(new TreePath(root));
        selectNewestState();

        treeScollPane.getVerticalScrollBar().setValue(scrollPosition);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JTree();
        tree.setCellRenderer(new JIPipeCacheStateTreeCellRenderer());
        treeScollPane = new JScrollPane(tree);
        treeScollPane.getVerticalScrollBar().setUI(new ArrowLessScrollBarUI());
        treeScollPane.getHorizontalScrollBar().setUI(new ArrowLessScrollBarUI());
        add(treeScollPane, BorderLayout.CENTER);
    }

    /**
     * @return The tree component
     */
    public JTree getTree() {
        return tree;
    }

    public void selectNewestState() {
        if (!stateTreeNodeMap.isEmpty()) {
            JIPipeProjectCacheState state = stateTreeNodeMap.keySet().stream().max(Comparator.naturalOrder()).get();
            DefaultMutableTreeNode treeNode = stateTreeNodeMap.get(state);
            tree.setSelectionPath(new TreePath(treeNode.getPath()));
        }
    }
}
