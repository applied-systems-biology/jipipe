package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQRunSample;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.stream.Collectors;

public class ACAQResultSampleManagerUI extends JPanel {
    private ACAQResultUI resultUI;
    private JTree sampleTree;

    public ACAQResultSampleManagerUI(ACAQResultUI resultUI) {
        this.resultUI = resultUI;
        initialize();
        rebuildSampleListTree();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        sampleTree = new JTree();
        sampleTree.setCellRenderer(new ACAQRunSampleTreeCellRenderer());
        add(new JScrollPane(sampleTree), BorderLayout.CENTER);
    }

    public ACAQRun getRun() {
        return resultUI.getRun();
    }

    private void rebuildSampleListTree() {
        String rootNodeName = "Samples";
        if(getRun().getSamples().isEmpty()) {
            rootNodeName = "No samples";
        }

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootNodeName);

        for(ACAQRunSample sample : getRun().getSamples().values().stream().sorted().collect(Collectors.toList())) {
            DefaultMutableTreeNode sampleNode = new DefaultMutableTreeNode(sample);
            rootNode.add(sampleNode);
        }

        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        sampleTree.setModel(model);
        UIUtils.expandAllTree(sampleTree);
    }


    public JTree getSampleTree() {
        return sampleTree;
    }
}
