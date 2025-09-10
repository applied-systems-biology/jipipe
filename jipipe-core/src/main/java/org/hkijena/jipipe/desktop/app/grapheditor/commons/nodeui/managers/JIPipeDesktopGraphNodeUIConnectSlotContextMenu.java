package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.managers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasUIConnectHighlight;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasUIDisconnectHighlight;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class JIPipeDesktopGraphNodeUIConnectSlotContextMenu {
    private final JIPipeDesktopGraphNodeUI nodeUI;

    public JIPipeDesktopGraphNodeUIConnectSlotContextMenu(JIPipeDesktopGraphNodeUI nodeUI) {
        this.nodeUI = nodeUI;
    }

    public void openSlotMenuAddInputManageExistingConnectionsMenuItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> sourceSlots, JMenu menu) {

        if (!sourceSlots.isEmpty()) {
            JMenuItem rewireItem = new JMenuItem("Rewire to different input ...", JIPipe.RESOURCES.getIcon16("actions/go-jump.png"));
            rewireItem.setToolTipText("Opens a tool that allows to rewire the connections of this slot to another input.");
            rewireItem.addActionListener(e -> nodeUI.getSlotManager().openRewireInputTool(slot, sourceSlots));
            menu.add(rewireItem);
        }

        JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(slot);
        for (JIPipeDataSlot sourceSlot : nodeUI.getSlotManager().sortSlotsByDistance(slot, sourceSlots)) {
            JMenu sourceSlotMenu = new JMenu("<html>" + sourceSlot.getName() + "<br><small>" + sourceSlot.getNode().getDisplayName() + "</small></html>");
            sourceSlotMenu.setIcon(JIPipe.getDataTypes().getIconFor(sourceSlot.getAcceptedDataType()));

            JMenuItem disconnectButton = new JMenuItem("Disconnect", JIPipe.RESOURCES.getIcon16("actions/cancel.png"));
            disconnectButton.addActionListener(e -> nodeUI.getGraphCanvasUI().disconnectAll(slot, Collections.singleton(sourceSlot)));
            if (slotActiveArea != null) {
                openSlotMenuInstallHighlightForDisconnect(slotActiveArea, disconnectButton, Collections.singleton(sourceSlot));
            }
            sourceSlotMenu.add(disconnectButton);

            JIPipeGraphEdge edge = nodeUI.getGraphCanvasUI().getGraph().getGraph().getEdge(sourceSlot, slot);

            // Shape menu
            nodeUI.getSlotContextMenu().openSlotMenuAddShapeToggle(slot, sourceSlotMenu, edge);

            menu.add(sourceSlotMenu);
        }
    }

    public void openSlotMenuAddOutputConnectTargetSlotItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> availableTargets, JMenu menu) {
        JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(slot);
        Object currentMenu = menu;
        int itemCount = 0;
        for (JIPipeDataSlot target : nodeUI.getSlotManager().sortSlotsByDistance(slot, availableTargets)) {
            if (itemCount >= 6) {
                JMenu moreMenu = new JMenu("More targets ...");
                if (currentMenu instanceof JMenu)
                    ((JMenu) currentMenu).add(moreMenu);
                else
                    ((JPopupMenu) currentMenu).add(moreMenu);
                currentMenu = moreMenu;
                itemCount = 0;
            }
            JMenuItem connectButton = new JMenuItem("<html>" + target.getNode().getName() + "<br/><small>" + target.getName() + "</html>",
                    JIPipe.getDataTypes().getIconFor(target.getAcceptedDataType()));
            connectButton.addActionListener(e -> nodeUI.getGraphCanvasUI().connectSlot(slot, target));
            connectButton.setToolTipText(TooltipUtils.getAlgorithmTooltip(target.getNode().getInfo()));
            JIPipeDesktopGraphNodeUI targetNodeUI = nodeUI.getGraphCanvasUI().getNodeUIs().getOrDefault(target.getNode(), null);

            if (targetNodeUI != null) {
                openSlotMenuInstallHighlightForConnect(slotActiveArea, target, connectButton);
            }

            if (currentMenu instanceof JMenu) {
                ((JMenu) currentMenu).add(connectButton);
            } else {
                ((JPopupMenu) currentMenu).add(connectButton);
            }
            ++itemCount;
        }
    }

    public void openSlotMenuAddInputConnectSourceSlotItems(JIPipeDataSlot slot, Set<JIPipeDataSlot> availableSources, JMenu menu) {
        UUID compartment = nodeUI.getGraphCanvasUI().getCompartmentUUID();
        availableSources.removeIf(s -> !s.getNode().isVisibleIn(compartment));
        JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(slot);

        Object currentMenu = menu;
        int itemCount = 0;
        for (JIPipeDataSlot source : nodeUI.getSlotManager().sortSlotsByDistance(slot, availableSources)) {
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
            JMenuItem connectButton = new JMenuItem("<html>" + source.getNode().getName() + "<br/><small>" + source.getName() + "</html>",
                    JIPipe.getDataTypes().getIconFor(source.getAcceptedDataType()));
            connectButton.addActionListener(e -> nodeUI.getGraphCanvasUI().connectSlot(source, slot));
            if (slotActiveArea != null) {
                openSlotMenuInstallHighlightForConnect(slotActiveArea, source, connectButton);
            }
            if (currentMenu instanceof JMenu) {
                ((JMenu) currentMenu).add(connectButton);
            } else {
                ((JPopupMenu) currentMenu).add(connectButton);
            }
            ++itemCount;
        }
    }

    private void openSlotMenuInstallHighlightForConnect(JIPipeDesktopGraphNodeUISlotActiveArea current, JIPipeDataSlot source, JMenuItem connectButton) {
        connectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JIPipeDesktopGraphNodeUI sourceNodeUI = nodeUI.getGraphCanvasUI().getNodeUIs().getOrDefault(source.getNode(), null);
                if (sourceNodeUI != null) {
                    if (source.isOutput()) {
                        JIPipeDesktopGraphNodeUISlotActiveArea sourceUI = sourceNodeUI.getOutputSlotMap().getOrDefault(source.getName(), null);
                        if (sourceUI != null) {
                            nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setConnectHighlights(Collections.singletonList(new JIPipeDesktopGraphCanvasUIConnectHighlight(sourceUI, current)));
                        }
                    } else {
                        JIPipeDesktopGraphNodeUISlotActiveArea sourceUI = sourceNodeUI.getInputSlotMap().getOrDefault(source.getName(), null);
                        if (sourceUI != null) {
                            nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setConnectHighlights(Collections.singletonList(new JIPipeDesktopGraphCanvasUIConnectHighlight(current, sourceUI)));
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setConnectHighlights(Collections.emptyList());
            }
        });
    }

    public void openSlotMenuInstallHighlightForDisconnect(JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea, JMenuItem disconnectButton, Set<JIPipeDataSlot> sourceSlots) {
        disconnectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setDisconnectHighlight(new JIPipeDesktopGraphCanvasUIDisconnectHighlight(slotActiveArea, sourceSlots));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                nodeUI.getGraphCanvasUI().getConnectionHighlightManager().setDisconnectHighlight(null);
            }
        });
    }
}
