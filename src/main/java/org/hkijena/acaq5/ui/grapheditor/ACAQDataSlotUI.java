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

package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.history.ACAQAlgorithmGraphHistory;
import org.hkijena.acaq5.api.history.CompoundGraphHistorySnapshot;
import org.hkijena.acaq5.api.history.EdgeConnectGraphHistorySnapshot;
import org.hkijena.acaq5.api.history.EdgeDisconnectAllTargetsGraphHistorySnapshot;
import org.hkijena.acaq5.api.history.EdgeDisconnectGraphHistorySnapshot;
import org.hkijena.acaq5.api.history.MoveNodesGraphHistorySnapshot;
import org.hkijena.acaq5.api.history.SlotConfigurationHistorySnapshot;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbenchPanel;
import org.hkijena.acaq5.ui.components.EditAlgorithmSlotPanel;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.ui.grapheditor.algorithmfinder.ACAQAlgorithmFinderUI;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * UI around an {@link ACAQDataSlot}
 */
public abstract class ACAQDataSlotUI extends ACAQWorkbenchPanel {
    protected JPopupMenu assignButtonMenu;
    private ACAQAlgorithmUI algorithmUI;
    private String compartment;
    private ACAQDataSlot slot;

    /**
     * Creates a new UI
     *
     * @param workbench   the workbench
     * @param algorithmUI The parent algorithm UI
     * @param compartment The compartment ID
     * @param slot        The slot instance
     */
    public ACAQDataSlotUI(ACAQWorkbench workbench, ACAQAlgorithmUI algorithmUI, String compartment, ACAQDataSlot slot) {
        super(workbench);
        this.algorithmUI = algorithmUI;
        this.compartment = compartment;
        this.slot = slot;

        getGraph().getEventBus().register(this);
        slot.getDefinition().getEventBus().register(this);
    }

    public ACAQGraph getGraph() {
        return getGraphUI().getAlgorithmGraph();
    }

    public ACAQAlgorithmGraphCanvasUI getGraphUI() {
        return algorithmUI.getGraphUI();
    }

    private List<ACAQDataSlot> sortSlotsByDistance(Set<ACAQDataSlot> unsorted) {
        Point thisLocation = getGraphUI().getSlotLocation(slot);
        if (thisLocation == null)
            return new ArrayList<>(unsorted);
        Map<ACAQDataSlot, Double> distances = new HashMap<>();
        for (ACAQDataSlot dataSlot : unsorted) {
            Point location = getGraphUI().getSlotLocation(dataSlot);
            if (location != null) {
                distances.put(dataSlot, Math.pow(location.x - thisLocation.x, 2) + Math.pow(location.y - thisLocation.y, 2));
            } else {
                distances.put(dataSlot, Double.POSITIVE_INFINITY);
            }
        }
        return unsorted.stream().sorted(Comparator.comparing(distances::get)).collect(Collectors.toList());
    }

