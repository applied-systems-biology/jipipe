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

package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotType;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.history.SlotConfigurationHistorySnapshot;
import org.hkijena.acaq5.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.acaq5.ui.components.EditAlgorithmSlotPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphEditorUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * An extended slot editor that is more powerful than the "in-place" slot editor
 */
public class ACAQSlotEditorUI extends JPanel {
    private final ACAQGraphEditorUI editorUI;
    private final ACAQGraphNode algorithm;
    private JTree slotTree;

    /**
     * @param editorUI  the editor that shows the slot editor
     * @param algorithm The algorithm
     */
    public ACAQSlotEditorUI(ACAQGraphEditorUI editorUI, ACAQGraphNode algorithm) {
        this.editorUI = editorUI;
        this.algorithm = algorithm;
        initialize();
        reloadList();
        algorithm.getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        slotTree = new JTree();
        slotTree.setCellRenderer(new ACAQDataSlotTreeCellRenderer());

        MarkdownReader helpPanel = new MarkdownReader(false);
        helpPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-slots.md"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(slotTree), helpPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        add(splitPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        if (canModifyInputSlots()) {
            JButton addInputButton = new JButton("Add input", UIUtils.getIconFromResources("database.png"));
            addInputButton.addActionListener(e -> {
                AddAlgorithmSlotPanel.showDialog(this, editorUI.getCanvasUI().getGraphHistory(), algorithm, ACAQSlotType.Input);
            });
            toolBar.add(addInputButton);
        }

        if (canModifyOutputSlots()) {
            JButton addOutputButton = new JButton("Add output", UIUtils.getIconFromResources("database.png"));
            addOutputButton.addActionListener(e -> {
                AddAlgorithmSlotPanel.showDialog(this, editorUI.getCanvasUI().getGraphHistory(), algorithm, ACAQSlotType.Output);
            });
            toolBar.add(addOutputButton);
        }

        toolBar.add(Box.createHorizontalGlue());

        JButton relabelButton = new JButton("Label", UIUtils.getIconFromResources("label.png"));
        relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
        relabelButton.addActionListener(e -> relabelSlot());
        toolBar.add(relabelButton);

        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {

            if (canModifyInputSlots() || canModifyOutputSlots()) {
                JButton editButton = new JButton("Edit", UIUtils.getIconFromResources("edit.png"));
                editButton.setToolTipText("Edit selected slot");
                editButton.addActionListener(e -> editSlot());
                toolBar.add(editButton);
            }

            JButton moveUpButton = new JButton(UIUtils.getIconFromResources("arrow-up.png"));
            moveUpButton.setToolTipText("Move up");
            moveUpButton.addActionListener(e -> moveSlotUp());
            toolBar.add(moveUpButton);

            JButton moveDownButton = new JButton(UIUtils.getIconFromResources("arrow-down.png"));
            moveDownButton.setToolTipText("Move down");
            moveDownButton.addActionListener(e -> moveSlotDown());
            toolBar.add(moveDownButton);
        }

