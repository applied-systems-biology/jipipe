package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.ui.grapheditor.algorithmfinder.ACAQAlgorithmFinderUI;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.function.Consumer;

/**
 * UI around an {@link ACAQDataSlot}
 */
public abstract class ACAQDataSlotUI extends JPanel {
    protected JPopupMenu assignButtonMenu;
    private ACAQAlgorithmUI algorithmUI;
    private ACAQAlgorithmGraph graph;
    private String compartment;
    private ACAQDataSlot slot;
    private ACAQAlgorithmGraphCanvasUI.Direction direction;

    /**
     * Creates a new UI
     *
     * @param algorithmUI The parent algorithm UI
     * @param graph       The graph
     * @param compartment The compartment ID
     * @param slot        The slot instance
     * @param direction   The directionality of this slot UI
     */
    public ACAQDataSlotUI(ACAQAlgorithmUI algorithmUI, ACAQAlgorithmGraph graph, String compartment, ACAQDataSlot slot, ACAQAlgorithmGraphCanvasUI.Direction direction) {
        this.algorithmUI = algorithmUI;
        this.graph = graph;
        this.compartment = compartment;
        this.slot = slot;
        this.direction = direction;

        graph.getEventBus().register(this);
        slot.getDefinition().getEventBus().register(this);
    }

