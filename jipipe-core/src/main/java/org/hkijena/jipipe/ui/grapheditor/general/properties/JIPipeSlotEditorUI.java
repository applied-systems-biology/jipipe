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

package org.hkijena.jipipe.ui.grapheditor.general.properties;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.EditAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * An extended slot editor that is more powerful than the "in-place" slot editor
 */
public class JIPipeSlotEditorUI extends JPanel implements JIPipeGraphNode.NodeSlotsChangedEventListener, JIPipeParameterCollection.ParameterChangedEventListener {
    private final JIPipeGraphEditorUI editorUI;
    private final JIPipeGraphNode node;
    private JTree slotTree;

    /**
     * @param editorUI the editor that shows the slot editor
     * @param node     The algorithm
     */
    public JIPipeSlotEditorUI(JIPipeGraphEditorUI editorUI, JIPipeGraphNode node) {
        this.editorUI = editorUI;
        this.node = node;
        initialize();
        reloadList();
        node.getNodeSlotsChangedEventEmitter().subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        slotTree = new JTree();
        slotTree.setCellRenderer(new JIPipeDataSlotTreeCellRenderer());

        MarkdownReader helpPanel = new MarkdownReader(false);
        helpPanel.setDocument(MarkdownDocument.fromPluginResource("documentation/algorithm-slots.md", new HashMap<>()));
        JScrollPane scrollPane = new JScrollPane(slotTree);
        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, helpPanel, AutoResizeSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);

        initializeToolbar();
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        if (canModifyInputSlots()) {
            JButton addInputButton = new JButton("Add input", UIUtils.getIconFromResources("actions/database.png"));
            addInputButton.addActionListener(e -> {
                if (!JIPipeProjectWorkbench.canModifySlots(editorUI.getWorkbench()))
                    return;
                AddAlgorithmSlotPanel.showDialog(this, editorUI.getCanvasUI().getHistoryJournal(), node, JIPipeSlotType.Input);
            });
            toolBar.add(addInputButton);
        }

        if (canModifyOutputSlots()) {
            JButton addOutputButton = new JButton("Add output", UIUtils.getIconFromResources("actions/database.png"));
            addOutputButton.addActionListener(e -> {
                if (!JIPipeProjectWorkbench.canModifySlots(editorUI.getWorkbench()))
                    return;
                AddAlgorithmSlotPanel.showDialog(this, editorUI.getCanvasUI().getHistoryJournal(), node, JIPipeSlotType.Output);
            });
            toolBar.add(addOutputButton);
        }

        toolBar.add(Box.createHorizontalGlue());

        JButton relabelButton = new JButton("Label", UIUtils.getIconFromResources("actions/tag.png"));
        relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
        relabelButton.addActionListener(e -> relabelSlot());
        toolBar.add(relabelButton);

        if (node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {

            if (canModifyInputSlots() || canModifyOutputSlots()) {
                JButton editButton = new JButton("Edit", UIUtils.getIconFromResources("actions/edit.png"));
                editButton.setToolTipText("Edit selected slot");
                editButton.addActionListener(e -> editSlot());
                toolBar.add(editButton);
            }

            JButton moveUpButton = new JButton(UIUtils.getIconFromResources("actions/caret-up.png"));
            moveUpButton.setToolTipText("Move up");
            moveUpButton.addActionListener(e -> moveSlotUp());
            toolBar.add(moveUpButton);

            JButton moveDownButton = new JButton(UIUtils.getIconFromResources("actions/caret-down.png"));
            moveDownButton.setToolTipText("Move down");
            moveDownButton.addActionListener(e -> moveSlotDown());
            toolBar.add(moveDownButton);
        }

        if (canModifyInputSlots() || canModifyOutputSlots()) {
            JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
            removeButton.setToolTipText("Remove selected slots");
            removeButton.addActionListener(e -> removeSelectedSlots());
            toolBar.add(removeButton);
        }
    }

