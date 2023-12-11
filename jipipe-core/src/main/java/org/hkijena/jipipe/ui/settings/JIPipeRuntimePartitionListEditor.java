package org.hkijena.jipipe.ui.settings;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JIPipeRuntimePartitionListEditor extends JIPipeProjectWorkbenchPanel {
    private final JList<JIPipeRuntimePartition> jList = new JList<>();

    public JIPipeRuntimePartitionListEditor(JIPipeProjectWorkbench workbench) {
        super(workbench);
        initialize();
        refresh();
    }
    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createButton("Add", UIUtils.getIconFromResources("actions/add.png"), this::addNewItem));
        toolBar.add(UIUtils.createButton("Remove", UIUtils.getIconFromResources("actions/trash-empty.png"), this::removeSelectedItems));
        toolBar.addSeparator();
        toolBar.add(UIUtils.createButton("Edit", UIUtils.getIconFromResources("actions/edit.png"), this::editSelectedItem));

        add(toolBar, BorderLayout.NORTH);

        jList.setCellRenderer(new JIPipeRuntimePartitionListCellRenderer(getProjectWorkbench().getProject().getRuntimePartitions()));
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editSelectedItem();
                }
            }
        });
        add(new JScrollPane(jList), BorderLayout.CENTER);
    }

    private void removeSelectedItems() {
        Set<Integer> indices = new HashSet<>();
        for (JIPipeRuntimePartition partition : jList.getSelectedValuesList()) {
            int idx = getProjectWorkbench().getProject().getRuntimePartitions().indexOf(partition);
            if(idx >= 0) {
                indices.add(idx);
            }
        }

        // The default cannot be removed
        indices.remove(0);

        // Check if any of the nodes use the partitions and warn them
        Set<Integer> usedPartitions = new HashSet<>();
        for (JIPipeGraphNode graphNode : getProject().getGraph().getGraphNodes()) {
            if(graphNode instanceof JIPipeAlgorithm) {
                usedPartitions.add(((JIPipeAlgorithm) graphNode).getRuntimePartition().getIndex());
            }
        }

        Sets.SetView<Integer> intersection = Sets.intersection(indices, usedPartitions);
        if(!intersection.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("The following partitions are currently used in the pipeline:\n\n");
            for (Integer usedIdx : intersection) {
                message.append("- ").append(getProjectWorkbench().getProject().getRuntimePartitions().get(usedIdx)).append("\n");
            }
            message.append("\nDo you really want to remove the selected partitions?");

            if(JOptionPane.showConfirmDialog(this, message.toString(), "Delete runtime partitions", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }

        List<Integer> sortedIndices = new ArrayList<>(indices);
        sortedIndices.sort(Comparator.naturalOrder());

        for (int i = sortedIndices.size() - 1; i >= 0; i--) {
            Integer sortedIndex = sortedIndices.get(i);
            getProject().getRuntimePartitions().remove(sortedIndex);
        }

        refresh();
    }

    private void addNewItem() {
        getProject().getRuntimePartitions().add();
        refresh();
    }

    private void editSelectedItem() {
        JIPipeRuntimePartition value = jList.getSelectedValue();
        if(value != null) {
            editRuntimePartition(getWorkbench(), value);
            refresh();
        }
    }

    public static void editRuntimePartition(JIPipeWorkbench workbench, JIPipeRuntimePartition value) {
        JIPipeRuntimePartition copy = new JIPipeRuntimePartition(value);
        if(ParameterPanel.showDialog(workbench,
                copy,
                MarkdownDocument.fromPluginResource("documentation/project-info-runtime-partitions-editor.md"),
                "Edit runtime partition",
                ParameterPanel.DEFAULT_DIALOG_FLAGS)) {
            value.setTo(copy);

            // Go and update existing graph editors
            for (DocumentTabPane.DocumentTab tab : workbench.getDocumentTabPane().getTabs()) {
                if(tab.getContent() instanceof JIPipeGraphEditorUI) {
                    JIPipeGraphCanvasUI canvasUI = ((JIPipeGraphEditorUI) tab.getContent()).getCanvasUI();
                    for (JIPipeGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
                        ui.updateView(false, false, false);
                    }
                    canvasUI.repaintLowLag();
                }
            }

        }
    }

    private void refresh() {
        DefaultListModel<JIPipeRuntimePartition> model = new DefaultListModel<>();
        for (JIPipeRuntimePartition partition : getProjectWorkbench().getProject().getRuntimePartitions().toList()) {
            model.addElement(partition);
        }
        jList.setModel(model);
    }

}
