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

package org.hkijena.jipipe.desktop.app.settings;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.extensions.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class JIPipeDesktopRuntimePartitionListEditor extends JIPipeDesktopProjectWorkbenchPanel {
    private final JList<JIPipeRuntimePartition> jList = new JList<>();

    public JIPipeDesktopRuntimePartitionListEditor(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        initialize();
        refresh();
    }

    public static void editRuntimePartition(JIPipeDesktopWorkbench workbench, JIPipeRuntimePartition value) {
        JIPipeRuntimePartition copy = new JIPipeRuntimePartition(value);
        if (JIPipeDesktopParameterPanel.showDialog(workbench,
                copy,
                MarkdownText.fromPluginResource("documentation/project-info-runtime-partitions-editor.md"),
                "Edit runtime partition",
                JIPipeDesktopParameterPanel.DEFAULT_DIALOG_FLAGS)) {
            value.setTo(copy);

            // Go and update existing graph editors
            for (JIPipeDesktopTabPane.DocumentTab tab : workbench.getDocumentTabPane().getTabs()) {
                if (tab.getContent() instanceof JIPipeDesktopGraphEditorUI) {
                    JIPipeDesktopGraphCanvasUI canvasUI = ((JIPipeDesktopGraphEditorUI) tab.getContent()).getCanvasUI();
                    for (JIPipeDesktopGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
                        ui.updateView(false, false, false);
                    }
                    canvasUI.repaintLowLag();
                }
            }

        }
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

        jList.setCellRenderer(new JIPipeDesktopRuntimePartitionListCellRenderer(getDesktopProjectWorkbench().getProject().getRuntimePartitions()));
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editSelectedItem();
                }
            }
        });
        add(new JScrollPane(jList), BorderLayout.CENTER);
    }

    private void removeSelectedItems() {
        Set<Integer> indices = new HashSet<>();
        for (JIPipeRuntimePartition partition : jList.getSelectedValuesList()) {
            int idx = getDesktopProjectWorkbench().getProject().getRuntimePartitions().indexOf(partition);
            if (idx >= 0) {
                indices.add(idx);
            }
        }

        // The default cannot be removed
        indices.remove(0);

        // Check if any of the nodes use the partitions and warn them
        Set<Integer> usedPartitions = new HashSet<>();
        for (JIPipeGraphNode graphNode : getProject().getGraph().getGraphNodes()) {
            if (graphNode instanceof JIPipeAlgorithm) {
                usedPartitions.add(((JIPipeAlgorithm) graphNode).getRuntimePartition().getIndex());
            }
        }

        Sets.SetView<Integer> intersection = Sets.intersection(indices, usedPartitions);
        if (!intersection.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("The following partitions are currently used in the pipeline:\n\n");
            for (Integer usedIdx : intersection) {
                message.append("- ").append(getDesktopProjectWorkbench().getProject().getRuntimePartitions().get(usedIdx)).append("\n");
            }
            message.append("\nDo you really want to remove the selected partitions?");

            if (JOptionPane.showConfirmDialog(this, message.toString(), "Delete runtime partitions", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
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
        JIPipeRuntimePartition runtimePartition = getProject().getRuntimePartitions().add();
        editRuntimePartition(getDesktopWorkbench(), runtimePartition);
        refresh();
    }

    private void editSelectedItem() {
        JIPipeRuntimePartition value = jList.getSelectedValue();
        if (value != null) {
            editRuntimePartition(getDesktopWorkbench(), value);
            refresh();
        }
    }

    private void refresh() {
        DefaultListModel<JIPipeRuntimePartition> model = new DefaultListModel<>();
        for (JIPipeRuntimePartition partition : getDesktopProjectWorkbench().getProject().getRuntimePartitions().toList()) {
            model.addElement(partition);
        }
        jList.setModel(model);
    }

}
