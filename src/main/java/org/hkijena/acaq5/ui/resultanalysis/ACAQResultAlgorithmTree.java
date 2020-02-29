package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.stream.Collectors;

public class ACAQResultAlgorithmTree extends ACAQUIPanel {
    private ACAQRun run;
    private JScrollPane treeScollPane;
    private JTree tree;

    public ACAQResultAlgorithmTree(ACAQWorkbenchUI workbenchUI, ACAQRun run) {
        super(workbenchUI);
        this.run = run;
        initialize();
        refreshTree();
    }

    private void refreshTree() {
        int scrollPosition = treeScollPane.getVerticalScrollBar().getValue();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        for (ACAQProjectCompartment compartment : run.getProject().getCompartmentGraph().traverseAlgorithms()
                .stream().map(a -> (ACAQProjectCompartment)a).collect(Collectors.toList())) {
            DefaultMutableTreeNode compartmentNode = new DefaultMutableTreeNode(compartment);
            for (ACAQAlgorithm algorithm : run.getLastExecutedAlgorithms()) {
                if(algorithm.getCompartment().equals(compartment.getProjectCompartmentId())) {
                    DefaultMutableTreeNode algorithmNode = new DefaultMutableTreeNode(algorithm);
                    for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                        DefaultMutableTreeNode slotNode = new DefaultMutableTreeNode(outputSlot);
                        algorithmNode.add(slotNode);
                    }
                    compartmentNode.add(algorithmNode);
                }
            }
            root.add(compartmentNode);
        }
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        UIUtils.expandAllTree(tree);

        treeScollPane.getVerticalScrollBar().setValue(scrollPosition);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tree = new JTree();
        tree.setCellRenderer(new ACAQResultTreeCellRenderer());
        treeScollPane = new JScrollPane(tree);
        add(treeScollPane, BorderLayout.CENTER);
    }
}
