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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.hkijena.jipipe.ui.components.ZoomViewPort;
import org.hkijena.jipipe.ui.components.renderers.DropShadowRenderer;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.ui.grapheditor.NodeHotKeyStorage;
import org.hkijena.jipipe.ui.grapheditor.general.actions.JIPipeNodeUIAction;
import org.hkijena.jipipe.ui.grapheditor.general.actions.OpenContextMenuAction;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.layout.MSTGraphAutoLayoutMethod;
import org.hkijena.jipipe.ui.grapheditor.general.layout.SugiyamaGraphAutoLayoutMethod;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeDataSlotUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeHorizontalNodeUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeVerticalNodeUI;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.ScreenImage;
import org.hkijena.jipipe.utils.ui.ScreenImageSVG;
import org.jfree.graphics2d.svg.SVGGraphics2D;

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
public class JIPipeGraphCanvasUI extends JLayeredPane implements JIPipeWorkbenchAccess, MouseMotionListener, MouseListener, MouseWheelListener, ZoomViewPort {

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
    public static final Stroke STROKE_UNIT = new BasicStroke(1);
    public static final Stroke STROKE_UNIT_COMMENT = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{1}, 0);
    public static final Stroke STROKE_DEFAULT = new BasicStroke(4);
    public static final Stroke STROKE_DEFAULT_BORDER = new BasicStroke(6);
    public static final Stroke STROKE_HIGHLIGHT = new BasicStroke(8);
    public static final Stroke STROKE_SELECTION = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
    public static final Stroke STROKE_MARQUEE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Stroke STROKE_COMMENT = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Stroke STROKE_COMMENT_HIGHLIGHT = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{8}, 0);
    private static final Color COMMENT_EDGE_COLOR = new Color(194, 141, 0);

