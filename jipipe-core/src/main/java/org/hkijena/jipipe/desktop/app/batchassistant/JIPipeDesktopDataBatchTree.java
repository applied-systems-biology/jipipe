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

package org.hkijena.jipipe.desktop.app.batchassistant;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.cache.cachetree.JIPipeDesktopCacheStateTreeCellRenderer;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopArrowLessScrollBarUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Map;
import java.util.Set;

/**
 * Displays a tree that contains data stored in a iteration step
 */
public class JIPipeDesktopDataBatchTree extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeMultiIterationStep iterationStep;
    private JScrollPane treeScollPane;
    private JTree tree;

    /**
     * @param workbenchUI   Workbench ui
     * @param iterationStep the iteration step
     */
    public JIPipeDesktopDataBatchTree(JIPipeDesktopWorkbench workbenchUI, JIPipeMultiIterationStep iterationStep) {
        super(workbenchUI);
        this.iterationStep = iterationStep;
        initialize();
        refreshTree();
    }

    /**
     * Rebuilds the tree
     */
    public void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);

        for (Map.Entry<JIPipeDataSlot, Set<Integer>> slotEntry : iterationStep.getInputSlotRows().entrySet()) {
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
        tree.setCellRenderer(new JIPipeDesktopCacheStateTreeCellRenderer());
        treeScollPane = new JScrollPane(tree);
        treeScollPane.getVerticalScrollBar().setUI(new JIPipeDesktopArrowLessScrollBarUI());
        treeScollPane.getHorizontalScrollBar().setUI(new JIPipeDesktopArrowLessScrollBarUI());
        add(treeScollPane, BorderLayout.CENTER);
    }

    /**
     * @return The tree component
     */
    public JTree getTree() {
        return tree;
    }
}
