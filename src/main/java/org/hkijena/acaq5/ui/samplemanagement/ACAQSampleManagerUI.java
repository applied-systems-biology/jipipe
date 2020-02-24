package org.hkijena.acaq5.ui.samplemanagement;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProjectSample;
import org.hkijena.acaq5.api.events.SampleAddedEvent;
import org.hkijena.acaq5.api.events.SampleRemovedEvent;
import org.hkijena.acaq5.api.events.SampleRenamedEvent;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.stream.Collectors;

public class ACAQSampleManagerUI extends ACAQUIPanel {

    private JTree sampleTree;

    public ACAQSampleManagerUI(ACAQWorkbenchUI workbenchUI) {
        super(workbenchUI);

        // Register events
        getWorkbenchUI().getProject().getEventBus().register(this);
        initialize();

        rebuildSampleListTree();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        sampleTree = new JTree();
        sampleTree.setCellRenderer(new ACAQSampleTreeCellRenderer());
        add(new JScrollPane(sampleTree), BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.NORTH);

        JButton addSamplesButton = new JButton("Add ...", UIUtils.getIconFromResources("add.png"));
        addSamplesButton.addActionListener(e -> addSamples());
        toolBar.add(addSamplesButton);

        JButton batchImportSamplesButton = new JButton("Batch import ...", UIUtils.getIconFromResources("import.png"));
        batchImportSamplesButton.addActionListener(e -> batchImportSamples());
        toolBar.add(batchImportSamplesButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton removeButton = new JButton( UIUtils.getIconFromResources("delete.png"));
        removeButton.setToolTipText("Remove selected samples");
        toolBar.add(removeButton);
    }

    private void addSamples() {
        ACAQAddSamplesDialog dialog = new ACAQAddSamplesDialog(getWorkbenchUI());
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(500,400));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void batchImportSamples() {
    }

    public void rebuildSampleListTree() {

        ACAQProjectSample selectedSample = null;
        if(sampleTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode nd = (DefaultMutableTreeNode) sampleTree.getLastSelectedPathComponent();
            if(nd.getUserObject() instanceof ACAQProjectSample) {
                selectedSample = (ACAQProjectSample)nd.getUserObject();
            }
        }

        DefaultMutableTreeNode toSelect = null;

        String rootNodeName = "Samples";
        if(getProject().getSamples().isEmpty()) {
            rootNodeName = "No samples";
        }
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootNodeName);
        for(ACAQProjectSample sample : getProject().getSamples().values().stream().sorted().collect(Collectors.toList())) {
            DefaultMutableTreeNode sampleNode = new DefaultMutableTreeNode(sample);
            if(sample == selectedSample) {
                toSelect = sampleNode;
            }
            rootNode.add(sampleNode);
        }

        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        sampleTree.setModel(model);
        UIUtils.expandAllTree(sampleTree);
        if(toSelect != null) {
            sampleTree.setSelectionPath(new TreePath(model.getPathToRoot(toSelect)));
        }

    }

    @Subscribe
    public void onSampleAdded(SampleAddedEvent event) {
        rebuildSampleListTree();
    }

    @Subscribe
    public void onSampleRemoved(SampleRemovedEvent event) {
        rebuildSampleListTree();
    }

    @Subscribe
    public void onSampleRenamed(SampleRenamedEvent event) {
        rebuildSampleListTree();
    }

    public JTree getSampleTree() {
        return sampleTree;
    }
}