//    public static final Color SHADOW_BASE_COLOR = Color.BLACK;
//    public static final int SHADOW_BASE_OPACITY = 30;
//    public static final int SHADOW_WIDTH = 5;
//    public static final int SHADOW_SHIFT = 2;

    private final JIPipeWorkbench workbench;
    private final JIPipeGraphEditorUI graphEditorUI;
    private final ImageIcon cursorImage = UIUtils.getIconFromResources("actions/target.png");
    private final JIPipeGraph graph;
    private final BiMap<JIPipeGraphNode, JIPipeNodeUI> nodeUIs = HashBiMap.create();
    private final Set<JIPipeNodeUI> selection = new HashSet<>();
    private final EventBus eventBus = new EventBus();
    private final GraphEditorUISettings settings;
    private final JIPipeHistoryJournal historyJournal;
    private final UUID compartment;
    private final Map<JIPipeNodeUI, Point> currentlyDraggedOffsets = new HashMap<>();
    private final NodeHotKeyStorage nodeHotKeyStorage;
    private final Color improvedStrokeBackgroundColor = UIManager.getColor("Panel.background");
    private boolean layoutHelperEnabled;
    private JIPipeGraphViewMode viewMode;
    private JIPipeGraphDragAndDropBehavior dragAndDropBehavior;
    private Point graphEditCursor;
    private Point selectionFirst;
    private Point selectionSecond;
    private long lastTimeExpandedNegative = 0;
    private List<NodeUIContextAction> contextActions = new ArrayList<>();
    private JIPipeDataSlotUI currentConnectionDragSource;

    private boolean currentConnectionDragSourceDragged;
    private JIPipeDataSlotUI currentConnectionDragTarget;
    private JIPipeDataSlotUI currentHighlightedForDisconnect;
    private Set<JIPipeDataSlot> currentHighlightedForDisconnectSourceSlots;
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
        JIPipeGraphViewMode restoredViewMode = graph.getAdditionalMetadata(JIPipeGraphViewMode.class, "jipipe:graph:view-mode");
        if (restoredViewMode != null) {
            this.viewMode = restoredViewMode;
        } else {
            this.viewMode = settings.getDefaultViewMode();
        }
        graph.attachAdditionalMetadata("jipipe:graph:view-mode", this.viewMode);
        initialize();
        addNewNodes(false);
        graph.getEventBus().register(this);
        initializeHotkeys();
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
            getEventBus().post(new GraphCanvasUpdatedEvent(this));
        }
    }

    /**
     * Adds node UIs that are not in the canvas yet
     *
     * @param force if the positioning is forced
     */
    private void addNewNodes(boolean force) {
        int newlyPlacedAlgorithms = 0;
        JIPipeNodeUI ui = null;
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            if (!algorithm.isVisibleIn(getCompartment()))
                continue;
            if (nodeUIs.containsKey(algorithm))
                continue;

            switch (viewMode) {
                case Horizontal:
                    ui = new JIPipeHorizontalNodeUI(getWorkbench(), this, algorithm);
                    break;
                case Vertical:
                    ui = new JIPipeVerticalNodeUI(getWorkbench(), this, algorithm, false);
                    break;
                case VerticalCompact:
                    ui = new JIPipeVerticalNodeUI(getWorkbench(), this, algorithm, true);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown view mode!");
            }

            ui.getEventBus().register(this);
            add(ui, new Integer(currentNodeLayer++)); // Layered pane
            nodeUIs.put(algorithm, ui);
            if (!ui.moveToStoredGridLocation(force)) {
                autoPlaceCloseToCursor(ui, force);
                ++newlyPlacedAlgorithms;
            }
//            ui.addComponentListener(new ComponentAdapter() {
//                @Override
//                public void componentResized(ComponentEvent e) {
//                    removeComponentOverlaps();
//                }
//            });
        }
        revalidate();
        repaint();

        if (newlyPlacedAlgorithms == nodeUIs.size()) {
            autoLayoutAll();
        }
        if (ui != null) {
            getEventBus().post(new JIPipeNodeUI.AlgorithmEvent(ui));
        }
        if (newlyPlacedAlgorithms > 0) {
            getEventBus().post(new GraphCanvasUpdatedEvent(this));
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
        getEventBus().post(new GraphCanvasUpdatedEvent(this));
    }

    /**
     * Triggers when an {@link JIPipeNodeUI} requests an action.
     * Passes the action to its own event bus
     *
     * @param event event
     */
    @Subscribe
    public void onActionRequested(NodeUIActionRequestedEvent event) {
        if (event.getAction() instanceof OpenContextMenuAction) {
            if (event.getUi() != null) {
                openContextMenu(getMousePosition());
            }
        }
        getEventBus().post(event);
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
                if (viewMode == JIPipeGraphViewMode.Horizontal) {
                    currentShape.y += viewMode.getGridHeight();
                } else {
                    currentShape.x += viewMode.getGridWidth();
                }
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
        if (viewMode == JIPipeGraphViewMode.Vertical) {
            for (JIPipeNodeUI ui : nodeUIs.values()) {
                if (ui.getY() >= y)
                    result.add(ui);
            }
        } else {
            for (JIPipeNodeUI ui : nodeUIs.values()) {
                if (ui.getX() >= x)
                    result.add(ui);
            }
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
        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            int sourceSlotInternalY = sourceSlotIndex * viewMode.getGridHeight();
            int targetSlotInternalY = targetSlotIndex * viewMode.getGridHeight();

            int minX = (int) Math.round(sourceAlgorithmUI.getWidth() + sourceAlgorithmUI.getX() + viewMode.getGridWidth() * zoom * 2);
            int targetY = sourceAlgorithmUI.getY() + sourceSlotInternalY - targetSlotInternalY;

            Point targetPoint = new Point(minX, targetY);

            if (GraphEditorUISettings.getInstance().isAutoLayoutMovesOtherNodes()) {
                if (!targetAlgorithmUI.moveToClosestGridPoint(targetPoint, false, true)) {
                    if (nodesAfter.isEmpty())
                        return;
                    // Move all other algorithms
                    int minDistance = Integer.MAX_VALUE;
                    for (JIPipeNodeUI ui : nodesAfter) {
                        if (ui == targetAlgorithmUI || ui == sourceAlgorithmUI)
                            continue;
                        minDistance = Math.min(minDistance, ui.getX() - sourceAlgorithmUI.getRightX());
                    }
                    int translateX = (int) Math.round(targetAlgorithmUI.getWidth() + viewMode.getGridWidth() * zoom * 4 - minDistance);
                    for (JIPipeNodeUI ui : nodesAfter) {
                        if (ui == targetAlgorithmUI || ui == sourceAlgorithmUI)
                            continue;
                        ui.moveToClosestGridPoint(new Point(ui.getX() + translateX, ui.getY()), true, true);
                    }
                    if (!targetAlgorithmUI.moveToClosestGridPoint(targetPoint, false, true)) {
                        autoPlaceCloseToCursor(targetAlgorithmUI, true);
                    }
                }
            } else {
                autoPlaceCloseToLocation(targetAlgorithmUI, targetPoint);
            }

        } else {
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
        if(currentConnectionDragSource != null) {
            // Mark this as actual dragging
            this.currentConnectionDragSourceDragged = true;

            JIPipeNodeUI nodeUI = pickComponent(mouseEvent);
            if(nodeUI != null && currentConnectionDragSource.getNodeUI() != nodeUI) {
                // Advanced dragging behavior
                boolean snapped = false;

                /*
                Auto snap to input/output if there is only one
                 */
                if(currentConnectionDragSource.getSlot().isInput()) {
                   if(nodeUI.getNode().getOutputSlots().size() == 1) {
                       // Auto snap to output
                       JIPipeDataSlotUI slotUI = nodeUI.getOutputSlotUIs().values().iterator().next();
                       setCurrentConnectionDragTarget(slotUI);
                       snapped = true;
                   }
                }
                else {
                    if(nodeUI.getNode().getInputSlots().size() == 1) {
                        // Auto snap to input
                        JIPipeDataSlotUI slotUI = nodeUI.getInputSlotUIs().values().iterator().next();
                        setCurrentConnectionDragTarget(slotUI);
                        snapped = true;
                    }
                }

                /*
                Sticky snap: Stay in last snapped position if we were in it before
                 */
                if(currentConnectionDragTarget != null && currentConnectionDragTarget.getNodeUI() == nodeUI) {
                    JIPipeDataSlotUI slotUI = nodeUI.pickSlotComponent(mouseEvent);
                    if(slotUI != null && slotUI.getSlot().isInput() != currentConnectionDragSource.getSlot().isInput()) {
                        setCurrentConnectionDragTarget(slotUI);
                    }
                    snapped = true;
                }

                /*
                Default: Snap exactly to input/output
                 */
                if(!snapped) {
                    JIPipeDataSlotUI slotUI = nodeUI.pickSlotComponent(mouseEvent);
                    if(slotUI != null && slotUI.getSlot().isInput() != currentConnectionDragSource.getSlot().isInput()) {
                        setCurrentConnectionDragTarget(slotUI);
                    }
                    else {
                        setCurrentConnectionDragTarget(null);
                    }
                }
            }
            else {
                setCurrentConnectionDragTarget(null);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            repaint(50);
        } else if (!currentlyDraggedOffsets.isEmpty()) {
            for (Map.Entry<JIPipeNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                JIPipeNodeUI currentlyDragged = entry.getKey();
                Point currentlyDraggedOffset = entry.getValue();

                int xu = currentlyDraggedOffset.x + mouseEvent.getX();
                int yu = currentlyDraggedOffset.y + mouseEvent.getY();
                if (xu < 0 || yu < 0) {
                    long currentTimeMillis = System.currentTimeMillis();
                    int dx = xu < 0 ? 1 : 0;
                    int dy = yu < 0 ? 1 : 0;
                    if (currentTimeMillis - lastTimeExpandedNegative > 100) {
                        for (JIPipeNodeUI value : nodeUIs.values()) {
                            if (value != currentlyDragged) {
                                Point storedGridLocation = value.getStoredGridLocation();
                                value.moveToGridLocation(new Point(storedGridLocation.x + dx, storedGridLocation.y + dy), true, true);
                            }
                        }
                        lastTimeExpandedNegative = currentTimeMillis;
                    }
                }

                int x = Math.max(0, currentlyDraggedOffset.x + mouseEvent.getX());
                int y = Math.max(0, currentlyDraggedOffset.y + mouseEvent.getY());

                if (!hasDragSnapshot) {
                    // Check if something would change
                    if (!Objects.equals(currentlyDragged.getLocation(), viewMode.realLocationToGrid(new Point(x, y), zoom))) {
                        createMoveSnapshotIfNeeded();
                    }
                }

                currentlyDragged.moveToClosestGridPoint(new Point(x, y), true, true);
            }
            repaint();
            if(SystemUtils.IS_OS_LINUX) {
                Toolkit.getDefaultToolkit().sync();
            }
            if (getParent() != null)
                getParent().revalidate();
            getEventBus().post(new GraphCanvasUpdatedEvent(this));
        } else {
            if (selectionFirst != null) {
                selectionSecond = mouseEvent.getPoint();
                repaint();
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
        if (gridLeft == 0 && gridTop == 0)
            return;
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

    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getClickCount() == 2) {
            JIPipeNodeUI ui = pickComponent(mouseEvent);
            if (ui != null)
                eventBus.post(new DefaultAlgorithmUIActionRequestedEvent(ui));
        } else if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            setGraphEditCursor(new Point(mouseEvent.getX(), mouseEvent.getY()));
            requestFocusInWindow();
            repaint();
        } else if (SwingUtilities.isRightMouseButton(mouseEvent)) {
            if (selection.size() <= 1) {
                JIPipeNodeUI ui = pickComponent(mouseEvent);
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
        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            if (currentlyDraggedOffsets.isEmpty()) {
                JIPipeNodeUI ui = pickComponent(mouseEvent);
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
                    JIPipeDataSlotUI slotUI = ui.pickSlotComponent(mouseEvent);
                    if(slotUI != null) {
                        startDragSlot(slotUI);
                    }
                    else {
                        startDragCurrentNodeSelection(mouseEvent);
                    }
                } else {
                    selectionFirst = mouseEvent.getPoint();
                }
            }
        }
    }

    private void startDragSlot(JIPipeDataSlotUI startSlot) {
        this.currentConnectionDragSourceDragged = false;
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        setCurrentConnectionDragSource(startSlot);
    }

    private void startDragCurrentNodeSelection(MouseEvent mouseEvent) {
        this.hasDragSnapshot = false;
        this.currentConnectionDragSourceDragged = false;
        for (JIPipeNodeUI nodeUI : selection) {
            Point offset = new Point();
            offset.x = nodeUI.getX() - mouseEvent.getX();
            offset.y = nodeUI.getY() - mouseEvent.getY();
            currentlyDraggedOffsets.put(nodeUI, offset);
        }
    }

    private JIPipeNodeUI pickComponent(MouseEvent mouseEvent) {
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

        if (mouseEvent.getButton() != MouseEvent.BUTTON1) {
            stopAllDragging();
            return;
        }
        else {
            if(currentConnectionDragSource != null && currentConnectionDragTarget != null) {
                connectOrDisconnectSlots(currentConnectionDragSource, currentConnectionDragTarget);
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
                JIPipeNodeUI ui = pickComponent(mouseEvent);
                if (ui == null) {
                    selectOnly(null);
                }
                selectionFirst = null;
                selectionSecond = null;
            }
        }
    }

    private void connectOrDisconnectSlots(JIPipeDataSlotUI firstSlot, JIPipeDataSlotUI secondSlot) {
        JIPipeGraph graph = getGraph();
        if (graph != secondSlot.getSlot().getNode().getParentGraph())
            return;
        if (firstSlot.getSlot().isInput() != secondSlot.getSlot().isInput()) {
            if (firstSlot.getSlot().isInput()) {
                if (!graph.getGraph().containsEdge(secondSlot.getSlot(), firstSlot.getSlot())) {
                    firstSlot.connectSlot(secondSlot.getSlot(), firstSlot.getSlot());
                } else {
                    firstSlot.disconnectSlot(secondSlot.getSlot(), firstSlot.getSlot());
                }
            } else {
                if (!graph.getGraph().containsEdge(firstSlot.getSlot(), secondSlot.getSlot())) {
                    secondSlot.connectSlot(firstSlot.getSlot(), secondSlot.getSlot());
                } else {
                    secondSlot.disconnectSlot(firstSlot.getSlot(), secondSlot.getSlot());
                }
            }
        }
    }

    private void stopAllDragging() {
        // Slot dragging
        this.currentConnectionDragSourceDragged = false;
        setCurrentConnectionDragSource(null);
        setCurrentConnectionDragTarget(null);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        repaint(50);

        // Node dragging
        currentlyDraggedOffsets.clear();
        hasDragSnapshot = false;
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

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
     * Should be triggered when the algorithm graph is changed.
     * Triggers a node update.
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        // Update the location of existing nodes
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            ui.moveToStoredGridLocation(true);
        }
        removeOldNodes();
        addNewNodes(true);
        requestFocusInWindow();
    }

    /**
     * Should be triggered when a connection is made
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmConnected(JIPipeGraph.NodeConnectedEvent event) {
        JIPipeNodeUI sourceNode = nodeUIs.getOrDefault(event.getSource().getNode(), null);
        JIPipeNodeUI targetNode = nodeUIs.getOrDefault(event.getTarget().getNode(), null);

        // Check if we actually need to auto-place
        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            if (sourceNode != null && targetNode != null && targetNode.getX() >= sourceNode.getRightX() + viewMode.getGridWidth()) {
                return;
            }
        } else if (viewMode == JIPipeGraphViewMode.Vertical || viewMode == JIPipeGraphViewMode.VerticalCompact) {
            if (sourceNode != null && targetNode != null && targetNode.getY() >= sourceNode.getBottomY() + viewMode.getGridHeight()) {
                return;
            }
        }

        if (sourceNode != null && targetNode != null && layoutHelperEnabled) {

            // Disabled for comment nodes
            if (sourceNode.getNode() instanceof JIPipeCommentNode || targetNode.getNode() instanceof JIPipeCommentNode) {
                return;
            }

            Point cursorBackup = graphEditCursor;
            try {
                if (viewMode == JIPipeGraphViewMode.Horizontal)
                    setGraphEditCursor(new Point(targetNode.getRightX() + 4 * viewMode.getGridWidth(),
                            targetNode.getY()));
                else
                    setGraphEditCursor(new Point(targetNode.getX(), targetNode.getBottomY() + 4 * viewMode.getGridHeight()));
                autoPlaceTargetAdjacent(sourceNode, event.getSource(), targetNode, event.getTarget());
                autoExpandLeftTop();
            } finally {
                setGraphEditCursor(cursorBackup);
            }
        }

        requestFocusInWindow();
        getEventBus().post(new GraphCanvasUpdatedEvent(this));
    }


    /**
     * Should be triggered when an {@link JIPipeNodeUI} requests that the algorithm settings should be opened
     *
     * @param event The generated event
     */
    @Subscribe
    public void onOpenAlgorithmSettings(AlgorithmSelectedEvent event) {
        eventBus.post(event);
    }

    /**
     * @return The event bus instance
     */
    @Override
    public EventBus getEventBus() {
        return eventBus;
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

        for (JIPipeNodeUI nodeUI : nodeUIs.values()) {
            int x = (int) (nodeUI.getX() * scale) + viewX;
            int y = (int) (nodeUI.getY() * scale) + viewY;
            int width = (int) (nodeUI.getWidth() * scale);
            int height = (int) (nodeUI.getHeight() * scale);

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
                false,
                scale,
                viewX,
                viewY);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (settings.isDrawNodeShadows()) {
            for (JIPipeNodeUI ui : nodeUIs.values()) {
                DROP_SHADOW_BORDER.paint(g, ui.getX() - 3, ui.getY() - 3, ui.getWidth() + 8, ui.getHeight() + 8);
                if (ui.getNode().isBookmarked()) {
                    BOOKMARK_SHADOW_BORDER.paint(g, ui.getX() - 12, ui.getY() - 12, ui.getWidth() + 24, ui.getHeight() + 24);
                }
            }
        }

        if (renderOutsideEdges && getCompartment() != null && settings.isDrawOutsideEdges())
            paintOutsideEdges(g, false, Color.DARK_GRAY, STROKE_DEFAULT, STROKE_DEFAULT_BORDER);
        paintEdges(g, STROKE_DEFAULT, STROKE_DEFAULT_BORDER, STROKE_COMMENT, false, false, false, 1, 0, 0);

        if (renderOutsideEdges && getCompartment() != null && settings.isDrawOutsideEdges())
            paintOutsideEdges(g, true, Color.DARK_GRAY, STROKE_HIGHLIGHT, null);
        if (!selection.isEmpty())
            paintEdges(g, STROKE_HIGHLIGHT, null, STROKE_COMMENT_HIGHLIGHT, true, true, settings.isColorSelectedNodeEdges(), 1, 0, 0);

        // Draw currently dragged connection
        if (currentConnectionDragSourceDragged && currentConnectionDragSource != null) {
            g.setStroke(STROKE_HIGHLIGHT);
            PointRange sourcePoint;
            PointRange targetPoint = null;

            sourcePoint = currentConnectionDragSource.getNodeUI().getSlotLocation(currentConnectionDragSource.getSlot());
            sourcePoint.add(currentConnectionDragSource.getNodeUI().getLocation());

            if (currentConnectionDragTarget != null &&
                    currentConnectionDragTarget != currentConnectionDragSource &&
                    currentConnectionDragTarget.getNodeUI().getNode() != currentConnectionDragSource.getNodeUI().getNode()) {
                JIPipeNodeUI nodeUI = currentConnectionDragTarget.getNodeUI();
//                Point mousePosition = currentConnectionDragTarget.getMousePosition();
//                int width = currentConnectionDragTarget.getWidth();
//                int height = currentConnectionDragTarget.getHeight();
//                if (getMousePosition() == null || (mousePosition != null && mousePosition.x >= 5 && mousePosition.y >= 5 && mousePosition.x <= (width - 5) && mousePosition.y <= (height - 5))) {
//                    targetPoint = nodeUI.getSlotLocation(currentConnectionDragTarget.getSlot());
//                    targetPoint.add(nodeUI.getLocation());
//                }
                targetPoint = nodeUI.getSlotLocation(currentConnectionDragTarget.getSlot());
                targetPoint.add(nodeUI.getLocation());
            }
            if (targetPoint != null) {
                if (currentConnectionDragTarget == null || (!graph.getGraph().containsEdge(currentConnectionDragSource.getSlot(), currentConnectionDragTarget.getSlot())
                        && !graph.getGraph().containsEdge(currentConnectionDragTarget.getSlot(), currentConnectionDragSource.getSlot()))) {
                    graphics.setColor(new Color(0, 128, 0));
                } else {
                    graphics.setColor(Color.RED);
                }
            } else {
                graphics.setColor(Color.DARK_GRAY);
            }
            if (targetPoint == null) {
                Point mousePosition = getMousePosition();
                if (mousePosition != null) {
                    targetPoint = new PointRange(mousePosition.x, mousePosition.y);
                }
            }

            if (targetPoint != null) {
                // Tighten the point ranges: Bringing the centers together
                PointRange.tighten(sourcePoint, targetPoint);

                // Draw arrow
                if (currentConnectionDragSource.getSlot().isOutput())
                    drawEdge(g, sourcePoint.center, currentConnectionDragSource.getNodeUI().getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0);
                else
                    drawEdge(g, targetPoint.center, currentConnectionDragSource.getNodeUI().getBounds(), sourcePoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0);
            }
        }

        if (currentHighlightedForDisconnect != null) {
            g.setStroke(STROKE_HIGHLIGHT);
            g.setColor(Color.RED);
            if (currentHighlightedForDisconnect.getSlot().isInput()) {
                Set<JIPipeDataSlot> sources = currentHighlightedForDisconnectSourceSlots;
                for (JIPipeDataSlot source : sources) {
                    JIPipeDataSlot target = currentHighlightedForDisconnect.getSlot();
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
                        drawEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0);
                    }
                }
            } else if (currentHighlightedForDisconnect.getSlot().isOutput()) {
                JIPipeDataSlot source = currentHighlightedForDisconnect.getSlot();
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
                        drawEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, JIPipeGraphEdge.Shape.Elbow, 1, 0, 0);
                    }
                }
            }
        }

        g.setStroke(STROKE_UNIT);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D graphics2D = (Graphics2D) g;

        // Draw cursor over the components
        if (graphEditCursor != null && renderCursor) {
            g.drawImage(cursorImage.getImage(),
                    graphEditCursor.x - cursorImage.getIconWidth() / 2,
                    graphEditCursor.y - cursorImage.getIconHeight() / 2,
                    null);
        }
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
    }

    private void paintEdges(Graphics2D g, Stroke stroke, Stroke strokeBorder, Stroke strokeComment, boolean onlySelected, boolean withHidden, boolean multicolor, double scale, int viewX, int viewY) {
        int multiColorMax = 1;
        Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> slotEdges = graph.getSlotEdges();
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
        int multiColorIndex = 0;
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> kv : slotEdges) {
            JIPipeDataSlot source = kv.getKey();
            JIPipeDataSlot target = kv.getValue();
            JIPipeGraphEdge edge = graph.getGraph().getEdge(kv.getKey(), kv.getValue());

            // Check for only showing selected nodes
            JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
            JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

            if (sourceUI == null || targetUI == null) {
                continue;
            }
            if (onlySelected && !selection.contains(sourceUI) && !selection.contains(targetUI)) {
                continue;
            }

            // Hidden edges
            if (!withHidden) {
                if (edge.isUiHidden()) {
                    paintHiddenSlotEdge(g, source, target, multicolor, multiColorIndex, multiColorMax);
                    continue;
                }
            }
            if (source.getNode() instanceof JIPipeCommentNode || target.getNode() instanceof JIPipeCommentNode) {
                paintSlotEdge(g, strokeComment, null, source, target, sourceUI, targetUI, edge.getUiShape(), multicolor, multiColorIndex, multiColorMax, scale, viewX, viewY);
            } else {
                paintSlotEdge(g, stroke, strokeBorder, source, target, sourceUI, targetUI, edge.getUiShape(), multicolor, multiColorIndex, multiColorMax, scale, viewX, viewY);
            }

            ++multiColorIndex;
        }
    }

    private void paintHiddenSlotEdge(Graphics2D g, JIPipeDataSlot source, JIPipeDataSlot target, boolean multicolor, int multiColorIndex, int multiColorMax) {
        JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
        JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

        if (sourceUI == null || targetUI == null)
            return;

        g.setStroke(STROKE_UNIT);
        g.setColor(getEdgeColor(source, target, multicolor, multiColorIndex, multiColorMax));

        PointRange sourcePoint;
        PointRange targetPoint;

        sourcePoint = sourceUI.getSlotLocation(source);
        sourcePoint.add(sourceUI.getLocation());
        targetPoint = targetUI.getSlotLocation(target);
        targetPoint.add(targetUI.getLocation());

        // Tighten the point ranges: Bringing the centers together
        PointRange.tighten(sourcePoint, targetPoint);

        if (viewMode == JIPipeGraphViewMode.Vertical || viewMode == JIPipeGraphViewMode.VerticalCompact) {
            // From source to target
            {
                int x = sourcePoint.center.x;
                int y = sourcePoint.center.y;
                int h = viewMode.getGridHeight() / 3;
                int d = 6;
                g.drawLine(x, y, x, y + h);
                g.drawOval(x - d / 2, y + h, d, d);
            }

            // At target
            {
                int x = targetPoint.center.x;
                int y = targetPoint.center.y;
                int h = viewMode.getGridHeight() / 3;
                int d = 6;
                g.drawLine(x, y, x, y - h);
                g.drawOval(x - d / 2, y - h - d, d, d);
            }
        } else {
            // From source to target
            {
                int x = sourcePoint.center.x;
                int y = sourcePoint.center.y;
                int w = viewMode.getGridWidth() / 2;
                int d = 7;
                g.drawLine(x, y, x + w, y);
                g.fillOval(x + w, y - d / 2, d, d);
            }

            // At target
            {
                int x = targetPoint.center.x;
                int y = targetPoint.center.y;
                int w = viewMode.getGridWidth() / 2;
                int d = 7;
                g.drawLine(x, y, x - w, y);
                g.fillOval(x - w - d, y - d / 2, d, d);
            }
        }
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
        // Generate fading color
//        if(fade && getScrollPane() != null) {
//            JIPipeNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
//            JIPipeNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);
//            if(sourceUI != null && targetUI != null) {
//                boolean visibleSource = getScrollPane().getViewport().getViewRect().intersects(sourceUI.getBounds());
//                boolean visibleTarget = getScrollPane().getViewport().getViewRect().intersects(targetUI.getBounds());
//                if (!visibleSource && !visibleTarget) {
//                    result = new Color(result.getRed(), result.getGreen(), result.getBlue(), 80);
//                } else if (!visibleSource || !visibleTarget) {
//                    result = new Color(result.getRed(), result.getGreen(), result.getBlue(), 128);
//                }
//            }
//        }
        return result;
    }

    private void paintSlotEdge(Graphics2D g,
                               Stroke stroke,
                               Stroke strokeBorder,
                               JIPipeDataSlot source,
                               JIPipeDataSlot target,
                               JIPipeNodeUI sourceUI,
                               JIPipeNodeUI targetUI,
                               JIPipeGraphEdge.Shape uiShape,
                               boolean multicolor,
                               int multiColorIndex,
                               int multiColorMax,
                               double scale,
                               int viewX,
                               int viewY) {

        if (sourceUI == null || targetUI == null)
            return;

        PointRange sourcePoint;
        PointRange targetPoint;

        sourcePoint = sourceUI.getSlotLocation(source);
        sourcePoint.add(sourceUI.getLocation());
        targetPoint = targetUI.getSlotLocation(target);
        targetPoint.add(targetUI.getLocation());

        // Tighten the point ranges: Bringing the centers together
        PointRange.tighten(sourcePoint, targetPoint);

        // Draw arrow
        if (settings.isDrawImprovedEdges() && strokeBorder != null) {
            g.setStroke(strokeBorder);
            g.setColor(getEdgeColor(source, target, multicolor, multiColorIndex, multiColorMax));
            drawEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY);
            g.setStroke(stroke);
            g.setColor(improvedStrokeBackgroundColor);
            drawEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY);
        } else {
            g.setStroke(stroke);
            g.setColor(getEdgeColor(source, target, multicolor, multiColorIndex, multiColorMax));
            drawEdge(g, sourcePoint.center, sourceUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY);
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
                if (viewMode == JIPipeGraphViewMode.Horizontal) {
                    if (getCompartment() == null || getCompartment().equals(ui.getNode().getCompartmentUUIDInParentGraph())) {
                        sourcePoint.x = ui.getX() + ui.getWidth();
                        sourcePoint.y = ui.getY() + viewMode.getGridHeight() / 2;
                        targetPoint.x = getWidth();
                        targetPoint.y = sourcePoint.y;
                    } else {
                        sourcePoint.x = 0;
                        sourcePoint.y = ui.getY() + viewMode.getGridHeight() / 2;
                        targetPoint.x = ui.getX();
                        targetPoint.y = sourcePoint.y;
                    }
                } else {
                    if (getCompartment() == null || getCompartment().equals(ui.getNode().getCompartmentUUIDInParentGraph())) {
                        sourcePoint.x = ui.getX() + ui.getWidth() / 2;
                        sourcePoint.y = ui.getY() + ui.getHeight();
                        targetPoint.x = sourcePoint.x;
                        targetPoint.y = getHeight();
                    } else {
                        sourcePoint.x = ui.getX() + ui.getWidth() / 2;
                        sourcePoint.y = ui.getY();
                        targetPoint.x = sourcePoint.x;
                        targetPoint.y = 0;
                    }
                }
                if (settings.isDrawImprovedEdges() && borderStroke != null) {
                    g.setStroke(borderStroke);
                    g.setColor(baseColor);
                    drawOutsideEdge(g, sourcePoint, targetPoint);
                    g.setStroke(stroke);
                    g.setColor(improvedStrokeBackgroundColor);
                    drawOutsideEdge(g, sourcePoint, targetPoint);
                } else {
                    g.setStroke(stroke);
                    g.setColor(baseColor);
                    drawOutsideEdge(g, sourcePoint, targetPoint);
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

    private void drawOutsideEdge(Graphics2D g, Point sourcePoint, Point targetPoint) {
        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            g.drawLine(sourcePoint.x, sourcePoint.y, targetPoint.x, sourcePoint.y);
        } else {
            g.drawLine(sourcePoint.x, sourcePoint.y, sourcePoint.x, targetPoint.y);
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
     */
    private void drawEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, JIPipeGraphEdge.Shape shape, double scale, int viewX, int viewY) {
        switch (shape) {
            case Elbow:
                drawElbowEdge(g, sourcePoint, sourceBounds, targetPoint, scale, viewX, viewY);
                break;
            case Line:
                g.drawLine((int) (scale * sourcePoint.x) + viewX,
                        (int) (scale * sourcePoint.y) + viewY,
                        (int) (scale * targetPoint.x) + viewX,
                        (int) (scale * targetPoint.y) + viewY);
                break;
        }
    }

    private void drawElbowEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, double scale, int viewX, int viewY) {
        int buffer;
        int sourceA;
        int targetA;
        int sourceB;
        int targetB;
        int componentStartB;
        int componentEndB;

        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            buffer = viewMode.getGridWidth();
            sourceA = sourcePoint.x;
            targetA = targetPoint.x;
            sourceB = sourcePoint.y;
            targetB = targetPoint.y;
            componentStartB = sourceBounds.y;
            componentEndB = sourceBounds.y + sourceBounds.height;
        } else {
            buffer = viewMode.getGridHeight() / 2;
            sourceA = sourcePoint.y;
            targetA = targetPoint.y;
            sourceB = sourcePoint.x;
            targetB = targetPoint.x;
            componentStartB = sourceBounds.x;
            componentEndB = sourceBounds.x + sourceBounds.width;
        }

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
        if (viewMode == JIPipeGraphViewMode.Horizontal) {
            x2 = (int) (a1 * scale) + viewX;
            y2 = (int) (b1 * scale) + viewY;
        } else {
            x2 = (int) (b1 * scale) + viewX;
            y2 = (int) (a1 * scale) + viewY;
        }
        xCoords.add(x2);
        yCoords.add(y2);
    }

    /**
     * @return Returns true if the layout helper is enabled. It auto-layouts when connections are made
     */
    public boolean isLayoutHelperEnabled() {
        return layoutHelperEnabled;
    }

    /**
     * Enables or disables the layout helper
     *
     * @param layoutHelperEnabled If the layout helper should be enabled
     */
    public void setLayoutHelperEnabled(boolean layoutHelperEnabled) {
        this.layoutHelperEnabled = layoutHelperEnabled;
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

    public void setViewMode(JIPipeGraphViewMode viewMode) {
        if (viewMode != this.viewMode) {
            this.viewMode = viewMode;
            removeAllNodes();
            addNewNodes(true);
            crop(true);
        }
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
     * @return the list of selected {@link JIPipeNodeUI}
     */
    public Set<JIPipeNodeUI> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * @return the list of selected nodes
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
        eventBus.post(new AlgorithmSelectionChangedEvent(this));
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

    public JIPipeDataSlotUI getCurrentConnectionDragSource() {
        return currentConnectionDragSource;
    }

    public void setCurrentConnectionDragSource(JIPipeDataSlotUI currentConnectionDragSource) {
        this.currentConnectionDragSource = currentConnectionDragSource;
    }

    public JIPipeDataSlotUI getCurrentConnectionDragTarget() {
        return currentConnectionDragTarget;
    }

    public void setCurrentConnectionDragTarget(JIPipeDataSlotUI currentConnectionDragTarget) {
        this.currentConnectionDragTarget = currentConnectionDragTarget;
    }

    public JIPipeDataSlotUI getCurrentHighlightedForDisconnect() {
        return currentHighlightedForDisconnect;
    }

    public void setCurrentHighlightedForDisconnect(JIPipeDataSlotUI currentHighlightedForDisconnect, Set<JIPipeDataSlot> sourceSlots) {
        this.currentHighlightedForDisconnect = currentHighlightedForDisconnect;
        currentHighlightedForDisconnectSourceSlots = sourceSlots;
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
        // Zoom the cursor
        double oldZoom = this.zoom;
        double normalizedCursorX = getGraphEditorCursor().x / oldZoom;
        double normalizedCursorY = getGraphEditorCursor().y / oldZoom;
        setGraphEditCursor(new Point((int) Math.round(normalizedCursorX * zoom), (int) Math.round(normalizedCursorY * zoom)));

        // Zoom nodes
        this.zoom = zoom;
        eventBus.post(new ZoomChangedEvent(this));
        for (JIPipeNodeUI ui : nodeUIs.values()) {
            ui.moveToStoredGridLocation(true);
            ui.updateSize();
        }
        eventBus.post(new GraphCanvasUpdatedEvent(this));
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

    /**
     * Generated when an algorithm is selected
     */
    public static class AlgorithmSelectedEvent extends JIPipeNodeUI.AlgorithmEvent {
        private boolean addToSelection;

        /**
         * @param ui             the algorithm UI
         * @param addToSelection if the algorithm should be added to the selection
         */
        public AlgorithmSelectedEvent(JIPipeNodeUI ui, boolean addToSelection) {
            super(ui);
            this.addToSelection = addToSelection;
        }

        public boolean isAddToSelection() {
            return addToSelection;
        }
    }

    /**
     * Triggered when An {@link JIPipeGraphCanvasUI} selection was changed
     */
    public static class AlgorithmSelectionChangedEvent {
        private JIPipeGraphCanvasUI canvasUI;

        /**
         * @param canvasUI the canvas that triggered the event
         */
        public AlgorithmSelectionChangedEvent(JIPipeGraphCanvasUI canvasUI) {

            this.canvasUI = canvasUI;
        }

        public JIPipeGraphCanvasUI getCanvasUI() {
            return canvasUI;
        }
    }

    /**
     * An action that is requested by an {@link JIPipeNodeUI} and passed down to a {@link JIPipeGraphEditorUI}
     */
    public static class NodeUIActionRequestedEvent {
        private final JIPipeNodeUI ui;
        private final JIPipeNodeUIAction action;

        /**
         * Initializes a new instance
         *
         * @param ui     the requesting UI
         * @param action the action parameter
         */
        public NodeUIActionRequestedEvent(JIPipeNodeUI ui, JIPipeNodeUIAction action) {
            this.ui = ui;
            this.action = action;
        }

        public JIPipeNodeUI getUi() {
            return ui;
        }

        public JIPipeNodeUIAction getAction() {
            return action;
        }
    }

    /**
     * Triggered when an {@link JIPipeNodeUI} requests a default action (double click)
     */
    public static class DefaultAlgorithmUIActionRequestedEvent {
        private JIPipeNodeUI ui;

        /**
         * @param ui event source
         */
        public DefaultAlgorithmUIActionRequestedEvent(JIPipeNodeUI ui) {
            this.ui = ui;
        }

        public JIPipeNodeUI getUi() {
            return ui;
        }
    }

    /**
     * Triggered when a graph canvas was updated
     */
    public static class GraphCanvasUpdatedEvent {
        private final JIPipeGraphCanvasUI graphCanvasUI;

        public GraphCanvasUpdatedEvent(JIPipeGraphCanvasUI graphCanvasUI) {
            this.graphCanvasUI = graphCanvasUI;
        }

        public JIPipeGraphCanvasUI getGraphCanvasUI() {
            return graphCanvasUI;
        }
    }
}