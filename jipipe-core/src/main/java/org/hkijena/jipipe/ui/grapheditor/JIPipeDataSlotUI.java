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

package org.hkijena.jipipe.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.events.GraphChangedEvent;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.history.CompoundGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.EdgeConnectGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.EdgeDisconnectAllTargetsGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.EdgeDisconnectGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.GraphChangedHistorySnapshot;
import org.hkijena.jipipe.api.history.JIPipeGraphHistory;
import org.hkijena.jipipe.api.history.MoveNodesGraphHistorySnapshot;
import org.hkijena.jipipe.api.history.SlotConfigurationHistorySnapshot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.EditAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.jipipe.ui.grapheditor.algorithmfinder.JIPipeAlgorithmFinderUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * UI around an {@link JIPipeDataSlot}
 */
public abstract class JIPipeDataSlotUI extends JIPipeWorkbenchPanel {
    protected JPopupMenu assignButtonMenu;
    private JIPipeNodeUI nodeUI;
    private String compartment;
    private JIPipeDataSlot slot;

    /**
     * Creates a new UI
     *
     * @param workbench   the workbench
     * @param nodeUI      The parent algorithm UI
     * @param compartment The compartment ID
     * @param slot        The slot instance
     */
    public JIPipeDataSlotUI(JIPipeWorkbench workbench, JIPipeNodeUI nodeUI, String compartment, JIPipeDataSlot slot) {
        super(workbench);
        this.nodeUI = nodeUI;
        this.compartment = compartment;
        this.slot = slot;

        getGraph().getEventBus().register(this);
        slot.getDefinition().getEventBus().register(this);
    }

    public JIPipeNodeUI getNodeUI() {
        return nodeUI;
    }

    public JIPipeGraph getGraph() {
        return getGraphUI().getGraph();
    }

    public JIPipeGraphCanvasUI getGraphUI() {
        return nodeUI.getGraphUI();
    }

