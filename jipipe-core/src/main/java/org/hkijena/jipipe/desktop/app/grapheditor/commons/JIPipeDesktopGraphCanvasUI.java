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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.*;
import com.google.common.collect.Sets;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDefaultGraphEditorTool;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartitionConfiguration;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.actions.JIPipeDesktopOpenContextMenuAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays.*;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.layout.MSTGraphAutoLayoutImplementation;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.layout.SugiyamaGraphAutoLayoutImplementation;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUIActiveArea;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUIUpdateViewCommand;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.desktop.app.settings.JIPipeDesktopRuntimePartitionListEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopAddAlgorithmSlotPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopZoomViewPort;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.ui.ScreenImage;
import org.hkijena.jipipe.utils.ui.ScreenImageSVG;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.scijava.Disposable;

import javax.swing.*;
import javax.swing.FocusManager;
import javax.tools.Tool;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * UI that displays an {@link JIPipeGraph}
 */
public class JIPipeDesktopGraphCanvasUI extends JLayeredPane implements JIPipeDesktopWorkbenchAccess, MouseMotionListener, MouseListener, MouseWheelListener, JIPipeDesktopZoomViewPort, Disposable,
        JIPipeGraph.GraphChangedEventListener, JIPipeGraph.NodeConnectedEventListener, JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEventListener {

    private final JIPipeDesktopWorkbench desktopWorkbench;
    private final JIPipeDesktopGraphEditorUI graphEditorUI;

    private final JIPipeGraph graph;

    public final JIPipeDesktopGraphCanvasNodeManager nodeManager = new JIPipeDesktopGraphCanvasNodeManager(this);
    private final JIPipeDesktopGraphCanvasEdgeManager edgeManager = new JIPipeDesktopGraphCanvasEdgeManager(this);
    private final JIPipeDesktopGraphCanvasDragManagerConnect dragManagerConnect = new JIPipeDesktopGraphCanvasDragManagerConnect(this);
    private final JIPipeDesktopGraphCanvasDragManagerMove dragManagerMove = new JIPipeDesktopGraphCanvasDragManagerMove(this);
    private final JIPipeDesktopGraphCanvasConnectionHighlightManager connectionHighlightManager = new JIPipeDesktopGraphCanvasConnectionHighlightManager(this);
    private final JIPipeDesktopGraphCanvasSelectionManager selectionManager = new JIPipeDesktopGraphCanvasSelectionManager(this);
    private final JIPipeDesktopGraphCanvasSelectionBoxManager selectionBoxManager = new JIPipeDesktopGraphCanvasSelectionBoxManager(this);
    private final JIPipeDesktopGraphCanvasNodeResizeManager resizeManager = new JIPipeDesktopGraphCanvasNodeResizeManager(this);
    private final JIPipeDesktopGraphCanvasToolManager toolManager = new JIPipeDesktopGraphCanvasToolManager(this);
    private final JIPipeDesktopGraphCanvasResources resources = new JIPipeDesktopGraphCanvasResources(this);

    private final List<JIPipeDesktopGraphCanvasOverlay> overlays = new ArrayList<>();

    private final BiMap<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> nodeUIs = HashBiMap.create();
    private final BiMap<JIPipeGraphEdge, JIPipeDesktopGraphEdgeUI> edgeUIs = HashBiMap.create();
    private final JIPipeGraphEditorUIApplicationSettings settings;
    private final JIPipeHistoryJournal historyJournal;
    private final UUID compartmentUUID;

    private final Map<?, ?> desktopRenderingHints = UIUtils.getDesktopRenderingHints();
    private final ZoomChangedEventEmitter zoomChangedEventEmitter = new ZoomChangedEventEmitter();
    private final JIPipeDesktopGraphCanvasUIUpdatedEventEmitter graphCanvasUpdatedEventEmitter = new JIPipeDesktopGraphCanvasUIUpdatedEventEmitter();
    private final JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEventEmitter defaultNodeUIActionRequestedEventEmitter = new JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEventEmitter();
    private final JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEventEmitter nodeUIActionRequestedEventEmitter = new JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEventEmitter();
    private final StampedLock stampedLock = new StampedLock();
    private JIPipeDesktopGraphDragAndDropBehavior dragAndDropBehavior;
    private Point graphEditCursor;

    private List<GraphInteractiveObjectUIContextAction> contextActions = new ArrayList<>();
    private double zoom = 1.0;
    private int currentNodeLayer = 0;
    private boolean renderCursor = true;
    private boolean renderOutsideEdges = true;
    /**
     * Used to store the minimum dimensions of the canvas to reduce user disruption
     */
    private Dimension minDimensions = null;
    private JIPipeDesktopGraphNodeUI currentlyMouseEnteredNode;
    private JIPipeDesktopGraphNodeUIActiveArea currentlyMouseEnteredNodeActiveArea;

    private List<JIPipeDesktopGraphEdgeUI> lastDisplayedMainEdges;
    private boolean autoMuteEdges;
    private Point lastMousePosition;
    private boolean mouseIsEntered;
    private boolean disposed;
    private boolean graphAnnotationsLocked;

    /**
     * Creates a new UI
     *
     * @param desktopWorkbench the workbench
     * @param graphEditorUI    the graph editor UI that contains this canvas. can be null.
     * @param graph            The algorithm graph
     * @param compartmentUUID  The compartment to show
     * @param historyJournal   object that tracks the history of this graph. Set to null to disable the undo feature.
     */
    public JIPipeDesktopGraphCanvasUI(JIPipeDesktopWorkbench desktopWorkbench,
                                      JIPipeDesktopGraphEditorUI graphEditorUI,
                                      JIPipeGraph graph,
                                      UUID compartmentUUID,
                                      JIPipeHistoryJournal historyJournal) {
        this.desktopWorkbench = desktopWorkbench;
        this.graphEditorUI = graphEditorUI;
        this.historyJournal = historyJournal;
        setLayout(null);
        this.graph = graph;
        this.compartmentUUID = compartmentUUID;
        this.settings = JIPipeGraphEditorUIApplicationSettings.getInstance();
        this.autoMuteEdges = settings.isAutoMuteEdgesEnabled();

        initialize();

        // Initialize paint layers
        initializeOverlays();

        // Add the initial set of nodes and edges
        addNewNodes(true);
        addNewEdges();

        // Subscribe events
        graph.getGraphChangedEventEmitter().subscribeWeak(this);
        graph.getNodeConnectedEventEmitter().subscribeWeak(this);

        // Initialize all context menu action hotkeys etc.
        initializeHotkeys();

        // Reload resources
        resources.updateAssets();
    }

    private void initializeOverlays() {
        overlays.add(new JIPipeDesktopGraphCanvasNodeShadowOverlay(this));
        overlays.add(new JIPipeDesktopGraphCanvasAnnotationNodesOverlay(this));
        overlays.add(new JIPipeDesktopGraphCanvasIOOverlay(this));
        overlays.add(new JIPipeDesktopGraphCanvasConnectionHighlightsOverlay(this));
        overlays.add(new JIPipeDesktopGraphCanvasObjectSelectionOverlay(this));
        overlays.add(new JIPipeDesktopGraphCanvasCursorOverlay(this));
        overlays.add(new JIPipeDesktopGraphCanvasResizeHandlesOverlay(this));
        overlays.add(new JIPipeDesktopGraphCanvasSelectionBoxOverlay(this));
        overlays.add(new JIPipeDesktopGraphCanvasToolInfoOverlay(this));
    }

    public JIPipeDesktopGraphCanvasSelectionManager getSelectionManager() {
        return selectionManager;
    }

    public JIPipeDesktopGraphCanvasNodeResizeManager getResizeManager() {
        return resizeManager;
    }


    public boolean isMouseIsEntered() {
        return mouseIsEntered;
    }

    public Point getLastMousePosition() {
        return lastMousePosition;
    }

    public JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEventEmitter getDefaultAlgorithmUIActionRequestedEventEmitter() {
        return defaultNodeUIActionRequestedEventEmitter;
    }

    public JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEventEmitter getNodeUIActionRequestedEventEmitter() {
        return nodeUIActionRequestedEventEmitter;
    }

    @Override
    public void dispose() {
        this.disposed = true;
        graph.getGraphChangedEventEmitter().unsubscribe(this);
        graph.getNodeConnectedEventEmitter().unsubscribe(this);
        for (JIPipeDesktopGraphNodeUI nodeUI : nodeUIs.values()) {
            try {
                unregisterNodeUIEvents(nodeUI);
            } catch (Throwable e) {
            }
        }
        removeAllNodes();
    }

    @Override
    public ZoomChangedEventEmitter getZoomChangedEventEmitter() {
        return zoomChangedEventEmitter;
    }

    public boolean isAutoMuteEdges() {
        return autoMuteEdges;
    }

    public void setAutoMuteEdges(boolean autoMuteEdges) {
        this.autoMuteEdges = autoMuteEdges;
        settings.setAutoMuteEdgesEnabled(autoMuteEdges);
        if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
            JIPipe.getSettings().save();
        }
        repaint(50);
    }

    public boolean isRenderOutsideEdges() {
        return renderOutsideEdges;
    }

    public void setRenderOutsideEdges(boolean renderOutsideEdges) {
        this.renderOutsideEdges = renderOutsideEdges;
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return desktopWorkbench;
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public JIPipeGraphEditorUIApplicationSettings getSettings() {
        return settings;
    }

    private void initializeHotkeys() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventDispatcher(e -> {
            KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
            if (this.isDisplayable() && FocusManager.getCurrentManager().getFocusOwner() == this) {
                for (GraphInteractiveObjectUIContextAction contextAction : contextActions) {
                    if (contextAction == null)
                        continue;
                    if (!contextAction.matches(selectionManager.getSelection()))
                        continue;
                    if (contextAction.getKeyboardShortcut() != null && Objects.equals(contextAction.getKeyboardShortcut(), keyStroke)) {
                        Point mousePosition = this.getMousePosition(true);
                        if (mousePosition != null) {
                            setGraphEditCursor(mousePosition);
                        }
                        getDesktopWorkbench().sendStatusBarText("Executed: " + contextAction.getName());
                        SwingUtilities.invokeLater(() -> contextAction.run(this, selectionManager.getSelection()));
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void initialize() {
        setOpaque(true);
        setBackground(UIManager.getColor("EditorPane.background"));
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    /**
     * @return The displayed graph
     */
    public JIPipeGraph getGraph() {
        return graph;
    }

    public boolean isRenderCursor() {
        return renderCursor;
    }

    public void setRenderCursor(boolean renderCursor) {
        this.renderCursor = renderCursor;
    }

    /**
     * Removes all node UIs
     */
    private void removeAllNodes() {
        for (JIPipeDesktopGraphNodeUI ui : ImmutableList.copyOf(nodeUIs.values())) {
            remove(ui);
            try {
                unregisterNodeUIEvents(ui);
            } catch (Throwable e) {
            }
        }
        nodeUIs.clear();
        selectionManager.clearSelection(false);
        revalidate();
        repaint();
        selectionManager.updateSelection();
    }

    /**
     * Removes node UIs that are not valid anymore
     */
    private void removeOldNodes() {
        Set<JIPipeGraphNode> toRemove = new HashSet<>();
        for (Map.Entry<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> kv : nodeUIs.entrySet()) {
            if (!graph.containsNode(kv.getKey()) || !kv.getKey().isVisibleIn(getCompartmentUUID()))
                toRemove.add(kv.getKey());
        }
        for (JIPipeGraphNode algorithm : toRemove) {
            JIPipeDesktopGraphNodeUI ui = nodeUIs.get(algorithm);
            selectionManager.removeFromSelection(ui, false);
            remove(ui);
            nodeUIs.remove(algorithm);
        }
        if (!toRemove.isEmpty()) {
            selectionManager.updateSelection();
            revalidate();
            repaint();
            graphCanvasUpdatedEventEmitter.emit(new JIPipeDesktopGraphCanvasUIUpdatedEvent(this));
        }
    }

    /**
     * Adds node UIs that are not in the canvas yet
     *
     * @param force if the positioning is forced
     */
    private void addNewNodes(boolean force) {
        List<JIPipeDesktopGraphNodeUI> newlyPlacedAlgorithms = new ArrayList<>();
        JIPipeDesktopGraphNodeUI ui = null;
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            if (!algorithm.isVisibleIn(getCompartmentUUID()))
                continue;
            if (nodeUIs.containsKey(algorithm))
                continue;

            try {
                ui = algorithm.getNodeUiClass().getConstructor(JIPipeDesktopWorkbench.class, JIPipeDesktopGraphCanvasUI.class, JIPipeGraphNode.class).newInstance(
                        getDesktopWorkbench(), this, algorithm
                );
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            Integer layer;
            registerNodeUIEvents(ui);

            if (algorithm instanceof JIPipeAnnotationGraphNode) {
                layer = Integer.MIN_VALUE;
            } else {
                layer = currentNodeLayer++;
            }

            add(ui, layer); // Layered pane

            nodeUIs.put(algorithm, ui);
            if (!ui.moveToStoredGridLocation(force)) {
                nodeManager.autoPlaceCloseToCursor(ui, force);
                newlyPlacedAlgorithms.add(ui);
            }
        }

        updateAnnotationNodeLayers();

        revalidate();
        repaint();

        if (newlyPlacedAlgorithms.size() == nodeUIs.size()) {
            autoLayoutAll();
        }
        if (!newlyPlacedAlgorithms.isEmpty()) {
            graphCanvasUpdatedEventEmitter.emit(new JIPipeDesktopGraphCanvasUIUpdatedEvent(this));
        }
        selectionManager.applyScheduledSelection();
    }

    public void updateAnnotationNodeLayers() {
        // Collect all annotations
        List<JIPipeAnnotationGraphNode> annotationGraphNodes = new ArrayList<>();
        List<JIPipeAnnotationGraphNode> selectedAnnotationGraphNodes = new ArrayList<>();
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeAnnotationGraphNode annotationGraphNode) {
                annotationGraphNodes.add(annotationGraphNode);
                JIPipeDesktopGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
                if (nodeUI != null) {
                    if (selectionManager.getSelection().contains(nodeUI)) {
                        selectedAnnotationGraphNodes.add(annotationGraphNode);
                    }
                }
            }
        }
        if (annotationGraphNodes.isEmpty())
            return;

        // Sort by Z-order (so we can do a rank transformation)
        // Rank transformation into negative space to fix z-order
        annotationGraphNodes.sort(Comparator.comparing(JIPipeAnnotationGraphNode::getzOrder));
        int nextZOrder = 0;
        for (int i = annotationGraphNodes.size() - 1; i >= 0; i--) {
            JIPipeAnnotationGraphNode annotationGraphNode = annotationGraphNodes.get(i);
            annotationGraphNode.setzOrder(nextZOrder--);
        }

        // Determine the displayed Z-order: selected nodes need to be at the front of the other annotations
        for (JIPipeAnnotationGraphNode annotationGraphNode : annotationGraphNodes) {
            JIPipeDesktopGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
            if (nodeUI != null) {
                setLayer(nodeUI, annotationGraphNode.getzOrder() - selectedAnnotationGraphNodes.size());
            }
        }
//        for (int i = 0; i < selectedAnnotationGraphNodes.size(); i++) {
//            JIPipeAnnotationGraphNode annotationGraphNode = selectedAnnotationGraphNodes.get(i);
//            JIPipeGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
//            if(nodeUI != null) {
//                setLayer(nodeUI, -i);
//            }
//        }
    }

    public void sendSelectionToForeground(Set<JIPipeDesktopGraphNodeUI> selection) {
        boolean updated = false;
        for (JIPipeDesktopGraphNodeUI nodeUI : selection) {
            if (nodeUI.getNode().isUiLocked())
                continue;
            if (nodeUI.getNode() instanceof JIPipeAnnotationGraphNode) {

                if (!updated) {
                    getHistoryJournal().snapshot("Send selected nodes to foreground",
                            "Sent a selection of graph annotations to the foreground",
                            getCompartmentUUID(),
                            JIPipe.RESOURCES.getIcon16("actions/object-order-front.png"));
                }

                ((JIPipeAnnotationGraphNode) nodeUI.getNode()).setzOrder(Integer.MAX_VALUE);
                updated = true;
            }
        }
        if (updated)
            updateAnnotationNodeLayers();
    }

    public void sendSelectionToBackground(Set<JIPipeDesktopGraphNodeUI> selection) {
        boolean updated = false;
        for (JIPipeDesktopGraphNodeUI nodeUI : selection) {
            if (nodeUI.getNode().isUiLocked())
                continue;
            if (nodeUI.getNode() instanceof JIPipeAnnotationGraphNode) {

                if (!updated) {
                    getHistoryJournal().snapshot("Send selected nodes to background",
                            "Sent a selection of graph annotations to the background",
                            getCompartmentUUID(),
                            JIPipe.RESOURCES.getIcon16("actions/object-order-back.png"));
                }

                ((JIPipeAnnotationGraphNode) nodeUI.getNode()).setzOrder(Integer.MIN_VALUE);
                updated = true;
            }
        }
        if (updated)
            updateAnnotationNodeLayers();
    }

    public void raiseSelection(Set<JIPipeDesktopGraphNodeUI> selection) {
        TIntObjectMap<JIPipeAnnotationGraphNode> zOrderAnnotations = new TIntObjectHashMap<>();
        List<JIPipeAnnotationGraphNode> selectedAnnotationGraphNodes = new ArrayList<>();
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeAnnotationGraphNode) {
                JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) graphNode;
                zOrderAnnotations.put(annotationGraphNode.getzOrder(), annotationGraphNode);
                JIPipeDesktopGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
                if (nodeUI != null) {
                    if (selection.contains(nodeUI)) {
                        selectedAnnotationGraphNodes.add(annotationGraphNode);
                    }
                }
            }
        }

        if (!selectedAnnotationGraphNodes.isEmpty()) {
            getHistoryJournal().snapshot("Raise selected nodes",
                    "Raised a selection of graph annotations",
                    getCompartmentUUID(),
                    JIPipe.RESOURCES.getIcon16("actions/object-order-raise.png"));
        } else {
            return;
        }

        // Iterate from hi to low
        selectedAnnotationGraphNodes.sort(Comparator.comparing(JIPipeAnnotationGraphNode::getzOrder).reversed());
        boolean updated = false;
        for (JIPipeAnnotationGraphNode annotationGraphNode : selectedAnnotationGraphNodes) {
            if (annotationGraphNode.isUiLocked())
                continue;
            int oldZ = annotationGraphNode.getzOrder();
            int newZ = annotationGraphNode.getzOrder() + 1;
            JIPipeAnnotationGraphNode existing = zOrderAnnotations.get(newZ);
            if (existing != null) {
                // Swap
                existing.setzOrder(oldZ);
                zOrderAnnotations.put(oldZ, existing);
            }
            annotationGraphNode.setzOrder(newZ);
            zOrderAnnotations.put(newZ, annotationGraphNode);
            updated = true;
        }

        if (updated)
            updateAnnotationNodeLayers();
    }

    public void lowerSelection(Set<JIPipeDesktopGraphNodeUI> selection) {
        TIntObjectMap<JIPipeAnnotationGraphNode> zOrderAnnotations = new TIntObjectHashMap<>();
        List<JIPipeAnnotationGraphNode> selectedAnnotationGraphNodes = new ArrayList<>();
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeAnnotationGraphNode annotationGraphNode) {
                zOrderAnnotations.put(annotationGraphNode.getzOrder(), annotationGraphNode);
                JIPipeDesktopGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
                if (nodeUI != null) {
                    if (selection.contains(nodeUI)) {
                        selectedAnnotationGraphNodes.add(annotationGraphNode);
                    }
                }
            }
        }

        if (!selectedAnnotationGraphNodes.isEmpty()) {
            getHistoryJournal().snapshot("Lowered selected nodes",
                    "Lowered a selection of graph annotations",
                    getCompartmentUUID(),
                    JIPipe.RESOURCES.getIcon16("actions/object-order-lower.png"));
        } else {
            return;
        }

        // Iterate from low to hi
        selectedAnnotationGraphNodes.sort(Comparator.comparing(JIPipeAnnotationGraphNode::getzOrder));
        boolean updated = false;
        for (JIPipeAnnotationGraphNode annotationGraphNode : selectedAnnotationGraphNodes) {
            if (annotationGraphNode.isUiLocked())
                continue;
            int oldZ = annotationGraphNode.getzOrder();
            int newZ = annotationGraphNode.getzOrder() - 1;
            JIPipeAnnotationGraphNode existing = zOrderAnnotations.get(newZ);
            if (existing != null) {
                // Swap
                existing.setzOrder(oldZ);
                zOrderAnnotations.put(oldZ, existing);
            }
            annotationGraphNode.setzOrder(newZ);
            zOrderAnnotations.put(newZ, annotationGraphNode);
            updated = true;
        }

        if (updated)
            updateAnnotationNodeLayers();
    }

    private void registerNodeUIEvents(JIPipeDesktopGraphNodeUI ui) {
        ui.getNodeUIActionRequestedEventEmitter().subscribe(this);
    }

    private void unregisterNodeUIEvents(JIPipeDesktopGraphNodeUI ui) {
        ui.getNodeUIActionRequestedEventEmitter().unsubscribe(this);
    }

    /**
     * Auto-layouts all UIs
     */
    public void autoLayoutAll() {
        if (nodeUIs.isEmpty())
            return;
        minDimensions = null;
        autoLayout();
        if (getParent() != null)
            getParent().revalidate();
    }

    /**
     * Applies a full auto-layout method
     */
    public void autoLayout() {
        switch (settings.getAutoLayout()) {
            case Sugiyama:
                (new SugiyamaGraphAutoLayoutImplementation()).accept(this);
                break;
            case MST:
                (new MSTGraphAutoLayoutImplementation()).accept(this);
                break;
        }
        repaint();
        graphCanvasUpdatedEventEmitter.emit(new JIPipeDesktopGraphCanvasUIUpdatedEvent(this));
    }


    @Override
    public void mouseDragged(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (toolManager.getCurrentTool() != null) {
            toolManager.getCurrentTool().mouseDragged(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        // Resize dragging
        resizeManager.mouseDragged(mouseEvent);

        // Node connection dragging
        if (dragManagerConnect.mouseDragged(mouseEvent)) {
            return;
        }
        if (dragManagerMove.mouseDragged(mouseEvent)) {
            return;
        }
        if (selectionBoxManager.mouseDragged(mouseEvent)) {
            return;
        }
    }

    public void cancelAllDraggingOperations() {
        dragManagerMove.cancelDragging();
        dragManagerConnect.cancelDragging();
    }


    public void repaintLowLag() {
        repaint(50);
        if (SystemUtils.IS_OS_LINUX) {
            Toolkit.getDefaultToolkit().sync();
        }
    }

    public void autoExpandLeftTop() {
        int minX = 0;
        int minY = 0;
        for (JIPipeDesktopGraphNodeUI ui : nodeUIs.values()) {
            minX = Math.min(ui.getX(), minX);
            minY = Math.min(ui.getY(), minY);
        }
        minX = -minX;
        minY = -minY;
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        Point nextGridPoint = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(new Point(minX, minY), zoom);
        int ex = nextGridPoint.x;
        int ey = nextGridPoint.y;
        for (JIPipeDesktopGraphNodeUI value : nodeUIs.values()) {
            if (!dragManagerMove.getCurrentlyDraggedOffsets().containsKey(value)) {
                value.moveToClosestGridPoint(new Point(value.getX() + ex, value.getY() + ey), false, true);
            }
        }
        Point cursor = getGraphEditorCursor();
        if (cursor != null) {
            cursor.x += ex;
            cursor.y += ey;
        }
        if (getParent() != null)
            getParent().revalidate();
        repaint();
    }

    /**
     * Expands the canvas by moving all algorithms.
     * Has no effect if the coordinates are both zero
     *
     * @param gridLeft expand left (in grid coordinates)
     * @param gridTop  expand top (in grid coordinates)
     */
    public void expandLeftTop(int gridLeft, int gridTop) {
        if (gridLeft == 0 && gridTop == 0) {
            return;
        }
        for (JIPipeDesktopGraphNodeUI value : nodeUIs.values()) {
            if (!dragManagerMove.getCurrentlyDraggedOffsets().containsKey(value)) {
                Point gridLocation = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(value.getLocation(), zoom);
                gridLocation.x += gridLeft;
                gridLocation.y += gridTop;
                value.moveToGridLocation(gridLocation, true, true);
            }
        }
        Point cursor = getGraphEditorCursor();
        if (cursor != null) {
            Point realLeftTop = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(new Point(gridLeft, gridTop), zoom);
            cursor.x = Math.round(cursor.x + realLeftTop.x);
            cursor.y = Math.round(cursor.y + realLeftTop.y);
        }
//        if (getParent() != null)
//            getParent().revalidate();
        repaint();
    }

    /**
     * Expands the canvas
     *
     * @param right  expand right
     * @param bottom expand bottom
     */
    public void expandRightBottom(int right, int bottom) {
        if (minDimensions == null)
            minDimensions = new Dimension(getWidth(), getHeight());
        minDimensions.width += right;
        minDimensions.height += bottom;
        if (getParent() != null)
            getParent().revalidate();
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        if (!toolManager.hasDefaultTool()) {
            toolManager.getCurrentTool().mouseMoved(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
            defaultCursor = toolManager.getCurrentTool().getCursor();
        }

        // Resize handler
        if (resizeManager.mouseMoved(mouseEvent)) {
            return;
        }

        // Handling by node
        boolean changed = false;
        JIPipeDesktopGraphNodeUI nodeUI = pickNodeUI(mouseEvent);
        if (nodeUI != null) {
            if (nodeUI != currentlyMouseEnteredNode) {
                if (currentlyMouseEnteredNode != null) {
                    currentlyMouseEnteredNode.mouseExited(mouseEvent);
                }
                currentlyMouseEnteredNode = nodeUI;
                currentlyMouseEnteredNode.mouseEntered(mouseEvent);
                currentlyMouseEnteredNodeActiveArea = nodeUI.getCurrentActiveArea();
                changed = true;
            }
        } else if (currentlyMouseEnteredNode != null) {
            currentlyMouseEnteredNode.mouseExited(mouseEvent);
            currentlyMouseEnteredNode = null;
            currentlyMouseEnteredNodeActiveArea = null;
            changed = true;
        }
        if (currentlyMouseEnteredNode != null) {
            currentlyMouseEnteredNode.mouseMoved(mouseEvent);
            JIPipeDesktopGraphNodeUIActiveArea currentActiveArea = currentlyMouseEnteredNode.getCurrentActiveArea();
            if (currentActiveArea != currentlyMouseEnteredNodeActiveArea) {
                currentlyMouseEnteredNodeActiveArea = currentActiveArea;
                changed = true;
            }
        }
        if (toolManager.getCurrentTool() != null && settings.isShowToolInfo() && !(toolManager.getCurrentTool() instanceof JIPipeDefaultGraphEditorTool)) {
            changed = true;
        }
        if (changed && settings.isDrawLabelsOnHover()) {
            repaintLowLag();
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (toolManager.getCurrentTool() != null) {
            toolManager.getCurrentTool().mouseClicked(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        JIPipeDesktopGraphNodeUI ui = pickNodeUI(mouseEvent);

        if (ui != null) {
            ui.mouseClicked(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        if (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getClickCount() == 2) {
            if (ui != null) {
                defaultNodeUIActionRequestedEventEmitter.emit(new JIPipeDesktopGraphNodeUI.DefaultNodeUIActionRequestedEvent(ui));
            } else if (graphEditorUI != null) {
                graphEditorUI.onCanvasEmptyDoubleClick(mouseEvent);
            }
        } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            setGraphEditCursor(new Point(mouseEvent.getX(), mouseEvent.getY()));
            requestFocusInWindow();
            repaint();
        } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            if (selectionManager.getSelection().size() <= 1) {
                selectionManager.selectOnly(ui);
            }
            if (graphEditorUI != null && graphEditorUI.getCurrentTool() != graphEditorUI.getDefaultTool()) {
                graphEditorUI.selectDefaultTool();
                return;
            }
            openContextMenu(new Point(mouseEvent.getX(), mouseEvent.getY()));
        }

//        {
//            int hValue = scrollPane.getHorizontalScrollBar().getValue();
//            int vValue = scrollPane.getVerticalScrollBar().getValue();
//            int hWidth = scrollPane.getHorizontalScrollBar().getVisibleAmount();
//            int vHeight = scrollPane.getVerticalScrollBar().getVisibleAmount();
//            System.out.println(new Rectangle(hValue, vValue, hWidth, vHeight));
//        }
    }

    /**
     * Opens the context menu at the location.
     * The menu is generated based on the current node selection
     *
     * @param point the location
     */
    public void openContextMenu(Point point) {
        setGraphEditCursor(new Point(point.x, point.y));
        JPopupMenu menu = new JPopupMenu();
        boolean scheduleSeparator = false;
        for (GraphInteractiveObjectUIContextAction action : contextActions) {
            if (action == null) {
                scheduleSeparator = true;
                continue;
            }
            if (action.isHidden())
                continue;
            boolean matches = action.matches(selectionManager.getSelection());
            if (!matches && !action.disableOnNonMatch())
                continue;
            if (scheduleSeparator) {
                scheduleSeparator = false;
                menu.addSeparator();
            }
            JMenuItem item = new JMenuItem(action.getName(), action.getIcon());
            item.setToolTipText(action.getDescription());
            if (matches) {
                item.addActionListener(e -> action.run(this, ImmutableSet.copyOf(selectionManager.getSelection())));
                if (action.getKeyboardShortcut() != null) {
                    item.setAccelerator(action.getKeyboardShortcut());
                }
            } else
                item.setEnabled(false);
            menu.add(item);
        }

        if (graphEditorUI != null) {
            graphEditorUI.beforeOpenContextMenu(menu);
        }

        // Node partitioning menus
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            JIPipeProject project = getDesktopWorkbench().getProject();
            JIPipeRuntimePartitionConfiguration runtimePartitions = project.getRuntimePartitions();

            // Algorithms
            Set<JIPipeDesktopGraphNodeUI> selectedNodeUIs = selectionManager.getSelectionByType(JIPipeDesktopGraphNodeUI.class);
            if (selectedNodeUIs.stream().anyMatch(ui -> ui.getNode() instanceof JIPipeAlgorithm)) {
                menu.addSeparator();
                Set<Integer> partitionIds = selectedNodeUIs.stream().filter(ui -> ui.getNode() instanceof JIPipeAlgorithm).map(ui -> ((JIPipeAlgorithm) ui.getNode()).getRuntimePartition().getIndex()).collect(Collectors.toSet());
                if (partitionIds.size() == 1) {
                    JIPipeRuntimePartition runtimePartition = project.getRuntimePartitions().get(partitionIds.iterator().next());
                    menu.add(UIUtils.createMenuItem("Edit partition '" + project.getRuntimePartitions().getFullName(runtimePartition) + "'",
                            "Edit the partition configuration", JIPipe.RESOURCES.getIcon16("actions/edit.png"), () -> {
                                JIPipeDesktopRuntimePartitionListEditor.editRuntimePartition(getDesktopWorkbench(), runtimePartition);
                            }));
                }
                JMenu partitionMenu = new JMenu("Move to partition ...");
                for (JIPipeRuntimePartition runtimePartition : runtimePartitions.toList()) {
                    JMenuItem item = new JMenuItem(runtimePartitions.getFullName(runtimePartition), runtimePartitions.getIcon(runtimePartition));
                    item.addActionListener(e -> partitionSelectedAlgorithms(runtimePartition));
                    item.setToolTipText("Partitions all selected nodes to '" + runtimePartitions.getFullName(runtimePartition) + "'");
                    partitionMenu.add(item);
                }
                partitionMenu.addSeparator();
                partitionMenu.add(UIUtils.createMenuItem("Create new partition", "Creates a new partition", JIPipe.RESOURCES.getIcon16("actions/add.png"), () -> {
                    JIPipeRuntimePartition runtimePartition = project.getRuntimePartitions().add();
                    JIPipeDesktopRuntimePartitionListEditor.editRuntimePartition(getDesktopWorkbench(), runtimePartition);
                }));
                menu.add(partitionMenu);
            }

            // Compartments
            if (selectedNodeUIs.stream().anyMatch(ui -> ui.getNode() instanceof JIPipeProjectCompartment)) {
                menu.addSeparator();
                JMenu partitionMenu = new JMenu("Move contents to partition ...");
                for (JIPipeRuntimePartition runtimePartition : runtimePartitions.toList()) {
                    JMenuItem item = new JMenuItem(runtimePartitions.getFullName(runtimePartition), runtimePartitions.getIcon(runtimePartition));
                    item.addActionListener(e -> partitionSelectedAlgorithms(runtimePartition));
                    item.setToolTipText("Partitions the contained nodes to '" + runtimePartitions.getFullName(runtimePartition) + "'");
                    partitionMenu.add(item);
                }
                menu.add(partitionMenu);
            }
        }

        menu.show(this, point.x, point.y);
    }

    private void partitionSelectedAlgorithms(JIPipeRuntimePartition runtimePartition) {
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            JIPipeRuntimePartitionConfiguration runtimePartitions = getDesktopWorkbench().getProject().getRuntimePartitions();
            int newIndex = runtimePartitions.indexOf(runtimePartition);
            if (newIndex == -1) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "Unable to find selected partition!", "Partition nodes", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI : selectionManager.getSelection()) {
                for (JIPipeGraphNode node : interactiveObjectUI.getNodes()) {
                    if (node instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) node).getRuntimePartition().setIndex(newIndex);
                        interactiveObjectUI.updateView(new JIPipeDesktopGraphNodeUIUpdateViewCommand(false, false, false));
                    } else if (node instanceof JIPipeProjectCompartment) {
                        UUID uuid = ((JIPipeProjectCompartment) node).getProjectCompartmentUUID();
                        for (JIPipeGraphNode graphNode : getDesktopWorkbench().getProject().getGraph().getGraphNodes()) {
                            if (graphNode instanceof JIPipeAlgorithm && Objects.equals(uuid, graphNode.getCompartmentUUIDInParentGraph())) {
                                ((JIPipeAlgorithm) graphNode).getRuntimePartition().setIndex(newIndex);
                                graphNode.getParameterChangedEventEmitter().emit(new JIPipeParameterCollection.ParameterChangedEvent(graphNode, "jipipe:algorithm:runtime-partition"));
                            }
                        }
                        interactiveObjectUI.updateView(new JIPipeDesktopGraphNodeUIUpdateViewCommand(false, false, false));
                    }
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (!toolManager.hasDefaultTool()) {
            toolManager.getCurrentTool().mousePressed(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {

            // Resize handling
            if (resizeManager.mousePressed(mouseEvent)) {
                return;
            }

            // Slot dragging
            if (dragManagerConnect.mousePressed(mouseEvent)) {
                return;
            }

            // Node dragging
            if (dragManagerMove.mousePressed(mouseEvent)) {
                return;
            }

            // Selection box
            if (selectionBoxManager.mousePressed(mouseEvent)) {
                return;
            }
        }
    }


    public JIPipeDesktopGraphNodeUI pickNodeUI(MouseEvent mouseEvent) {
        for (int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            if (component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                if (component instanceof JIPipeDesktopGraphNodeUI) {
                    if (graphAnnotationsLocked && ((JIPipeDesktopGraphNodeUI) component).getNode() instanceof JIPipeAnnotationGraphNode) {
                        continue;
                    }
                    return (JIPipeDesktopGraphNodeUI) component;
                }
            }
        }
        return null;
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // End resize operation
        if (resizeManager.isResizing()) {
            resizeManager.stopAllResizing();
            return;
        }
        resizeManager.stopAllResizing();

        // Let the tool handle the event
        if (!toolManager.hasDefaultTool()) {
            toolManager.getCurrentTool().mouseReleased(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        if (mouseEvent.getButton() != MouseEvent.BUTTON1) {
            cancelAllDraggingOperations();
        } else {
            // Handle dragging
            dragManagerConnect.mouseReleased(mouseEvent);
            cancelAllDraggingOperations();

            // Selection box
            if (selectionBoxManager.mouseReleased(mouseEvent)) {
                return;
            }

            // Fallback: pick the node at the mouse location
            JIPipeDesktopGraphNodeUI ui = pickNodeUI(mouseEvent);
            if (ui == null) {
                selectionManager.selectOnly(null);
            }
            selectionBoxManager.clear();
        }
    }

    public void connectCreateNewSlot(JIPipeDataSlot sourceSlot, JIPipeDesktopGraphNodeUI nodeUI) {
        JIPipeGraphNode node = nodeUI.getNode();
        JIPipeSlotType addedSlotType = sourceSlot.getSlotType() == JIPipeSlotType.Input ? JIPipeSlotType.Output : JIPipeSlotType.Input;
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) node.getSlotConfiguration();
        Set<JIPipeDataInfo> availableTypes = switch (addedSlotType) {
            case Input -> slotConfiguration.getAllowedInputSlotTypes()
                    .stream().map(JIPipeDataInfo::getInstance).collect(Collectors.toSet());
            case Output -> slotConfiguration.getAllowedOutputSlotTypes()
                    .stream().map(JIPipeDataInfo::getInstance).collect(Collectors.toSet());
            default -> throw new UnsupportedOperationException();
        };

        Class<? extends JIPipeData> sourceSlotType = sourceSlot.getAcceptedDataType();
        if (addedSlotType == JIPipeSlotType.Input) {
            availableTypes.removeIf(info -> !JIPipe.getDataTypes().isConvertible(sourceSlotType, info.getDataClass()));
        } else {
            availableTypes.removeIf(info -> !JIPipe.getDataTypes().isConvertible(info.getDataClass(), sourceSlotType));
        }

        if (availableTypes.isEmpty()) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "There is no possibility to create a compatible slot for the data.", "Incompatible node", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JIPipeDataInfo selectedInfo;
        if (availableTypes.contains(JIPipeDataInfo.getInstance(sourceSlot.getAcceptedDataType()))) {
            selectedInfo = JIPipeDataInfo.getInstance(sourceSlot.getAcceptedDataType());
        } else {
            if (addedSlotType == JIPipeSlotType.Input) {
                selectedInfo = availableTypes.stream().min(Comparator.comparing(info -> JIPipe.getDataTypes().getConversionDistance(sourceSlotType, info.getDataClass()))).get();
            } else {
                selectedInfo = availableTypes.stream().min(Comparator.comparing(info -> JIPipe.getDataTypes().getConversionDistance(info.getDataClass(), sourceSlotType))).get();
            }
        }


        JDialog dialog = new JDialog();
        JIPipeDesktopAddAlgorithmSlotPanel panel = new JIPipeDesktopAddAlgorithmSlotPanel(nodeUI.getNode(), addedSlotType, historyJournal);
        panel.setAvailableTypes(availableTypes);
        panel.getDatatypeList().setSelectedValue(selectedInfo, true);
        panel.setDialog(dialog);
        dialog.setContentPane(panel);
        dialog.setTitle("Add slot");
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(640, 480));
        dialog.setLocationRelativeTo(nodeUI);
        UIUtils.addEscapeListener(dialog);
        SwingUtilities.invokeLater(() -> {
            panel.getDatatypeList().ensureIndexIsVisible(panel.getDatatypeList().getSelectedIndex());
        });
        dialog.setVisible(true);

        if (!panel.getAddedSlots().isEmpty()) {
            if (addedSlotType == JIPipeSlotType.Input) {
                connectSlot(sourceSlot, panel.getAddedSlots().get(0));
            } else {
                connectSlot(panel.getAddedSlots().get(0), sourceSlot);
            }
        }

    }

    public void connectOrDisconnectSlots(JIPipeDataSlot firstSlot, JIPipeDataSlot secondSlot) {
        JIPipeGraph graph = getGraph();
        if (graph != secondSlot.getNode().getParentGraph())
            return;
        if (firstSlot.isInput() != secondSlot.isInput()) {
            if (firstSlot.isInput()) {
                if (!graph.getGraph().containsEdge(secondSlot, firstSlot)) {
                    connectSlot(secondSlot, firstSlot);
                } else {
                    disconnectSlot(secondSlot, firstSlot);
                }
            } else {
                if (!graph.getGraph().containsEdge(firstSlot, secondSlot)) {
                    connectSlot(firstSlot, secondSlot);
                } else {
                    disconnectSlot(firstSlot, secondSlot);
                }
            }
        }
    }

    /**
     * Connects the two slots
     *
     * @param source source slot
     * @param target target slot
     */
    public void connectSlot(JIPipeDataSlot source, JIPipeDataSlot target) {
        if (getGraph().canConnect(source, target, true)) {
            JIPipeGraph graph = source.getNode().getParentGraph();
            if (graph.getGraph().containsEdge(source, target))
                return;
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshotBeforeConnect(source, target, source.getNode().getCompartmentUUIDInParentGraph());
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
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshotBeforeDisconnect(source, target, compartmentUUID);
            }
            getGraph().disconnect(source, target, true);
        }
    }

    public void disconnectAll(JIPipeDataSlot slot, Set<JIPipeDataSlot> otherSlots) {
        if (getHistoryJournal() != null) {
            getHistoryJournal().snapshotBeforeDisconnectAll(slot, slot.getNode().getCompartmentUUIDInParentGraph());
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

    public void resetCursor() {
        setCursor(!toolManager.hasDefaultTool() ? toolManager.getCurrentTool().getCursor() : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());
        mouseIsEntered = true;

        // End resize operation
        resizeManager.stopAllResizing();

        // Let the tool handle the event
        if (!toolManager.hasDefaultTool()) {
            toolManager.getCurrentTool().mouseEntered(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());
        mouseIsEntered = false;

        // End resize operation
        resizeManager.stopAllResizing();

        // Let the tool handle the event
        if (!toolManager.hasDefaultTool()) {
            toolManager.getCurrentTool().mouseExited(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }
    }


    @Override
    public Dimension getPreferredSize() {
        int width = 0;
        int height = 0;
        for (int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            width = Math.max(width, component.getX() + component.getWidth() + 2 * JIPipeDesktopGraphCanvasGrid.GRID_WIDTH);
            height = Math.max(height, component.getY() + component.getHeight() + 2 * JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT);
        }
        if (minDimensions != null) {
            width = Math.max(minDimensions.width, width);
            height = Math.max(minDimensions.height, height);
            minDimensions.width = width;
            minDimensions.height = height;
        } else {
            minDimensions = new Dimension(width, height);
        }
        return new Dimension(width, height);
    }

    /**
     * Draws a minimap of this canvas.
     *
     * @param graphics2D the graphics
     * @param scale      the scale
     * @param viewX      move the locations by this value
     * @param viewY      move the locations by this value
     */
    public void paintMiniMap(Graphics2D graphics2D, double scale, int viewX, int viewY) {

        paintMinimapEdges(graphics2D, scale, viewX, viewY);

        Stroke defaultStroke = JIPipeDesktopGraphCanvasResources.STROKE_UNIT;
        Stroke selectedStroke = JIPipeDesktopGraphCanvasResources.STROKE_SELECTION;

        for (JIPipeDesktopGraphNodeUI nodeUI : nodeUIs.values()) {
            int x = (int) (nodeUI.getX() * scale) + viewX;
            int y = (int) (nodeUI.getY() * scale) + viewY;
            int width = (int) (nodeUI.getWidth() * scale);
            int height = (int) (nodeUI.getHeight() * scale);

            nodeUI.paintMinimap(graphics2D, x, y, width, height, (BasicStroke) defaultStroke, (BasicStroke) selectedStroke, selectionManager.getSelection());
        }
    }

    private void paintMinimapEdges(Graphics2D graphics2D, double scale, int viewX, int viewY) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setColor(Color.LIGHT_GRAY);
        paintEdges(graphics2D,
                JIPipeDesktopGraphCanvasResources.STROKE_UNIT,
                null,
                JIPipeDesktopGraphCanvasResources.STROKE_UNIT_COMMENT,
                false,
                false,
                scale,
                viewX,
                viewY,
                false,
                false,
                JIPipeDesktopGraphCanvasUIEdgeMuteMode.Auto);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics;

        final Stroke strokeDefault = resources.getStrokeDefault();
        final Stroke strokeDefaultBorder = resources.getStrokeDefaultBorder();
        final Stroke strokeHighlight = resources.getStrokeHighlight();

        // Set render settings (HQ)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Below node paint
        if (!toolManager.hasDefaultTool()) {
            toolManager.getCurrentTool().paintBelowNodesAndEdges(g);
        }

        if (renderOutsideEdges && getCompartmentUUID() != null && settings.isDrawOutsideEdges()) {
            paintOutsideEdges(g, false, Color.DARK_GRAY, strokeDefault, strokeDefaultBorder);
        }

        // Main edge drawing
        lastDisplayedMainEdges = paintEdges(g,
                strokeDefault,
                strokeDefaultBorder,
                JIPipeDesktopGraphCanvasResources.STROKE_COMMENT,
                false,
                false,
                1,
                0,
                0,
                true,
                isAutoMuteEdges(),
                (selectionManager.getSelection().isEmpty() || !settings.isAutoMuteBySelection()) ? JIPipeDesktopGraphCanvasUIEdgeMuteMode.Auto : JIPipeDesktopGraphCanvasUIEdgeMuteMode.ForceMuted);

        // Outside edges drawing
        if (renderOutsideEdges && getCompartmentUUID() != null && settings.isDrawOutsideEdges()) {
            paintOutsideEdges(g, true, Color.DARK_GRAY, strokeHighlight, null);
        }

        // Selected edges drawing
        if (!selectionManager.getSelection().isEmpty()) {
            paintEdges(g,
                    strokeHighlight,
                    null,
                    JIPipeDesktopGraphCanvasResources.STROKE_COMMENT_HIGHLIGHT,
                    true,
                    settings.isColorSelectedNodeEdges(),
                    1,
                    0,
                    0,
                    true,
                    false,
                    JIPipeDesktopGraphCanvasUIEdgeMuteMode.ForceVisible);
        }

        // Paint overlays that follow the standard overlay API
        for (JIPipeDesktopGraphCanvasOverlay overlay : overlays) {
            overlay.paintComponent(g);
        }

        // Tool-specific painting operations
        if (!toolManager.hasDefaultTool()) {
            toolManager.getCurrentTool().paintBelowNodesAfterEdges(g);
        }

        // Reset stroke
        g.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_UNIT);
    }


    public Map<?, ?> getDesktopRenderingHints() {
        return desktopRenderingHints;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (disposed) {
            return;
        }

        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHints(desktopRenderingHints);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Above node paint
        if (!toolManager.hasDefaultTool()) {
            if (toolManager.getCurrentTool() instanceof JIPipeAnnotationGraphNodeTool && ((JIPipeAnnotationGraphNodeTool<?>) toolManager.getCurrentTool()).isDrawWithAntialiasing()) {
                try {
                    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    toolManager.getCurrentTool().paintAfterNodesAndEdges(graphics2D);
                } finally {
                    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                }
            } else {
                toolManager.getCurrentTool().paintAfterNodesAndEdges(graphics2D);
            }
        }

        // Paint overlays that follow the standard overlay API
        for (JIPipeDesktopGraphCanvasOverlay overlay : overlays) {
            overlay.paint(graphics2D);
        }
    }

    public JIPipeDesktopGraphNodeUI getCurrentlyMouseEnteredNode() {
        return currentlyMouseEnteredNode;
    }

    public JIPipeDesktopGraphNodeUIActiveArea getCurrentlyMouseEnteredNodeActiveArea() {
        return currentlyMouseEnteredNodeActiveArea;
    }

    private List<JIPipeDesktopGraphEdgeUI> paintEdges(Graphics2D g, Stroke stroke, Stroke strokeBorder, Stroke strokeComment, boolean onlySelected, boolean multicolor, double scale, int viewX, int viewY, boolean enableArrows, boolean enableAutoHide, JIPipeDesktopGraphCanvasUIEdgeMuteMode muteMode) {
        Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> slotEdges = graph.getSlotEdges();
        List<JIPipeDesktopGraphEdgeUI> edgeUIs = new ArrayList<>();

        int multiColorMax = findMultiColorMax(slotEdges, multicolor, onlySelected);
        int multiColorIndex = 0;

        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> kv : slotEdges) {
            JIPipeDataSlot source = kv.getKey();
            JIPipeDataSlot target = kv.getValue();
            JIPipeGraphEdge edge = graph.getGraph().getEdge(kv.getKey(), kv.getValue());

            if (!toolManager.hasDefaultTool()) {
                if (!toolManager.getCurrentTool().canRenderEdge(source, target, edge)) {
                    continue;
                }
            }

            // Check for only showing selected nodes
            JIPipeDesktopGraphNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
            JIPipeDesktopGraphNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

            if (sourceUI == null || targetUI == null) {
                continue;
            }
            if (onlySelected && !selectionManager.getSelection().contains(sourceUI) && !selectionManager.getSelection().contains(targetUI)) {
                continue;
            }

            // Get the locations
            PointRange sourcePoint = sourceUI.getSlotLocation(source);
            PointRange targetPoint = targetUI.getSlotLocation(target);
            sourcePoint.add(sourceUI.getLocation());
            targetPoint.add(targetUI.getLocation());

            // Save into the slot edges list
            JIPipeDesktopGraphEdgeUI displayedSlotEdge = new JIPipeDesktopGraphEdgeUI(source, target, edge);
            displayedSlotEdge.setMultiColorMax(multiColorMax);
            displayedSlotEdge.setMultiColorIndex(multiColorIndex);
            displayedSlotEdge.setSourcePoint(sourcePoint);
            displayedSlotEdge.setTargetPoint(targetPoint);
            displayedSlotEdge.setSourceCenter(new Point(sourcePoint.center));
            displayedSlotEdge.setTargetCenter(new Point(targetPoint.center));
            displayedSlotEdge.setSourceUI(sourceUI);
            displayedSlotEdge.setTargetUI(targetUI);
            edgeUIs.add(displayedSlotEdge);

            ++multiColorIndex;
        }

        if (enableAutoHide) {
            edgeUIs.sort(Comparator.naturalOrder());
        }

        Set<Rectangle> existingDrawnSlots = new HashSet<>();

        for (JIPipeDesktopGraphEdgeUI displayedSlotEdge : edgeUIs) {

            Rectangle rectangle = null;
            if (enableAutoHide) {
                rectangle = new Rectangle(displayedSlotEdge.getSourcePoint().center);
                rectangle.add(displayedSlotEdge.getTargetPoint().center);
            }

            // Hidden edges
            if (displayedSlotEdge.isCommentEdge()) {
                paintSlotEdge(g,
                        strokeComment,
                        null,
                        displayedSlotEdge,
                        multicolor,
                        scale,
                        viewX,
                        viewY,
                        enableArrows
                );
            } else {

                boolean hidden;

                if (muteMode == JIPipeDesktopGraphCanvasUIEdgeMuteMode.ForceVisible) {
                    hidden = false;
                } else if (muteMode == JIPipeDesktopGraphCanvasUIEdgeMuteMode.ForceMuted) {
                    hidden = true;
                } else {
                    switch (displayedSlotEdge.getEdge().getUiVisibility()) {
                        case Smart:
                        case SmartSilent: {
                            hidden = false;
                            if (enableAutoHide) {
                                if (displayedSlotEdge.getUIManhattanDistance() > settings.getAutoHideEdgeDistanceThreshold()) {
                                    int currentArea = rectangle.width * rectangle.height;
                                    for (Rectangle existingDrawnSlot : existingDrawnSlots) {
                                        Rectangle intersection = rectangle.intersection(existingDrawnSlot);
                                        int intersectionArea = intersection.width * intersection.height;
                                        int existingArea = existingDrawnSlot.width * existingDrawnSlot.height;
                                        double diceScore = (2.0 * intersectionArea) / (existingArea + currentArea);
                                        if (diceScore > settings.getAutoHideEdgeOverlapThreshold()) {
                                            hidden = true;
                                            break;
                                        }
//                                    if(intersectionArea > 0) {
//                                        g.setPaint(Color.RED);
//                                        g.draw(intersection);
//                                    }
//                                    g.setPaint(Color.BLUE);
//                                    g.draw(rectangle);
                                    }
                                }
                            }
                        }
                        break;
                        case AlwaysHiddenWithLabel:
                        case AlwaysHidden: {
                            hidden = true;
                        }
                        break;
                        case AlwaysVisible: {
                            hidden = false;
                        }
                        break;
                        default:
                            hidden = false;
                            break;
                    }
                }

                displayedSlotEdge.setHidden(hidden);

                paintSlotEdge(g,
                        stroke,
                        strokeBorder,
                        displayedSlotEdge,
                        multicolor,
                        scale,
                        viewX,
                        viewY,
                        enableArrows
                );
            }

            // Save for later
            if (enableAutoHide) {
                existingDrawnSlots.add(rectangle);
            }
        }

        return edgeUIs;
    }

    /**
     * Gets the edgeUIs collection
     * @return Immutable copy of edgeUIs map
     */
    public BiMap<JIPipeGraphEdge, JIPipeDesktopGraphEdgeUI> getEdgeUIs() {
        return ImmutableBiMap.copyOf(edgeUIs);
    }
    
    /**
     * Removes edge UIs that no longer exist in the graph
     */
    private void removeOldEdges() {
        Set<JIPipeGraphEdge> currentEdges = new HashSet<>();
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> slotEdge : graph.getSlotEdges()) {
            JIPipeGraphEdge edge = graph.getGraph().getEdge(slotEdge.getKey(), slotEdge.getValue());
            if (edge != null) {
                currentEdges.add(edge);
            }
        }
        
        // Remove edges that are no longer in the graph
        Set<JIPipeGraphEdge> removedEdges = Sets.difference(edgeUIs.keySet(), currentEdges);
        for (JIPipeGraphEdge removedEdge : removedEdges) {
            edgeUIs.remove(removedEdge);
        }
    }
    
    /**
     * Adds edge UIs for new edges in the graph
     */
    private void addNewEdges() {
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> slotEdge : graph.getSlotEdges()) {
            JIPipeGraphEdge edge = graph.getGraph().getEdge(slotEdge.getKey(), slotEdge.getValue());
            if (edge != null && !edgeUIs.containsKey(edge)) {
                JIPipeDesktopGraphEdgeUI edgeUI = new JIPipeDesktopGraphEdgeUI(slotEdge.getKey(), slotEdge.getValue(), edge);
                edgeUIs.put(edge, edgeUI);
            }
        }
    }

    private int findMultiColorMax(Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> slotEdges, boolean multicolor, boolean onlySelected) {
        int multiColorMax = 1;
        if (multicolor) {
            if (onlySelected) {
                multiColorMax = 0;
                for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> kv : slotEdges) {
                    JIPipeDataSlot source = kv.getKey();
                    JIPipeDataSlot target = kv.getValue();
                    JIPipeDesktopGraphNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeDesktopGraphNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

                    if (sourceUI == null || targetUI == null)
                        continue;
                    if (selectionManager.getSelection().contains(sourceUI) || selectionManager.getSelection().contains(targetUI)) {
                        ++multiColorMax;
                    }
                }
            } else {
                multiColorMax = slotEdges.size();
            }
        }
        return multiColorMax;
    }

    private void paintNodeIOLabels(Graphics2D g, List<JIPipeDesktopGraphEdgeUI> displayedMainEdges) {

        // Modified to only show on hover, as input labels are not very helpful in complex graphs

        if (!settings.isDrawLabelsOnHover())
            return;

//        FontMetrics slotFontMetrics = g.getFontMetrics(resources.getSmartEdgeTooltipSlotFont());
//        FontMetrics nodeFontMetrics = g.getFontMetrics(smartEdgeTooltipNodeFont);

        // Collect labelled inputs based on pre-calculated
        // Target to sources
        Map<JIPipeDesktopGraphEdgeUI, Integer> edgeIds = new IdentityHashMap<>();
        Multimap<JIPipeDataSlot, JIPipeDesktopGraphEdgeUI> highlightedEdges = HashMultimap.create();

        // Find edges of interest
//        JIPipeDataSlot targetSlot = null;
        if (settings.isDrawLabelsOnHover() && !dragManagerMove.isCurrentlyDraggingNode() && !dragManagerConnect.isCurrentlyDraggingConnection() && lastMousePosition != null) {
            if (currentlyMouseEnteredNode != null && currentlyMouseEnteredNodeActiveArea instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                JIPipeDesktopGraphNodeUISlotActiveArea slot = (JIPipeDesktopGraphNodeUISlotActiveArea) currentlyMouseEnteredNodeActiveArea;
//                targetSlot = slot.getSlot();
                for (JIPipeDesktopGraphEdgeUI displayedSlotEdge : displayedMainEdges) {
                    if (slot.getSlot() == displayedSlotEdge.getTarget() || slot.getSlot() == displayedSlotEdge.getSource()) {

                        // Set ID
                        int id = edgeIds.getOrDefault(displayedSlotEdge, edgeIds.size() + 1);
                        edgeIds.put(displayedSlotEdge, id);

                        if (slot.getSlot() == displayedSlotEdge.getTarget()) {
                            highlightedEdges.put(displayedSlotEdge.getSource(), displayedSlotEdge);
                        } else if (slot.getSlot() == displayedSlotEdge.getSource()) {
                            highlightedEdges.put(displayedSlotEdge.getTarget(), displayedSlotEdge);
                        }

                    }
                }
            }
        }

        // Cancel if there is only a single edge and not far away
        if (edgeIds.isEmpty()) {
            return;
        }
        g.setPaint(ThemeUtils.getCurrentStyle().getPrimaryColor());
        g.setStroke(new BasicStroke((int) Math.round(2 * zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (JIPipeDataSlot dataSlot : highlightedEdges.keySet()) {
            JIPipeDesktopGraphNodeUI nodeUI = nodeUIs.get(dataSlot.getNode());
            if (nodeUI != null) {
                JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(dataSlot);
                int nodeX = nodeUI.getX();
                int nodeY = nodeUI.getY();
                if (slotActiveArea != null && slotActiveArea.getLastFillRect() != null) {
                    Rectangle lastFillRect = slotActiveArea.getLastFillRect();
                    int x = (int) Math.round(lastFillRect.x + 2 * zoom);
                    int y = (int) Math.round(lastFillRect.y + 2 * zoom);
                    int width = lastFillRect.width - (int) Math.round(2 * 2 * zoom);
                    int height = (int) Math.round(lastFillRect.height - 2 * 2 * zoom);
                    int arc = (int) Math.round(2 * zoom);

//                    if(dataSlot.isInput()) {
//                        int indicatorY =  (int) Math.round(2 * zoom);
//                        g.fillRoundRect(nodeX + x, nodeY + indicatorY, width, height, arc, arc);
//                    }
//                    else {
//                        int indicatorY =  (int) Math.round(nodeUI.getHeight() - 4 * zoom);
//                        g.fillRoundRect(nodeX + x, nodeY + indicatorY, width, height, arc, arc);
//                    }
//                    g.fillRoundRect(nodeX + x, (int) Math.round(nodeY + lastFillRect.y + 2 * zoom), width, height, arc, arc);
//                    g.fillRoundRect(nodeX + x, (int) Math.round(nodeY + lastFillRect.y + lastFillRect.height - 4 * zoom), width, height, arc, arc);
                    g.drawRoundRect(nodeX + x, nodeY + y, width - 1, height, arc, arc);
                }
            }
        }
    }

    private void paintSlotEdge(Graphics2D g,
                               Stroke stroke,
                               Stroke strokeBorder,
                               JIPipeDesktopGraphEdgeUI displayedSlotEdge,
                               boolean multicolor,
                               double scale,
                               int viewX,
                               int viewY,
                               boolean enableArrows) {

        PointRange sourcePoint = displayedSlotEdge.getSourcePoint();
        PointRange targetPoint = displayedSlotEdge.getTargetPoint();
        JIPipeDesktopGraphNodeUI sourceUI = displayedSlotEdge.getSourceUI();
        JIPipeDataSlot source = displayedSlotEdge.getSource();
        JIPipeDataSlot target = displayedSlotEdge.getTarget();
        JIPipeGraphEdge.Shape uiShape = displayedSlotEdge.getEdge().getUiShape();
        int multiColorMax = displayedSlotEdge.getMultiColorMax();
        int multiColorIndex = displayedSlotEdge.getMultiColorIndex();

        if (displayedSlotEdge.isHidden()) {
            // Tighten the point ranges: Bringing the centers together
            PointRange.tighten(sourcePoint, targetPoint);

            g.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_SMART_EDGE);
            g.setPaint(resources.getEdgeBackgroundPaint(source, target, sourcePoint, targetPoint, Color.LIGHT_GRAY));
            paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, false);
            return;
        }

        // Tighten the point ranges: Bringing the centers together
        PointRange.tighten(sourcePoint, targetPoint);

        // Draw arrow
        if (settings.isDrawImprovedEdges() && strokeBorder != null) {
            g.setStroke(strokeBorder);
            g.setColor(resources.getEdgeColor(source, target, multicolor, multiColorIndex, multiColorMax));
            paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, enableArrows);
            g.setStroke(stroke);

            g.setPaint(resources.getEdgeBackgroundPaint(source, target, sourcePoint, targetPoint, resources.getImprovedStrokeBackgroundColor()));
            paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, enableArrows);
        } else {
            g.setStroke(stroke);
            g.setColor(resources.getEdgeColor(source, target, multicolor, multiColorIndex, multiColorMax));
            paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, enableArrows);
        }
    }

    private void paintOutsideEdges(Graphics2D g, boolean onlySelected, Color baseColor, Stroke stroke, Stroke borderStroke) {
        for (JIPipeDesktopGraphNodeUI ui : nodeUIs.values()) {
            Set<UUID> visibleCompartments = graph.getVisibleCompartmentUUIDsOf(ui.getNode());
            if (!visibleCompartments.isEmpty()) {
                if (onlySelected) {
                    if (!selectionManager.getSelection().contains(ui))
                        continue;
                } else {
                    if (selectionManager.getSelection().contains(ui))
                        continue;
                }
                Point sourcePoint = new Point();
                Point targetPoint = new Point();
                boolean uiIsOutput = getCompartmentUUID() == null || getCompartmentUUID().equals(ui.getNode().getCompartmentUUIDInParentGraph());
                if (uiIsOutput) {
                    // This is an output -> line goes outside
                    targetPoint.x = ui.getX() + ui.getWidth() / 2;
                    targetPoint.y = ui.getY() + ui.getHeight();
                    sourcePoint.x = targetPoint.x;
                    sourcePoint.y = getHeight();
                } else {
                    // This is an input
                    targetPoint.x = ui.getX() + ui.getWidth() / 2;
                    targetPoint.y = ui.getY();
                    sourcePoint.x = targetPoint.x;
                    sourcePoint.y = 0;
                }
                if (settings.isDrawImprovedEdges() && borderStroke != null) {
                    g.setStroke(borderStroke);
                    g.setColor(baseColor);
                    paintOutsideEdge(g, sourcePoint, targetPoint, !uiIsOutput);
                    g.setStroke(stroke);
                    g.setColor(resources.getImprovedStrokeBackgroundColor());
                    paintOutsideEdge(g, sourcePoint, targetPoint, !uiIsOutput);
                } else {
                    g.setStroke(stroke);
                    g.setColor(baseColor);
                    paintOutsideEdge(g, sourcePoint, targetPoint, !uiIsOutput);
                }
            }
        }
    }

    /**
     * Returns the UI location of a data slot that is located in the graph
     *
     * @param slot the data slot
     * @return the center slot location. Null if the algorithm has no UI or the returned location is null
     */
    public Point getSlotLocation(JIPipeDataSlot slot) {
        JIPipeDesktopGraphNodeUI algorithmUI = nodeUIs.getOrDefault(slot.getNode(), null);
        if (algorithmUI != null) {
            PointRange location = algorithmUI.getSlotLocation(slot);
            if (location != null) {
                return new Point(algorithmUI.getX() + location.center.x, algorithmUI.getY() + location.center.y);
            }
        }
        return null;
    }

    private void paintOutsideEdge(Graphics2D g, Point sourcePoint, Point targetPoint, boolean drawArrowHead) {
        int arrowHeadShift = resources.getArrowHeadShift();
        int dx;
        int dy;
        if (drawArrowHead) {
            dx = 0;
            dy = arrowHeadShift;
        } else {
            dx = 0;
            dy = 0;
        }
        g.drawLine(sourcePoint.x, sourcePoint.y, targetPoint.x + dx, targetPoint.y + dy);
        if (drawArrowHead && settings.isDrawArrowHeads()) {
            paintArrowHead(g, targetPoint.x, targetPoint.y);
        }

    }

    /**
     * Draws an edge between source point and the target point
     *
     * @param g            the graphics
     * @param sourcePoint  the source point
     * @param sourceBounds bounds of the source
     * @param targetPoint  the target point
     * @param shape        the line shape
     * @param scale        the scale
     * @param viewX        the view x
     * @param viewY        the view y
     * @param enableArrows enable arrows
     */
    public void paintEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, JIPipeGraphEdge.Shape shape, double scale, int viewX, int viewY, boolean enableArrows) {
        switch (shape) {
            case Elbow:
                paintElbowEdge(g, sourcePoint, sourceBounds, targetPoint, scale, viewX, viewY, enableArrows);
                break;
            case Line: {
                int arrowHeadShift = enableArrows ? resources.getArrowHeadShift() : 0;
                int dx;
                int dy;
                dx = 0;
                dy = arrowHeadShift;
                g.drawLine((int) (scale * sourcePoint.x) + viewX,
                        (int) (scale * sourcePoint.y) + viewY,
                        (int) (scale * targetPoint.x) + viewX + dx,
                        (int) (scale * targetPoint.y) + viewY + dy);
            }
            break;
        }
        if (enableArrows && settings.isDrawArrowHeads()) {
            paintArrowHead(g, targetPoint.x, targetPoint.y);
        }
    }

    private void paintArrowHead(Graphics2D g, int x, int y) {
        int sz = 1;
        int dy = -2 * sz - 4;
        g.drawPolygon(new int[]{x - sz, x + sz, x}, new int[]{y - sz + dy, y - sz + dy, y + dy}, 3);
    }

    private void paintElbowEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, double scale, int viewX, int viewY, boolean enableArrows) {
        int buffer;
        int sourceA;
        int targetA;
        int sourceB;
        int targetB;
        int componentStartB;
        int componentEndB;

        buffer = JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT / 2;
        sourceA = sourcePoint.y;
        targetA = targetPoint.y;
        if (enableArrows) {
            targetA += resources.getArrowHeadShift();
        }
        sourceB = sourcePoint.x;
        targetB = targetPoint.x;
        componentStartB = sourceBounds.x;
        componentEndB = sourceBounds.x + sourceBounds.width;

        int a0 = sourceA;
        int b0 = sourceB;
        int a1 = sourceA;
        int b1 = sourceB;

        TIntArrayList xCoords = new TIntArrayList(8);
        TIntArrayList yCoords = new TIntArrayList(8);

        addElbowPolygonCoordinate(a0, b0, scale, viewX, viewY, xCoords, yCoords);

        // Target point is above the source. We have to navigate around it
        if (sourceA > targetA) {
            // Add some space in major direction
            a1 += buffer;
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);

            // Go left or right
            if (targetB <= b1) {
                b1 = Math.max(0, componentStartB - buffer);
            } else {
                b1 = componentEndB + buffer;
            }
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);

            // Go to target height
            a1 = Math.max(0, targetA - buffer);
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);
        } else if (sourceB != targetB) {
            // Add some space in major direction
            int dA = targetA - sourceA;
            a1 = Math.min(sourceA + buffer, sourceA + dA / 2);
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);
        }

        // Target point X is shifted
        if (b1 != targetB) {
            b1 = targetB;
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);
        }

        // Go to end point
        a1 = targetA;
        addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);

        // Draw the polygon
        g.drawPolyline(xCoords.toArray(), yCoords.toArray(), xCoords.size());

    }

    private void addElbowPolygonCoordinate(int a1, int b1, double scale, int viewX, int viewY, TIntList xCoords, TIntList yCoords) {
        int x2, y2;
        x2 = (int) (b1 * scale) + viewX;
        y2 = (int) (a1 * scale) + viewY;
        xCoords.add(x2);
        yCoords.add(y2);
    }

    /**
     * @return The displayed compartment
     */
    public UUID getCompartmentUUID() {
        return compartmentUUID;
    }

    /**
     * Creates a screenshot of the whole graph compartment
     *
     * @return The screenshot image
     */
    public BufferedImage createScreenshotPNG() {
        return ScreenImage.createImage(this);
    }

    /**
     * Creates a screenshot of the whole graph compartment
     *
     * @return The screenshot image
     */
    public SVGGraphics2D createScreenshotSVG() {
        return ScreenImageSVG.createImage(this);
    }

    public BiMap<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> getNodeUIs() {
        return ImmutableBiMap.copyOf(nodeUIs);
    }

    public JIPipeDesktopGraphDragAndDropBehavior getDragAndDropBehavior() {
        return dragAndDropBehavior;
    }

    public void setDragAndDropBehavior(JIPipeDesktopGraphDragAndDropBehavior dragAndDropBehavior) {
        this.dragAndDropBehavior = dragAndDropBehavior;
        dragAndDropBehavior.setCanvas(this);
        new DropTarget(this, dragAndDropBehavior);
    }


    /**
     * Sets node positions to make the top left to 0, 0
     *
     * @param save if the locations should be saved
     */
    public void crop(boolean save) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (JIPipeDesktopGraphNodeUI ui : nodeUIs.values()) {
            minX = Math.min(ui.getX(), minX);
            minY = Math.min(ui.getY(), minY);
        }
        boolean oldModified = getDesktopWorkbench().isProjectModified();
        for (JIPipeDesktopGraphNodeUI ui : nodeUIs.values()) {
            ui.moveToClosestGridPoint(new Point(ui.getX() - minX + JIPipeDesktopGraphCanvasGrid.GRID_WIDTH,
                    ui.getY() - minY + JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT), true, save);
        }
        getDesktopWorkbench().setProjectModified(oldModified);
        setGraphEditCursor(JIPipeDesktopGraphCanvasGrid.gridToRealLocation(new Point(1, 1), zoom));
        minDimensions = null;
        if (getParent() != null)
            getParent().revalidate();
        repaintLowLag();
    }

    public Point getGraphEditorCursor() {
//        if(System.identityHashCode(this) == 1274726433) {
//            System.out.println("wtdf");
//        }
        long stamp = stampedLock.readLock();
//        System.out.println("is: " + graphEditCursor + " in " + System.identityHashCode(this));
        try {
            if (graphEditCursor == null)
                return new Point(0, 0);
            return new Point(graphEditCursor.x, graphEditCursor.y);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Removes all UIs and adds them back in
     */
    public void fullRedraw() {
        removeAllNodes();
        addNewNodes(true);
    }

    public List<GraphInteractiveObjectUIContextAction> getContextActions() {
        return contextActions;
    }

    public void setContextActions(List<GraphInteractiveObjectUIContextAction> contextActions) {
        this.contextActions = contextActions;
    }

    public <T extends JIPipeGraphNode> Set<JIPipeDesktopGraphNodeUI> getNodeUIsFor(Set<T> nodes) {
        Set<JIPipeDesktopGraphNodeUI> uis = new HashSet<>();
        for (T node : nodes) {
            JIPipeDesktopGraphNodeUI ui = nodeUIs.getOrDefault(node, null);
            if (ui != null) {
                uis.add(ui);
            }
        }
        return uis;
    }

    public void setGraphEditCursor(Point graphEditCursor) {
        long stamp = stampedLock.writeLock();
        try {
            this.graphEditCursor = graphEditCursor != null ? new Point(graphEditCursor.x, graphEditCursor.y) : new Point();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    @Override
    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        double oldZoom = this.zoom;
        this.zoom = zoom;

        // Recalculate assets
        resources.updateAssets();

        // Zoom the cursor
        Point cursor = getGraphEditorCursor();
        double normalizedCursorX = cursor.x / oldZoom;
        double normalizedCursorY = cursor.y / oldZoom;
        setGraphEditCursor(new Point((int) Math.round(normalizedCursorX * zoom), (int) Math.round(normalizedCursorY * zoom)));

        // Zoom nodes

        zoomChangedEventEmitter.emit(new ZoomChangedEvent(this));
        for (JIPipeDesktopGraphNodeUI ui : nodeUIs.values()) {
            ui.moveToStoredGridLocation(true);
            ui.setZoom(zoom);
        }
        graphCanvasUpdatedEventEmitter.emit(new JIPipeDesktopGraphCanvasUIUpdatedEvent(this));
    }

    public JIPipeDesktopGraphCanvasUIUpdatedEventEmitter getGraphCanvasUpdatedEventEmitter() {
        return graphCanvasUpdatedEventEmitter;
    }


    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isControlDown()) {
            // We move the graph cursor to the mouse
            // The zoom will "focus" on this cursor and modify the scroll bars accordingly
            int x = e.getX();
            int y = e.getY();
            double beforeZoomX = x * zoom;
            double beforeZoomY = y * zoom;

            if (e.getWheelRotation() < 0) {
                zoomIn();
            } else {
                zoomOut();
            }

            double afterZoomX = x * zoom;
            double afterZoomY = y * zoom;

            double dX = afterZoomX - beforeZoomX;
            double dY = afterZoomY - beforeZoomY;

            JScrollPane scrollPane = getScrollPane();
            if (scrollPane != null) {
                scrollPane.getHorizontalScrollBar().setValue(scrollPane.getHorizontalScrollBar().getValue() + (int) dX);
                scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getValue() + (int) dY);
            }

        } else {
            getParent().dispatchEvent(e);
        }
    }

    public void resetZoom() {
        setZoom(1.0);
    }

    public void zoomOut() {
        setZoom(Math.max(0.1, zoom - 0.05));
    }

    public void zoomIn() {
        setZoom(Math.min(3, zoom + 0.05));
    }

    public JScrollPane getScrollPane() {
        return graphEditorUI != null ? graphEditorUI.getScrollPane() : null;
    }


    public JIPipeHistoryJournal getHistoryJournal() {
        return historyJournal;
    }

    @Override
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {

        if (disposed) {
            return;
        }

        // Updates existing nodes positions
        for (JIPipeDesktopGraphNodeUI ui : nodeUIs.values()) {
            ui.moveToStoredGridLocation(true);
        }
        removeOldNodes();     // Remove invalid UIs
        addNewNodes(true);   // Add missing UIs
        
        // Update edge UIs
        removeOldEdges();
        addNewEdges();

        requestFocusInWindow();
    }

    @Override
    public void onNodeConnected(JIPipeGraph.NodeConnectedEvent event) {

        if (disposed) {
            return;
        }

        JIPipeDesktopGraphNodeUI sourceNode = nodeUIs.getOrDefault(event.getSource().getNode(), null);
        JIPipeDesktopGraphNodeUI targetNode = nodeUIs.getOrDefault(event.getTarget().getNode(), null);

        // Check if we actually need to auto-place
        if (sourceNode != null && targetNode != null && targetNode.getY() >= sourceNode.getBottomY() + JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT) {
            return;
        }

        boolean layoutHelperEnabled = settings != null && settings.isLayoutAfterConnect();
        if (sourceNode != null && targetNode != null && layoutHelperEnabled) {

            // Disabled for comment nodes
            if (sourceNode.getNode() instanceof JIPipeCommentNode || targetNode.getNode() instanceof JIPipeCommentNode) {
                return;
            }

            Point cursorBackup = getGraphEditorCursor();
            try {
                setGraphEditCursor(new Point(targetNode.getX(), targetNode.getBottomY() + 4 * JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT));
                nodeManager.autoPlaceTargetAdjacent(sourceNode, event.getSource(), targetNode, event.getTarget());
                autoExpandLeftTop();
            } finally {
                setGraphEditCursor(cursorBackup);
            }
        }

        requestFocusInWindow();
        graphCanvasUpdatedEventEmitter.emit(new JIPipeDesktopGraphCanvasUIUpdatedEvent(this));
    }

    @Override
    public void onNodeUIActionRequested(JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEvent event) {

        if (disposed) {
            return;
        }

        if (event.getAction() instanceof JIPipeDesktopOpenContextMenuAction) {
            if (event.getUi() != null) {
                openContextMenu(getLastMousePosition());
            }
        }
        nodeUIActionRequestedEventEmitter.emit(event);
    }

    public void moveSelection(int gridDx, int gridDy, boolean force) {

        if (selectionManager.getSelection().isEmpty())
            return;

        int negativeDx = 0;
        int negativeDy = 0;
        for (JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI : selectionManager.getSelection()) {
            if (interactiveObjectUI instanceof JIPipeDesktopGraphNodeUI nodeUI) {
                if (force || nodeUI.getNode().isUiLocked())
                    continue;

                Point newGridLocation = new Point(nodeUI.getStoredGridLocation().x + gridDx, nodeUI.getStoredGridLocation().y + gridDy);
                if (newGridLocation.x <= 0) {
                    negativeDx = Math.min(negativeDx, newGridLocation.x - 1);
                }
                if (newGridLocation.y <= 0) {
                    negativeDy = Math.min(negativeDy, newGridLocation.y - 1);
                }
            }
        }

        if (negativeDx < 0 || negativeDy < 0) {
            // Negative expansion
            for (JIPipeDesktopGraphNodeUI value : nodeUIs.values()) {
                if (!dragManagerMove.isBeingDragged(value)) {
                    Point storedGridLocation = value.getStoredGridLocation();
                    value.moveToGridLocation(new Point(storedGridLocation.x - negativeDx, storedGridLocation.y - negativeDy), true, true);
                }
            }
        }

        for (JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI : selectionManager.getSelection()) {
            if (interactiveObjectUI instanceof JIPipeDesktopGraphNodeUI nodeUI) {
                if (force || nodeUI.getNode().isUiLocked())
                    continue;

                Point newGridLocation = new Point(nodeUI.getStoredGridLocation().x + gridDx, nodeUI.getStoredGridLocation().y + gridDy);
                if (!dragManagerMove.isCurrentlyDraggingNode()) {
                    // Check if something would change
                    if (!Objects.equals(nodeUI.getStoredGridLocation(), newGridLocation)) {
                        dragManagerMove.createMoveSnapshotIfNeeded();
                    }
                }
                nodeUI.moveToGridLocation(newGridLocation, true, true);
            }
        }

        repaintLowLag();
        revalidateParent();
        graphCanvasUpdatedEventEmitter.emit(new JIPipeDesktopGraphCanvasUIUpdatedEvent(this));
    }

    public Map<UUID, JIPipeGraphNode> pasteNodes(JIPipeGraph graph) throws JsonProcessingException {
        return nodeManager.pasteNodes(JsonUtils.toJsonString(graph));
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return desktopWorkbench;
    }

    public Set<JIPipeGraphNode> getVisibleNodes() {
        Set<JIPipeGraphNode> visibleNodes = new HashSet<>();
        for (JIPipeGraphNode graphNode : getGraph().getGraphNodes()) {
            if (getCompartmentUUID() != null) {
                if (graphNode.isVisibleIn(getCompartmentUUID())) {
                    visibleNodes.add(graphNode);
                }
            } else {
                visibleNodes.add(graphNode);
            }
        }
        return visibleNodes;
    }

    public boolean isGraphAnnotationsLocked() {
        return graphAnnotationsLocked;
    }

    public void setGraphAnnotationsLocked(boolean graphAnnotationsLocked) {
        this.graphAnnotationsLocked = graphAnnotationsLocked;
        if (graphAnnotationsLocked) {
            for (JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI : ImmutableList.copyOf(selectionManager.getSelection())) {
                if (interactiveObjectUI instanceof JIPipeDesktopAnnotationGraphNodeUI) {
                    selectionManager.removeFromSelection(interactiveObjectUI);
                }
            }
        }
    }

    public void moveToFrontLayer(JIPipeDesktopGraphInteractiveObjectUI ui) {
        if (ui instanceof Component c) {
            if (getLayer(c) < currentNodeLayer) {
                setLayer(c, ++currentNodeLayer);
            }
        }
    }

    public JIPipeDesktopGraphCanvasNodeManager getNodeManager() {
        return nodeManager;
    }

    public JIPipeDesktopGraphCanvasToolManager getToolManager() {
        return toolManager;
    }

    public JIPipeDesktopGraphCanvasEdgeManager getEdgeManager() {
        return edgeManager;
    }

    public JIPipeDesktopGraphCanvasSelectionBoxManager getSelectionBoxManager() {
        return selectionBoxManager;
    }

    public JIPipeDesktopGraphCanvasConnectionHighlightManager getConnectionHighlightManager() {
        return connectionHighlightManager;
    }

    public JIPipeDesktopGraphCanvasDragManagerMove getDragManagerMove() {
        return dragManagerMove;
    }

    public JIPipeDesktopGraphCanvasDragManagerConnect getDragManagerConnect() {
        return dragManagerConnect;
    }

    public void revalidateParent() {
        if (getParent() != null) {
            getParent().revalidate();
        }
    }

    public JIPipeDesktopGraphCanvasResources getResources() {
        return resources;
    }
}
