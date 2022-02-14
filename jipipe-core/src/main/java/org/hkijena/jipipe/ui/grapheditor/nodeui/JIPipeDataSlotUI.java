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

package org.hkijena.jipipe.ui.grapheditor.nodeui;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.EditAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.algorithmfinder.AlgorithmFinderSuccessEvent;
import org.hkijena.jipipe.ui.grapheditor.algorithmfinder.JIPipeAlgorithmSourceFinderUI;
import org.hkijena.jipipe.ui.grapheditor.algorithmfinder.JIPipeAlgorithmTargetFinderUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * UI around an {@link JIPipeDataSlot}
 */
public abstract class JIPipeDataSlotUI extends JIPipeWorkbenchPanel {
    protected JPopupMenu assignButtonMenu;
    private JIPipeNodeUI nodeUI;
    private UUID compartment;
    private JIPipeDataSlot slot;

    /**
     * Creates a new UI
     *
     * @param workbench   the workbench
     * @param nodeUI      The parent algorithm UI
     * @param compartment The compartment ID
     * @param slot        The slot instance
     */
    public JIPipeDataSlotUI(JIPipeWorkbench workbench, JIPipeNodeUI nodeUI, UUID compartment, JIPipeDataSlot slot) {
        super(workbench);
        this.nodeUI = nodeUI;
        this.compartment = compartment;
        this.slot = slot;

        getGraph().getEventBus().register(this);
        slot.getInfo().getEventBus().register(this);
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
                getGraphUI().setCurrentHighlightedForDisconnect(null, Collections.emptySet());
                getGraphUI().repaint();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                getGraphUI().setCurrentConnectionDragSource(null);
                getGraphUI().setCurrentConnectionDragTarget(null);
                getGraphUI().setCurrentHighlightedForDisconnect(null, Collections.emptySet());
                getGraphUI().repaint();
            }
        });

        if (slot.isInput()) {
            JMenuItem findAlgorithmButton = new JMenuItem("Find matching algorithm ...", UIUtils.getIconFromResources("actions/find.png"));
            findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
            findAlgorithmButton.addActionListener(e -> findSourceAlgorithm(slot));
            assignButtonMenu.add(findAlgorithmButton);

            addInputConnectSourceSlotMenu();

            Set<JIPipeDataSlot> sourceSlots = getGraph().getSourceSlots(slot);

            if (!sourceSlots.isEmpty()) {
                JMenuItem disconnectButton = new JMenuItem("Disconnect all", UIUtils.getIconFromResources("actions/cancel.png"));
                disconnectButton.addActionListener(e -> disconnectAll(sourceSlots));
                installHighlightForDisconnect(disconnectButton, sourceSlots);
                assignButtonMenu.add(disconnectButton);
            }

            if (!sourceSlots.isEmpty())
                assignButtonMenu.addSeparator();

            addInputSourceSlotMenu(sourceSlots);

            if (!sourceSlots.isEmpty())
                assignButtonMenu.addSeparator();

            addInputSlotEditMenu(sourceSlots);

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
                    disconnectButton.addActionListener(e -> disconnectAll(targetSlots));
                    installHighlightForDisconnect(disconnectButton, targetSlots);
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
                        if (getGraphUI().getHistoryJournal() != null) {
                            getGraphUI().getHistoryJournal().snapshot("Un-hide edges",
                                    "Shown all edges of slot " + getSlot().getDisplayName(),
                                    getSlot().getNode().getCompartmentUUIDInGraph(),
                                    UIUtils.getIconFromResources("actions/eye.png"));
                        }
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
                        if (getGraphUI().getHistoryJournal() != null) {
                            getGraphUI().getHistoryJournal().snapshot("Hide edges",
                                    "Hidden all edges of slot " + getSlot().getDisplayName(),
                                    getSlot().getNode().getCompartmentUUIDInGraph(),
                                    UIUtils.getIconFromResources("actions/eye-slash.png"));
                        }
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
            findAlgorithmButton.addActionListener(e -> findTargetAlgorithm(slot));
            assignButtonMenu.add(findAlgorithmButton);

            if (!availableTargets.isEmpty())
                assignButtonMenu.addSeparator();

            addOutputConnectTargetSlotMenu(availableTargets);

            if (!targetSlots.isEmpty())
                assignButtonMenu.addSeparator();
            addOutputTargetSlotMenu(targetSlots);
            if (!targetSlots.isEmpty())
                assignButtonMenu.addSeparator();

            addOutputSlotEditMenu();

            if (assignButtonMenu.getComponentCount() > 0)
                assignButtonMenu.addSeparator();

            if (getSlot().getInfo().isSaveOutputs()) {
                JMenuItem toggleSaveOutputsButton = new JMenuItem("Disable saving outputs", UIUtils.getIconFromResources("actions/no-save.png"));
                toggleSaveOutputsButton.setToolTipText("Makes that the data stored in this slot are not saved in a full analysis. Does not have an effect when updating the cache.");
                toggleSaveOutputsButton.addActionListener(e -> setSaveOutputs(false));
                assignButtonMenu.add(toggleSaveOutputsButton);
            } else {
                JMenuItem toggleSaveOutputsButton = new JMenuItem("Enable saving outputs", UIUtils.getIconFromResources("actions/save.png"));
                toggleSaveOutputsButton.setToolTipText("Makes that the data stored in this slot are saved in a full analysis.");
                toggleSaveOutputsButton.addActionListener(e -> setSaveOutputs(true));
                assignButtonMenu.add(toggleSaveOutputsButton);
            }

            if (getSlot().isVirtual()) {
                JMenuItem toggleVirtualButton = new JMenuItem("Store in memory", UIUtils.getIconFromResources("devices/media-memory.png"));
                toggleVirtualButton.setToolTipText("Makes that data generated by this slot is stored in memory during a full analysis. This raises the requirements on system memory significantly if the data is very large.");
                toggleVirtualButton.addActionListener(e -> makeNonVirtual());
                assignButtonMenu.add(toggleVirtualButton);
            } else {
                JMenuItem toggleVirtualButton = new JMenuItem("Store on hard drive", UIUtils.getIconFromResources("actions/rabbitvcs-drive.png"));
                toggleVirtualButton.setToolTipText("Makes that data generated by this slot is stored in a temporary folder if not used during a full analysis. This is slower than memory access and requires disk space on the hard drive. Useful if you have large amounts of data. Only takes effect if you enable 'Reduce memory'.");
                toggleVirtualButton.addActionListener(e -> makeVirtual());
                assignButtonMenu.add(toggleVirtualButton);
            }
        }

        // Global actions at the end
        JMenuItem relabelButton = new JMenuItem("Label this slot", UIUtils.getIconFromResources("actions/tag.png"));
        relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
        relabelButton.addActionListener(e -> relabelSlot());
        assignButtonMenu.add(relabelButton);

        if ((slot.isInput() && nodeUI.getNode().getInputSlots().size() > 1) || (slot.isOutput() && nodeUI.getNode().getOutputSlots().size() > 1)) {
            JMenuItem moveUpButton = new JMenuItem(getGraphUI().getViewMode() == JIPipeGraphViewMode.Horizontal ? "Move up" : "Move to the left",
                    getGraphUI().getViewMode() == JIPipeGraphViewMode.Horizontal ? UIUtils.getIconFromResources("actions/up.png") : UIUtils.getIconFromResources("actions/go-left.png"));
            moveUpButton.setToolTipText("Reorders the slots");
            moveUpButton.addActionListener(e -> moveSlotUp());
            assignButtonMenu.add(moveUpButton);

            JMenuItem moveDownButton = new JMenuItem(getGraphUI().getViewMode() == JIPipeGraphViewMode.Horizontal ? "Move down" : "Move to the right",
                    getGraphUI().getViewMode() == JIPipeGraphViewMode.Horizontal ? UIUtils.getIconFromResources("actions/down.png") : UIUtils.getIconFromResources("actions/go-right.png"));
            moveDownButton.setToolTipText("Reorders the slots");
            moveDownButton.addActionListener(e -> moveSlotDown());
            assignButtonMenu.add(moveDownButton);
        }
    }

    private void addInputConnectSourceSlotMenu() {
        Set<JIPipeDataSlot> availableSources = getGraph().getAvailableSources(slot, true, false);
        availableSources.removeIf(slot -> !slot.getNode().isVisibleIn(compartment));

        Object currentMenu = assignButtonMenu;
        int itemCount = 0;
        for (JIPipeDataSlot source : sortSlotsByDistance(availableSources)) {
            if (!source.getNode().isVisibleIn(compartment))
                continue;
            if (itemCount >= 6) {
                JMenu moreMenu = new JMenu("More sources ...");
                if (currentMenu instanceof JMenu)
                    ((JMenu) currentMenu).add(moreMenu);
                else
                    ((JPopupMenu) currentMenu).add(moreMenu);
                currentMenu = moreMenu;
                itemCount = 0;
            }
            JMenuItem connectButton = new JMenuItem("Connect to " + source.getDisplayName(),
                    JIPipe.getDataTypes().getIconFor(source.getAcceptedDataType()));
            connectButton.addActionListener(e -> connectSlot(source, slot));
            installHighlightForConnect(source, connectButton);
            if (currentMenu instanceof JMenu)
                ((JMenu) currentMenu).add(connectButton);
            else
                ((JPopupMenu) currentMenu).add(connectButton);
            ++itemCount;
        }
    }

    private void addInputSlotEditMenu(Set<JIPipeDataSlot> sourceSlots) {
        if (slot.getInfo().isUserModifiable() && slot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
            JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
            if (slotConfiguration.canModifyInputSlots()) {
                if (assignButtonMenu.getComponentCount() > 0)
                    assignButtonMenu.addSeparator();
                JMenuItem deleteButton = new JMenuItem("Delete this slot", UIUtils.getIconFromResources("actions/delete.png"));
                deleteButton.addActionListener(e -> deleteSlot());
                installHighlightForDisconnect(deleteButton, sourceSlots);
                assignButtonMenu.add(deleteButton);

                JMenuItem editButton = new JMenuItem("Edit this slot", UIUtils.getIconFromResources("actions/edit.png"));
                editButton.addActionListener(e -> editSlot());
                assignButtonMenu.add(editButton);
            }
        }
    }

    private void addOutputSlotEditMenu() {
        if (slot.getInfo().isUserModifiable() && slot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration) {
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
    }

    private void addOutputConnectTargetSlotMenu(Set<JIPipeDataSlot> availableTargets) {
        Object currentMenu = assignButtonMenu;
        int itemCount = 0;
        for (JIPipeDataSlot target : sortSlotsByDistance(availableTargets)) {
            if (itemCount >= 6) {
                JMenu moreMenu = new JMenu("More targets ...");
                if (currentMenu instanceof JMenu)
                    ((JMenu) currentMenu).add(moreMenu);
                else
                    ((JPopupMenu) currentMenu).add(moreMenu);
                currentMenu = moreMenu;
                itemCount = 0;
            }
            JMenuItem connectButton = new JMenuItem(target.getDisplayName(),
                    JIPipe.getDataTypes().getIconFor(target.getAcceptedDataType()));
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
            if (currentMenu instanceof JMenu)
                ((JMenu) currentMenu).add(connectButton);
            else
                ((JPopupMenu) currentMenu).add(connectButton);
            ++itemCount;
        }
    }

    private void addInputSourceSlotMenu(Set<JIPipeDataSlot> sourceSlots) {
        JPopupMenu currentMenu = assignButtonMenu;
        for (JIPipeDataSlot sourceSlot : sortSlotsByDistance(sourceSlots)) {
            JMenu sourceSlotMenu = new JMenu(sourceSlot.getDisplayName());
            sourceSlotMenu.setIcon(JIPipe.getDataTypes().getIconFor(sourceSlot.getAcceptedDataType()));

            JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("actions/cancel.png"));
            disconnectButton.addActionListener(e -> disconnectAll(Collections.singleton(sourceSlot)));
            installHighlightForDisconnect(disconnectButton, Collections.singleton(sourceSlot));
            sourceSlotMenu.add(disconnectButton);

            JIPipeGraphEdge edge = nodeUI.getGraphUI().getGraph().getGraph().getEdge(sourceSlot, slot);
            if (edge.isUiHidden()) {
                JMenuItem showButton = new JMenuItem("Show incoming edge", UIUtils.getIconFromResources("actions/eye.png"));
                showButton.setToolTipText("Un-hides the incoming edge");
                showButton.addActionListener(e -> {
                    if (getGraphUI().getHistoryJournal() != null) {
                        getGraphUI().getHistoryJournal().snapshot("Un-hide edge",
                                "Shown the edge " + sourceSlot.getDisplayName() + " -> " + slot.getDisplayName(),
                                getNodeUI().getNode().getCompartmentUUIDInGraph(),
                                UIUtils.getIconFromResources("actions/eye.png"));
                    }
                    edge.setUiHidden(false);
                    nodeUI.getGraphUI().repaint();
                });
                installHighlightForConnect(sourceSlot, showButton);
                sourceSlotMenu.add(showButton);
            } else {
                JMenuItem hideButton = new JMenuItem("Hide incoming edge", UIUtils.getIconFromResources("actions/eye-slash.png"));
                hideButton.setToolTipText("Hides the incoming edge");
                hideButton.addActionListener(e -> {
                    if (getGraphUI().getHistoryJournal() != null) {
                        getGraphUI().getHistoryJournal().snapshot("Hide edge",
                                "Hidden the edge " + sourceSlot.getDisplayName() + " -> " + slot.getDisplayName(),
                                getNodeUI().getNode().getCompartmentUUIDInGraph(),
                                UIUtils.getIconFromResources("actions/eye-slash.png"));
                    }
                    edge.setUiHidden(true);
                    nodeUI.getGraphUI().repaint();
                });
                installHighlightForDisconnect(hideButton, Collections.singleton(sourceSlot));
                sourceSlotMenu.add(hideButton);
            }
            addShapeToggle(sourceSlotMenu, edge);
            currentMenu.add(sourceSlotMenu);
        }
    }

    private void addOutputTargetSlotMenu(Set<JIPipeDataSlot> targetSlots) {
        JPopupMenu currentMenu = assignButtonMenu;
        for (JIPipeDataSlot targetSlot : sortSlotsByDistance(targetSlots)) {
            JMenu targetSlotMenu = new JMenu(targetSlot.getDisplayName());
            targetSlotMenu.setIcon(JIPipe.getDataTypes().getIconFor(targetSlot.getAcceptedDataType()));

            JMenuItem disconnectButton = new JMenuItem("Disconnect", UIUtils.getIconFromResources("actions/cancel.png"));
            disconnectButton.addActionListener(e -> disconnectAll(Collections.singleton(targetSlot)));
            installHighlightForDisconnect(disconnectButton, Collections.singleton(targetSlot));
            targetSlotMenu.add(disconnectButton);

            JIPipeGraphEdge edge = nodeUI.getGraphUI().getGraph().getGraph().getEdge(slot, targetSlot);
            if (edge.isUiHidden()) {
                JMenuItem showButton = new JMenuItem("Show outgoing edge", UIUtils.getIconFromResources("actions/eye.png"));
                showButton.setToolTipText("Un-hides the outgoing edge");
                showButton.addActionListener(e -> {
                    if (getGraphUI().getHistoryJournal() != null) {
                        getGraphUI().getHistoryJournal().snapshot("Un-hide edge",
                                "Shown the edge " + slot.getDisplayName() + " -> " + targetSlot.getDisplayName(),
                                getNodeUI().getNode().getCompartmentUUIDInGraph(),
                                UIUtils.getIconFromResources("actions/eye.png"));
                    }
                    edge.setUiHidden(false);
                    nodeUI.getGraphUI().repaint();
                });
                installHighlightForConnect(targetSlot, showButton);
                targetSlotMenu.add(showButton);
            } else {
                JMenuItem hideButton = new JMenuItem("Hide outgoing edge", UIUtils.getIconFromResources("actions/eye-slash.png"));
                hideButton.setToolTipText("Hides the outgoing edge");
                hideButton.addActionListener(e -> {
                    if (getGraphUI().getHistoryJournal() != null) {
                        getGraphUI().getHistoryJournal().snapshot("Hide edge",
                                "Hidden the edge " + slot.getDisplayName() + " -> " + targetSlot.getDisplayName(),
                                getNodeUI().getNode().getCompartmentUUIDInGraph(),
                                UIUtils.getIconFromResources("actions/eye-slash.png"));
                    }
                    edge.setUiHidden(true);
                    nodeUI.getGraphUI().repaint();
                });
                installHighlightForDisconnect(hideButton, Collections.singleton(targetSlot));
                targetSlotMenu.add(hideButton);
            }
            addShapeToggle(targetSlotMenu, edge);
            currentMenu.add(targetSlotMenu);
        }
    }

    private void addShapeToggle(JMenu menu, JIPipeGraphEdge edge) {
        menu.addSeparator();
        if (edge.getUiShape() != JIPipeGraphEdge.Shape.Elbow) {
            JMenuItem setShapeItem = new JMenuItem("Draw as elbow", UIUtils.getIconFromResources("actions/standard-connector.png"));
            setShapeItem.addActionListener(e -> {
                if (getGraphUI().getHistoryJournal() != null) {
                    getGraphUI().getHistoryJournal().snapshot("Draw edge as elbow",
                            getSlot().getDisplayName(),
                            getNodeUI().getNode().getCompartmentUUIDInGraph(),
                            UIUtils.getIconFromResources("actions/standard-connector.png"));
                }
                edge.setUiShape(JIPipeGraphEdge.Shape.Elbow);
                nodeUI.getGraphUI().repaint();
            });
            menu.add(setShapeItem);
        }
        if (edge.getUiShape() != JIPipeGraphEdge.Shape.Line) {
            JMenuItem setShapeItem = new JMenuItem("Draw as line", UIUtils.getIconFromResources("actions/draw-line.png"));
            setShapeItem.addActionListener(e -> {
                if (getGraphUI().getHistoryJournal() != null) {
                    getGraphUI().getHistoryJournal().snapshot("Draw edge as line",
                            getSlot().getDisplayName(),
                            getNodeUI().getNode().getCompartmentUUIDInGraph(),
                            UIUtils.getIconFromResources("actions/draw-line.png"));
                }
                edge.setUiShape(JIPipeGraphEdge.Shape.Line);
                nodeUI.getGraphUI().repaint();
            });
            menu.add(setShapeItem);
        }
    }

    private void setSaveOutputs(boolean saveOutputs) {
        slot.getInfo().setSaveOutputs(saveOutputs);
        reloadButtonStatus();
    }

    private void makeVirtual() {
        slot.getInfo().setVirtual(true);
        reloadButtonStatus();
    }

    private void makeNonVirtual() {
        slot.getInfo().setVirtual(false);
        reloadButtonStatus();
    }

    private void findSourceAlgorithm(JIPipeDataSlot slot) {
        JIPipeAlgorithmSourceFinderUI algorithmFinderUI = new JIPipeAlgorithmSourceFinderUI(nodeUI.getGraphUI(), slot);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
        UIUtils.addEscapeListener(dialog);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(algorithmFinderUI);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(this);

        algorithmFinderUI.getEventBus().register(new Consumer<AlgorithmFinderSuccessEvent>() {
            @Override
            @Subscribe
            public void accept(AlgorithmFinderSuccessEvent event) {
                dialog.dispose();
            }
        });
//        Point slotLocation = new Point(getNodeUI().getSlotLocation(getSlot()).min);
//        slotLocation.x += getNodeUI().getX();
//        slotLocation.y += getNodeUI().getY();
//        getNodeUI().getGraphUI().setGraphEditCursor(slotLocation);

        dialog.setVisible(true);
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

    private void installHighlightForDisconnect(JMenuItem disconnectButton, Set<JIPipeDataSlot> sourceSlots) {
        disconnectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                getGraphUI().setCurrentHighlightedForDisconnect(JIPipeDataSlotUI.this, sourceSlots);
                getGraphUI().repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                getGraphUI().setCurrentHighlightedForDisconnect(null, Collections.emptySet());
                getGraphUI().repaint();
            }
        });
    }

    private void editSlot() {
        if (!JIPipeProjectWorkbench.canModifySlots(getWorkbench()))
            return;
        EditAlgorithmSlotPanel.showDialog(this, getGraphUI().getHistoryJournal(), slot);
    }

    private void relabelSlot() {
        String newLabel = JOptionPane.showInputDialog(this,
                "Please enter a new label for the slot.\nLeave the text empty to remove an existing label.",
                slot.getInfo().getCustomName());
        if (newLabel == null)
            return;
        if (getGraphUI().getHistoryJournal() != null) {
            getGraphUI().getHistoryJournal().snapshotBeforeLabelSlot(slot, slot.getNode().getCompartmentUUIDInGraph());
        }
        slot.getInfo().setCustomName(newLabel);
        getGraphUI().getWorkbench().setProjectModified(true);
    }

    private void deleteSlot() {
        if (!JIPipeProjectWorkbench.canModifySlots(getWorkbench()))
            return;
        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) slot.getNode().getSlotConfiguration();
        if (getGraphUI().getHistoryJournal() != null) {
            getGraphUI().getHistoryJournal().snapshotBeforeRemoveSlot(slot.getNode(), slot.getInfo(), slot.getNode().getCompartmentUUIDInGraph());
        }
        if (slot.isInput())
            slotConfiguration.removeInputSlot(slot.getName(), true);
        else if (slot.isOutput())
            slotConfiguration.removeOutputSlot(slot.getName(), true);
    }

    private void moveSlotDown() {
        if (slot != null) {
            if (getGraphUI().getHistoryJournal() != null) {
                getGraphUI().getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInGraph());
            }
            ((JIPipeMutableSlotConfiguration) nodeUI.getNode().getSlotConfiguration()).moveDown(slot.getName(), slot.getSlotType());
            getGraphUI().repaint();
        }
    }

    private void moveSlotUp() {
        if (slot != null) {
            if (getGraphUI().getHistoryJournal() != null) {
                getGraphUI().getHistoryJournal().snapshotBeforeMoveSlot(slot, slot.getNode().getCompartmentUUIDInGraph());
            }
            ((JIPipeMutableSlotConfiguration) nodeUI.getNode().getSlotConfiguration()).moveUp(slot.getName(), slot.getSlotType());
            getGraphUI().repaint();
        }
    }

    private void findTargetAlgorithm(JIPipeDataSlot slot) {
        JIPipeAlgorithmTargetFinderUI algorithmFinderUI = new JIPipeAlgorithmTargetFinderUI(nodeUI.getGraphUI(), slot);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Find matching algorithm");
        UIUtils.addEscapeListener(dialog);
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(algorithmFinderUI);
        dialog.pack();
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(this);

        algorithmFinderUI.getEventBus().register(new Consumer<AlgorithmFinderSuccessEvent>() {
            @Override
            @Subscribe
            public void accept(AlgorithmFinderSuccessEvent event) {
                dialog.dispose();
            }
        });
        if (getNodeUI().getGraphUI().isLayoutHelperEnabled()) {
            Point cursorLocation = new Point();
            if (getNodeUI().getGraphUI().getViewMode() == JIPipeGraphViewMode.Horizontal) {
                cursorLocation.x = getNodeUI().getRightX() + getNodeUI().getGraphUI().getViewMode().getGridWidth();
                cursorLocation.y = getNodeUI().getY();
            } else {
                cursorLocation.x = getNodeUI().getX();
                cursorLocation.y = getNodeUI().getBottomY() + getNodeUI().getGraphUI().getViewMode().getGridHeight();
            }
            getNodeUI().getGraphUI().setGraphEditCursor(cursorLocation);
            getNodeUI().getGraphUI().repaint();
        }

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
            if (getGraphUI().getHistoryJournal() != null) {
                getGraphUI().getHistoryJournal().snapshotBeforeConnect(source, target, getNodeUI().getNode().getCompartmentUUIDInGraph());
            }
            getGraph().connect(source, target);
        } else {
            UIUtils.showConnectionErrorMessage(this, source, target);
        }
    }

    /**
     * Disconnects two slots
     *
     * @param source the source
     * @param target the target
     */
    public void disconnectSlot(JIPipeDataSlot source, JIPipeDataSlot target) {
        if (getGraph().getGraph().containsEdge(source, target)) {
            if (getGraphUI().getHistoryJournal() != null) {
                getGraphUI().getHistoryJournal().snapshotBeforeDisconnect(source, target, getNodeUI().getNode().getCompartmentUUIDInGraph());
            }
            getGraph().disconnect(source, target, true);
        }
    }

    private void disconnectAll(Set<JIPipeDataSlot> otherSlots) {
        JIPipeGraph graph = slot.getNode().getGraph();
        if (getGraphUI().getHistoryJournal() != null) {
            getGraphUI().getHistoryJournal().snapshotBeforeDisconnectAll(slot, slot.getNode().getCompartmentUUIDInGraph());
        }
        if (slot.isInput()) {
            for (JIPipeDataSlot sourceSlot : otherSlots) {
                getGraph().disconnect(sourceSlot, slot, true);
            }
        } else {
            for (JIPipeDataSlot targetSlot : otherSlots) {
                getGraph().disconnect(slot, targetSlot, true);
            }
        }
    }

    /**
     * Is called when the name parameter was changed
     */
    protected abstract void reloadName();

    protected Class<? extends JIPipeData> getSlotDataType() {
        if (getGraph() != null) {
            if (getGraph().containsNode(slot)) {
                Set<JIPipeDataSlot> sourceSlots = getGraph().getSourceSlots(slot);
                if (!sourceSlots.isEmpty())
                    return sourceSlots.iterator().next().getAcceptedDataType();
            }
        }
        return slot.getAcceptedDataType();
    }

    /**
     * @return The name that should be displayed
     */
    public String getDisplayedName() {
        boolean hasCustomName = !StringUtils.isNullOrEmpty(slot.getInfo().getCustomName());
        if (hasCustomName) {
            return slot.getInfo().getCustomName();
        }
        String name = slot.getName();
        if (name.startsWith("{") && name.endsWith("}"))
            return name.substring(1, name.length() - 1);
        else
            return name;
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
    public void onAlgorithmGraphChanged(JIPipeGraph.GraphChangedEvent event) {
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
    public void onSlotNameChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("custom-name".equals(event.getKey())) {
            reloadName();
            nodeUI.updateSize();
        }
    }

    /**
     * @return The compartment
     */
    public UUID getCompartment() {
        return compartment;
    }

    public abstract boolean needsRecalculateGridSize();
}