    private void editSlot() {
        if (!JIPipeProjectWorkbench.canModifySlots(editorUI.getWorkbench()))
            return;
        JIPipeDataSlot slot = getSelectedSlot();
        if (slot == null) {
            return;
        }
        if (!slot.getInfo().isUserModifiable()) {
            JOptionPane.showMessageDialog(this, "This slot cannot be edited.", "Edit slot", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (slot.getSlotType() == JIPipeSlotType.Input && canModifyInputSlots()) {
            EditAlgorithmSlotPanel.showDialog(this, editorUI.getCanvasUI().getHistoryJournal(), slot);
        } else if (slot.getSlotType() == JIPipeSlotType.Output && canModifyOutputSlots()) {
            EditAlgorithmSlotPanel.showDialog(this, editorUI.getCanvasUI().getHistoryJournal(), slot);
        }
    }

    private void relabelSlot() {
        JIPipeDataSlot slot = getSelectedSlot();
        if (slot != null) {
            String newLabel = JOptionPane.showInputDialog(this,
                    "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                    slot.getInfo().getCustomName());
            if (editorUI.getHistoryJournal() != null) {
                editorUI.getHistoryJournal().snapshotBeforeLabelSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            slot.getInfo().setCustomName(newLabel);
            slot.getNode().getNodeSlotsChangedEventEmitter().emit(new JIPipeGraphNode.NodeSlotsChangedEvent(slot.getNode()));
            reloadList();
        }
    }

    private void moveSlotDown() {
        JIPipeDataSlot slot = getSelectedSlot();
        if (slot != null) {
            if (editorUI.getHistoryJournal() != null) {
                editorUI.getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).moveDown(slot.getName(), slot.getSlotType());
            editorUI.getCanvasUI().repaint(50);
        }
    }

    private void moveSlotUp() {
        JIPipeDataSlot slot = getSelectedSlot();
        if (slot != null) {
            if (editorUI.getHistoryJournal() != null) {
                editorUI.getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInParentGraph());
            }
            ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).moveUp(slot.getName(), slot.getSlotType());
            editorUI.getCanvasUI().repaint(50);
        }
    }

    public JIPipeDataSlot getSelectedSlot() {
        JIPipeDataSlot selectedSlot = null;
        if (slotTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode nd = (DefaultMutableTreeNode) slotTree.getLastSelectedPathComponent();
            if (nd.getUserObject() instanceof JIPipeDataSlot) {
                selectedSlot = (JIPipeDataSlot) nd.getUserObject();
            }
        }
        return selectedSlot;
    }

    private boolean canModifyOutputSlots() {
        if (node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            return !((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).isOutputSlotsSealed() &&
                    ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).allowsOutputSlots();
        }
        return false;
    }

    private boolean canModifyInputSlots() {
        if (node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            return !((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).isInputSlotsSealed() &&
                    ((JIPipeMutableSlotConfiguration) node.getSlotConfiguration()).allowsInputSlots();
        }
        return false;
    }

    private void removeSelectedSlots() {
        if (!canModifyInputSlots() && !canModifyOutputSlots())
            return;
        Set<JIPipeDataSlot> toRemove = new HashSet<>();
        if (slotTree.getSelectionPaths() != null) {
            for (TreePath path : slotTree.getSelectionPaths()) {
                DefaultMutableTreeNode nd = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (nd.getUserObject() instanceof JIPipeDataSlot) {
                    toRemove.add((JIPipeDataSlot) nd.getUserObject());
                }
            }
        }
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) node.getSlotConfiguration();
        for (JIPipeDataSlot slot : toRemove) {
            if (!slot.getInfo().isUserModifiable()) {
                JOptionPane.showMessageDialog(this,
                        String.format("The slot '%s' cannot be remove.", slot.getName()),
                        "Remove slot",
                        JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (editorUI.getHistoryJournal() != null) {
                editorUI.getHistoryJournal().snapshotBeforeRemoveSlot(slot.getNode(), slot.getInfo(), slot.getNode().getCompartmentUUIDInParentGraph());
            }
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

        if (node.getParentGraph() == null || !node.getParentGraph().containsNode(node)) {
            return;
        }

        JIPipeDataSlot selectedSlot = getSelectedSlot();

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Data slots");
        DefaultMutableTreeNode inputNode = new DefaultMutableTreeNode("Input");
        DefaultMutableTreeNode outputNode = new DefaultMutableTreeNode("Output");
        rootNode.add(inputNode);
        rootNode.add(outputNode);

        DefaultMutableTreeNode toSelect = null;

        for (JIPipeDataSlot slot : node.getInputSlots()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(slot);
            slot.getInfo().getParameterChangedEventEmitter().subscribeWeak(this);
            if (slot == selectedSlot)
                toSelect = node;
            inputNode.add(node);
        }
        for (JIPipeDataSlot slot : node.getOutputSlots()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(slot);
            slot.getInfo().getParameterChangedEventEmitter().subscribeWeak(this);
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
    @Override
    public void onNodeSlotsChanged(JIPipeGraphNode.NodeSlotsChangedEvent event) {
        if (isDisplayable()) {
            reloadList();
        }
    }

    /**
     * Triggered when the custom name of the slot definition is changed
     *
     * @param event Generated event
     */
    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("custom-name".equals(event.getKey())) {
            reloadList();
        }
    }
}