    /**
     * Reloads the "Assign" popup menu
     */
    protected void reloadPopupMenu() {
        assignButtonMenu.removeAll();

        if (slot.isInput()) {
            if (graph.getSourceSlot(slot) == null) {
                Set<ACAQDataSlot> availableSources = graph.getAvailableSources(slot, true);
                availableSources.removeIf(slot -> !slot.getAlgorithm().isVisibleIn(compartment));
                for (ACAQDataSlot source : availableSources) {
                    if (!source.getAlgorithm().isVisibleIn(compartment))
                        continue;
                    JMenuItem connectButton = new JMenuItem(source.getNameWithAlgorithmName(),
                            ACAQUIDatatypeRegistry.getInstance().getIconFor(source.getAcceptedDataType()));
                    connectButton.addActionListener(e -> connectSlot(source, slot));
                    assignButtonMenu.add(connectButton);
                }
            } else {
                JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("remove.png"));
                disconnectButton.addActionListener(e -> disconnectSlot());
                assignButtonMenu.add(disconnectButton);
            }
            if (slot.getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
                ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) slot.getAlgorithm().getSlotConfiguration();
                if (slotConfiguration.canModifyInputSlots()) {
                    if (assignButtonMenu.getComponentCount() > 0)
                        assignButtonMenu.addSeparator();
                    JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("remove.png"));
                    deleteButton.addActionListener(e -> deleteSlot());
                    assignButtonMenu.add(deleteButton);
                }
            }
            if (assignButtonMenu.getComponentCount() > 0)
                assignButtonMenu.addSeparator();
            JMenuItem relabelButton = new JMenuItem("Label this slot", UIUtils.getIconFromResources("label.png"));
            relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
            relabelButton.addActionListener(e -> relabelSlot());
            assignButtonMenu.add(relabelButton);
        } else if (slot.isOutput()) {
            Set<ACAQDataSlot> targetSlots = graph.getTargetSlots(slot);
            if (!targetSlots.isEmpty()) {

                boolean allowDisconnect = false;
                for (ACAQDataSlot targetSlot : targetSlots) {
                    if (graph.canUserDisconnect(slot, targetSlot)) {
                        allowDisconnect = true;
                        break;
                    }
                }

                if (allowDisconnect) {
                    JMenuItem disconnectButton = new JMenuItem("Disconnect all", UIUtils.getIconFromResources("remove.png"));
                    disconnectButton.addActionListener(e -> disconnectSlot());
                    assignButtonMenu.add(disconnectButton);

                    assignButtonMenu.addSeparator();
                }
            }
            Set<ACAQDataSlot> availableTargets = graph.getAvailableTargets(slot, true);
            availableTargets.removeIf(slot -> !slot.getAlgorithm().isVisibleIn(compartment));

            JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("search.png"));
            findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
            findAlgorithmButton.addActionListener(e -> findAlgorithm(slot));
            assignButtonMenu.add(findAlgorithmButton);
            if (!availableTargets.isEmpty())
                assignButtonMenu.addSeparator();

            for (ACAQDataSlot target : availableTargets) {
                JMenuItem connectButton = new JMenuItem(target.getNameWithAlgorithmName(),
                        ACAQUIDatatypeRegistry.getInstance().getIconFor(target.getAcceptedDataType()));
                connectButton.addActionListener(e -> connectSlot(slot, target));
                connectButton.setToolTipText(TooltipUtils.getAlgorithmTooltip(target.getAlgorithm().getDeclaration()));
                assignButtonMenu.add(connectButton);
            }

            if (slot.getAlgorithm().getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
                ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) slot.getAlgorithm().getSlotConfiguration();
                if (slotConfiguration.canModifyOutputSlots()) {
                    if (assignButtonMenu.getComponentCount() > 0)
                        assignButtonMenu.addSeparator();
                    JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("remove.png"));
                    deleteButton.addActionListener(e -> deleteSlot());
                    assignButtonMenu.add(deleteButton);
                }
            }
            if (assignButtonMenu.getComponentCount() > 0)
                assignButtonMenu.addSeparator();
            JMenuItem relabelButton = new JMenuItem("Label this slot", UIUtils.getIconFromResources("label.png"));
            relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
            relabelButton.addActionListener(e -> relabelSlot());
            assignButtonMenu.add(relabelButton);
        }
    }

    private void relabelSlot() {
        String newLabel = JOptionPane.showInputDialog(this,
                "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                slot.getDefinition().getCustomName());
        slot.getDefinition().setCustomName(newLabel);
    }

    private void deleteSlot() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) slot.getAlgorithm().getSlotConfiguration();
        slotConfiguration.removeSlot(slot.getName());
    }

    private void findAlgorithm(ACAQDataSlot slot) {
        ACAQAlgorithmFinderUI algorithmFinderUI = new ACAQAlgorithmFinderUI(slot, graph, compartment);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
        UIUtils.addEscapeListener(dialog);
        dialog.setModal(true);
        dialog.setContentPane(algorithmFinderUI);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(this);

        algorithmFinderUI.getEventBus().register(new Consumer<AlgorithmFinderSuccessEvent>() {
            @Override
            @Subscribe
            public void accept(AlgorithmFinderSuccessEvent event) {
                dialog.setVisible(false);
            }
        });

        dialog.setVisible(true);
    }

    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    /**
     * Is called when the button's visual feedback should be updated
     */
    protected abstract void reloadButtonStatus();

    private void connectSlot(ACAQDataSlot source, ACAQDataSlot target) {
        if (graph.canConnect(source, target, true)) {
            graph.connect(source, target);
        }
    }

    private void disconnectSlot() {
        graph.disconnectAll(slot, true);
    }

    /**
     * Is called when the name parameter was changed
     */
    protected abstract void reloadName();

    protected Class<? extends ACAQData> getSlotDataType() {
        if (graph != null) {
            if (graph.containsNode(slot)) {
                ACAQDataSlot sourceSlot = graph.getSourceSlot(slot);
                if (sourceSlot != null)
                    return sourceSlot.getAcceptedDataType();
            }
        }
        return slot.getAcceptedDataType();
    }

    /**
     * @return The name that should be displayed
     */
    public String getDisplayedName() {
        boolean hasCustomName = !StringUtils.isNullOrEmpty(slot.getDefinition().getCustomName());
        if (hasCustomName) {
            return slot.getDefinition().getCustomName();
        }
        if (slot.getAlgorithm() instanceof ACAQCompartmentOutput) {
            if (slot.isOutput()) {
                return slot.getName().substring("Output ".length());
            } else {
                return slot.getName();
            }
        } else {
            return slot.getName();
        }
    }

    /**
     * @return The width needed to display the slot
     */
    public abstract int calculateWidth();

    /**
     * @return The slot
     */
    public ACAQDataSlot getSlot() {
        return slot;
    }

    /**
     * Should be triggered when the slots are changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        if (graph.containsNode(slot)) {
            reloadPopupMenu();
            reloadButtonStatus();
        }
    }

    /**
     * Triggered when the custom name of the slot definition is changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onSlotNameChanged(ParameterChangedEvent event) {
        if ("custom-name".equals(event.getKey())) {
            reloadName();
            algorithmUI.updateSize();
        }
    }

    /**
     * @return The compartment
     */
    public String getCompartment() {
        return compartment;
    }

    public ACAQAlgorithmGraphCanvasUI.Direction getDirection() {
        return direction;
    }

    protected JPopupMenu getAssignButtonMenu() {
        return assignButtonMenu;
    }

    protected void setAssignButtonMenu(JPopupMenu assignButtonMenu) {
        this.assignButtonMenu = assignButtonMenu;
    }
}