        if (canModifyInputSlots() || canModifyOutputSlots()) {
            JButton removeButton = new JButton(UIUtils.getIconFromResources("delete.png"));
            removeButton.setToolTipText("Remove selected slots");
            removeButton.addActionListener(e -> removeSelectedSlots());
            toolBar.add(removeButton);
        }
    }

    private void editSlot() {
        ACAQDataSlot slot = getSelectedSlot();
        if (slot != null) {
            if (slot.getSlotType() == ACAQSlotType.Input && canModifyInputSlots()) {
                EditAlgorithmSlotPanel.showDialog(this, editorUI.getCanvasUI().getGraphHistory(), slot);
            } else if (slot.getSlotType() == ACAQSlotType.Output && canModifyOutputSlots()) {
                EditAlgorithmSlotPanel.showDialog(this, editorUI.getCanvasUI().getGraphHistory(), slot);
            }
        }
    }

    private void relabelSlot() {
        ACAQDataSlot slot = getSelectedSlot();
        if (slot != null) {
            String newLabel = JOptionPane.showInputDialog(this,
                    "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                    slot.getDefinition().getCustomName());
            editorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(slot.getNode(),
                    "Relabel slot '" + slot.getNameWithAlgorithmName() + "'"));
            slot.getDefinition().setCustomName(newLabel);
        }
    }

    private void moveSlotDown() {
        ACAQDataSlot slot = getSelectedSlot();
        if (slot != null) {
            editorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(slot.getNode(),
                    "Move slot '" + slot.getNameWithAlgorithmName() + "' down"));
            ((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).moveDown(slot.getName(), slot.getSlotType());
            editorUI.getCanvasUI().repaint();
        }
    }

    private void moveSlotUp() {
        ACAQDataSlot slot = getSelectedSlot();
        if (slot != null) {
            editorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(slot.getNode(),
                    "Move slot '" + slot.getNameWithAlgorithmName() + "' up"));
            ((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).moveUp(slot.getName(), slot.getSlotType());
            editorUI.getCanvasUI().repaint();
        }
    }

    public ACAQDataSlot getSelectedSlot() {
        ACAQDataSlot selectedSlot = null;
        if (slotTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode nd = (DefaultMutableTreeNode) slotTree.getLastSelectedPathComponent();
            if (nd.getUserObject() instanceof ACAQDataSlot) {
                selectedSlot = (ACAQDataSlot) nd.getUserObject();
            }
        }
        return selectedSlot;
    }

    private boolean canModifyOutputSlots() {
        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            return !((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).isOutputSlotsSealed() &&
                    ((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).allowsOutputSlots();
        }
        return false;
    }

    private boolean canModifyInputSlots() {
        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            return !((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).isInputSlotsSealed() &&
                    ((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).allowsInputSlots();
        }
        return false;
    }

    private void removeSelectedSlots() {
        if (!canModifyInputSlots() && !canModifyOutputSlots())
            return;
        Set<ACAQDataSlot> toRemove = new HashSet<>();
        if (slotTree.getSelectionPaths() != null) {
            for (TreePath path : slotTree.getSelectionPaths()) {
                DefaultMutableTreeNode nd = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (nd.getUserObject() instanceof ACAQDataSlot) {
                    toRemove.add((ACAQDataSlot) nd.getUserObject());
                }
            }
        }
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
        for (ACAQDataSlot slot : toRemove) {
            editorUI.getCanvasUI().getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(slot.getNode(),
                    "Remove slot '" + slot.getNameWithAlgorithmName() + "'"));
            if (slot.isInput())
                slotConfiguration.removeInputSlot(slot.getName(), true);
            else
                slotConfiguration.removeOutputSlot(slot.getName(), true);
        }
    }

    /**
     * Reloads the list
     */
    public void reloadList() {

        ACAQDataSlot selectedSlot = getSelectedSlot();

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Data slots");
        DefaultMutableTreeNode inputNode = new DefaultMutableTreeNode("Input");
        DefaultMutableTreeNode outputNode = new DefaultMutableTreeNode("Output");
        rootNode.add(inputNode);
        rootNode.add(outputNode);

        DefaultMutableTreeNode toSelect = null;

        for (ACAQDataSlot slot : algorithm.getInputSlots()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(slot);
            slot.getDefinition().getEventBus().register(this);
            if (slot == selectedSlot)
                toSelect = node;
            inputNode.add(node);
        }
        for (ACAQDataSlot slot : algorithm.getOutputSlots()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(slot);
            slot.getDefinition().getEventBus().register(this);
            if (slot == selectedSlot)
                toSelect = node;
            outputNode.add(node);
        }

        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        slotTree.setModel(model);
        UIUtils.expandAllTree(slotTree);

        if (toSelect != null) {
            slotTree.setSelectionPath(new TreePath(model.getPathToRoot(toSelect)));
        }
    }

    /**
     * Triggered when the algorithm slots are changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        reloadList();
    }

    /**
     * Triggered when the custom name of the slot definition is changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onSlotNameChanged(ParameterChangedEvent event) {
        if ("custom-name".equals(event.getKey())) {
            reloadList();
        }
    }
}
