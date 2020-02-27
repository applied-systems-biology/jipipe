package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.components.MarkdownReader;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
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
    private ACAQAlgorithm algorithm;
    private JTree slotTree;
    private MarkdownReader helpPanel;

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
        helpPanel.loadDefaultDocument("documentation/algorithm-slots.md");

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

        if(canModifyInputSlots()) {
            JButton addInputButton = new JButton("Add input", UIUtils.getIconFromResources("database.png"));
            initializeAddButton(addInputButton, ACAQDataSlot.SlotType.Input);
            toolBar.add(addInputButton);
        }

        if(canModifyOutputSlots()) {
            JButton addOutputButton = new JButton("Add output", UIUtils.getIconFromResources("database.png"));
            initializeAddButton(addOutputButton, ACAQDataSlot.SlotType.Output);
            toolBar.add(addOutputButton);
        }

        toolBar.add(Box.createHorizontalGlue());

        if(algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            JButton moveUpButton = new JButton(UIUtils.getIconFromResources("arrow-up.png"));
            moveUpButton.setToolTipText("Move up");
            moveUpButton.addActionListener(e -> moveSlotUp());
            toolBar.add(moveUpButton);

            JButton moveDownButton = new JButton(UIUtils.getIconFromResources("arrow-down.png"));
            moveDownButton.setToolTipText("Move down");
            moveDownButton.addActionListener(e -> moveSlotDown());
            toolBar.add(moveDownButton);
        }

        if(canModifyInputSlots() || canModifyOutputSlots()) {
            JButton removeButton = new JButton( UIUtils.getIconFromResources("delete.png"));
            removeButton.setToolTipText("Remove selected slots");
            removeButton.addActionListener(e -> removeSelectedSlots());
            toolBar.add(removeButton);
        }
    }

    private void moveSlotDown() {
        ACAQDataSlot<?> slot = getSelectedSlot();
        if(slot != null) {
            ((ACAQMutableSlotConfiguration)algorithm.getSlotConfiguration()).moveDown(slot.getName());
        }
    }

    private void moveSlotUp() {
        ACAQDataSlot<?> slot = getSelectedSlot();
        if(slot != null) {
            ((ACAQMutableSlotConfiguration)algorithm.getSlotConfiguration()).moveUp(slot.getName());
        }
    }

    public ACAQDataSlot<?> getSelectedSlot() {
        ACAQDataSlot<?> selectedSlot = null;
        if(slotTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode nd = (DefaultMutableTreeNode) slotTree.getLastSelectedPathComponent();
            if(nd.getUserObject() instanceof ACAQDataSlot<?>) {
                selectedSlot = (ACAQDataSlot<?>)nd.getUserObject();
            }
        }
        return selectedSlot;
    }

    private boolean canModifyOutputSlots() {
        if( algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            return !((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).isOutputSlotsSealed() &&
                    ((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).allowsOutputSlots();
        }
        return false;
    }

    private boolean canModifyInputSlots() {
        if( algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            return !((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).isInputSlotsSealed() &&
                    ((ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration()).allowsInputSlots();
        }
        return false;
    }

    private void removeSelectedSlots() {
        if(!canModifyInputSlots() && !canModifyOutputSlots())
            return;
        Set<ACAQDataSlot<?>> toRemove = new HashSet<>();
        if(slotTree.getSelectionPaths() != null) {
            for(TreePath path : slotTree.getSelectionPaths()) {
                DefaultMutableTreeNode nd = (DefaultMutableTreeNode)path.getLastPathComponent();
                if(nd.getUserObject() instanceof ACAQDataSlot<?>) {
                    toRemove.add((ACAQDataSlot<?>)nd.getUserObject());
                }
            }
        }
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)algorithm.getSlotConfiguration();
        for(ACAQDataSlot<?> sample : toRemove) {
            slotConfiguration.removeSlot(sample.getName());
        }
    }

    public void reloadList() {

        ACAQDataSlot<?> selectedSlot = getSelectedSlot();

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Data slots");
        DefaultMutableTreeNode inputNode = new DefaultMutableTreeNode("Input");
        DefaultMutableTreeNode outputNode = new DefaultMutableTreeNode("Output");
        rootNode.add(inputNode);
        rootNode.add(outputNode);

        DefaultMutableTreeNode toSelect = null;

        for(ACAQDataSlot<?> slot : algorithm.getInputSlots()){
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(slot);
            if(slot == selectedSlot)
                toSelect = node;
            inputNode.add(node);
        }
        for(ACAQDataSlot<?> slot : algorithm.getOutputSlots()){
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(slot);
            if(slot == selectedSlot)
                toSelect = node;
            outputNode.add(node);
        }

        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        slotTree.setModel(model);
        UIUtils.expandAllTree(slotTree);

        if(toSelect != null) {
            slotTree.setSelectionPath(new TreePath(model.getPathToRoot(toSelect)));
        }
    }

    private void initializeAddButton(JButton button, ACAQDataSlot.SlotType slotType) {
        JPopupMenu menu = UIUtils.addPopupMenuToComponent(button);
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)algorithm.getSlotConfiguration();

        Set<Class<? extends ACAQDataSlot<?>>> allowedSlotTypes;
        switch(slotType) {
            case Input:
                allowedSlotTypes = slotConfiguration.getAllowedInputSlotTypes();
                break;
            case Output:
                allowedSlotTypes = slotConfiguration.getAllowedOutputSlotTypes();
                break;
            default:
                throw new RuntimeException();
        }

        for(Class<? extends ACAQDataSlot<?>> slotClass : allowedSlotTypes) {
            Class<? extends ACAQData> dataClass = ACAQDatatypeRegistry.getInstance().getRegisteredSlotDataTypes().inverse().get(slotClass);
            JMenuItem item = new JMenuItem(ACAQData.getNameOf(dataClass), ACAQUIDatatypeRegistry.getInstance().getIconFor(dataClass));
            item.addActionListener(e -> addNewSlot(slotType, slotClass));
            menu.add(item);
        }
    }

    private void addNewSlot(ACAQDataSlot.SlotType slotType, Class<? extends ACAQDataSlot<?>> klass) {
        if(algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
            int existingSlots = slotType == ACAQDataSlot.SlotType.Input ? algorithm.getInputSlots().size() : algorithm.getOutputSlots().size();
            String name = null;
            while(name == null) {
                String newName = JOptionPane.showInputDialog(this,"Please a data slot name", slotType + " data " + (existingSlots + 1));
                if(newName == null || newName.trim().isEmpty())
                    return;
                if(slotConfiguration.hasSlot(newName))
                    continue;
                name = newName;
            }
            switch (slotType) {
                case Input:
                    slotConfiguration.addInputSlot(name, klass);
                    break;
                case Output:
                    slotConfiguration.addOutputSlot(name, klass);
                    break;
            }
        }
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        reloadList();
    }
}
