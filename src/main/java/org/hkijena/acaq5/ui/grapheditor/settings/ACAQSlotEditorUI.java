package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.acaq5.ui.components.EditAlgorithmSlotPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * An extended slot editor that is more powerful than the "in-place" slot editor
 */
public class ACAQSlotEditorUI extends JPanel {
    private ACAQAlgorithm algorithm;
    private JTree slotTree;
    private MarkdownReader helpPanel;

    /**
     * @param algorithm The algorithm
     */
    public ACAQSlotEditorUI(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
        initialize();
        reloadList();
        algorithm.getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        slotTree = new JTree();
        slotTree.setCellRenderer(new ACAQDataSlotTreeCellRenderer());

        helpPanel = new MarkdownReader(false);
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
        add(toolBar, BorderLayout.NORTH);

        if (canModifyInputSlots()) {
            JButton addInputButton = new JButton("Add input", UIUtils.getIconFromResources("database.png"));
            addInputButton.addActionListener(e -> {
                AddAlgorithmSlotPanel.showDialog(this, algorithm, ACAQDataSlot.SlotType.Input);
            });
            toolBar.add(addInputButton);
        }

        if (canModifyOutputSlots()) {
            JButton addOutputButton = new JButton("Add output", UIUtils.getIconFromResources("database.png"));
            addOutputButton.addActionListener(e -> {
                AddAlgorithmSlotPanel.showDialog(this, algorithm, ACAQDataSlot.SlotType.Output);
            });
            toolBar.add(addOutputButton);
        }

        toolBar.add(Box.createHorizontalGlue());

        JButton relabelButton = new JButton(UIUtils.getIconFromResources("label.png"));
        relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
        relabelButton.addActionListener(e -> relabelSlot());
        toolBar.add(relabelButton);

        if (algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {

            if (canModifyInputSlots() || canModifyOutputSlots()) {
                JButton editButton = new JButton(UIUtils.getIconFromResources("edit.png"));
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
            if (slot.getSlotType() == ACAQDataSlot.SlotType.Input && canModifyInputSlots()) {
                EditAlgorithmSlotPanel.showDialog(this, slot);
            } else if (slot.getSlotType() == ACAQDataSlot.SlotType.Output && canModifyOutputSlots()) {
                EditAlgorithmSlotPanel.showDialog(this, slot);
            }
        }
    }

    private void relabelSlot() {
        ACAQDataSlot slot = getSelectedSlot();
        if (slot != null) {
            String newLabel = JOptionPane.showInputDialog(this,
                    "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                    slot.getDefinition().getCustomName());
            slot.getDefinition().setCustomName(newLabel);
        }
    }

    private void moveSlotDown() {
        ACAQDataSlot slot = getSelectedSlot();
        if (slot != null) {
            ((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).moveDown(slot.getName());
        }
    }

    private void moveSlotUp() {
        ACAQDataSlot slot = getSelectedSlot();
        if (slot != null) {
            ((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).moveUp(slot.getName());
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
        for (ACAQDataSlot sample : toRemove) {
            slotConfiguration.removeSlot(sample.getName(), true);
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