    /**
     * Reloads the "Assign" popup menu
     */
    protected void reloadPopupMenu() {
        assignButtonMenu.removeAll();

        if (slot.isInput()) {
            if (getGraph().getSourceSlot(slot) == null) {
                Set<ACAQDataSlot> availableSources = getGraph().getAvailableSources(slot, true, false);
                availableSources.removeIf(slot -> !slot.getAlgorithm().isVisibleIn(compartment));
                for (ACAQDataSlot source : sortSlotsByDistance(availableSources)) {
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

                    JMenuItem editButton = new JMenuItem("Edit this slot", UIUtils.getIconFromResources("edit.png"));
                    editButton.addActionListener(e -> editSlot());
                    assignButtonMenu.add(editButton);
                }
            }
            if (assignButtonMenu.getComponentCount() > 0)
                assignButtonMenu.addSeparator();
            JMenuItem relabelButton = new JMenuItem("Label this slot", UIUtils.getIconFromResources("label.png"));
            relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
            relabelButton.addActionListener(e -> relabelSlot());
            assignButtonMenu.add(relabelButton);
        } else if (slot.isOutput()) {
            Set<ACAQDataSlot> targetSlots = getGraph().getTargetSlots(slot);
            if (!targetSlots.isEmpty()) {

                boolean allowDisconnect = false;
                for (ACAQDataSlot targetSlot : targetSlots) {
                    if (getGraph().canUserDisconnect(slot, targetSlot)) {
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
            Set<ACAQDataSlot> availableTargets = getGraph().getAvailableTargets(slot, true, true);
            availableTargets.removeIf(slot -> !slot.getAlgorithm().isVisibleIn(compartment));

            JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("search.png"));
            findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
            findAlgorithmButton.addActionListener(e -> findAlgorithm(slot));
            assignButtonMenu.add(findAlgorithmButton);
            if (!availableTargets.isEmpty())
                assignButtonMenu.addSeparator();

            for (ACAQDataSlot target : sortSlotsByDistance(availableTargets)) {
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

                    JMenuItem editButton = new JMenuItem("Edit this slot", UIUtils.getIconFromResources("edit.png"));
                    editButton.addActionListener(e -> editSlot());
                    assignButtonMenu.add(editButton);
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

    private void editSlot() {
        EditAlgorithmSlotPanel.showDialog(this, getGraphUI().getGraphHistory(), slot);
    }

    private void relabelSlot() {
        String newLabel = JOptionPane.showInputDialog(this,
                "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                slot.getDefinition().getCustomName());
        getGraphUI().getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(slot.getAlgorithm(), "Relabel slot '" + slot.getNameWithAlgorithmName() + "'"));
        slot.getDefinition().setCustomName(newLabel);
    }

    private void deleteSlot() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) slot.getAlgorithm().getSlotConfiguration();
        getGraphUI().getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(slot.getAlgorithm(), "Remove slot '" + slot.getNameWithAlgorithmName() + "'"));
        if (slot.isInput())
            slotConfiguration.removeInputSlot(slot.getName(), true);
        else if (slot.isOutput())
            slotConfiguration.removeOutputSlot(slot.getName(), true);
    }

    private void findAlgorithm(ACAQDataSlot slot) {
        ACAQAlgorithmFinderUI algorithmFinderUI = new ACAQAlgorithmFinderUI(algorithmUI.getGraphUI(), slot);
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

    /**
     * Is called when the button's visual feedback should be updated
     */
    protected abstract void reloadButtonStatus();

    private void connectSlot(ACAQDataSlot source, ACAQDataSlot target) {
        if (getGraph().canConnect(source, target, true)) {
            ACAQGraph graph = slot.getAlgorithm().getGraph();
            ACAQAlgorithmGraphHistory graphHistory = algorithmUI.getGraphUI().getGraphHistory();
            if (getGraphUI().isLayoutHelperEnabled()) {
                graphHistory.addSnapshotBefore(new CompoundGraphHistorySnapshot(Arrays.asList(
                        new EdgeConnectGraphHistorySnapshot(graph, source, target),
                        new MoveNodesGraphHistorySnapshot(graph, "Move to target slot")
                )));
            } else {
                graphHistory.addSnapshotBefore(new EdgeConnectGraphHistorySnapshot(graph, source, target));
            }
            getGraph().connect(source, target);
        } else {
            JOptionPane.showMessageDialog(this, "The data slots could not be connected. Is this connection causing loops?",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnectSlot() {
        ACAQGraph graph = slot.getAlgorithm().getGraph();
        ACAQAlgorithmGraphHistory graphHistory = algorithmUI.getGraphUI().getGraphHistory();
        if (slot.isInput()) {
            ACAQDataSlot sourceSlot = graph.getSourceSlot(slot);
            if (sourceSlot != null) {
                graphHistory.addSnapshotBefore(new EdgeDisconnectGraphHistorySnapshot(graph, sourceSlot, slot));
            }
        } else {
            graphHistory.addSnapshotBefore(new EdgeDisconnectAllTargetsGraphHistorySnapshot(graph, slot));
        }
        getGraph().disconnectAll(slot, true);
    }

    /**
     * Is called when the name parameter was changed
     */
    protected abstract void reloadName();

    protected Class<? extends ACAQData> getSlotDataType() {
        if (getGraph() != null) {
            if (getGraph().containsNode(slot)) {
                ACAQDataSlot sourceSlot = getGraph().getSourceSlot(slot);
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
        return slot.getName();
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
        if (getGraph().containsNode(slot)) {
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

}
