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

package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeStaticCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeUserCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class JIPipeRewireGraphEditorTool implements JIPipeToggleableGraphEditorTool {

    private JIPipeDesktopGraphEditorUI graphEditorUI;
    private JIPipeDesktopGraphNodeUISlotActiveArea currentRewireDragSource;

    private JIPipeDesktopGraphNodeUISlotActiveArea currentRewireDragTarget;

    @Override
    public String getName() {
        return "Rewire";
    }

    @Override
    public String getTooltip() {
        return "Drag one or multiple connections from one slot to another";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-node.png");
    }

    @Override
    public KeyStroke getKeyBinding() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
    }

    @Override
    public JIPipeDesktopGraphEditorUI getGraphEditor() {
        return graphEditorUI;
    }

    @Override
    public void setGraphEditor(JIPipeDesktopGraphEditorUI graphEditorUI) {
        this.graphEditorUI = graphEditorUI;
    }

    @Override
    public boolean supports(JIPipeDesktopGraphEditorUI graphEditorUI) {
        return graphEditorUI.getGraph().getAttachment(JIPipeGraphType.class) != JIPipeGraphType.ProjectCompartments;
    }

    @Override
    public int getPriority() {
        return -9700;
    }

    @Override
    public void activate() {
        setCurrentRewireDragSource(null);
    }

    @Override
    public int getCategory() {
        return -10000;
    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void paintBelowNodesAfterEdges(Graphics2D graphics2D) {
        paintCurrentlyDraggedRewire(graphics2D);
    }

    private void paintCurrentlyDraggedRewire(Graphics2D g) {
        if (currentRewireDragSource != null) {

            Point mousePosition = graphEditorUI.getCanvasUI().getLastMousePosition();

            if (mousePosition == null) {
                return;
            }

            g.setStroke(getGraphCanvas().getStrokeHighlight());
            g.setColor(Color.ORANGE);

            JIPipeGraph graph = graphEditorUI.getCanvasUI().getGraph();
            if (currentRewireDragSource.isInput()) {
                for (JIPipeDataSlot inputIncomingSourceSlot : graph.getInputIncomingSourceSlots(currentRewireDragSource.getSlot())) {
                    // The slot is an output
                    JIPipeDesktopGraphNodeUI nodeUI = graphEditorUI.getCanvasUI().getNodeUIs().get(inputIncomingSourceSlot.getNode());
                    if (nodeUI != null) {
                        PointRange slotLocation = nodeUI.getSlotLocation(inputIncomingSourceSlot);
                        slotLocation.add(nodeUI.getLocation());
                        paintRewireEdge(g, slotLocation, mousePosition);
                    }
                }
            } else {
                for (JIPipeDataSlot outputOutgoingTargetSlot : graph.getOutputOutgoingTargetSlots(currentRewireDragSource.getSlot())) {
                    // The slot is an input
                    JIPipeDesktopGraphNodeUI nodeUI = graphEditorUI.getCanvasUI().getNodeUIs().get(outputOutgoingTargetSlot.getNode());
                    if (nodeUI != null) {
                        PointRange slotLocation = nodeUI.getSlotLocation(outputOutgoingTargetSlot);
                        slotLocation.add(nodeUI.getLocation());
                        paintRewireEdge(g, slotLocation, mousePosition);
                    }
                }
            }
        }
    }

    private void paintRewireEdge(Graphics2D g, PointRange sourcePoint, Point mousePosition) {
        PointRange targetPoint = null;
        if (currentRewireDragTarget != null) {
            JIPipeDesktopGraphNodeUI nodeUI = graphEditorUI.getCanvasUI().getNodeUIs().get(currentRewireDragTarget.getSlot().getNode());
            PointRange slotLocation = nodeUI.getSlotLocation(currentRewireDragTarget.getSlot());
            slotLocation.add(nodeUI.getLocation());
            targetPoint = slotLocation;
        }
        if (targetPoint == null) {
            targetPoint = new PointRange(mousePosition.x, mousePosition.y);
        }
        // Tighten the point ranges: Bringing the centers together
        PointRange.tighten(sourcePoint, targetPoint);

        // Draw arrow
        if (currentRewireDragSource.isInput())
            graphEditorUI.getCanvasUI().paintEdge(g, sourcePoint.center, currentRewireDragSource.getNodeUI().getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
        else
            graphEditorUI.getCanvasUI().paintEdge(g, targetPoint.center, currentRewireDragSource.getNodeUI().getBounds(), sourcePoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            JIPipeDesktopGraphNodeUI nodeUI = graphEditorUI.getCanvasUI().pickNodeUI(e);
            if (nodeUI != null) {
                JIPipeDesktopGraphNodeUISlotActiveArea slot = nodeUI.pickSlotAtMousePosition(e);
                setCurrentRewireDragSource(slot);
                e.consume();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && currentRewireDragSource != null) {
            JIPipeDesktopGraphNodeUI nodeUI = graphEditorUI.getCanvasUI().pickNodeUI(e);
            if (nodeUI != null) {
                JIPipeDesktopGraphNodeUISlotActiveArea target = nodeUI.pickSlotAtMousePosition(e);
                if (target != null) {
                    currentRewireDragTarget = target;
                }
                rewire(currentRewireDragSource, currentRewireDragTarget);
                e.consume();
            }
        }
        setCurrentRewireDragSource(null);
    }

    private void rewire(JIPipeDesktopGraphNodeUISlotActiveArea source, JIPipeDesktopGraphNodeUISlotActiveArea target) {
        if (source == null || target == null) {
            return;
        }
        if (source == target) {
            return;
        }
        if (source.isInput() != target.isInput()) {
            return;
        }
        JIPipeDesktopGraphCanvasUI graphCanvasUI = graphEditorUI.getCanvasUI();
        JIPipeDataSlot currentSlot = currentRewireDragSource.getSlot();
        UUID compartment = graphEditorUI.getCompartment();
        Set<JIPipeDataSlot> enabledConnections;

        if (currentSlot.isInput()) {
            enabledConnections = graphCanvasUI.getGraph().getInputIncomingSourceSlots(currentSlot);
        } else {
            enabledConnections = graphCanvasUI.getGraph().getOutputOutgoingTargetSlots(currentSlot);
        }

        JIPipeDataSlot selectedAlternative = currentRewireDragTarget.getSlot();
        if (selectedAlternative == null) {
            JOptionPane.showMessageDialog(graphEditorUI,
                    "Please select an alternative target from the list.",
                    "No alternative selected",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JIPipeGraph graph = currentSlot.getNode().getParentGraph();
        JIPipeGraph copyGraph = new JIPipeGraph(graph);

        // First simulate everything
        try {
            JIPipeDataSlot copyCurrentSlot = copyGraph.getEquivalentSlot(currentSlot);
            JIPipeDataSlot copySelectedAlternative = copyGraph.getEquivalentSlot(selectedAlternative);
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                JIPipeDataSlot copyEnabledConnection = copyGraph.getEquivalentSlot(enabledConnection);
                if (copyCurrentSlot.isOutput()) {
                    if (!copyGraph.disconnect(copyCurrentSlot, copyEnabledConnection, true)) {
                        throw new RuntimeException("Unable to disconnect!");
                    }
                } else {
                    if (!copyGraph.disconnect(copyEnabledConnection, copyCurrentSlot, true)) {
                        throw new RuntimeException("Unable to disconnect!");
                    }
                }
            }
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                JIPipeDataSlot copyEnabledConnection = copyGraph.getEquivalentSlot(enabledConnection);
                if (copyCurrentSlot.isOutput()) {
                    copyGraph.connect(copySelectedAlternative, copyEnabledConnection, true);
                } else {
                    copyGraph.connect(copyEnabledConnection, copySelectedAlternative, true);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(graphEditorUI,
                    "The rewire operation failed. No changes were applied.\nPlease check if the new connections lead to the creation of loops.",
                    "Rewire not possible",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create snapshot
        if (graphCanvasUI.getHistoryJournal() != null) {
            graphCanvasUI.getHistoryJournal().snapshot("Rewire connection(s)",
                    "Rewire connections of " + currentSlot.getDisplayName() + " to " + selectedAlternative.getDisplayName(),
                    compartment,
                    UIUtils.getIconFromResources("actions/go-jump.png"));
        }

        // Simulation OK. Apply in real graph
        try {
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                if (currentSlot.isOutput()) {
                    if (!graph.disconnect(currentSlot, enabledConnection, true)) {
                        throw new RuntimeException("Unable to disconnect!");
                    }
                } else {
                    if (!graph.disconnect(enabledConnection, currentSlot, true)) {
                        throw new RuntimeException("Unable to disconnect!");
                    }
                }
            }
            for (JIPipeDataSlot enabledConnection : enabledConnections) {
                if (currentSlot.isOutput()) {
                    graph.connect(selectedAlternative, enabledConnection, true);
                } else {
                    graph.connect(enabledConnection, selectedAlternative, true);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(graphEditorUI,
                    "The rewire operation failed at phase 2. Please report this to the developer. JIPipe will attempt to undo the changes.",
                    "Rewire not possible",
                    JOptionPane.ERROR_MESSAGE);
            if (graphCanvasUI.getHistoryJournal() != null) {
                graphCanvasUI.getHistoryJournal().undo(compartment);
            }
        }

        // Select the targeted node
        graphCanvasUI.selectOnly(graphCanvasUI.getNodeUIs().get(selectedAlternative.getNode()));

        setCurrentRewireDragSource(null);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentRewireDragSource != null) {

            JIPipeDesktopGraphNodeUI nodeUI = graphEditorUI.getCanvasUI().pickNodeUI(e);
            if (nodeUI != null) {
                // Advanced dragging behavior
                boolean snapped = false;

                /*
                Auto snap to input/output if there is only one
                 */
                if (currentRewireDragSource.getSlot().isOutput()) {
                    if (nodeUI.getNode().getOutputSlots().size() == 1) {
                        if (!nodeUI.getOutputSlotMap().values().isEmpty()) {
                            // Auto snap to output
                            JIPipeDesktopGraphNodeUISlotActiveArea slotUI = nodeUI.getOutputSlotMap().values().iterator().next();
                            setCurrentRewireDragTarget(slotUI);
                            snapped = true;
                        }
                    }
                } else {
                    if (nodeUI.getNode().getInputSlots().size() == 1) {
                        // Auto snap to input
                        if (!nodeUI.getInputSlotMap().values().isEmpty()) {
                            JIPipeDesktopGraphNodeUISlotActiveArea slotUI = nodeUI.getInputSlotMap().values().iterator().next();
                            setCurrentRewireDragTarget(slotUI);
                            snapped = true;
                        }
                    }
                }

                /*
                Sticky snap: Stay in last snapped position if we were in it before
                 */
                if (currentRewireDragTarget != null && currentRewireDragTarget.getNodeUI() == nodeUI) {
                    JIPipeDesktopGraphNodeUISlotActiveArea slotState = nodeUI.pickSlotAtMousePosition(e);
                    if (slotState != null && slotState.getSlot().isInput() == currentRewireDragSource.getSlot().isInput()) {
                        setCurrentRewireDragTarget(slotState);
                    }
                    snapped = true;
                }

                /*
                Default: Snap exactly to input/output
                 */
                if (!snapped) {
                    JIPipeDesktopGraphNodeUISlotActiveArea slotState = nodeUI.pickSlotAtMousePosition(e);
                    if (slotState != null && slotState.getSlot().isInput() == currentRewireDragSource.getSlot().isInput()) {
                        setCurrentRewireDragTarget(slotState);
                    } else {
                        setCurrentRewireDragTarget(null);
                    }
                }
            } else {
                setCurrentRewireDragTarget(null);
            }

            // Prevent rewire to the output of the current compartment
            if (currentRewireDragSource.isOutput() && currentRewireDragTarget != null &&
                    currentRewireDragTarget.getSlot().getNode() instanceof JIPipeCompartmentOutput &&
                    Objects.equals(currentRewireDragTarget.getNodeUI().getNode().getCompartmentUUIDInParentGraph(), graphEditorUI.getCompartment())) {
                currentRewireDragTarget = null;
            }

            graphEditorUI.getCanvasUI().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            graphEditorUI.repaint(50);
            e.consume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (currentRewireDragSource != null) {
            graphEditorUI.repaint(50);
            e.consume();
        }
    }

    public JIPipeDesktopGraphNodeUISlotActiveArea getCurrentRewireDragTarget() {
        return currentRewireDragTarget;
    }

    public void setCurrentRewireDragTarget(JIPipeDesktopGraphNodeUISlotActiveArea currentRewireDragTarget) {
        this.currentRewireDragTarget = currentRewireDragTarget;
        graphEditorUI.getCanvasUI().repaint(50);
    }

    public JIPipeDesktopGraphNodeUISlotActiveArea getCurrentRewireDragSource() {
        return currentRewireDragSource;
    }

    public void setCurrentRewireDragSource(JIPipeDesktopGraphNodeUISlotActiveArea currentRewireDragSource) {
        this.currentRewireDragSource = currentRewireDragSource;
        if (currentRewireDragSource == null) {
            this.currentRewireDragTarget = null;
        }
        graphEditorUI.getCanvasUI().repaint(50);
    }

    @Override
    public boolean canRenderEdge(JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge) {
        if (currentRewireDragSource != null) {
            return source != currentRewireDragSource.getSlot() && target != currentRewireDragSource.getSlot();
        } else {
            return true;
        }
    }

    @Override
    public void deactivate() {
        setCurrentRewireDragSource(null);
    }

    @Override
    public boolean allowsDragNodes() {
        return true;
    }

    @Override
    public boolean allowsDragConnections() {
        return false;
    }
}
