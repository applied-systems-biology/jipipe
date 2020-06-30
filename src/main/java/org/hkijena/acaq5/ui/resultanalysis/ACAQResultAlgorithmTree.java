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

package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
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
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Displays a tree where the user can select data slots
 */
public class ACAQResultAlgorithmTree extends ACAQProjectWorkbenchPanel {
    private ACAQRun run;
    private JScrollPane treeScollPane;
    private JTree tree;

    /**
     * @param workbenchUI Workbench ui
     * @param run         The run
     */
    public ACAQResultAlgorithmTree(ACAQProjectWorkbench workbenchUI, ACAQRun run) {
        super(workbenchUI);
        this.run = run;
        initialize();
        refreshTree();
    }

    private void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
        for (ACAQProjectCompartment compartment : run.getProject().getCompartmentGraph().traverseAlgorithms()
                .stream().map(a -> (ACAQProjectCompartment) a).collect(Collectors.toList())) {
            DefaultMutableTreeNode compartmentNode = new DefaultMutableTreeNode(compartment);
            for (ACAQGraphNode algorithm : run.getGraph().traverseAlgorithms()) {
                if (algorithm.getCompartment().equals(compartment.getProjectCompartmentId())) {
                    DefaultMutableTreeNode algorithmNode = new DefaultMutableTreeNode(algorithm);
                    for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                        if (!Files.exists(outputSlot.getStoragePath().resolve("data-table.json")))
                            continue;
                        DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(outputSlot);
                        algorithmNode.add(slotNode);
                    }
                    if (algorithmNode.getChildCount() > 0)
                        compartmentNode.add(algorithmNode);
                }
            }
            if (compartmentNode.getChildCount() > 0)
                root.add(compartmentNode);
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
        tree.setCellRenderer(new ACAQResultTreeCellRenderer());
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
