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

package org.hkijena.jipipe.ui.batchassistant;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.cache.JIPipeCacheStateTreeCellRenderer;
import org.hkijena.jipipe.ui.theme.ArrowLessScrollBarUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.Set;

/**
 * Displays a tree that contains data stored in a data batch
 */
public class DataBatchTree extends JIPipeWorkbenchPanel {
    private final JIPipeMergingDataBatch dataBatch;
    private JScrollPane treeScollPane;
    private JTree tree;

    /**
     * @param workbenchUI Workbench ui
     * @param dataBatch   the data batch
     */
    public DataBatchTree(JIPipeWorkbench workbenchUI, JIPipeMergingDataBatch dataBatch) {
        super(workbenchUI);
        this.dataBatch = dataBatch;
        initialize();
        refreshTree();
    }

    /**
     * Rebuilds the tree
     */
    public void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        for (Map.Entry<JIPipeDataSlot, Set<Integer>> slotEntry : dataBatch.getInputSlotRows().entrySet()) {
            DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(slotEntry.getKey());
            root.add(slotNode);
        }

        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        UIUtils.expandAllTree(tree);

        tree.setSelectionPath(new TreePath(root.getPath()));

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
}