    private List<JIPipeDataSlot> sortSlotsByDistance(Set<JIPipeDataSlot> unsorted) {
        Point thisLocation = getGraphUI().getSlotLocation(slot);
        if (thisLocation == null)
            return new ArrayList<>(unsorted);
        Map<JIPipeDataSlot, Double> distances = new HashMap<>();
        for (JIPipeDataSlot dataSlot : unsorted) {
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
        assignButtonMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                getGraphUI().setCurrentConnectionDragSource(null);
                getGraphUI().setCurrentConnectionDragTarget(null);
                getGraphUI().setCurrentHighlightedForDisconnect(null);
                getGraphUI().repaint();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                getGraphUI().setCurrentConnectionDragSource(null);
                getGraphUI().setCurrentConnectionDragTarget(null);
                getGraphUI().setCurrentHighlightedForDisconnect(null);
                getGraphUI().repaint();
            }
        });

        if (slot.isInput()) {
            JIPipeDataSlot sourceSlot = getGraph().getSourceSlot(slot);
            if (sourceSlot == null) {
                Set<JIPipeDataSlot> availableSources = getGraph().getAvailableSources(slot, true, false);
                availableSources.removeIf(slot -> !slot.getNode().isVisibleIn(compartment));
                for (JIPipeDataSlot source : sortSlotsByDistance(availableSources)) {
                    if (!source.getNode().isVisibleIn(compartment))
                        continue;
                    JMenuItem connectButton = new JMenuItem(source.getDisplayName(),
                            JIPipeUIDatatypeRegistry.getInstance().getIconFor(source.getAcceptedDataType()));
                    connectButton.addActionListener(e -> connectSlot(source, slot));
                    installHighlightForConnect(source, connectButton);
                    assignButtonMenu.add(connectButton);
                }
            } else {
                JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("actions/cancel.png"));
                disconnectButton.addActionListener(e -> disconnectSlot());
                installHighlightForDisconnect(disconnectButton);
                assignButtonMenu.add(disconnectButton);

                JIPipeGraphEdge edge = nodeUI.getGraphUI().getGraph().getGraph().getEdge(sourceSlot, slot);
                if (edge.isUiHidden()) {
                    JMenuItem showButton = new JMenuItem("Show incoming edge", UIUtils.getIconFromResources("actions/eye.png"));
                    showButton.setToolTipText("Un-hides the incoming edge");
                    showButton.addActionListener(e -> {
                        nodeUI.getGraphUI().getGraphHistory().addSnapshotBefore(new GraphChangedHistorySnapshot(getGraph(), "Un-hide edge"));
                        edge.setUiHidden(false);
                        nodeUI.getGraphUI().repaint();
                    });
                    installHighlightForConnect(sourceSlot, showButton);
                    assignButtonMenu.add(showButton);
                } else {
                    JMenuItem hideButton = new JMenuItem("Hide incoming edge", UIUtils.getIconFromResources("actions/eye-slash.png"));
                    hideButton.setToolTipText("Hides the incoming edge");
                    hideButton.addActionListener(e -> {
                        nodeUI.getGraphUI().getGraphHistory().addSnapshotBefore(new GraphChangedHistorySnapshot(getGraph(), "Hide edge"));
                        edge.setUiHidden(true);
                        nodeUI.getGraphUI().repaint();
                    });
                    installHighlightForDisconnect(hideButton);
                    assignButtonMenu.add(hideButton);
                }
            }
            if (slot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
                JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
                if (slotConfiguration.canModifyInputSlots()) {
                    if (assignButtonMenu.getComponentCount() > 0)
                        assignButtonMenu.addSeparator();
                    JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("actions/delete.png"));
                    deleteButton.addActionListener(e -> deleteSlot());
                    installHighlightForDisconnect(deleteButton);
                    assignButtonMenu.add(deleteButton);

                    JMenuItem editButton = new JMenuItem("Edit this slot", UIUtils.getIconFromResources("actions/edit.png"));
                    editButton.addActionListener(e -> editSlot());
                    assignButtonMenu.add(editButton);
                }
            }
            if (assignButtonMenu.getComponentCount() > 0)
                assignButtonMenu.addSeparator();
            JMenuItem relabelButton = new JMenuItem("Label this slot", UIUtils.getIconFromResources("actions/tag.png"));
            relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
            relabelButton.addActionListener(e -> relabelSlot());
            assignButtonMenu.add(relabelButton);
        } else if (slot.isOutput()) {
            Set<JIPipeDataSlot> targetSlots = getGraph().getTargetSlots(slot);
            if (!targetSlots.isEmpty()) {

                boolean allowDisconnect = false;
                for (JIPipeDataSlot targetSlot : targetSlots) {
                    if (getGraph().canUserDisconnect(slot, targetSlot)) {
                        allowDisconnect = true;
                        break;
                    }
                }


                if (allowDisconnect) {
                    JMenuItem disconnectButton = new JMenuItem("Disconnect all", UIUtils.getIconFromResources("actions/cancel.png"));
                    disconnectButton.addActionListener(e -> disconnectSlot());
                    installHighlightForDisconnect(disconnectButton);
                    assignButtonMenu.add(disconnectButton);

                    assignButtonMenu.addSeparator();
                }

                Set<JIPipeGraphEdge> hiddenEdges = new HashSet<>();
                Set<JIPipeGraphEdge> visibleEdges = new HashSet<>();
                for (JIPipeDataSlot targetSlot : targetSlots) {
                    JIPipeGraphEdge edge = getGraph().getGraph().getEdge(slot, targetSlot);
                    if (edge.isUiHidden()) {
                        hiddenEdges.add(edge);
                    } else {
                        visibleEdges.add(edge);
                    }
                }

                if (!hiddenEdges.isEmpty()) {
                    JMenuItem showButton = new JMenuItem("Show all outgoing edges", UIUtils.getIconFromResources("actions/eye.png"));
                    showButton.setToolTipText("Un-hides all outgoing edges");
                    showButton.addActionListener(e -> {
                        nodeUI.getGraphUI().getGraphHistory().addSnapshotBefore(new GraphChangedHistorySnapshot(getGraph(), "Un-hide edges"));
                        for (JIPipeGraphEdge target : hiddenEdges) {
                            target.setUiHidden(false);
                        }
                        nodeUI.getGraphUI().repaint();
                    });
                    assignButtonMenu.add(showButton);
                }
                if (!visibleEdges.isEmpty()) {
                    JMenuItem showButton = new JMenuItem("Hide all outgoing edges", UIUtils.getIconFromResources("actions/eye-slash.png"));
                    showButton.setToolTipText("Hides all outgoing edges");
                    showButton.addActionListener(e -> {
                        nodeUI.getGraphUI().getGraphHistory().addSnapshotBefore(new GraphChangedHistorySnapshot(getGraph(), "Hide edges"));
                        for (JIPipeGraphEdge target : visibleEdges) {
                            target.setUiHidden(true);
                        }
                        nodeUI.getGraphUI().repaint();
                    });
                    assignButtonMenu.add(showButton);
                }

            }
            Set<JIPipeDataSlot> availableTargets = getGraph().getAvailableTargets(slot, true, true);
            availableTargets.removeIf(slot -> !slot.getNode().isVisibleIn(compartment));

            JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("actions/find.png"));
            findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
            findAlgorithmButton.addActionListener(e -> findAlgorithm(slot));
            assignButtonMenu.add(findAlgorithmButton);
            if (!availableTargets.isEmpty())
                assignButtonMenu.addSeparator();

            for (JIPipeDataSlot target : sortSlotsByDistance(availableTargets)) {
                JMenuItem connectButton = new JMenuItem(target.getDisplayName(),
                        JIPipeUIDatatypeRegistry.getInstance().getIconFor(target.getAcceptedDataType()));
                connectButton.addActionListener(e -> connectSlot(slot, target));
                connectButton.setToolTipText(TooltipUtils.getAlgorithmTooltip(target.getNode().getInfo()));
                connectButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        JIPipeNodeUI targetNodeUI = getGraphUI().getNodeUIs().getOrDefault(target.getNode(), null);
                        if (targetNodeUI != null) {
                            if (target.isInput()) {
                                JIPipeDataSlotUI targetUI = targetNodeUI.getInputSlotUIs().getOrDefault(target.getName(), null);
                                if (targetUI != null) {
                                    getGraphUI().setCurrentConnectionDragTarget(targetUI);
                                    getGraphUI().setCurrentConnectionDragSource(JIPipeDataSlotUI.this);
                                }
                            }
                        }
                        getGraphUI().repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        getGraphUI().setCurrentConnectionDragSource(null);
                        getGraphUI().setCurrentConnectionDragTarget(null);
                        getGraphUI().repaint();
                    }
                });
                assignButtonMenu.add(connectButton);
            }

            if (slot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
                JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
                if (slotConfiguration.canModifyOutputSlots()) {
                    if (assignButtonMenu.getComponentCount() > 0)
                        assignButtonMenu.addSeparator();

                    JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("actions/delete.png"));
                    deleteButton.addActionListener(e -> deleteSlot());
                    assignButtonMenu.add(deleteButton);

                    JMenuItem editButton = new JMenuItem("Edit this slot", UIUtils.getIconFromResources("actions/edit.png"));
                    editButton.addActionListener(e -> editSlot());
                    assignButtonMenu.add(editButton);
                }
            }
            if (assignButtonMenu.getComponentCount() > 0)
                assignButtonMenu.addSeparator();
            JMenuItem relabelButton = new JMenuItem("Label this slot", UIUtils.getIconFromResources("actions/tag.png"));
            relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
            relabelButton.addActionListener(e -> relabelSlot());
            assignButtonMenu.add(relabelButton);
        }
    }

    private void installHighlightForConnect(JIPipeDataSlot source, JMenuItem connectButton) {
        connectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JIPipeNodeUI sourceNodeUI = getGraphUI().getNodeUIs().getOrDefault(source.getNode(), null);
                if (sourceNodeUI != null) {
                    if (source.isOutput()) {
                        JIPipeDataSlotUI sourceUI = sourceNodeUI.getOutputSlotUIs().getOrDefault(source.getName(), null);
                        if (sourceUI != null) {
                            getGraphUI().setCurrentConnectionDragTarget(JIPipeDataSlotUI.this);
                            getGraphUI().setCurrentConnectionDragSource(sourceUI);
                        }
                    }
                }
                getGraphUI().repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                getGraphUI().setCurrentConnectionDragSource(null);
                getGraphUI().setCurrentConnectionDragTarget(null);
                getGraphUI().repaint();
            }
        });
    }

    private void installHighlightForDisconnect(JMenuItem disconnectButton) {
        disconnectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                getGraphUI().setCurrentHighlightedForDisconnect(JIPipeDataSlotUI.this);
                getGraphUI().repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                getGraphUI().setCurrentHighlightedForDisconnect(null);
                getGraphUI().repaint();
            }
        });
    }

    private void editSlot() {
        EditAlgorithmSlotPanel.showDialog(this, getGraphUI().getGraphHistory(), slot);
    }

    private void relabelSlot() {
        String newLabel = JOptionPane.showInputDialog(this,
                "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                slot.getDefinition().getCustomName());
        getGraphUI().getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(slot.getNode(), "Relabel slot '" + slot.getDisplayName() + "'"));
        slot.getDefinition().setCustomName(newLabel);
    }

    private void deleteSlot() {
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
        getGraphUI().getGraphHistory().addSnapshotBefore(new SlotConfigurationHistorySnapshot(slot.getNode(), "Remove slot '" + slot.getDisplayName() + "'"));
        if (slot.isInput())
            slotConfiguration.removeInputSlot(slot.getName(), true);
        else if (slot.isOutput())
            slotConfiguration.removeOutputSlot(slot.getName(), true);
    }

    private void findAlgorithm(JIPipeDataSlot slot) {
        JIPipeAlgorithmFinderUI algorithmFinderUI = new JIPipeAlgorithmFinderUI(nodeUI.getGraphUI(), slot);
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

    /**
     * Calculates the size in grid coordinates
     *
     * @return the size
     */
    public abstract Dimension calculateGridSize();

    /**
     * Connects the two slots
     *
     * @param source source slot
     * @param target target slot
     */
    public void connectSlot(JIPipeDataSlot source, JIPipeDataSlot target) {
        if (getGraph().canConnect(source, target, true)) {
            JIPipeGraph graph = slot.getNode().getGraph();
            if (graph.getGraph().containsEdge(source, target))
                return;
            JIPipeGraphHistory graphHistory = nodeUI.getGraphUI().getGraphHistory();
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
        JIPipeGraph graph = slot.getNode().getGraph();
        JIPipeGraphHistory graphHistory = nodeUI.getGraphUI().getGraphHistory();
        if (slot.isInput()) {
            JIPipeDataSlot sourceSlot = graph.getSourceSlot(slot);
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

    protected Class<? extends JIPipeData> getSlotDataType() {
        if (getGraph() != null) {
            if (getGraph().containsNode(slot)) {
                JIPipeDataSlot sourceSlot = getGraph().getSourceSlot(slot);
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
     * @return The slot
     */
    public JIPipeDataSlot getSlot() {
        return slot;
    }

    /**
     * Should be triggered when the slots are changed
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmGraphChanged(GraphChangedEvent event) {
        if (getGraph().containsNode(slot)) {
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
            nodeUI.updateSize();
        }
    }

    /**
     * @return The compartment
     */
    public String getCompartment() {
        return compartment;
    }

}
