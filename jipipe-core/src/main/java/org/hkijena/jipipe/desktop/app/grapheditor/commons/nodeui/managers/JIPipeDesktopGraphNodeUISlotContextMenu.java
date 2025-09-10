package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.managers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.ViewOnlyMenuItem;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class JIPipeDesktopGraphNodeUISlotContextMenu {
    private final JIPipeDesktopGraphNodeUI nodeUI;

    public JIPipeDesktopGraphNodeUISlotContextMenu(JIPipeDesktopGraphNodeUI nodeUI) {
        this.nodeUI = nodeUI;
    }

    public void openSlotMenu(JIPipeDesktopGraphNodeUISlotActiveArea slotState, MouseEvent mouseEvent) {
        JIPipeDataSlot slot = slotState.getSlot();
        if (slot == null) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();

        openSlotMenuGenerateInformationItems(slotState, slot, menu);

        UIUtils.addSeparatorIfNeeded(menu);

        // Connection menus
        if (slotState.isInput()) {
            openSlotMenuAddInputSlotMenuItems(slot, menu);
        } else {
            openSlotMenuAddOutputSlotMenuItems(slot, menu);
        }

        // Customization

        UIUtils.addSeparatorIfNeeded(menu);

        // Global actions at the end
        JMenuItem relabelButton = new JMenuItem("Label this slot", JIPipe.RESOURCES.getIcon16("actions/tag.png"));
        relabelButton.setToolTipText("Sets a custom name for this slot without deleting it");
        relabelButton.addActionListener(e -> nodeUI.getSlotManager().relabelSlot(slotState.getSlot()));
        menu.add(relabelButton);

        if ((slot.isInput() && nodeUI.getNode().getInputSlots().size() > 1) || (slot.isOutput() && nodeUI.getNode().getOutputSlots().size() > 1)) {
            JMenuItem moveUpButton = new JMenuItem("Move to the left",
                    JIPipe.RESOURCES.getIcon16("actions/go-left.png"));
            moveUpButton.setToolTipText("Reorders the slots");
            moveUpButton.addActionListener(e -> nodeUI.getSlotManager().moveSlotLeft(slotState.getSlot()));
            menu.add(moveUpButton);

            JMenuItem moveDownButton = new JMenuItem("Move to the right",
                    JIPipe.RESOURCES.getIcon16("actions/go-right.png"));
            moveDownButton.setToolTipText("Reorders the slots");
            moveDownButton.addActionListener(e -> nodeUI.getSlotManager().moveSlotRight(slotState.getSlot()));
            menu.add(moveDownButton);
        }

        MouseEvent convertMouseEvent = SwingUtilities.convertMouseEvent(nodeUI.getGraphCanvasUI(), mouseEvent, nodeUI);
        Point mousePosition = convertMouseEvent.getPoint();

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                nodeUI.getGraphCanvasUI().getDragManagerConnect().setCurrentConnectionDragSource(null);
                nodeUI.getGraphCanvasUI().getDragManagerConnect().setCurrentConnectionDragTarget(null);
                nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setDisconnectHighlight(null);
                nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setConnectHighlight(null);
                nodeUI.invalidateAndRepaint(false, true);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                nodeUI.getGraphCanvasUI().getDragManagerConnect().setCurrentConnectionDragSource(null);
                nodeUI.getGraphCanvasUI().getDragManagerConnect().setCurrentConnectionDragTarget(null);
                nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setDisconnectHighlight(null);
                nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setConnectHighlight(null);
                nodeUI.invalidateAndRepaint(false, true);
            }
        });

        menu.show(nodeUI, mousePosition.x, mousePosition.y);
    }

    private void openSlotMenuAddOutputSlotMenuItems(JIPipeDataSlot slot, JPopupMenu menu) {
        Set<JIPipeDataSlot> targetSlots = nodeUI.getGraphCanvasUI().getGraph().getOutputOutgoingTargetSlots(slot);
        if (!targetSlots.isEmpty()) {

            boolean allowDisconnect = false;
            for (JIPipeDataSlot targetSlot : targetSlots) {
                if (nodeUI.getGraphCanvasUI().getGraph().canUserDisconnect(slot, targetSlot)) {
                    allowDisconnect = true;
                    break;
                }
            }

            if (allowDisconnect) {
                JMenuItem disconnectButton = new JMenuItem("Disconnect all", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
                disconnectButton.addActionListener(e -> nodeUI.getGraphCanvasUI().disconnectAll(slot, targetSlots));
                JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(slot);
                if (slotActiveArea != null) {
                    nodeUI.getConnectSlotContextMenu().openSlotMenuInstallHighlightForDisconnect(slotActiveArea, disconnectButton, targetSlots);
                }
                menu.add(disconnectButton);

                UIUtils.addSeparatorIfNeeded(menu);
            }
        }

        UUID compartment = nodeUI.getGraphCanvasUI().getCompartmentUUID();
        Set<JIPipeDataSlot> availableTargets = nodeUI.getGraphCanvasUI().getGraph().getAvailableTargets(slot, true, true);
        availableTargets.removeIf(s -> !s.getNode().isVisibleIn(compartment));

        JMenuItem findAlgorithmButton = new JMenuItem("Find matching node ...", JIPipe.RESOURCES.getIcon16("actions/find.png"));
        findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
        findAlgorithmButton.addActionListener(e -> nodeUI.getSlotManager().openOutputAlgorithmFinder(slot));
        menu.add(findAlgorithmButton);

        if (!availableTargets.isEmpty()) {
            JMenu connectMenu = new JMenu("Connect to ...");
            connectMenu.setIcon(JIPipe.RESOURCES.getIcon16("actions/plug.png"));
            nodeUI.getConnectSlotContextMenu().openSlotMenuAddOutputConnectTargetSlotItems(slot, availableTargets, connectMenu);
            menu.add(connectMenu);
        }
        if (!targetSlots.isEmpty()) {
            JMenu manageMenu = new JMenu("Manage existing connections ...");
            manageMenu.setIcon(JIPipe.RESOURCES.getIcon16("actions/lines-connector.png"));
            openSlotMenuAddOutputManageExistingConnectionsMenuItems(slot, targetSlots, manageMenu);
            menu.add(manageMenu);
        }
        if (!targetSlots.isEmpty()) {
            // Customize menu
            JMenu edgeMenu = new JMenu("Customize edges");
            edgeMenu.setIcon(JIPipe.RESOURCES.getIcon16("actions/draw-connector.png"));
            edgeMenu.add(UIUtils.createMenuItem("Draw all outputs as elbow",
                    "All outgoing edges will be drawn as elbow",
                    JIPipe.RESOURCES.getIcon16("actions/standard-connector.png"),
                    () -> nodeUI.getEdgeManager().setOutputEdgesShape(JIPipeGraphEdge.Shape.Elbow)));
            edgeMenu.add(UIUtils.createMenuItem("Draw all outputs as line",
                    "All outgoing edges will be drawn as line",
                    JIPipe.RESOURCES.getIcon16("actions/draw-line.png"),
                    () -> nodeUI.getEdgeManager().setOutputEdgesShape(JIPipeGraphEdge.Shape.Line)));
            menu.add(edgeMenu);
        }

        UIUtils.addSeparatorIfNeeded(menu);

        openSlotMenuAddOutputSlotEditItems(slot, menu);

        UIUtils.addSeparatorIfNeeded(menu);

        if (!(nodeUI.getNode() instanceof JIPipeProjectCompartment)) {
            if (slot.getInfo().isStoreToDisk()) {
                JMenuItem toggleSaveOutputsButton = new JMenuItem("Disable saving outputs", JIPipe.RESOURCES.getIcon16("actions/no-save.png"));
                toggleSaveOutputsButton.setToolTipText("Makes that the data stored in this slot are not saved in a full analysis. Does not have an effect when updating the cache.");
                toggleSaveOutputsButton.addActionListener(e -> nodeUI.getSlotManager().setSaveOutputs(slot, false));
                menu.add(toggleSaveOutputsButton);
            } else {
                JMenuItem toggleSaveOutputsButton = new JMenuItem("Enable saving outputs", JIPipe.RESOURCES.getIcon16("actions/filesave.png"));
                toggleSaveOutputsButton.setToolTipText("Makes that the data stored in this slot are saved in a full analysis.");
                toggleSaveOutputsButton.addActionListener(e -> nodeUI.getSlotManager().setSaveOutputs(slot, true));
                menu.add(toggleSaveOutputsButton);
            }
        }

    }

    private void openSlotMenuAddOutputSlotEditItems(JIPipeDataSlot slot, JPopupMenu menu) {
        if (slot.getInfo().isUserModifiable() && slot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration slotConfiguration) {
            if (slotConfiguration.canModifyOutputSlots()) {
                UIUtils.addSeparatorIfNeeded(menu);

                JMenuItem deleteButton = new JMenuItem("Delete this slot", JIPipe.RESOURCES.getIcon16("actions/delete.png"));
                deleteButton.addActionListener(e -> nodeUI.getSlotManager().deleteSlot(slot));
                menu.add(deleteButton);

                JMenuItem editButton = new JMenuItem("Edit this slot", JIPipe.RESOURCES.getIcon16("actions/edit.png"));
                editButton.addActionListener(e -> nodeUI.getSlotManager().editSlot(slot));
                menu.add(editButton);
            }
        }
    }

    private void openSlotMenuGenerateInformationItems(JIPipeDesktopGraphNodeUISlotActiveArea slotState, JIPipeDataSlot slot, JPopupMenu menu) {
        // Information item
        JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(slot.getAcceptedDataType());
        ViewOnlyMenuItem infoItem = new ViewOnlyMenuItem("<html>" + dataInfo.getName() + "<br><small>" + StringUtils.orElse(dataInfo.getDescription(), "No description provided") + "</small></html>", JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()));
        menu.add(infoItem);

        // Optional info
        if (slot.getInfo().isOptional()) {
            menu.add(new ViewOnlyMenuItem("<html>Optional slot<br><small>This slot requires no input connections.</small></html>", JIPipe.RESOURCES.getIcon16("actions/checkbox.png")));
        }

        // Role information item
        if (slot.getInfo().getRole() == JIPipeDataSlotRole.Parameters) {
            ViewOnlyMenuItem roleInfoItem = new ViewOnlyMenuItem("<html>Parameter-like data<br><small>This slot contains parametric data that is not considered for iteration step generation.</small></html>", JIPipe.RESOURCES.getIcon16("actions/wrench.png"));
            menu.add(roleInfoItem);
        } else if (slot.getInfo().getRole() == JIPipeDataSlotRole.ParametersLooping) {
            ViewOnlyMenuItem roleInfoItem = new ViewOnlyMenuItem("<html>Parameter-like data<br><small>This slot contains parametric data that is not considered for iteration step generation. Workloads may be repeated per input of this slot.</small></html>",
                    JIPipe.RESOURCES.getIcon16("actions/wrench.png"));
            menu.add(roleInfoItem);
        }

        // Input info
        List<ViewOnlyMenuItem> additionalItems = new ArrayList<>();
        nodeUI.getNode().createUIInputSlotIconDescriptionMenuItems(slot.getName(), additionalItems);
        for (ViewOnlyMenuItem additionalItem : additionalItems) {
            menu.add(additionalItem);
        }

        // Missing input item
        if (slotState.getSlotStatus() == JIPipeDesktopGraphNodeUI.SlotStatus.Unconnected) {
            menu.add(new ViewOnlyMenuItem("<html>This slot is not connected to an output!<br/><small>The node will not be able to work with a missing input.</small></html>", JIPipe.RESOURCES.getIcon16("emblems/warning.png")));
        }

        // Cache info
        if (nodeUI.getNode().getParentGraph() != null) {
            JIPipeGraph graph = nodeUI.getNode().getParentGraph();
            Map<String, JIPipeDataTable> cachedData = null;
            if (graph != null && graph.getProject() != null) {
                cachedData = graph.getProject().getCache().query(nodeUI.getNode(), nodeUI.getNode().getUUIDInParentGraph(), new JIPipeProgressInfo());
            }
            if (cachedData != null && cachedData.containsKey(slotState.getSlotName())) {
                int itemCount = cachedData.get(slotState.getSlotName()).getRowCount();
                ViewOnlyMenuItem cacheInfoItem = new ViewOnlyMenuItem("<html>Outputs are cached<br/><small>" + (itemCount == 1 ? "1 item" : itemCount + " items") + " </small></html>",
                        JIPipe.RESOURCES.getIcon16("actions/database.png"));
                menu.add(cacheInfoItem);
            }
        }
    }

    private void openSlotMenuAddInputSlotEditItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> sourceSlots, JPopupMenu menu) {
        if (slot.getInfo().isUserModifiable() && slot.getNode().getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration slotConfiguration) {
            if (slotConfiguration.canModifyInputSlots()) {
                UIUtils.addSeparatorIfNeeded(menu);
                JMenuItem deleteButton = new JMenuItem("Delete this slot", JIPipe.RESOURCES.getIcon16("actions/delete.png"));
                deleteButton.addActionListener(e -> nodeUI.getSlotManager().deleteSlot(slot));
                JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(slot);
                if (slotActiveArea != null) {
                    nodeUI.getConnectSlotContextMenu().openSlotMenuInstallHighlightForDisconnect(slotActiveArea, deleteButton, sourceSlots);
                }
                menu.add(deleteButton);

                JMenuItem editButton = new JMenuItem("Edit this slot", JIPipe.RESOURCES.getIcon16("actions/edit.png"));
                editButton.addActionListener(e -> nodeUI.getSlotManager().editSlot(slot));
                menu.add(editButton);
            }
        }
    }


    private void openSlotMenuAddOutputManageExistingConnectionsMenuItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> targetSlots, JMenu menu) {

        if (!targetSlots.isEmpty()) {
            JMenuItem rewireItem = new JMenuItem("Rewire to different output ...", JIPipe.RESOURCES.getIcon16("actions/go-jump.png"));
            rewireItem.setToolTipText("Opens a tool that allows to rewire the connections of this slot to another output.");
            rewireItem.addActionListener(e -> nodeUI.getSlotManager().openRewireOutputTool(slot, targetSlots));
            menu.add(rewireItem);
        }

        for (JIPipeDataSlot targetSlot : nodeUI.getSlotManager().sortSlotsByDistance(slot, targetSlots)) {
            JMenu targetSlotMenu = new JMenu("<html>" + targetSlot.getName() + "<br><small>" + targetSlot.getNode().getDisplayName() + "</small></html>");
            targetSlotMenu.setIcon(JIPipe.getDataTypes().getIconFor(targetSlot.getAcceptedDataType()));

            JMenuItem disconnectButton = new JMenuItem("Disconnect", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
            disconnectButton.addActionListener(e -> nodeUI.getGraphCanvasUI().disconnectAll(slot, Collections.singleton(targetSlot)));
            JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(slot);
            if (slotActiveArea != null) {
                nodeUI.getConnectSlotContextMenu().openSlotMenuInstallHighlightForDisconnect(slotActiveArea, disconnectButton, Collections.singleton(targetSlot));
            }
            targetSlotMenu.add(disconnectButton);

            JIPipeGraphEdge edge = nodeUI.getGraphCanvasUI().getGraph().getGraph().getEdge(slot, targetSlot);

            // Shape menu
            openSlotMenuAddShapeToggle(slot, targetSlotMenu, edge);

            menu.add(targetSlotMenu);
        }
    }

    public void openSlotMenuAddShapeToggle(JIPipeDataSlot slot, JMenu menu, JIPipeGraphEdge edge) {
        UIUtils.addSeparatorIfNeeded(menu);
        if (edge.getUiShape() != JIPipeGraphEdge.Shape.Elbow) {
            JMenuItem setShapeItem = new JMenuItem("Draw as elbow", JIPipe.RESOURCES.getIcon16("actions/standard-connector.png"));
            setShapeItem.addActionListener(e -> {
                if (nodeUI.getGraphCanvasUI().getHistoryJournal() != null) {
                    nodeUI.getGraphCanvasUI().getHistoryJournal().snapshot("Draw edge as elbow",
                            slot.getDisplayName(),
                            nodeUI.getNode().getCompartmentUUIDInParentGraph(),
                            JIPipe.RESOURCES.getIcon16("actions/standard-connector.png"));
                }
                edge.setUiShape(JIPipeGraphEdge.Shape.Elbow);
                nodeUI.invalidateAndRepaint(false, true);
            });
            menu.add(setShapeItem);
        }
        if (edge.getUiShape() != JIPipeGraphEdge.Shape.Line) {
            JMenuItem setShapeItem = new JMenuItem("Draw as line", JIPipe.RESOURCES.getIcon16("actions/draw-line.png"));
            setShapeItem.addActionListener(e -> {
                if (nodeUI.getGraphCanvasUI().getHistoryJournal() != null) {
                    nodeUI.getGraphCanvasUI().getHistoryJournal().snapshot("Draw edge as line",
                            slot.getDisplayName(),
                            nodeUI.getNode().getCompartmentUUIDInParentGraph(),
                            JIPipe.RESOURCES.getIcon16("actions/draw-line.png"));
                }
                edge.setUiShape(JIPipeGraphEdge.Shape.Line);
                nodeUI.invalidateAndRepaint(false, true);
            });
            menu.add(setShapeItem);
        }
    }

    private void openSlotMenuAddInputSlotMenuItems(JIPipeDataSlot slot, JPopupMenu menu) {

        Set<JIPipeDataSlot> sourceSlots = nodeUI.getGraphCanvasUI().getGraph().getInputIncomingSourceSlots(slot);

        if (!sourceSlots.isEmpty()) {
            JMenuItem disconnectButton = new JMenuItem("Disconnect all", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
            disconnectButton.addActionListener(e -> nodeUI.getGraphCanvasUI().disconnectAll(slot, sourceSlots));
            JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(slot);
            if (slotActiveArea != null) {
                nodeUI.getConnectSlotContextMenu().openSlotMenuInstallHighlightForDisconnect(slotActiveArea, disconnectButton, sourceSlots);
            }
            menu.add(disconnectButton);
        }

        UIUtils.addSeparatorIfNeeded(menu);

        JMenuItem findAlgorithmButton = new JMenuItem("Find matching node ...", JIPipe.RESOURCES.getIcon16("actions/find.png"));
        findAlgorithmButton.setToolTipText("Opens a tool to find a matching algorithm based on the data");
        findAlgorithmButton.addActionListener(e -> nodeUI.getSlotManager().openInputAlgorithmFinder(slot));
        menu.add(findAlgorithmButton);

        // Special case for parameter slots
        if (slot.getName().equals(JIPipeParameterSlotAlgorithm.SLOT_PARAMETERS)) {
            menu.add(UIUtils.createMenuItem("Create parameter sets ...", "Creates a nodes that supplies a selection of parameter data for the external parameters",
                    JIPipe.RESOURCES.getIcon16("data-types/parameters.png"), () -> nodeUI.getSlotManager().createParameterSetsNode(slot)));
        }

        Set<JIPipeDataSlot> availableSources = nodeUI.getGraphCanvasUI().getGraph().getAvailableSources(slot, true, false);
        if (!availableSources.isEmpty()) {
            JMenu connectMenu = new JMenu("Connect to ...");
            connectMenu.setIcon(JIPipe.RESOURCES.getIcon16("actions/plug.png"));
            nodeUI.getConnectSlotContextMenu().openSlotMenuAddInputConnectSourceSlotItems(slot, availableSources, connectMenu);
            menu.add(connectMenu);
        }

        if (!sourceSlots.isEmpty()) {
            JMenu manageMenu = new JMenu("Manage existing connections ...");
            manageMenu.setIcon(JIPipe.RESOURCES.getIcon16("actions/lines-connector.png"));
            nodeUI.getConnectSlotContextMenu().openSlotMenuAddInputManageExistingConnectionsMenuItems(slot, sourceSlots, manageMenu);
            menu.add(manageMenu);
        }
        if (!sourceSlots.isEmpty()) {
            // Customize menu
            JMenu edgeMenu = new JMenu("Customize edges");
            edgeMenu.setIcon(JIPipe.RESOURCES.getIcon16("actions/draw-connector.png"));
            edgeMenu.add(UIUtils.createMenuItem("Draw all inputs as elbow",
                    "All outgoing edges will be drawn as elbow",
                    JIPipe.RESOURCES.getIcon16("actions/standard-connector.png"),
                    () -> nodeUI.getEdgeManager().setInputEdgesShape(JIPipeGraphEdge.Shape.Elbow)));
            edgeMenu.add(UIUtils.createMenuItem("Draw all inputs as line",
                    "All outgoing edges will be drawn as line",
                    JIPipe.RESOURCES.getIcon16("actions/draw-line.png"),
                    () -> nodeUI.getEdgeManager().setInputEdgesShape(JIPipeGraphEdge.Shape.Line)));
            menu.add(edgeMenu);
        }

        UIUtils.addSeparatorIfNeeded(menu);

        openSlotMenuAddInputSlotEditItems(slot, sourceSlots, menu);
    }
}
