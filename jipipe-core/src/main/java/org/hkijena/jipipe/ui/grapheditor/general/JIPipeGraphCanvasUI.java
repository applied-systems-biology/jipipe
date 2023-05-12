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

package org.hkijena.jipipe.ui.grapheditor.general;

import com.google.common.collect.*;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.grapheditortool.JIPipeToggleableGraphEditorTool;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.jipipe.ui.components.ZoomViewPort;
import org.hkijena.jipipe.ui.components.renderers.DropShadowRenderer;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.NodeHotKeyStorage;
import org.hkijena.jipipe.ui.grapheditor.general.actions.OpenContextMenuAction;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.layout.MSTGraphAutoLayoutMethod;
import org.hkijena.jipipe.ui.grapheditor.general.layout.SugiyamaGraphAutoLayoutMethod;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUIActiveArea;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUIAddSlotButtonActiveArea;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUISlotActiveArea;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.ScreenImage;
import org.hkijena.jipipe.utils.ui.ScreenImageSVG;
import org.jetbrains.annotations.NotNull;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.scijava.Disposable;

import javax.swing.FocusManager;
import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI that displays an {@link JIPipeGraph}
 */
public class JIPipeGraphCanvasUI extends JLayeredPane implements JIPipeWorkbenchAccess, MouseMotionListener, MouseListener, MouseWheelListener, ZoomViewPort, Disposable,
        JIPipeGraph.GraphChangedEventListener, JIPipeGraph.NodeConnectedEventListener, JIPipeNodeUI.NodeUIActionRequestedEventListener {

    public static final DropShadowRenderer DROP_SHADOW_BORDER = new DropShadowRenderer(Color.BLACK,
            5,
            0.3f,
            12,
            true,
            true,
            true,
            true);
    public static final DropShadowRenderer BOOKMARK_SHADOW_BORDER = new DropShadowRenderer(new Color(0x33cc33),
            12,
            0.3f,
            12,
            true,
            true,
            true,
            true);

    public static final Color COLOR_HIGHLIGHT_GREEN = new Color(0, 128, 0);
    public static final Stroke STROKE_UNIT = new BasicStroke(1);
    public static final Stroke STROKE_UNIT_COMMENT = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{1}, 0);
    public static final Stroke STROKE_DEFAULT = new BasicStroke(4);
    public static final Stroke STROKE_DEFAULT_BORDER = new BasicStroke(6);
    public static final Stroke STROKE_HIGHLIGHT = new BasicStroke(8);
    public static final Stroke STROKE_SELECTION = new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0);
    public static final Stroke STROKE_MARQUEE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Stroke STROKE_COMMENT = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Stroke STROKE_COMMENT_HIGHLIGHT = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{8}, 0);
    public static final Stroke STROKE_SMART_EDGE = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    private static final Color COMMENT_EDGE_COLOR = new Color(194, 141, 0);
    private final JIPipeWorkbench workbench;
    private final JIPipeGraphEditorUI graphEditorUI;
    private final ImageIcon cursorImage = UIUtils.getIconFromResources("actions/target.png");
    private final JIPipeGraph graph;
    private final BiMap<JIPipeGraphNode, JIPipeNodeUI> nodeUIs = HashBiMap.create();
    private final Set<JIPipeNodeUI> selection = new LinkedHashSet<>();
    private final GraphEditorUISettings settings;
    private final JIPipeHistoryJournal historyJournal;
    private final UUID compartment;
    private final Map<JIPipeNodeUI, Point> currentlyDraggedOffsets = new HashMap<>();
    private final NodeHotKeyStorage nodeHotKeyStorage;
    private final Color improvedStrokeBackgroundColor = UIManager.getColor("Panel.background");
    private final Color smartEdgeSlotBackground = UIManager.getColor("EditorPane.background");
    private final Color smartEdgeSlotForeground = UIManager.getColor("Label.foreground");
    private final JIPipeGraphViewMode viewMode = JIPipeGraphViewMode.VerticalCompact;
    private final Map<?, ?> desktopRenderingHints = UIUtils.getDesktopRenderingHints();
    private JIPipeGraphDragAndDropBehavior dragAndDropBehavior;
    private Point graphEditCursor;
    private Point selectionFirst;
    private Point selectionSecond;
    private long lastTimeExpandedNegative = 0;
    private List<NodeUIContextAction> contextActions = new ArrayList<>();
    private JIPipeNodeUIActiveArea currentConnectionDragSource;
    private boolean currentConnectionDragSourceDragged;
    private JIPipeNodeUIActiveArea currentConnectionDragTarget;
    private double zoom = 1.0;
    private Set<JIPipeGraphNode> scheduledSelection = new HashSet<>();
    private boolean hasDragSnapshot = false;
    private int currentNodeLayer = Integer.MIN_VALUE;
    private boolean renderCursor = true;
    private boolean renderOutsideEdges = true;
    /**
     * Used to store the minimum dimensions of the canvas to reduce user disruption
     */
    private Dimension minDimensions = null;
    private JIPipeNodeUI currentlyMouseEnteredNode;
    private DisconnectHighlight disconnectHighlight;
    private ConnectHighlight connectHighlight;
    private Font smartEdgeTooltipSlotFont;
    private Font smartEdgeTooltipNodeFont;
    private List<DisplayedSlotEdge> lastDisplayedMainEdges;
    private JIPipeToggleableGraphEditorTool currentTool;

    private boolean autoHideEdges;

    private boolean autoHideDrawLabels;
    private Point lastMousePosition;

    private final ZoomChangedEventEmitter zoomChangedEventEmitter = new ZoomChangedEventEmitter();
    private final GraphCanvasUpdatedEventEmitter graphCanvasUpdatedEventEmitter = new GraphCanvasUpdatedEventEmitter();
    private final NodeSelectionChangedEventEmitter nodeSelectionChangedEventEmitter = new NodeSelectionChangedEventEmitter();
    private final NodeUISelectedEventEmitter nodeUISelectedEventEmitter = new NodeUISelectedEventEmitter();
    private final JIPipeNodeUI.DefaultNodeUIActionRequestedEventEmitter defaultNodeUIActionRequestedEventEmitter = new JIPipeNodeUI.DefaultNodeUIActionRequestedEventEmitter();
    private final JIPipeNodeUI.NodeUIActionRequestedEventEmitter nodeUIActionRequestedEventEmitter = new JIPipeNodeUI.NodeUIActionRequestedEventEmitter();

    /**
     * Creates a new UI
     *
     * @param workbench      the workbench
     * @param graphEditorUI  the graph editor UI that contains this canvas. can be null.
     * @param graph          The algorithm graph
     * @param compartment    The compartment to show
     * @param historyJournal object that tracks the history of this graph. Set to null to disable the undo feature.
     */
    public JIPipeGraphCanvasUI(JIPipeWorkbench workbench, JIPipeGraphEditorUI graphEditorUI, JIPipeGraph graph, UUID compartment, JIPipeHistoryJournal historyJournal) {
        this.workbench = workbench;
        this.graphEditorUI = graphEditorUI;
        this.historyJournal = historyJournal;
        setLayout(null);
        this.graph = graph;
        this.nodeHotKeyStorage = NodeHotKeyStorage.getInstance(graph);
        this.compartment = compartment;
        this.settings = GraphEditorUISettings.getInstance();

        this.autoHideEdges = settings.isAutoHideEdgeEnabled();
        this.autoHideDrawLabels = settings.isAutoHideDrawLabels();

        graph.attachAdditionalMetadata("jipipe:graph:view-mode", JIPipeGraphViewMode.VerticalCompact);
        initialize();
        addNewNodes(false);

        graph.getGraphChangedEventEmitter().subscribeWeak(this);
        graph.getNodeConnectedEventEmitter().subscribeWeak(this);

        initializeHotkeys();
        updateAssets();
    }

    public Point getLastMousePosition() {
        return lastMousePosition;
    }

    public NodeSelectionChangedEventEmitter getNodeSelectionChangedEventEmitter() {
        return nodeSelectionChangedEventEmitter;
    }

    public NodeUISelectedEventEmitter getNodeUISelectedEventEmitter() {
        return nodeUISelectedEventEmitter;
    }

    public JIPipeNodeUI.DefaultNodeUIActionRequestedEventEmitter getDefaultAlgorithmUIActionRequestedEventEmitter() {
        return defaultNodeUIActionRequestedEventEmitter;
    }

    public JIPipeNodeUI.NodeUIActionRequestedEventEmitter getNodeUIActionRequestedEventEmitter() {
        return nodeUIActionRequestedEventEmitter;
    }

    @Override
    public void dispose() {
        graph.getGraphChangedEventEmitter().unsubscribe(this);
        graph.getNodeConnectedEventEmitter().unsubscribe(this);
        for (JIPipeNodeUI nodeUI : nodeUIs.values()) {
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

    public boolean isAutoHideEdges() {
        return autoHideEdges;
    }

    public void setAutoHideEdges(boolean autoHideEdges) {
        this.autoHideEdges = autoHideEdges;
        settings.setAutoHideEdgeEnabled(autoHideEdges);
        JIPipe.getSettings().save();
        repaint(50);
    }

    public boolean isAutoHideDrawLabels() {
        return autoHideDrawLabels;
    }

    public void setAutoHideDrawLabels(boolean autoHideDrawLabels) {
        this.autoHideDrawLabels = autoHideDrawLabels;
        settings.setAutoHideDrawLabels(autoHideDrawLabels);
        JIPipe.getSettings().save();
        repaint(50);
    }

    public boolean isRenderOutsideEdges() {
        return renderOutsideEdges;
    }

    public void setRenderOutsideEdges(boolean renderOutsideEdges) {
        this.renderOutsideEdges = renderOutsideEdges;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public JIPipeGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public GraphEditorUISettings getSettings() {
        return settings;
    }

    private void initializeHotkeys() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventDispatcher(e -> {
            KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
            if (this.isDisplayable() && FocusManager.getCurrentManager().getFocusOwner() == this) {
                for (NodeUIContextAction contextAction : contextActions) {
                    if (contextAction == null)
                        continue;
                    if (!contextAction.matches(selection))
                        continue;
                    if (contextAction.getKeyboardShortcut() != null && Objects.equals(contextAction.getKeyboardShortcut(), keyStroke)) {
                        Point mousePosition = this.getMousePosition(true);
                        if (mousePosition != null) {
                            setGraphEditCursor(mousePosition);
                        }
                        getWorkbench().sendStatusBarText("Executed: " + contextAction.getName());
                        SwingUtilities.invokeLater(() -> contextAction.run(this, selection));
                        return true;
                    }
                }
                if (keyStroke.getModifiers() == 0) {
                    NodeHotKeyStorage.Hotkey hotkey = NodeHotKeyStorage.Hotkey.fromKeyCode(keyStroke.getKeyCode());
                    if (hotkey != NodeHotKeyStorage.Hotkey.None) {
                        String nodeId = nodeHotKeyStorage.getNodeForHotkey(hotkey, getCompartment());
                        JIPipeGraphNode node = graph.findNode(nodeId);
                        if (node != null) {
                            JIPipeNodeUI nodeUI = nodeUIs.getOrDefault(node, null);
                            if (nodeUI != null) {
                                Container graphEditor = SwingUtilities.getAncestorOfClass(JIPipeGraphEditorUI.class, this);
                                if (graphEditor == null)
                                    selectOnly(nodeUI);
                                else
                                    ((JIPipeGraphEditorUI) graphEditor).selectOnly(nodeUI);
                            }
                        }
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
        for (JIPipeNodeUI ui : ImmutableList.copyOf(nodeUIs.values())) {
            remove(ui);
            try {
                unregisterNodeUIEvents(ui);
            } catch (Throwable e) {
            }
        }
        nodeUIs.clear();
        selection.clear();
        revalidate();
        repaint();
        updateSelection();
    }

    /**
     * Removes node UIs that are not valid anymore
     */
    private void removeOldNodes() {
        Set<JIPipeGraphNode> toRemove = new HashSet<>();
        for (Map.Entry<JIPipeGraphNode, JIPipeNodeUI> kv : nodeUIs.entrySet()) {
            if (!graph.containsNode(kv.getKey()) || !kv.getKey().isVisibleIn(getCompartment()))
                toRemove.add(kv.getKey());
        }
        for (JIPipeGraphNode algorithm : toRemove) {
            JIPipeNodeUI ui = nodeUIs.get(algorithm);
            selection.remove(ui);
            remove(ui);
            nodeUIs.remove(algorithm);
        }
        if (!toRemove.isEmpty()) {
            updateSelection();
            revalidate();
            repaint();
            graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
        }
    }

    /**
     * Adds node UIs that are not in the canvas yet
     *
     * @param force if the positioning is forced
     */
    private void addNewNodes(boolean force) {
        List<JIPipeNodeUI> newlyPlacedAlgorithms = new ArrayList<>();
        JIPipeNodeUI ui = null;
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            if (!algorithm.isVisibleIn(getCompartment()))
                continue;
            if (nodeUIs.containsKey(algorithm))
                continue;

            ui = new JIPipeNodeUI(getWorkbench(), this, algorithm);
            registerNodeUIEvents(ui);
            add(ui, new Integer(currentNodeLayer++)); // Layered pane
            nodeUIs.put(algorithm, ui);
            if (!ui.moveToStoredGridLocation(force)) {
                autoPlaceCloseToCursor(ui, force);
                newlyPlacedAlgorithms.add(ui);
            }
        }
        revalidate();
        repaint();

        if (newlyPlacedAlgorithms.size() == nodeUIs.size()) {
            autoLayoutAll();
        }
        if (newlyPlacedAlgorithms.size() > 0) {
           graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
        }
        if (scheduledSelection != null && !scheduledSelection.isEmpty()) {
            if (scheduledSelection.equals(getSelectedNodes()))
                return;
            clearSelection();
            for (JIPipeGraphNode node : scheduledSelection) {
                JIPipeNodeUI selected = nodeUIs.getOrDefault(node, null);
                if (selected != null) {
                    addToSelection(selected);
                }
            }
            scheduledSelection.clear();
        }
    }

    private void registerNodeUIEvents(JIPipeNodeUI ui) {
        ui.getNodeUIActionRequestedEventEmitter().subscribe(this);
    }

    private void unregisterNodeUIEvents(JIPipeNodeUI ui) {
        ui.getNodeUIActionRequestedEventEmitter().unsubscribe(this);
    }

    /**
     * Auto-layouts all UIs
     */
    public void autoLayoutAll() {
        if (nodeUIs.isEmpty())
            return;
//        int backup = newEntryLocationX;
//        newEntryLocationX = 0;
//        for (JIPipeAlgorithm algorithm : ImmutableList.copyOf(nodeUIs.keySet())) {
//            algorithm.setLocationWithin(compartment, null);
//            remove(nodeUIs.get(algorithm));
//        }
//        nodeUIs.clear();
//        addNewNodes();
//        newEntryLocationX = backup;
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
                (new SugiyamaGraphAutoLayoutMethod()).accept(this);
                break;
            case MST:
                (new MSTGraphAutoLayoutMethod()).accept(this);
                break;
        }
        repaint();
        graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
    }

    /**
     * Moves the node close to a real location
     *
     * @param ui       the node
     * @param location a real location
     */
    public void autoPlaceCloseToLocation(JIPipeNodeUI ui, Point location) {

        int minX = location.x;
        int minY = location.y;

        Set<Rectangle> otherShapes = new HashSet<>();
        for (JIPipeNodeUI otherUi : nodeUIs.values()) {
            if (ui != otherUi) {
                otherShapes.add(otherUi.getBounds());
            }
        }

        Rectangle viewRectangle = null;
        JScrollPane scrollPane = getScrollPane();
        if (scrollPane != null) {
            int hValue = scrollPane.getHorizontalScrollBar().getValue();
            int vValue = scrollPane.getVerticalScrollBar().getValue();
            int hWidth = scrollPane.getHorizontalScrollBar().getVisibleAmount();
            int vHeight = scrollPane.getVerticalScrollBar().getVisibleAmount();
            viewRectangle = new Rectangle(hValue, vValue, hWidth, vHeight);

            viewRectangle.width -= viewMode.getGridWidth() / 4;
            viewRectangle.height -= viewMode.getGridHeight() / 4;
        }

//        System.out.println("Loc: " + location);
//        System.out.println("View: " + viewRectangle);
        Rectangle currentShape = new Rectangle(minX, minY, ui.getWidth(), ui.getHeight());

        if (viewRectangle != null && !viewRectangle.contains(location)) {
            minX = viewRectangle.x + viewMode.getGridWidth();
            minY = viewRectangle.y + viewMode.getGridHeight();
        }

        boolean found;
        do {
            found = true;
            for (Rectangle otherShape : otherShapes) {
                if (otherShape.intersects(currentShape)) {
                    found = false;
                    break;
                }
            }
            if (!found) {
                currentShape.x += viewMode.getGridWidth();
            }
            /*
             * Check if we are still within the visible rectangle.
             * Prevent nodes going to somewhere else
             */
            if (viewRectangle != null && !viewRectangle.intersects(currentShape)) {
                currentShape.x = minX;
                currentShape.y = minY;
                break;
            }
            /*
             * Check if we are too far away
             * The user expects the new node to be close to the cursor
             */
            double relativeDistanceToOriginalPoint;
            if (viewMode == JIPipeGraphViewMode.Vertical) {
                relativeDistanceToOriginalPoint = Math.abs(1.0 * minX - currentShape.x) / currentShape.width;
            } else {
                relativeDistanceToOriginalPoint = Math.abs(1.0 * minY - currentShape.y) / currentShape.height;
            }
            if (relativeDistanceToOriginalPoint > 2) {
                currentShape.x = minX;
                currentShape.y = minY;
                break;
            }
        }
        while (!found);

//        System.out.println(currentShape);
        ui.moveToClosestGridPoint(new Point(currentShape.x, currentShape.y), true, true);
    }

    public void autoPlaceCloseToCursor(JIPipeNodeUI ui, boolean force) {
//        System.out.println("GE: " + getGraphEditorCursor());
//        int minX = 0;
//        int minY = 0;
//        if (getGraphEditorCursor() != null) {
//            minX = getGraphEditorCursor().x;
//            minY = getGraphEditorCursor().y;
//        } else if (getVisibleRect() != null) {
//            Rectangle rect = getVisibleRect();
//            minX = rect.x;
//            minY = rect.y;
//        }
//        autoPlaceCloseToLocation(ui, new Point(minX, minY));
        int minX = 0;
        int minY = 0;
        if (getGraphEditorCursor() != null) {
            minX = getGraphEditorCursor().x;
            minY = getGraphEditorCursor().y;
        }
        ui.moveToClosestGridPoint(new Point(minX, minY), force, true);
        if (graphEditorUI != null) {
            graphEditorUI.scrollToAlgorithm(ui);
        }
    }

    public Set<JIPipeNodeUI> getNodesAfter(int x, int y) {
        Set<JIPipeNodeUI> result = new HashSet<>();
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            if (ui.getY() >= y)
                result.add(ui);
        }
        return result;
    }

    private void autoPlaceTargetAdjacent(JIPipeNodeUI sourceAlgorithmUI, JIPipeDataSlot source, JIPipeNodeUI targetAlgorithmUI, JIPipeDataSlot target) {
        int sourceSlotIndex = source.getNode().getOutputSlots().indexOf(source);
        int targetSlotIndex = target.getNode().getInputSlots().indexOf(target);
        if (sourceSlotIndex < 0 || targetSlotIndex < 0) {
            autoPlaceCloseToCursor(targetAlgorithmUI, true);
            return;
        }

        Set<JIPipeNodeUI> nodesAfter = getNodesAfter(sourceAlgorithmUI.getRightX(), sourceAlgorithmUI.getBottomY());
        int x = sourceAlgorithmUI.getSlotLocation(source).center.x + sourceAlgorithmUI.getX();
        x -= targetAlgorithmUI.getSlotLocation(target).center.x;
        int y = (int) Math.round(sourceAlgorithmUI.getBottomY() + viewMode.getGridHeight() * zoom);
        Point targetPoint = new Point(x, y);
        if (GraphEditorUISettings.getInstance().isAutoLayoutMovesOtherNodes()) {
            if (!targetAlgorithmUI.moveToClosestGridPoint(targetPoint, false, true)) {
                if (nodesAfter.isEmpty())
                    return;
                // Move all other algorithms
                int minDistance = Integer.MAX_VALUE;
                for (JIPipeNodeUI ui : nodesAfter) {
                    if (ui == targetAlgorithmUI || ui == sourceAlgorithmUI)
                        continue;
                    minDistance = Math.min(minDistance, ui.getY() - sourceAlgorithmUI.getBottomY());
                }
                int translateY = (int) Math.round(targetAlgorithmUI.getHeight() + viewMode.getGridHeight() * zoom * 2 - minDistance);
                for (JIPipeNodeUI ui : nodesAfter) {
                    if (ui == targetAlgorithmUI || ui == sourceAlgorithmUI)
                        continue;
                    ui.moveToClosestGridPoint(new Point(ui.getX(), ui.getY() + translateY), true, true);
                }
                if (!targetAlgorithmUI.moveToClosestGridPoint(targetPoint, false, true)) {
                    autoPlaceCloseToCursor(targetAlgorithmUI, true);
                }
            }
        } else {
            autoPlaceCloseToLocation(targetAlgorithmUI, targetPoint);
        }
    }

    private void createMoveSnapshotIfNeeded() {
        if (!hasDragSnapshot) {
            if (getHistoryJournal() != null) {
                getHistoryJournal().snapshot("Move nodes", "Nodes were dragged with the mouse", getCompartment(), UIUtils.getIconFromResources("actions/transform-move.png"));
            }
            hasDragSnapshot = true;
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (currentTool != null) {
            currentTool.mouseDragged(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        if (currentConnectionDragSource != null) {
            // Mark this as actual dragging
            this.currentConnectionDragSourceDragged = true;

            JIPipeNodeUI nodeUI = pickNodeUI(mouseEvent);
            if (nodeUI != null && currentConnectionDragSource.getNodeUI() != nodeUI) {
                // Advanced dragging behavior
                boolean snapped = false;

                if (currentConnectionDragSource instanceof JIPipeNodeUISlotActiveArea) {
                    JIPipeNodeUISlotActiveArea currentConnectionDragSource_ = (JIPipeNodeUISlotActiveArea) currentConnectionDragSource;
                     /*
                    Auto snap to input/output if there is only one
                     */
                    if (currentConnectionDragSource_.getSlot().isInput()) {
                        if (nodeUI.getNode().getOutputSlots().size() == 1 && !nodeUI.isSlotsOutputsEditable()) {
                            if (!nodeUI.getOutputSlotMap().values().isEmpty()) {
                                // Auto snap to output
                                JIPipeNodeUISlotActiveArea slotUI = nodeUI.getOutputSlotMap().values().iterator().next();
                                setCurrentConnectionDragTarget(slotUI);
                                snapped = true;
                            }
                        }
                    } else {
                        if (nodeUI.getNode().getInputSlots().size() == 1 && !nodeUI.isSlotsInputsEditable()) {
                            // Auto snap to input
                            if (!nodeUI.getInputSlotMap().values().isEmpty()) {
                                JIPipeNodeUISlotActiveArea slotUI = nodeUI.getInputSlotMap().values().iterator().next();
                                setCurrentConnectionDragTarget(slotUI);
                                snapped = true;
                            }
                        }
                    }

                    /*
                    Sticky snap: Stay in last snapped position if we were in it before
                     */
                    if (currentConnectionDragTarget != null && currentConnectionDragTarget.getNodeUI() == nodeUI) {
                        JIPipeNodeUIActiveArea addSlotState = nodeUI.pickAddSlotAtMousePosition(mouseEvent);
                        if (addSlotState == null) {
                            JIPipeNodeUISlotActiveArea slotState = nodeUI.pickSlotAtMousePosition(mouseEvent);
                            if (slotState != null && slotState.getSlot().isInput() != currentConnectionDragSource_.getSlot().isInput()) {
                                setCurrentConnectionDragTarget(slotState);
                            }
                            snapped = true;
                        }
                    }

                    /*
                    Default: Snap exactly to input/output
                     */
                    if (!snapped) {
                        JIPipeNodeUISlotActiveArea slotState = nodeUI.pickSlotAtMousePosition(mouseEvent);
                        if (slotState != null && slotState.getSlot().isInput() != currentConnectionDragSource_.getSlot().isInput()) {
                            setCurrentConnectionDragTarget(slotState);
                            snapped = true;
                        } else {
                            setCurrentConnectionDragTarget(null);
                        }
                    }

                    /*
                    Snap to "create input"
                     */
                    if (!snapped) {
                        JIPipeNodeUIActiveArea slotState = nodeUI.pickAddSlotAtMousePosition(mouseEvent);
                        if (currentConnectionDragSource_.isOutput() && slotState != null && slotState == nodeUI.getAddInputSlotArea()) {
                            setCurrentConnectionDragTarget(slotState);
                            snapped = true;
                        } else if (currentConnectionDragSource_.isInput() && slotState != null && slotState == nodeUI.getAddOutputSlotArea()) {
                            setCurrentConnectionDragTarget(slotState);
                            snapped = true;
                        } else {
                            setCurrentConnectionDragTarget(null);
                        }
                    }
                } else {
                    // TODO?
                }

            } else {
                setCurrentConnectionDragTarget(null);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            repaint(50);
        } else if (!currentlyDraggedOffsets.isEmpty()) {
//            int negativeDx = 0;
//            int negativeDy = 0;
//
//            // Calculate dx, dy values for all nodes
//            for (Map.Entry<JIPipeNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
//                Point currentlyDraggedOffset = entry.getValue();
//
//                int xu = currentlyDraggedOffset.x + mouseEvent.getX();
//                int yu = currentlyDraggedOffset.y + mouseEvent.getY();
//                if (xu < 0 || yu < 0) {
//                    long currentTimeMillis = System.currentTimeMillis();
//                    if (currentTimeMillis - lastTimeExpandedNegative > 100) {
//                        negativeDx = xu < 0 ? 1 : 0;
//                        negativeDy = yu < 0 ? 1 : 0;
//                        lastTimeExpandedNegative = currentTimeMillis;
//                        break;
//                    }
//                    else {
//                        // Cancel the event
//                        return;
//                    }
//                }
//            }
//
//            // Negative expansion
//            for (JIPipeNodeUI value : nodeUIs.values()) {
//                if (!currentlyDraggedOffsets.containsKey(value)) {
//                    Point storedGridLocation = value.getStoredGridLocation();
//                    value.moveToGridLocation(new Point(storedGridLocation.x + negativeDx, storedGridLocation.y + negativeDy), true, true);
//                }
//            }

            // Calculate final movement for all nodes
            int gridDx = 0;
            int gridDy = 0;

            for (Map.Entry<JIPipeNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                Point currentlyDraggedOffset = entry.getValue();

                int x = Math.max(0, currentlyDraggedOffset.x + mouseEvent.getX());
                int y = Math.max(0, currentlyDraggedOffset.y + mouseEvent.getY());

                Point targetGridPoint = getViewMode().realLocationToGrid(new Point(x, y), getZoom());
                int dx = targetGridPoint.x - entry.getKey().getStoredGridLocation().x;
                int dy = targetGridPoint.y - entry.getKey().getStoredGridLocation().y;

                if (dx != 0 || dy != 0) {
                    gridDx = dx;
                    gridDy = dy;
                    break;
                }
            }

            int negativeDx = 0;
            int negativeDy = 0;
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastTimeExpandedNegative > 100) {
                for (Map.Entry<JIPipeNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                    JIPipeNodeUI currentlyDragged = entry.getKey();
                    Point newGridLocation = new Point(currentlyDragged.getStoredGridLocation().x + gridDx, currentlyDragged.getStoredGridLocation().y + gridDy);
                    if (newGridLocation.x <= 0) {
                        negativeDx = Math.min(negativeDx, newGridLocation.x - 1);
                        lastTimeExpandedNegative = currentTimeMillis;
                    }
                    if (newGridLocation.y <= 0) {
                        negativeDy = Math.min(negativeDy, newGridLocation.y - 1);
                        lastTimeExpandedNegative = currentTimeMillis;
                    }
                }
            }

            if (negativeDx < 0 || negativeDy < 0) {
                // Negative expansion
                for (JIPipeNodeUI value : nodeUIs.values()) {
                    if (!currentlyDraggedOffsets.containsKey(value)) {
                        Point storedGridLocation = value.getStoredGridLocation();
                        value.moveToGridLocation(new Point(storedGridLocation.x - negativeDx, storedGridLocation.y - negativeDy), true, true);
                    }
                }
            }

            for (Map.Entry<JIPipeNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                JIPipeNodeUI currentlyDragged = entry.getKey();
                Point newGridLocation = new Point(currentlyDragged.getStoredGridLocation().x + gridDx, currentlyDragged.getStoredGridLocation().y + gridDy);

                if (!hasDragSnapshot) {
                    // Check if something would change
                    if (!Objects.equals(currentlyDragged.getStoredGridLocation(), newGridLocation)) {
                        createMoveSnapshotIfNeeded();
                    }
                }

                currentlyDragged.moveToGridLocation(newGridLocation, true, true);
            }

            repaint();
            if (SystemUtils.IS_OS_LINUX) {
                Toolkit.getDefaultToolkit().sync();
            }
            if (getParent() != null)
                getParent().revalidate();
            graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
        } else {
            if (selectionFirst != null) {
                selectionSecond = mouseEvent.getPoint();
                repaint();
                if (SystemUtils.IS_OS_LINUX) {
                    Toolkit.getDefaultToolkit().sync();
                }
            }
        }
    }

    public void autoExpandLeftTop() {
        int minX = 0;
        int minY = 0;
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            minX = Math.min(ui.getX(), minX);
            minY = Math.min(ui.getY(), minY);
        }
        minX = -minX;
        minY = -minY;
        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        Point nextGridPoint = viewMode.realLocationToGrid(new Point(minX, minY), zoom);
        int ex = nextGridPoint.x;
        int ey = nextGridPoint.y;
        for (JIPipeNodeUI value : nodeUIs.values()) {
            if (!currentlyDraggedOffsets.containsKey(value)) {
                value.moveToClosestGridPoint(new Point(value.getX() + ex, value.getY() + ey), false, true);
            }
        }
        if (graphEditCursor != null) {
            graphEditCursor.x += ex;
            graphEditCursor.y += ey;
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
        for (JIPipeNodeUI value : nodeUIs.values()) {
            if (!currentlyDraggedOffsets.containsKey(value)) {
                Point gridLocation = viewMode.realLocationToGrid(value.getLocation(), zoom);
                gridLocation.x += gridLeft;
                gridLocation.y += gridTop;
                value.moveToGridLocation(gridLocation, true, true);
            }
        }
        if (graphEditCursor != null) {
            Point realLeftTop = viewMode.gridToRealLocation(new Point(gridLeft, gridTop), zoom);
            graphEditCursor.x = Math.round(graphEditCursor.x + realLeftTop.x);
            graphEditCursor.y = Math.round(graphEditCursor.y + realLeftTop.y);
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
        if (currentTool != null) {
            currentTool.mouseMoved(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        boolean changed = false;
        JIPipeNodeUI nodeUI = pickNodeUI(mouseEvent);
        if (nodeUI != null) {
            if (nodeUI != currentlyMouseEnteredNode) {
                if (currentlyMouseEnteredNode != null) {
                    currentlyMouseEnteredNode.mouseExited(mouseEvent);
                }
                currentlyMouseEnteredNode = nodeUI;
                currentlyMouseEnteredNode.mouseEntered(mouseEvent);
                changed = true;
            }
        } else if (currentlyMouseEnteredNode != null) {
            currentlyMouseEnteredNode.mouseExited(mouseEvent);
            currentlyMouseEnteredNode = null;
            changed = true;
        }
        if (currentlyMouseEnteredNode != null) {
            currentlyMouseEnteredNode.mouseMoved(mouseEvent);
        }
        if (changed && settings.isDrawLabelsOnHover()) {
            repaint(50);
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (currentTool != null) {
            currentTool.mouseClicked(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        JIPipeNodeUI ui = pickNodeUI(mouseEvent);

        if (ui != null) {
            ui.mouseClicked(mouseEvent);
            if (mouseEvent.isConsumed())
                return;
        }

        if (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getClickCount() == 2) {
            if (ui != null)
                defaultNodeUIActionRequestedEventEmitter.emit(new JIPipeNodeUI.DefaultNodeUIActionRequestedEvent(ui));
        } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            setGraphEditCursor(new Point(mouseEvent.getX(), mouseEvent.getY()));
            requestFocusInWindow();
            repaint();
        } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            if (selection.size() <= 1) {
                selectOnly(ui);
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
        for (NodeUIContextAction action : contextActions) {
            if (action == null) {
                scheduleSeparator = true;
                continue;
            }
            if (action.isHidden())
                continue;
            boolean matches = action.matches(selection);
            if (!matches && !action.disableOnNonMatch())
                continue;
            if (scheduleSeparator) {
                scheduleSeparator = false;
                menu.addSeparator();
            }
            JMenuItem item = new JMenuItem(action.getName(), action.getIcon());
            item.setToolTipText(action.getDescription());
            if (matches) {
                item.addActionListener(e -> action.run(this, ImmutableSet.copyOf(selection)));
                if (action.getKeyboardShortcut() != null) {
                    item.setAccelerator(action.getKeyboardShortcut());
                }
            } else
                item.setEnabled(false);
            menu.add(item);
        }
        menu.show(this, point.x, point.y);
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (currentTool != null) {
            currentTool.mousePressed(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            if (currentlyDraggedOffsets.isEmpty()) {
                JIPipeNodeUI ui = pickNodeUI(mouseEvent);
                if (ui != null) {
                    if (mouseEvent.isShiftDown()) {
                        if (getSelection().contains(ui))
                            removeFromSelection(ui);
                        else
                            addToSelection(ui);
                    } else {
                        if (selection.isEmpty() || selection.size() == 1) {
                            selectOnly(ui);
                        } else {
                            if (!selection.contains(ui)) {
                                selectOnly(ui);
                            }
                        }
                    }
                    this.hasDragSnapshot = false;
                    if (currentToolAllowsConnectionDragging()) {
                        // Attempt to drag a slot
                        JIPipeNodeUISlotActiveArea slotState = ui.pickSlotAtMousePosition(mouseEvent);
                        if (slotState != null) {
                            startDragSlot(slotState);
                        } else {
                            startDragCurrentNodeSelection(mouseEvent);
                        }
                    } else {
                        // Dragging slots disabled by tools
                        startDragCurrentNodeSelection(mouseEvent);
                    }
                } else {
                    selectionFirst = mouseEvent.getPoint();
                }
            }
        }
    }

    private void startDragSlot(JIPipeNodeUISlotActiveArea startSlot) {
        if (currentToolAllowsConnectionDragging()) {
            this.currentConnectionDragSourceDragged = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setCurrentConnectionDragSource(startSlot);
        } else {
            stopAllDragging();
        }
    }

    private void startDragCurrentNodeSelection(MouseEvent mouseEvent) {
        if (currentToolAllowsNodeDragging()) {
            this.hasDragSnapshot = false;
            this.currentConnectionDragSourceDragged = false;
            for (JIPipeNodeUI nodeUI : selection) {
                Point offset = new Point();
                offset.x = nodeUI.getX() - mouseEvent.getX();
                offset.y = nodeUI.getY() - mouseEvent.getY();
                currentlyDraggedOffsets.put(nodeUI, offset);
            }
        } else {
            stopAllDragging();
        }
    }

    public JIPipeNodeUI pickNodeUI(MouseEvent mouseEvent) {
        for (int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            if (component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                if (component instanceof JIPipeNodeUI) {
                    return (JIPipeNodeUI) component;
                }
            }
        }
        return null;
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (currentTool != null) {
            currentTool.mouseReleased(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        if (mouseEvent.getButton() != MouseEvent.BUTTON1) {
            stopAllDragging();
        } else {
            if (currentConnectionDragSource instanceof JIPipeNodeUISlotActiveArea && currentConnectionDragTarget instanceof JIPipeNodeUISlotActiveArea) {
                connectOrDisconnectSlots(((JIPipeNodeUISlotActiveArea) currentConnectionDragSource).getSlot(), ((JIPipeNodeUISlotActiveArea) currentConnectionDragTarget).getSlot());
            } else if (currentConnectionDragSource instanceof JIPipeNodeUISlotActiveArea && currentConnectionDragTarget instanceof JIPipeNodeUIAddSlotButtonActiveArea) {
                connectCreateNewSlot(((JIPipeNodeUISlotActiveArea) currentConnectionDragSource).getSlot(), currentConnectionDragTarget.getNodeUI());
            }
            stopAllDragging();
            if (selectionFirst != null && selectionSecond != null) {
                int x0 = selectionFirst.x;
                int y0 = selectionFirst.y;
                int x1 = selectionSecond.x;
                int y1 = selectionSecond.y;
                int x = Math.min(x0, x1);
                int y = Math.min(y0, y1);
                int w = Math.abs(x0 - x1);
                int h = Math.abs(y0 - y1);
                Rectangle selectionRectangle = new Rectangle(x, y, w, h);
                Set<JIPipeNodeUI> newSelection = new HashSet<>();
                for (JIPipeNodeUI ui : nodeUIs.values()) {
                    if (selectionRectangle.intersects(ui.getBounds())) {
                        newSelection.add(ui);
                    }
                }
                if (!newSelection.isEmpty()) {
                    if (!mouseEvent.isShiftDown()) {
                        this.selection.clear();
                    }
                    this.selection.addAll(newSelection);
                    updateSelection();
                }

                selectionFirst = null;
                selectionSecond = null;
                repaint();
            } else {
                JIPipeNodeUI ui = pickNodeUI(mouseEvent);
                if (ui == null) {
                    selectOnly(null);
                }
                selectionFirst = null;
                selectionSecond = null;
            }
        }
    }

    private void connectCreateNewSlot(JIPipeDataSlot sourceSlot, JIPipeNodeUI nodeUI) {
        JIPipeGraphNode node = nodeUI.getNode();
        JIPipeSlotType addedSlotType = sourceSlot.getSlotType() == JIPipeSlotType.Input ? JIPipeSlotType.Output : JIPipeSlotType.Input;
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) node.getSlotConfiguration();
        Set<JIPipeDataInfo> availableTypes;
        if (addedSlotType == JIPipeSlotType.Input) {
            availableTypes = slotConfiguration.getAllowedInputSlotTypes()
                    .stream().map(JIPipeDataInfo::getInstance).collect(Collectors.toSet());
        } else if (addedSlotType == JIPipeSlotType.Output) {
            availableTypes = slotConfiguration.getAllowedOutputSlotTypes()
                    .stream().map(JIPipeDataInfo::getInstance).collect(Collectors.toSet());
        } else {
            throw new UnsupportedOperationException();
        }

        Class<? extends JIPipeData> sourceSlotType = sourceSlot.getAcceptedDataType();
        if (addedSlotType == JIPipeSlotType.Input) {
            availableTypes.removeIf(info -> !JIPipe.getDataTypes().isConvertible(sourceSlotType, info.getDataClass()));
        } else {
            availableTypes.removeIf(info -> !JIPipe.getDataTypes().isConvertible(info.getDataClass(), sourceSlotType));
        }

        if (availableTypes.isEmpty()) {
            JOptionPane.showMessageDialog(getWorkbench().getWindow(),
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
        AddAlgorithmSlotPanel panel = new AddAlgorithmSlotPanel(nodeUI.getNode(), addedSlotType, historyJournal);
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

    private void connectOrDisconnectSlots(JIPipeDataSlot firstSlot, JIPipeDataSlot secondSlot) {
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
                getHistoryJournal().snapshotBeforeDisconnect(source, target, compartment);
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

    private void stopAllDragging() {
        // Slot dragging
        this.currentConnectionDragSourceDragged = false;
        setCurrentConnectionDragSource(null);
        setCurrentConnectionDragTarget(null);
        resetCursor();
        repaint(50);

        // Node dragging
        currentlyDraggedOffsets.clear();
        hasDragSnapshot = false;
    }

    private void resetCursor() {
        setCursor(currentTool != null ? currentTool.getCursor() : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public boolean isCurrentlyDraggingNode() {
        return hasDragSnapshot;
    }

    public boolean isCurrentlyDraggingConnection() {
        return currentConnectionDragSource != null && currentConnectionDragTarget != null;
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (currentTool != null) {
            currentTool.mouseEntered(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

        // Update last mouse position
        lastMousePosition = new Point(mouseEvent.getX(), mouseEvent.getY());

        // Let the tool handle the event
        if (currentTool != null) {
            currentTool.mouseExited(mouseEvent);
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
            width = Math.max(width, component.getX() + component.getWidth() + 2 * viewMode.getGridWidth());
            height = Math.max(height, component.getY() + component.getHeight() + 2 * viewMode.getGridHeight());
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

        BasicStroke defaultStroke = new BasicStroke(1);
        BasicStroke selectedStroke = new BasicStroke(3);

        for (JIPipeNodeUI nodeUI : nodeUIs.values()) {
            int x = (int) (nodeUI.getX() * scale) + viewX;
            int y = (int) (nodeUI.getY() * scale) + viewY;
            int width = (int) (nodeUI.getWidth() * scale);
            int height = (int) (nodeUI.getHeight() * scale);
            graphics2D.setStroke(selection.contains(nodeUI) ? selectedStroke : defaultStroke);
            graphics2D.setColor(nodeUI.getFillColor());
            graphics2D.fillRect(x, y, width, height);
            if (nodeUI.getNode().isBookmarked()) {
                graphics2D.setColor(new Color(0x33cc33));
            } else {
                graphics2D.setColor(nodeUI.getBorderColor());
            }
            graphics2D.drawRect(x, y, width, height);


            ImageIcon icon = JIPipe.getInstance().getNodeRegistry().getIconFor(nodeUI.getNode().getInfo());
            int iconSize = Math.min(16, Math.min(width, height)) - 3;
            if (iconSize > 4) {
                graphics2D.drawImage(icon.getImage(),
                        x + (int) Math.round((width / 2.0) - (iconSize / 2.0)),
                        y + (int) Math.round((height / 2.0) - (iconSize / 2.0)),
                        iconSize,
                        iconSize,
                        null);
            }
        }
    }

    private void paintMinimapEdges(Graphics2D graphics2D, double scale, int viewX, int viewY) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setColor(Color.LIGHT_GRAY);
        paintEdges(graphics2D,
                STROKE_UNIT,
                null,
                STROKE_UNIT_COMMENT,
                false,
                false,
                scale,
                viewX,
                viewY,
                false,
                false, false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        if (settings.isDrawNodeShadows()) {
            for (JIPipeNodeUI ui : nodeUIs.values()) {
                DROP_SHADOW_BORDER.paint(g, ui.getX() - 3, ui.getY() - 3, ui.getWidth() + 8, ui.getHeight() + 8);
                if (ui.getNode().isBookmarked()) {
                    BOOKMARK_SHADOW_BORDER.paint(g, ui.getX() - 12, ui.getY() - 12, ui.getWidth() + 24, ui.getHeight() + 24);
                }
            }
        }

        // Set render settings
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Below node paint
        if (currentTool != null) {
            currentTool.paintBelowNodesAndEdges(g);
        }

        if (renderOutsideEdges && getCompartment() != null && settings.isDrawOutsideEdges())
            paintOutsideEdges(g, false, Color.DARK_GRAY, STROKE_DEFAULT, STROKE_DEFAULT_BORDER);

        // Main edge drawing
        lastDisplayedMainEdges = paintEdges(g,
                STROKE_DEFAULT,
                STROKE_DEFAULT_BORDER,
                STROKE_COMMENT,
                false,
                false,
                1,
                0,
                0,
                true,
                isAutoHideEdges(), false);

        // Outside edges drawing
        if (renderOutsideEdges && getCompartment() != null && settings.isDrawOutsideEdges()) {
            paintOutsideEdges(g, true, Color.DARK_GRAY, STROKE_HIGHLIGHT, null);
        }

        // Selected edges drawing
        if (!selection.isEmpty()) {
            paintEdges(g,
                    STROKE_HIGHLIGHT,
                    null,
                    STROKE_COMMENT_HIGHLIGHT,
                    true,
                    settings.isColorSelectedNodeEdges(),
                    1,
                    0,
                    0,
                    true,
                    false,
                    true);
        }

        // Draw highlights
        paintCurrentlyDraggedConnection(g);
        paintDisconnectHighlight(g);
        paintConnectHighlight(g);

        if (currentTool != null) {
            currentTool.paintBelowNodesAfterEdges(g);
        }

        g.setStroke(STROKE_UNIT);
    }

    private void paintDisconnectHighlight(Graphics2D g) {
        if (disconnectHighlight != null) {
            g.setStroke(STROKE_HIGHLIGHT);
            g.setColor(Color.RED);
            if (disconnectHighlight.getTarget().getSlot().isInput()) {
                Set<JIPipeDataSlot> sources = disconnectHighlight.getSources();
                for (JIPipeDataSlot source : sources) {
                    JIPipeDataSlot target = disconnectHighlight.getTarget().getSlot();
                    JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

                    if (sourceUI != null && targetUI != null) {
                        PointRange sourcePoint;
                        PointRange targetPoint;

                        sourcePoint = sourceUI.getSlotLocation(source);
                        sourcePoint.add(sourceUI.getLocation());
                        targetPoint = targetUI.getSlotLocation(target);
                        targetPoint.add(targetUI.getLocation());

                        // Tighten the point ranges: Bringing the centers together
                        PointRange.tighten(sourcePoint, targetPoint);

                        // Draw arrow
                        paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
                    }
                }
            } else if (disconnectHighlight.getTarget().getSlot().isOutput()) {
                JIPipeDataSlot source = disconnectHighlight.getTarget().getSlot();
                for (JIPipeDataSlot target : getGraph().getOutputOutgoingTargetSlots(source)) {
                    JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

                    if (sourceUI != null && targetUI != null) {
                        PointRange sourcePoint;
                        PointRange targetPoint;

                        sourcePoint = sourceUI.getSlotLocation(source);
                        sourcePoint.add(sourceUI.getLocation());
                        targetPoint = targetUI.getSlotLocation(target);
                        targetPoint.add(targetUI.getLocation());

                        // Tighten the point ranges: Bringing the centers together
                        PointRange.tighten(sourcePoint, targetPoint);

                        // Draw arrow
                        paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
                    }
                }
            }
        }
    }

    private void paintConnectHighlight(Graphics2D g) {
        if (connectHighlight != null) {
            g.setStroke(STROKE_HIGHLIGHT);
            g.setColor(COLOR_HIGHLIGHT_GREEN);
            if (connectHighlight.getTarget().getSlot().isInput()) {
                JIPipeDataSlot source = connectHighlight.getSource().getSlot();
                JIPipeDataSlot target = connectHighlight.getTarget().getSlot();
                JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

                if (sourceUI != null && targetUI != null) {
                    PointRange sourcePoint;
                    PointRange targetPoint;

                    sourcePoint = sourceUI.getSlotLocation(source);
                    sourcePoint.add(sourceUI.getLocation());
                    targetPoint = targetUI.getSlotLocation(target);
                    targetPoint.add(targetUI.getLocation());

                    // Tighten the point ranges: Bringing the centers together
                    PointRange.tighten(sourcePoint, targetPoint);

                    // Draw arrow
                    paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
                }
            } else if (disconnectHighlight.getTarget().getSlot().isOutput()) {
                JIPipeDataSlot target = connectHighlight.getSource().getSlot();
                JIPipeDataSlot source = connectHighlight.getTarget().getSlot();
                JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);
                if (sourceUI != null && targetUI != null) {
                    PointRange sourcePoint;
                    PointRange targetPoint;

                    sourcePoint = sourceUI.getSlotLocation(source);
                    sourcePoint.add(sourceUI.getLocation());
                    targetPoint = targetUI.getSlotLocation(target);
                    targetPoint.add(targetUI.getLocation());

                    // Tighten the point ranges: Bringing the centers together
                    PointRange.tighten(sourcePoint, targetPoint);

                    // Draw arrow
                    paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
                }
            }
        }
    }

    private void paintCurrentlyDraggedConnection(Graphics2D g) {
        if (currentConnectionDragSourceDragged && currentConnectionDragSource != null) {
            g.setStroke(STROKE_HIGHLIGHT);
            PointRange sourcePoint;
            PointRange targetPoint = null;

            if (currentConnectionDragSource instanceof JIPipeNodeUISlotActiveArea) {
                sourcePoint = currentConnectionDragSource.getNodeUI().getSlotLocation(((JIPipeNodeUISlotActiveArea) currentConnectionDragSource).getSlot());
            } else {
                sourcePoint = currentConnectionDragSource.getZoomedHitAreaCenter();
            }

            sourcePoint.add(currentConnectionDragSource.getNodeUI().getLocation());

            if (currentConnectionDragTarget != null &&
                    currentConnectionDragTarget != currentConnectionDragSource &&
                    currentConnectionDragTarget.getNodeUI().getNode() != currentConnectionDragSource.getNodeUI().getNode()) {
                JIPipeNodeUI nodeUI = currentConnectionDragTarget.getNodeUI();
                if (currentConnectionDragTarget instanceof JIPipeNodeUISlotActiveArea) {
                    targetPoint = nodeUI.getSlotLocation(((JIPipeNodeUISlotActiveArea) currentConnectionDragTarget).getSlot());
                } else {
                    targetPoint = currentConnectionDragTarget.getZoomedHitAreaCenter();
                }
                targetPoint.add(nodeUI.getLocation());
            }
            if (targetPoint != null) {
                if (currentConnectionDragSource instanceof JIPipeNodeUISlotActiveArea && currentConnectionDragTarget instanceof JIPipeNodeUISlotActiveArea) {
                    JIPipeNodeUISlotActiveArea currentConnectionDragSource_ = (JIPipeNodeUISlotActiveArea) currentConnectionDragSource;
                    JIPipeNodeUISlotActiveArea currentConnectionDragTarget_ = (JIPipeNodeUISlotActiveArea) currentConnectionDragTarget;
                    if (currentConnectionDragTarget == null || (!graph.getGraph().containsEdge(currentConnectionDragSource_.getSlot(), currentConnectionDragTarget_.getSlot())
                            && !graph.getGraph().containsEdge(currentConnectionDragTarget_.getSlot(), currentConnectionDragSource_.getSlot()))) {
                        g.setColor(COLOR_HIGHLIGHT_GREEN);
                    } else {
                        g.setColor(Color.RED);
                    }
                } else {
                    g.setColor(COLOR_HIGHLIGHT_GREEN);
                }
            } else {
                g.setColor(Color.DARK_GRAY);
            }
            if (targetPoint == null) {
                Point mousePosition = getLastMousePosition();
                if (mousePosition != null) {
                    targetPoint = new PointRange(mousePosition.x, mousePosition.y);
                }
            }

            if (targetPoint != null) {
                // Tighten the point ranges: Bringing the centers together
                PointRange.tighten(sourcePoint, targetPoint);

                // Draw arrow
                if (currentConnectionDragSource instanceof JIPipeNodeUISlotActiveArea) {
                    if (((JIPipeNodeUISlotActiveArea) currentConnectionDragSource).getSlot().isOutput())
                        paintEdge(g, sourcePoint.center, currentConnectionDragSource.getNodeUI().getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
                    else
                        paintEdge(g, targetPoint.center, currentConnectionDragSource.getNodeUI().getBounds(), sourcePoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
                } else if (currentConnectionDragTarget instanceof JIPipeNodeUISlotActiveArea) {
                    if (((JIPipeNodeUISlotActiveArea) currentConnectionDragTarget).getSlot().isInput())
                        paintEdge(g, sourcePoint.center, currentConnectionDragSource.getNodeUI().getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
                    else
                        paintEdge(g, targetPoint.center, currentConnectionDragSource.getNodeUI().getBounds(), sourcePoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, true);
                } else {
                    paintEdge(g, targetPoint.center, currentConnectionDragSource.getNodeUI().getBounds(), sourcePoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0, false);
                }
            }
        }
    }

    public Map<?, ?> getDesktopRenderingHints() {
        return desktopRenderingHints;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHints(desktopRenderingHints);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Draw node selections
        graphics2D.setStroke(STROKE_SELECTION);
        for (JIPipeNodeUI ui : selection) {
            Rectangle bounds = ui.getBounds();
            bounds.x -= 4;
            bounds.y -= 4;
            bounds.width += 8;
            bounds.height += 8;
            g.setColor(ui.getBorderColor());
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        // Draw marquee rectangle
        if (selectionFirst != null && selectionSecond != null) {
            graphics2D.setStroke(STROKE_MARQUEE);
            graphics2D.setColor(Color.GRAY);
            int x0 = selectionFirst.x;
            int y0 = selectionFirst.y;
            int x1 = selectionSecond.x;
            int y1 = selectionSecond.y;
            int x = Math.min(x0, x1);
            int y = Math.min(y0, y1);
            int w = Math.abs(x0 - x1);
            int h = Math.abs(y0 - y1);
            graphics2D.drawRect(x, y, w, h);
        }

        // Above node paint
        if (currentTool != null) {
            currentTool.paintAfterNodesAndEdges(graphics2D);
        }

        // Smart edges drawing
        if (lastDisplayedMainEdges != null) {
            paintNodeInputLabels(graphics2D, lastDisplayedMainEdges);
        }

        // Draw cursor over the components
        if (graphEditCursor != null && renderCursor) {
            g.drawImage(cursorImage.getImage(),
                    graphEditCursor.x - cursorImage.getIconWidth() / 2,
                    graphEditCursor.y - cursorImage.getIconHeight() / 2,
                    null);
        }
    }

    private List<DisplayedSlotEdge> paintEdges(Graphics2D g, Stroke stroke, Stroke strokeBorder, Stroke strokeComment, boolean onlySelected, boolean multicolor, double scale, int viewX, int viewY, boolean enableArrows, boolean enableAutoHide, boolean forceVisible) {
        Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> slotEdges = graph.getSlotEdges();
        List<DisplayedSlotEdge> displayedSlotEdges = new ArrayList<>();

        int multiColorMax = findMultiColorMax(slotEdges, multicolor, onlySelected);
        int multiColorIndex = 0;

        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> kv : slotEdges) {
            JIPipeDataSlot source = kv.getKey();
            JIPipeDataSlot target = kv.getValue();
            JIPipeGraphEdge edge = graph.getGraph().getEdge(kv.getKey(), kv.getValue());

            if (currentTool != null) {
                if (!currentTool.canRenderEdge(source, target, edge)) {
                    continue;
                }
            }

            // Check for only showing selected nodes
            JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
            JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

            if (sourceUI == null || targetUI == null) {
                continue;
            }
            if (onlySelected && !selection.contains(sourceUI) && !selection.contains(targetUI)) {
                continue;
            }

            // Get the locations
            PointRange sourcePoint = sourceUI.getSlotLocation(source);
            PointRange targetPoint = targetUI.getSlotLocation(target);
            sourcePoint.add(sourceUI.getLocation());
            targetPoint.add(targetUI.getLocation());

            // Save into the slot edges list
            DisplayedSlotEdge displayedSlotEdge = new DisplayedSlotEdge(source, target, edge);
            displayedSlotEdge.setMultiColorMax(multiColorMax);
            displayedSlotEdge.setMultiColorIndex(multiColorIndex);
            displayedSlotEdge.setSourcePoint(sourcePoint);
            displayedSlotEdge.setTargetPoint(targetPoint);
            displayedSlotEdge.setSourceCenter(new Point(sourcePoint.center));
            displayedSlotEdge.setTargetCenter(new Point(targetPoint.center));
            displayedSlotEdge.setSourceUI(sourceUI);
            displayedSlotEdge.setTargetUI(targetUI);
            displayedSlotEdges.add(displayedSlotEdge);

            ++multiColorIndex;
        }

        if (enableAutoHide) {
            displayedSlotEdges.sort(Comparator.naturalOrder());
        }

        Set<Rectangle> existingDrawnSlots = new HashSet<>();

        for (DisplayedSlotEdge displayedSlotEdge : displayedSlotEdges) {

            Rectangle rectangle = null;
            if (enableAutoHide) {
                rectangle = new Rectangle(displayedSlotEdge.sourcePoint.center);
                rectangle.add(displayedSlotEdge.targetPoint.center);
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

                switch (displayedSlotEdge.edge.getUiVisibility()) {
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

                if (forceVisible) {
                    hidden = false;
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

        return displayedSlotEdges;
    }

    private int findMultiColorMax(Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> slotEdges, boolean multicolor, boolean onlySelected) {
        int multiColorMax = 1;
        if (multicolor) {
            if (onlySelected) {
                multiColorMax = 0;
                for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> kv : slotEdges) {
                    JIPipeDataSlot source = kv.getKey();
                    JIPipeDataSlot target = kv.getValue();
                    JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

                    if (sourceUI == null || targetUI == null)
                        continue;
                    if (selection.contains(sourceUI) || selection.contains(targetUI)) {
                        ++multiColorMax;
                    }
                }
            } else {
                multiColorMax = slotEdges.size();
            }
        }
        return multiColorMax;
    }

    public JIPipeToggleableGraphEditorTool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(JIPipeToggleableGraphEditorTool currentTool) {
        this.currentTool = currentTool;
        resetCursor();
        repaint(50);
    }

    public boolean currentToolAllowsNodeDragging() {
        return currentTool == null || currentTool.allowsDragNodes();
    }

    public boolean currentToolAllowsConnectionDragging() {
        return currentTool == null || currentTool.allowsDragConnections();
    }

    private void paintNodeInputLabels(Graphics2D g, List<DisplayedSlotEdge> displayedMainEdges) {

        if (!settings.isDrawLabelsOnHover() && !isAutoHideDrawLabels())
            return;

        FontMetrics slotFontMetrics = g.getFontMetrics(smartEdgeTooltipSlotFont);
        FontMetrics nodeFontMetrics = g.getFontMetrics(smartEdgeTooltipNodeFont);

        // Collect labelled inputs based on pre-calculated
        // Target to sources
        Multimap<JIPipeDataSlot, DisplayedSlotEdge> labelledEdges = HashMultimap.create();

        for (DisplayedSlotEdge displayedSlotEdge : displayedMainEdges) {
            JIPipeGraphEdge.Visibility uiVisibility = displayedSlotEdge.edge.getUiVisibility();
            if (displayedSlotEdge.isHidden() && (uiVisibility == JIPipeGraphEdge.Visibility.Smart || uiVisibility == JIPipeGraphEdge.Visibility.AlwaysHiddenWithLabel) && isAutoHideDrawLabels()) {
                labelledEdges.put(displayedSlotEdge.target, displayedSlotEdge);
            } else if (settings.isDrawLabelsOnHover() && displayedSlotEdge.getTargetUI() == currentlyMouseEnteredNode && !isCurrentlyDraggingNode() && !isCurrentlyDraggingConnection()) {
                labelledEdges.put(displayedSlotEdge.target, displayedSlotEdge);
            }
        }

        for (JIPipeNodeUI nodeUI : nodeUIs.values()) {
            for (JIPipeDataSlot inputSlot : nodeUI.getNode().getInputSlots()) {
                List<DisplayedSlotEdge> inputIncomingSourceSlots = labelledEdges.get(inputSlot).stream().sorted(Comparator.comparing(DisplayedSlotEdge::getUIManhattanDistance)).collect(Collectors.toList());

                // Render the smart edge
                if (!inputIncomingSourceSlots.isEmpty()) {
                    JIPipeNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(inputSlot);
                    if (slotActiveArea != null && slotActiveArea.getZoomedHitArea() != null) {

                        final int baseHeight = 28;
                        int tooltipHeight = (int) Math.round(zoom * inputIncomingSourceSlots.size() * baseHeight);
                        int maxAvailableWidth = slotActiveArea.getZoomedHitArea().width;
                        int tooltipWidth = 0;
                        for (DisplayedSlotEdge slotEdge : inputIncomingSourceSlots) {
                            JIPipeDataSlot sourceSlot = slotEdge.source;
                            tooltipWidth = Math.max(tooltipWidth, Math.max(slotFontMetrics.stringWidth(sourceSlot.getName()),
                                    nodeFontMetrics.stringWidth(sourceSlot.getNode().getName())) + (int) Math.round(zoom * 16 + zoom * 8));
                        }
                        tooltipWidth = Math.min(maxAvailableWidth, tooltipWidth);

                        // Generate tooltip
                        int spacing = (int) Math.round(zoom * viewMode.getGridHeight() / 3.0);
                        int tooltipX = nodeUI.getX() + slotActiveArea.getZoomedHitArea().x;
                        int tooltipY = nodeUI.getY() - tooltipHeight - spacing - (int) Math.round(zoom * 4);

                        Polygon tooltipPolygon = new Polygon(new int[]{tooltipX, tooltipX, tooltipX + tooltipWidth, tooltipX + tooltipWidth, tooltipX + tooltipWidth / 2 + spacing, tooltipX + tooltipWidth / 2, tooltipX + tooltipWidth / 2 - spacing},
                                new int[]{tooltipY + tooltipHeight, tooltipY, tooltipY, tooltipY + tooltipHeight, tooltipY + tooltipHeight, tooltipY + tooltipHeight + spacing, tooltipY + tooltipHeight},
                                7);
                        g.setStroke(STROKE_UNIT);
                        g.setPaint(smartEdgeSlotBackground);
                        g.fill(tooltipPolygon);
                        g.setPaint(nodeUI.getBorderColor());
                        g.draw(tooltipPolygon);

                        int startY = tooltipY;
                        int slotInfoHeight = (int) Math.round(zoom * baseHeight);

                        g.setPaint(smartEdgeSlotForeground);

                        for (DisplayedSlotEdge slotEdge : inputIncomingSourceSlots) {

                            JIPipeDataSlot sourceSlot = slotEdge.source;

                            // Draw icon
                            Image icon = JIPipe.getNodes().getIconFor(sourceSlot.getNode().getInfo()).getImage();
                            int iconX = tooltipX + (int) Math.round(zoom * 3);
                            int iconSize = (int) Math.round(12 * zoom);
                            g.drawImage(icon, iconX, startY + slotInfoHeight / 2 - iconSize / 2, iconSize, iconSize, null);

                            // Draw text
                            double centerY = startY + slotInfoHeight / 2.0;
                            int textX = tooltipX + (int) Math.round(zoom * 2) + (int) Math.round(zoom * 16);
                            int slotTextY = (int) Math.round(centerY);
                            int nodeTextY = (int) Math.round(centerY + (slotFontMetrics.getAscent() - slotFontMetrics.getLeading()));
                            int availableTextWidth = tooltipWidth - (textX - tooltipX) - (int) Math.round(zoom * 4);

                            g.setFont(smartEdgeTooltipSlotFont);
                            g.drawString(StringUtils.limitWithEllipsis(sourceSlot.getName(), availableTextWidth, slotFontMetrics), textX, slotTextY);

                            g.setFont(smartEdgeTooltipNodeFont);
                            g.drawString(StringUtils.limitWithEllipsis(sourceSlot.getNode().getName(), availableTextWidth, nodeFontMetrics), textX, nodeTextY);

                            startY += slotInfoHeight;
                        }
                    }
                }
            }
        }
    }

    private double calculateApproximateSlotManhattanDistance(JIPipeDataSlot source, JIPipeDataSlot target) {
        JIPipeNodeUI sourceNode = nodeUIs.get(source.getNode());
        JIPipeNodeUI targetNode = nodeUIs.get(target.getNode());
        if (sourceNode != null && targetNode != null) {
            PointRange sourceLocation = sourceNode.getSlotLocation(source);
            PointRange targetLocation = targetNode.getSlotLocation(target);
            if (sourceLocation != null && targetLocation != null) {
                int x1 = sourceNode.getX() + sourceLocation.center.x;
                int x2 = targetNode.getX() + targetLocation.center.x;
                int y1 = sourceNode.getY() + sourceLocation.center.y;
                int y2 = targetNode.getY() + targetLocation.center.y;
                return Math.abs(x1 - x2) + Math.abs(y1 - y2);
            }
        }
        return -1;
    }

    private Color getEdgeColor(JIPipeDataSlot source, JIPipeDataSlot target, boolean multicolor, int multiColorIndex, int multiColorMax) {
        Color result;
        if (source.getNode() instanceof JIPipeCommentNode || target.getNode() instanceof JIPipeCommentNode) {
            result = COMMENT_EDGE_COLOR;
        } else if (multicolor) {
            result = Color.getHSBColor(1.0f * multiColorIndex / multiColorMax, 0.45f, 0.65f);
        } else {
            if (JIPipeDatatypeRegistry.isTriviallyConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                result = Color.DARK_GRAY;
            else if (JIPipe.getDataTypes().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
                result = Color.BLUE;
            else
                result = Color.RED;
        }
        return result;
    }

    private void paintSlotEdge(Graphics2D g,
                               Stroke stroke,
                               Stroke strokeBorder,
                               DisplayedSlotEdge displayedSlotEdge,
                               boolean multicolor,
                               double scale,
                               int viewX,
                               int viewY,
                               boolean enableArrows) {

        PointRange sourcePoint = displayedSlotEdge.getSourcePoint();
        PointRange targetPoint = displayedSlotEdge.getTargetPoint();
        JIPipeNodeUI sourceUI = displayedSlotEdge.getSourceUI();
        JIPipeDataSlot source = displayedSlotEdge.getSource();
        JIPipeDataSlot target = displayedSlotEdge.getTarget();
        JIPipeGraphEdge.Shape uiShape = displayedSlotEdge.getEdge().getUiShape();
        int multiColorMax = displayedSlotEdge.getMultiColorMax();
        int multiColorIndex = displayedSlotEdge.getMultiColorIndex();

        if (displayedSlotEdge.isHidden()) {
            // Tighten the point ranges: Bringing the centers together
            PointRange.tighten(sourcePoint, targetPoint);

            g.setStroke(STROKE_SMART_EDGE);
            g.setColor(Color.LIGHT_GRAY);
            paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, false);
            return;
        }

        // Tighten the point ranges: Bringing the centers together
        PointRange.tighten(sourcePoint, targetPoint);

        // Draw arrow
        if (settings.isDrawImprovedEdges() && strokeBorder != null) {
            g.setStroke(strokeBorder);
            g.setColor(getEdgeColor(source, target, multicolor, multiColorIndex, multiColorMax));
            paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, enableArrows);
            g.setStroke(stroke);
            g.setColor(improvedStrokeBackgroundColor);
            paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, enableArrows);
        } else {
            g.setStroke(stroke);
            g.setColor(getEdgeColor(source, target, multicolor, multiColorIndex, multiColorMax));
            paintEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, enableArrows);
        }
    }

    private void paintOutsideEdges(Graphics2D g, boolean onlySelected, Color baseColor, Stroke stroke, Stroke borderStroke) {
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            Set<UUID> visibleCompartments = graph.getVisibleCompartmentUUIDsOf(ui.getNode());
            if (!visibleCompartments.isEmpty()) {
                if (onlySelected) {
                    if (!selection.contains(ui))
                        continue;
                } else {
                    if (selection.contains(ui))
                        continue;
                }
                Point sourcePoint = new Point();
                Point targetPoint = new Point();
                boolean uiIsOutput = getCompartment() == null || getCompartment().equals(ui.getNode().getCompartmentUUIDInParentGraph());
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
                    g.setColor(improvedStrokeBackgroundColor);
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
        JIPipeNodeUI algorithmUI = nodeUIs.getOrDefault(slot.getNode(), null);
        if (algorithmUI != null) {
            PointRange location = algorithmUI.getSlotLocation(slot);
            if (location != null) {
                return new Point(algorithmUI.getX() + location.center.x, algorithmUI.getY() + location.center.y);
            }
        }
        return null;
    }

    private void paintOutsideEdge(Graphics2D g, Point sourcePoint, Point targetPoint, boolean drawArrowHead) {
        int arrowHeadShift = getArrowHeadShift();
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
                int arrowHeadShift = enableArrows ? getArrowHeadShift() : 0;
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

    private int getArrowHeadShift() {
        if (settings.isDrawArrowHeads()) {
            int sz = 1;
            return -2 * sz - 6;
        } else {
            return 0;
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

        buffer = viewMode.getGridHeight() / 2;
        sourceA = sourcePoint.y;
        targetA = targetPoint.y;
        if (enableArrows) {
            targetA += getArrowHeadShift();
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
    public UUID getCompartment() {
        return compartment;
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

    public JIPipeGraphViewMode getViewMode() {
        return viewMode;
    }

    public BiMap<JIPipeGraphNode, JIPipeNodeUI> getNodeUIs() {
        return ImmutableBiMap.copyOf(nodeUIs);
    }

    public JIPipeGraphDragAndDropBehavior getDragAndDropBehavior() {
        return dragAndDropBehavior;
    }

    public void setDragAndDropBehavior(JIPipeGraphDragAndDropBehavior dragAndDropBehavior) {
        this.dragAndDropBehavior = dragAndDropBehavior;
        dragAndDropBehavior.setCanvas(this);
        new DropTarget(this, dragAndDropBehavior);
    }

    /**
     * @return the set of selected {@link JIPipeNodeUI}
     */
    public Set<JIPipeNodeUI> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * @return the set of selected nodes
     */
    public Set<JIPipeGraphNode> getSelectedNodes() {
        return selection.stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet());
    }

    /**
     * Clears the list of selected algorithms
     */
    public void clearSelection() {
        selection.clear();
        updateSelection();
    }

    private void updateSelection() {
        repaint();
        requestFocusInWindow();
        nodeSelectionChangedEventEmitter.emit(new NodeSelectionChangedEvent(this));
    }

    /**
     * Selects only the specified algorithm
     *
     * @param ui The algorithm UI
     */
    public void selectOnly(JIPipeNodeUI ui) {
        if (ui == null) {
            clearSelection();
            return;
        }
        if (selection.isEmpty()) {
            addToSelection(ui);
        } else if (selection.size() == 1) {
            if (selection.iterator().next() != ui) {
                clearSelection();
                addToSelection(ui);
            }
        } else {
            clearSelection();
            addToSelection(ui);
        }
    }

    /**
     * Removes an algorithm from the selection
     *
     * @param ui The algorithm UI
     */
    public void removeFromSelection(JIPipeNodeUI ui) {
        if (selection.contains(ui)) {
            selection.remove(ui);
            updateSelection();
        }
    }

    /**
     * Adds an algorithm to the selection
     *
     * @param ui The algorithm UI
     */
    public void addToSelection(JIPipeNodeUI ui) {
        selection.add(ui);
        if (getLayer(ui) < currentNodeLayer) {
            setLayer(ui, ++currentNodeLayer);
        }
        updateSelection();
    }

    /**
     * Sets node positions to make the top left to 0, 0
     *
     * @param save if the locations should be saved
     */
    public void crop(boolean save) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            minX = Math.min(ui.getX(), minX);
            minY = Math.min(ui.getY(), minY);
        }
        boolean oldModified = getWorkbench().isProjectModified();
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            ui.moveToClosestGridPoint(new Point(ui.getX() - minX + viewMode.getGridWidth(),
                    ui.getY() - minY + viewMode.getGridHeight()), true, save);
        }
        getWorkbench().setProjectModified(oldModified);
        setGraphEditCursor(viewMode.gridToRealLocation(new Point(1, 1), zoom));
        minDimensions = null;
        if (getParent() != null)
            getParent().revalidate();
    }

    public Point getGraphEditorCursor() {
        if (graphEditCursor == null)
            graphEditCursor = new Point(0, 0);
        return graphEditCursor;
    }

    /**
     * Removes all UIs and adds them back in
     */
    public void fullRedraw() {
        removeAllNodes();
        addNewNodes(true);
    }

    public List<NodeUIContextAction> getContextActions() {
        return contextActions;
    }

    public void setContextActions(List<NodeUIContextAction> contextActions) {
        this.contextActions = contextActions;
    }

    public <T extends JIPipeGraphNode> Set<JIPipeNodeUI> getNodeUIsFor(Set<T> nodes) {
        Set<JIPipeNodeUI> uis = new HashSet<>();
        for (T node : nodes) {
            JIPipeNodeUI ui = nodeUIs.getOrDefault(node, null);
            if (ui != null) {
                uis.add(ui);
            }
        }
        return uis;
    }

    public JIPipeNodeUIActiveArea getCurrentConnectionDragSource() {
        return currentConnectionDragSource;
    }

    public void setCurrentConnectionDragSource(JIPipeNodeUIActiveArea currentConnectionDragSource) {
        this.currentConnectionDragSource = currentConnectionDragSource;
    }

    public JIPipeNodeUIActiveArea getCurrentConnectionDragTarget() {
        return currentConnectionDragTarget;
    }

    public void setCurrentConnectionDragTarget(JIPipeNodeUIActiveArea currentConnectionDragTarget) {
        this.currentConnectionDragTarget = currentConnectionDragTarget;
    }

    public DisconnectHighlight getDisconnectHighlight() {
        return disconnectHighlight;
    }

    public void setDisconnectHighlight(DisconnectHighlight disconnectHighlight) {
        this.disconnectHighlight = disconnectHighlight;
        repaint(50);
    }

    public ConnectHighlight getConnectHighlight() {
        return connectHighlight;
    }

    public void setConnectHighlight(ConnectHighlight connectHighlight) {
        this.connectHighlight = connectHighlight;
        repaint(50);
    }

    public synchronized void setGraphEditCursor(Point graphEditCursor) {
        this.graphEditCursor = graphEditCursor;
    }

    public void selectAll() {
        selection.addAll(nodeUIs.values());
        updateSelection();
    }

    public void invertSelection() {
        ImmutableSet<JIPipeNodeUI> originalSelection = ImmutableSet.copyOf(selection);
        selection.clear();
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            if (!originalSelection.contains(ui))
                selection.add(ui);
        }
        updateSelection();
    }

    @Override
    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        // Recalculate assets
        updateAssets();

        // Zoom the cursor
        double oldZoom = this.zoom;
        double normalizedCursorX = getGraphEditorCursor().x / oldZoom;
        double normalizedCursorY = getGraphEditorCursor().y / oldZoom;
        setGraphEditCursor(new Point((int) Math.round(normalizedCursorX * zoom), (int) Math.round(normalizedCursorY * zoom)));

        // Zoom nodes
        this.zoom = zoom;
        zoomChangedEventEmitter.emit(new ZoomChangedEvent(this));
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            ui.moveToStoredGridLocation(true);
            ui.setZoom(zoom);
        }
        graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
    }

    public GraphCanvasUpdatedEventEmitter getGraphCanvasUpdatedEventEmitter() {
        return graphCanvasUpdatedEventEmitter;
    }

    private void updateAssets() {
        smartEdgeTooltipSlotFont = new Font(Font.DIALOG, Font.PLAIN, Math.max(1, (int) Math.round(10 * zoom)));
        smartEdgeTooltipNodeFont = new Font(Font.DIALOG, Font.PLAIN, Math.max(1, (int) Math.round(9 * zoom)));
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
        setZoom(Math.max(0.5, zoom - 0.05));
    }

    public void zoomIn() {
        setZoom(Math.min(2, zoom + 0.05));
    }

    public JScrollPane getScrollPane() {
        return graphEditorUI != null ? graphEditorUI.getScrollPane() : null;
    }

    public NodeHotKeyStorage getNodeHotKeyStorage() {
        return nodeHotKeyStorage;
    }

    public Set<JIPipeGraphNode> getScheduledSelection() {
        return scheduledSelection;
    }

    public void setScheduledSelection(Set<JIPipeGraphNode> scheduledSelection) {
        this.scheduledSelection = scheduledSelection;
    }

    public JIPipeHistoryJournal getHistoryJournal() {
        return historyJournal;
    }

    @Override
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        // Update the location of existing nodes
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            ui.moveToStoredGridLocation(true);
        }
        removeOldNodes();
        addNewNodes(true);
        requestFocusInWindow();
    }

    @Override
    public void onNodeConnected(JIPipeGraph.NodeConnectedEvent event) {
        JIPipeNodeUI sourceNode = nodeUIs.getOrDefault(event.getSource().getNode(), null);
        JIPipeNodeUI targetNode = nodeUIs.getOrDefault(event.getTarget().getNode(), null);

        // Check if we actually need to auto-place
        if (sourceNode != null && targetNode != null && targetNode.getY() >= sourceNode.getBottomY() + viewMode.getGridHeight()) {
            return;
        }

        boolean layoutHelperEnabled = settings != null && settings.isLayoutAfterConnect();
        if (sourceNode != null && targetNode != null && layoutHelperEnabled) {

            // Disabled for comment nodes
            if (sourceNode.getNode() instanceof JIPipeCommentNode || targetNode.getNode() instanceof JIPipeCommentNode) {
                return;
            }

            Point cursorBackup = graphEditCursor;
            try {
                setGraphEditCursor(new Point(targetNode.getX(), targetNode.getBottomY() + 4 * viewMode.getGridHeight()));
                autoPlaceTargetAdjacent(sourceNode, event.getSource(), targetNode, event.getTarget());
                autoExpandLeftTop();
            } finally {
                setGraphEditCursor(cursorBackup);
            }
        }

        requestFocusInWindow();
        graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
    }

    @Override
    public void onNodeUIActionRequested(JIPipeNodeUI.NodeUIActionRequestedEvent event) {
        if (event.getAction() instanceof OpenContextMenuAction) {
            if (event.getUi() != null) {
                openContextMenu(getLastMousePosition());
            }
        }
        nodeUIActionRequestedEventEmitter.emit(event);
    }

    public static class DisplayedSlotEdge implements Comparable<DisplayedSlotEdge> {
        private final JIPipeDataSlot source;
        private final JIPipeDataSlot target;
        private final JIPipeGraphEdge edge;

        private int multiColorIndex;

        private int multiColorMax;

        private JIPipeNodeUI sourceUI;

        private JIPipeNodeUI targetUI;

        private Point sourceCenter;

        private Point targetCenter;

        private PointRange sourcePoint;

        private PointRange targetPoint;

        private boolean hidden;

        public DisplayedSlotEdge(JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge) {
            this.source = source;
            this.target = target;
            this.edge = edge;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

        public int getMultiColorMax() {
            return multiColorMax;
        }

        public void setMultiColorMax(int multiColorMax) {
            this.multiColorMax = multiColorMax;
        }

        public Point getSourceCenter() {
            return sourceCenter;
        }

        public void setSourceCenter(Point sourceCenter) {
            this.sourceCenter = sourceCenter;
        }

        public Point getTargetCenter() {
            return targetCenter;
        }

        public void setTargetCenter(Point targetCenter) {
            this.targetCenter = targetCenter;
        }

        public JIPipeGraphEdge getEdge() {
            return edge;
        }

        public JIPipeDataSlot getSource() {
            return source;
        }

        public JIPipeDataSlot getTarget() {
            return target;
        }

        public int getMultiColorIndex() {
            return multiColorIndex;
        }

        public void setMultiColorIndex(int multiColorIndex) {
            this.multiColorIndex = multiColorIndex;
        }

        public PointRange getSourcePoint() {
            return sourcePoint;
        }

        public void setSourcePoint(PointRange sourcePoint) {
            this.sourcePoint = sourcePoint;
        }

        public PointRange getTargetPoint() {
            return targetPoint;
        }

        public void setTargetPoint(PointRange targetPoint) {
            this.targetPoint = targetPoint;
        }

        private boolean isCommentEdge() {
            return source.getNode() instanceof JIPipeCommentNode || target.getNode() instanceof JIPipeCommentNode;
        }

        public JIPipeNodeUI getSourceUI() {
            return sourceUI;
        }

        public void setSourceUI(JIPipeNodeUI sourceUI) {
            this.sourceUI = sourceUI;
        }

        public JIPipeNodeUI getTargetUI() {
            return targetUI;
        }

        public void setTargetUI(JIPipeNodeUI targetUI) {
            this.targetUI = targetUI;
        }

        public int getUIManhattanDistance() {
            if (sourcePoint != null && targetPoint != null) {
                return Math.abs(sourcePoint.center.x - targetPoint.center.x) + Math.abs(sourcePoint.center.y - targetPoint.center.y);
            } else {
                return -1;
            }
        }

        @Override
        public int compareTo(@NotNull JIPipeGraphCanvasUI.DisplayedSlotEdge o) {
            return Integer.compare(getUIManhattanDistance(), o.getUIManhattanDistance());
        }
    }

    /**
     * Generated when an algorithm is selected
     */
    public static class NodeUISelectedEvent extends AbstractJIPipeEvent {

        private final JIPipeNodeUI nodeUI;
        private boolean addToSelection;

        /**
         * @param nodeUI             the algorithm UI
         * @param addToSelection if the algorithm should be added to the selection
         */
        public NodeUISelectedEvent(JIPipeNodeUI nodeUI, boolean addToSelection) {
            super(nodeUI);
            this.nodeUI = nodeUI;
            this.addToSelection = addToSelection;
        }

        public JIPipeNodeUI getNodeUI() {
            return nodeUI;
        }

        public boolean isAddToSelection() {
            return addToSelection;
        }
    }

    public interface NodeUISelectedEventListener {
        void onNodeUISelected(NodeUISelectedEvent event);
    }

    public static class NodeUISelectedEventEmitter extends JIPipeEventEmitter<NodeUISelectedEvent, NodeUISelectedEventListener> {
        @Override
        protected void call(NodeUISelectedEventListener nodeUISelectedEventListener, NodeUISelectedEvent event) {
            nodeUISelectedEventListener.onNodeUISelected(event);
        }
    }

    /**
     * Triggered when An {@link JIPipeGraphCanvasUI} selection was changed
     */
    public static class NodeSelectionChangedEvent extends AbstractJIPipeEvent {
        private JIPipeGraphCanvasUI canvasUI;

        /**
         * @param canvasUI the canvas that triggered the event
         */
        public NodeSelectionChangedEvent(JIPipeGraphCanvasUI canvasUI) {
            super(canvasUI);
            this.canvasUI = canvasUI;
        }

        public JIPipeGraphCanvasUI getCanvasUI() {
            return canvasUI;
        }
    }

    public interface NodeSelectionChangedEventListener {
        void onGraphCanvasNodeSelectionChanged(NodeSelectionChangedEvent event);
    }

    public static class NodeSelectionChangedEventEmitter extends JIPipeEventEmitter<NodeSelectionChangedEvent, NodeSelectionChangedEventListener> {

        @Override
        protected void call(NodeSelectionChangedEventListener nodeSelectionChangedEventListener, NodeSelectionChangedEvent event) {
            nodeSelectionChangedEventListener.onGraphCanvasNodeSelectionChanged(event);
        }
    }

    /**
     * Triggered when a graph canvas was updated
     */
    public static class GraphCanvasUpdatedEvent extends AbstractJIPipeEvent {
        private final JIPipeGraphCanvasUI graphCanvasUI;

        public GraphCanvasUpdatedEvent(JIPipeGraphCanvasUI graphCanvasUI) {
            super(graphCanvasUI);
            this.graphCanvasUI = graphCanvasUI;
        }

        public JIPipeGraphCanvasUI getGraphCanvasUI() {
            return graphCanvasUI;
        }
    }

    public interface GraphCanvasUpdatedEventListener {
        void onGraphCanvasUpdated(GraphCanvasUpdatedEvent event);
    }

    public static class GraphCanvasUpdatedEventEmitter extends JIPipeEventEmitter<GraphCanvasUpdatedEvent, GraphCanvasUpdatedEventListener> {
        @Override
        protected void call(GraphCanvasUpdatedEventListener graphCanvasUpdatedEventListener, GraphCanvasUpdatedEvent event) {
            graphCanvasUpdatedEventListener.onGraphCanvasUpdated(event);
        }
    }

    public static class DisconnectHighlight {
        private final JIPipeNodeUISlotActiveArea target;
        private final Set<JIPipeDataSlot> sources;

        public DisconnectHighlight(JIPipeNodeUISlotActiveArea target, Set<JIPipeDataSlot> sources) {
            this.target = target;
            this.sources = sources;
        }

        public JIPipeNodeUISlotActiveArea getTarget() {
            return target;
        }

        public Set<JIPipeDataSlot> getSources() {
            return sources;
        }
    }

    public static class ConnectHighlight {
        private final JIPipeNodeUISlotActiveArea source;
        private final JIPipeNodeUISlotActiveArea target;

        public ConnectHighlight(JIPipeNodeUISlotActiveArea source, JIPipeNodeUISlotActiveArea target) {
            this.source = source;
            this.target = target;
        }

        public JIPipeNodeUISlotActiveArea getSource() {
            return source;
        }

        public JIPipeNodeUISlotActiveArea getTarget() {
            return target;
        }
    }
}
