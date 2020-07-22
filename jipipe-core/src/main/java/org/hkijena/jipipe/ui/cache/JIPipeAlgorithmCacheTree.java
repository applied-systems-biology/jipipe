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

package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.ArrowLessScrollBarUI;
import org.hkijena.jipipe.utils.CustomScrollPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Map;

/**
 * Displays a tree that contains all cached items of a state
 */
public class JIPipeAlgorithmCacheTree extends JIPipeProjectWorkbenchPanel {
    private final JIPipeGraphNode graphNode;
    private JScrollPane treeScollPane;
    private JTree tree;

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

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        Map<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateMap = getProject().getCache().extract((JIPipeAlgorithm) graphNode);
        if (stateMap != null) {
            for (Map.Entry<JIPipeProjectCache.State, Map<String, JIPipeDataSlot>> stateEntry : stateMap.entrySet()) {
                DefaultMutableTreeNode stateNode = new DefaultMutableTreeNode(stateEntry.getKey());

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

        tree.getSelectionModel().setSelectionPath(new TreePath(root));

        treeScollPane.getVerticalScrollBar().setValue(scrollPosition);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JTree();
        tree.setCellRenderer(new JIPipeCacheStateTreeCellRenderer());
        treeScollPane = new CustomScrollPane(tree);
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
}
