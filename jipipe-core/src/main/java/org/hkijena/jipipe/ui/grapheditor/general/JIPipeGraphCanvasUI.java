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

package org.hkijena.jipipe.ui.grapheditor.general;

import com.google.common.collect.*;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDefaultGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeToggleableGraphEditorTool;
import org.hkijena.jipipe.api.history.JIPipeHistoryJournal;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartitionConfiguration;
import org.hkijena.jipipe.extensions.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.extensions.parameters.library.roi.Anchor;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
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
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.*;
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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * UI that displays an {@link JIPipeGraph}
 */
public class JIPipeGraphCanvasUI extends JLayeredPane implements JIPipeWorkbenchAccess, MouseMotionListener, MouseListener, MouseWheelListener, ZoomViewPort, Disposable,
        JIPipeGraph.GraphChangedEventListener, JIPipeGraph.NodeConnectedEventListener, JIPipeGraphNodeUI.NodeUIActionRequestedEventListener {

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

    public static final Font GRAPH_TOOL_CURSOR_FONT = new Font("Dialog", Font.PLAIN, 12);
    public static final Color COLOR_HIGHLIGHT_GREEN = new Color(0, 128, 0);
    public static final Stroke STROKE_UNIT = new BasicStroke(1);
    public static final Stroke STROKE_UNIT_COMMENT = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{1}, 0);
    public static final Stroke STROKE_SELECTION = new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0);
    public static final Stroke STROKE_MARQUEE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Stroke STROKE_COMMENT = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Stroke STROKE_COMMENT_HIGHLIGHT = new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{8}, 0);
    public static final Stroke STROKE_SMART_EDGE = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);
    public static final Color COLOR_RESIZE_HANDLE_FILL = new Color(0x22A02D);
    public static final Color COLOR_RESIZE_HANDLE_BORDER = new Color(0x22A02D).darker();
    private static final int RESIZE_HANDLE_DISTANCE = 12;
    private static final int RESIZE_HANDLE_SIZE = 10;
    private static final Color COMMENT_EDGE_COLOR = new Color(194, 141, 0);
    private final JIPipeWorkbench workbench;
    private final JIPipeGraphEditorUI graphEditorUI;
    private final ImageIcon cursorImage = UIUtils.getIconFromResources("actions/target.png");
    private final JIPipeGraph graph;
    private final BiMap<JIPipeGraphNode, JIPipeGraphNodeUI> nodeUIs = HashBiMap.create();
    private final Set<JIPipeGraphNodeUI> selection = new LinkedHashSet<>();
    private final GraphEditorUISettings settings;
    private final JIPipeHistoryJournal historyJournal;
    private final UUID compartment;
    private final Map<JIPipeGraphNodeUI, Point> currentlyDraggedOffsets = new HashMap<>();
    private final NodeHotKeyStorage nodeHotKeyStorage;
    private final Color improvedStrokeBackgroundColor = UIManager.getColor("Panel.background");
    private final Color smartEdgeSlotBackground = UIManager.getColor("EditorPane.background");
    private final Color smartEdgeSlotForeground = UIManager.getColor("Label.foreground");
    private final ImageIcon lockIcon = UIUtils.getIconInvertedFromResources("actions/lock.png");
    private final JIPipeGraphViewMode viewMode = JIPipeGraphViewMode.VerticalCompact;
    private final Map<?, ?> desktopRenderingHints = UIUtils.getDesktopRenderingHints();
    private final ZoomChangedEventEmitter zoomChangedEventEmitter = new ZoomChangedEventEmitter();
    private final GraphCanvasUpdatedEventEmitter graphCanvasUpdatedEventEmitter = new GraphCanvasUpdatedEventEmitter();
    private final NodeSelectionChangedEventEmitter nodeSelectionChangedEventEmitter = new NodeSelectionChangedEventEmitter();
    private final NodeUISelectedEventEmitter nodeUISelectedEventEmitter = new NodeUISelectedEventEmitter();
    private final JIPipeGraphNodeUI.DefaultNodeUIActionRequestedEventEmitter defaultNodeUIActionRequestedEventEmitter = new JIPipeGraphNodeUI.DefaultNodeUIActionRequestedEventEmitter();
    private final JIPipeGraphNodeUI.NodeUIActionRequestedEventEmitter nodeUIActionRequestedEventEmitter = new JIPipeGraphNodeUI.NodeUIActionRequestedEventEmitter();
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
    private int currentNodeLayer = 0;
    private boolean renderCursor = true;
    private boolean renderOutsideEdges = true;
    /**
     * Used to store the minimum dimensions of the canvas to reduce user disruption
     */
    private Dimension minDimensions = null;
    private JIPipeGraphNodeUI currentlyMouseEnteredNode;
    private JIPipeNodeUIActiveArea currentlyMouseEnteredNodeActiveArea;
    private DisconnectHighlight disconnectHighlight;
    private ConnectHighlight connectHighlight;
    private Font smartEdgeTooltipSlotFont;
    private Font smartEdgeTooltipNodeFont;
    private List<DisplayedSlotEdge> lastDisplayedMainEdges;
    private JIPipeToggleableGraphEditorTool currentTool;
    private boolean autoMuteEdges;
    private Point lastMousePosition;
    private JIPipeAnnotationGraphNodeUI currentResizeTarget;
    private Rectangle currentResizeOperationStartProperties;
    private Anchor currentResizeOperationAnchor;
    private boolean mouseIsEntered;
    private final StampedLock stampedLock = new StampedLock();
    private boolean disposed;

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

        this.autoMuteEdges = settings.isAutoMuteEdgesEnabled();

        graph.attachAdditionalMetadata("jipipe:graph:view-mode", JIPipeGraphViewMode.VerticalCompact);
        initialize();
        addNewNodes(true);

        graph.getGraphChangedEventEmitter().subscribeWeak(this);
        graph.getNodeConnectedEventEmitter().subscribeWeak(this);

        initializeHotkeys();
        updateAssets();
    }

    public Stroke getStrokeHighlight() {
        int width = (int) Math.max(1, zoom * 8);
        return new BasicStroke(width);
    }

    public Stroke getStrokeDefault() {
        int width = (int) Math.max(1, zoom * 4);
        return new BasicStroke(width);
    }

    public Stroke getStrokeDefaultBorder() {
        int width = (int) Math.max(1, zoom * 4) + 2;
        return new BasicStroke(width);
    }

    public Color getImprovedStrokeBackgroundColor() {
        return improvedStrokeBackgroundColor;
    }

    public Color getSmartEdgeSlotBackground() {
        return smartEdgeSlotBackground;
    }

    public Color getSmartEdgeSlotForeground() {
        return smartEdgeSlotForeground;
    }

    public boolean isMouseIsEntered() {
        return mouseIsEntered;
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

    public JIPipeGraphNodeUI.DefaultNodeUIActionRequestedEventEmitter getDefaultAlgorithmUIActionRequestedEventEmitter() {
        return defaultNodeUIActionRequestedEventEmitter;
    }

    public JIPipeGraphNodeUI.NodeUIActionRequestedEventEmitter getNodeUIActionRequestedEventEmitter() {
        return nodeUIActionRequestedEventEmitter;
    }

    @Override
    public void dispose() {
        this.disposed = true;
        graph.getGraphChangedEventEmitter().unsubscribe(this);
        graph.getNodeConnectedEventEmitter().unsubscribe(this);
        for (JIPipeGraphNodeUI nodeUI : nodeUIs.values()) {
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
                            JIPipeGraphNodeUI nodeUI = nodeUIs.getOrDefault(node, null);
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
        for (JIPipeGraphNodeUI ui : ImmutableList.copyOf(nodeUIs.values())) {
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
        for (Map.Entry<JIPipeGraphNode, JIPipeGraphNodeUI> kv : nodeUIs.entrySet()) {
            if (!graph.containsNode(kv.getKey()) || !kv.getKey().isVisibleIn(getCompartment()))
                toRemove.add(kv.getKey());
        }
        for (JIPipeGraphNode algorithm : toRemove) {
            JIPipeGraphNodeUI ui = nodeUIs.get(algorithm);
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
        List<JIPipeGraphNodeUI> newlyPlacedAlgorithms = new ArrayList<>();
        JIPipeGraphNodeUI ui = null;
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            if (!algorithm.isVisibleIn(getCompartment()))
                continue;
            if (nodeUIs.containsKey(algorithm))
                continue;

            if (algorithm instanceof JIPipeAnnotationGraphNode) {
                ui = new JIPipeAnnotationGraphNodeUI(getWorkbench(), this, (JIPipeAnnotationGraphNode) algorithm);
                registerNodeUIEvents(ui);
                add(ui, new Integer(Integer.MIN_VALUE)); // Layered pane (initial value)
            } else {
                ui = new JIPipeGraphNodeUI(getWorkbench(), this, algorithm);
                registerNodeUIEvents(ui);
                add(ui, new Integer(currentNodeLayer++)); // Layered pane
            }

            nodeUIs.put(algorithm, ui);
            if (!ui.moveToStoredGridLocation(force)) {
                autoPlaceCloseToCursor(ui, force);
                newlyPlacedAlgorithms.add(ui);
            }
        }

        updateAnnotationNodeLayers();

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
                JIPipeGraphNodeUI selected = nodeUIs.getOrDefault(node, null);
                if (selected != null) {
                    addToSelection(selected);
                }
            }
            scheduledSelection.clear();
        }
    }

    public void updateAnnotationNodeLayers() {
        // Collect all annotations
        List<JIPipeAnnotationGraphNode> annotationGraphNodes = new ArrayList<>();
        List<JIPipeAnnotationGraphNode> selectedAnnotationGraphNodes = new ArrayList<>();
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeAnnotationGraphNode) {
                JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) graphNode;
                annotationGraphNodes.add(annotationGraphNode);
                JIPipeGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
                if (nodeUI != null) {
                    if (selection.contains(nodeUI)) {
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
            JIPipeGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
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

    public void sendSelectionToForeground(Set<JIPipeGraphNodeUI> selection) {
        boolean updated = false;
        for (JIPipeGraphNodeUI nodeUI : selection) {
            if (nodeUI.getNode().isUiLocked())
                continue;
            if (nodeUI.getNode() instanceof JIPipeAnnotationGraphNode) {

                if (!updated) {
                    getHistoryJournal().snapshot("Send selected nodes to foreground",
                            "Sent a selection of graph annotations to the foreground",
                            getCompartment(),
                            UIUtils.getIconFromResources("actions/object-order-front.png"));
                }

                ((JIPipeAnnotationGraphNode) nodeUI.getNode()).setzOrder(Integer.MAX_VALUE);
                updated = true;
            }
        }
        if (updated)
            updateAnnotationNodeLayers();
    }

    public void sendSelectionToBackground(Set<JIPipeGraphNodeUI> selection) {
        boolean updated = false;
        for (JIPipeGraphNodeUI nodeUI : selection) {
            if (nodeUI.getNode().isUiLocked())
                continue;
            if (nodeUI.getNode() instanceof JIPipeAnnotationGraphNode) {

                if (!updated) {
                    getHistoryJournal().snapshot("Send selected nodes to background",
                            "Sent a selection of graph annotations to the background",
                            getCompartment(),
                            UIUtils.getIconFromResources("actions/object-order-back.png"));
                }

                ((JIPipeAnnotationGraphNode) nodeUI.getNode()).setzOrder(Integer.MIN_VALUE);
                updated = true;
            }
        }
        if (updated)
            updateAnnotationNodeLayers();
    }

    public void raiseSelection(Set<JIPipeGraphNodeUI> selection) {
        TIntObjectMap<JIPipeAnnotationGraphNode> zOrderAnnotations = new TIntObjectHashMap<>();
        List<JIPipeAnnotationGraphNode> selectedAnnotationGraphNodes = new ArrayList<>();
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeAnnotationGraphNode) {
                JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) graphNode;
                zOrderAnnotations.put(annotationGraphNode.getzOrder(), annotationGraphNode);
                JIPipeGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
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
                    getCompartment(),
                    UIUtils.getIconFromResources("actions/object-order-raise.png"));
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

    public void lowerSelection(Set<JIPipeGraphNodeUI> selection) {
        TIntObjectMap<JIPipeAnnotationGraphNode> zOrderAnnotations = new TIntObjectHashMap<>();
        List<JIPipeAnnotationGraphNode> selectedAnnotationGraphNodes = new ArrayList<>();
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            if (graphNode instanceof JIPipeAnnotationGraphNode) {
                JIPipeAnnotationGraphNode annotationGraphNode = (JIPipeAnnotationGraphNode) graphNode;
                zOrderAnnotations.put(annotationGraphNode.getzOrder(), annotationGraphNode);
                JIPipeGraphNodeUI nodeUI = nodeUIs.getOrDefault(annotationGraphNode, null);
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
                    getCompartment(),
                    UIUtils.getIconFromResources("actions/object-order-lower.png"));
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

    private void registerNodeUIEvents(JIPipeGraphNodeUI ui) {
        ui.getNodeUIActionRequestedEventEmitter().subscribe(this);
    }

    private void unregisterNodeUIEvents(JIPipeGraphNodeUI ui) {
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
    public void autoPlaceCloseToLocation(JIPipeGraphNodeUI ui, Point location) {

        int minX = location.x;
        int minY = location.y;

        Set<Rectangle> otherShapes = new HashSet<>();
        for (JIPipeGraphNodeUI otherUi : nodeUIs.values()) {
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

    public void autoPlaceCloseToCursor(JIPipeGraphNodeUI ui, boolean force) {
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
        Point cursor = getGraphEditorCursor();
        if (cursor != null) {
            minX = cursor.x;
            minY = cursor.y;
        }
        ui.moveToClosestGridPoint(new Point(minX, minY), force, true);
        if (graphEditorUI != null) {
            graphEditorUI.scrollToAlgorithm(ui);
        }
    }

    public Set<JIPipeGraphNodeUI> getNodesAfter(int x, int y) {
        Set<JIPipeGraphNodeUI> result = new HashSet<>();
        for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
            if (ui.getY() >= y)
                result.add(ui);
        }
        return result;
    }

    private void autoPlaceTargetAdjacent(JIPipeGraphNodeUI sourceAlgorithmUI, JIPipeDataSlot source, JIPipeGraphNodeUI targetAlgorithmUI, JIPipeDataSlot target) {
        int sourceSlotIndex = source.getNode().getOutputSlots().indexOf(source);
        int targetSlotIndex = target.getNode().getInputSlots().indexOf(target);
        if (sourceSlotIndex < 0 || targetSlotIndex < 0) {
            autoPlaceCloseToCursor(targetAlgorithmUI, true);
            return;
        }

        Set<JIPipeGraphNodeUI> nodesAfter = getNodesAfter(sourceAlgorithmUI.getRightX(), sourceAlgorithmUI.getBottomY());
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
                for (JIPipeGraphNodeUI ui : nodesAfter) {
                    if (ui == targetAlgorithmUI || ui == sourceAlgorithmUI)
                        continue;
                    minDistance = Math.min(minDistance, ui.getY() - sourceAlgorithmUI.getBottomY());
                }
                int translateY = (int) Math.round(targetAlgorithmUI.getHeight() + viewMode.getGridHeight() * zoom * 2 - minDistance);
                for (JIPipeGraphNodeUI ui : nodesAfter) {
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

        // Resize dragging
        if (currentResizeTarget != null && currentResizeOperationAnchor != null && currentResizeOperationStartProperties != null) {
            mouseDraggedResizing(mouseEvent);
        }

        // Node connection dragging
        if (currentConnectionDragSource != null) {
            mouseDraggedConnectionDraggingTarget(mouseEvent);
        } else if (!currentlyDraggedOffsets.isEmpty()) {
            mouseDraggedNodeDragging(mouseEvent);
        } else {
            if (selectionFirst != null) {
                selectionSecond = mouseEvent.getPoint();
                repaintLowLag();
            }
        }
    }

    private void mouseDraggedResizing(MouseEvent mouseEvent) {
        Point mouseInGrid = viewMode.realLocationToGrid(lastMousePosition, zoom);
        int startGridX = currentResizeOperationStartProperties.x;
        int startGridY = currentResizeOperationStartProperties.y;
        int endGridX = currentResizeOperationStartProperties.x + currentResizeOperationStartProperties.width;
        int endGridY = currentResizeOperationStartProperties.y + currentResizeOperationStartProperties.height;
        switch (currentResizeOperationAnchor) {
            case TopLeft: {
                int dY = mouseInGrid.y - startGridY;
                int dGridHeight = -dY;
                int newY = startGridY + dY;
                int newHeight = currentResizeOperationStartProperties.height + dGridHeight;
                int dX = mouseInGrid.x - startGridX;
                int dGridWidth = -dX;
                int newX = startGridX + dX;
                int newWidth = currentResizeOperationStartProperties.width + dGridWidth;
                if ((dX != 0 || dY != 0) && newWidth > 0 && newHeight > 0) {
                    currentResizeTarget.moveToGridLocation(new Point(newX, newY), true, true);
                    currentResizeTarget.setNodeGridSize(newWidth, newHeight);
                }
            }
            break;
            case TopCenter: {
                int dY = mouseInGrid.y - startGridY;
                int dGridHeight = -dY;
                int newY = startGridY + dY;
                int newHeight = currentResizeOperationStartProperties.height + dGridHeight;
                if (dY != 0 && newHeight > 0) {
                    currentResizeTarget.moveToGridLocation(new Point(startGridX, newY), true, true);
                    currentResizeTarget.setNodeGridSize(currentResizeOperationStartProperties.width, newHeight);
                }
            }
            break;
            case TopRight: {
                int dY = mouseInGrid.y - startGridY;
                int dGridHeight = -dY;
                int dGridWidth = mouseInGrid.x - endGridX;
                int newY = startGridY + dY;
                int newHeight = currentResizeOperationStartProperties.height + dGridHeight;
                int newWidth = currentResizeOperationStartProperties.width + dGridWidth;
                if (newWidth > 0 && newHeight > 0) {
                    currentResizeTarget.moveToGridLocation(new Point(startGridX, newY), true, true);
                    currentResizeTarget.setNodeGridSize(newWidth, newHeight);
                }
            }
            break;
            case CenterLeft: {
                int dX = mouseInGrid.x - startGridX;
                int dGridWidth = -dX;
                int newX = startGridX + dX;
                int newWidth = currentResizeOperationStartProperties.width + dGridWidth;
                if (dX != 0 && newWidth > 0) {
                    currentResizeTarget.moveToGridLocation(new Point(newX, startGridY), true, true);
                    currentResizeTarget.setNodeGridSize(newWidth, currentResizeOperationStartProperties.height);
                }
            }
            break;
            case CenterRight: {
                int dGridWidth = mouseInGrid.x - endGridX;
                currentResizeTarget.setNodeGridSize(currentResizeOperationStartProperties.width + dGridWidth, currentResizeOperationStartProperties.height);
            }
            break;
            case BottomLeft: {
                int dX = mouseInGrid.x - startGridX;
                int dGridWidth = -dX;
                int dGridHeight = mouseInGrid.y - endGridY;
                int newX = startGridX + dX;
                int newWidth = currentResizeOperationStartProperties.width + dGridWidth;
                int newHeight = currentResizeOperationStartProperties.height + dGridHeight;
                if (newWidth > 0 && newHeight > 0) {
                    currentResizeTarget.moveToGridLocation(new Point(newX, startGridY), true, true);
                    currentResizeTarget.setNodeGridSize(newWidth, newHeight);
                }
            }
            break;
            case BottomCenter: {
                int dGridHeight = mouseInGrid.y - endGridY;
                currentResizeTarget.setNodeGridSize(currentResizeOperationStartProperties.width, currentResizeOperationStartProperties.height + dGridHeight);
            }
            break;
            case BottomRight: {
                int dGridWidth = mouseInGrid.x - endGridX;
                int dGridHeight = mouseInGrid.y - endGridY;
                currentResizeTarget.setNodeGridSize(currentResizeOperationStartProperties.width + dGridWidth, currentResizeOperationStartProperties.height + dGridHeight);
            }
            break;
        }
    }

    private void mouseDraggedNodeDragging(MouseEvent mouseEvent) {
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

        for (Map.Entry<JIPipeGraphNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
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
            for (Map.Entry<JIPipeGraphNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                JIPipeGraphNodeUI currentlyDragged = entry.getKey();

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
            for (JIPipeGraphNodeUI value : nodeUIs.values()) {
                if (!currentlyDraggedOffsets.containsKey(value)) {
                    Point storedGridLocation = value.getStoredGridLocation();
                    value.moveToGridLocation(new Point(storedGridLocation.x - negativeDx, storedGridLocation.y - negativeDy), true, true);
                }
            }
        }

        for (Map.Entry<JIPipeGraphNodeUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
            JIPipeGraphNodeUI currentlyDragged = entry.getKey();

            Point newGridLocation = new Point(currentlyDragged.getStoredGridLocation().x + gridDx, currentlyDragged.getStoredGridLocation().y + gridDy);

            if (!hasDragSnapshot) {
                // Check if something would change
                if (!Objects.equals(currentlyDragged.getStoredGridLocation(), newGridLocation)) {
                    createMoveSnapshotIfNeeded();
                }
            }

            currentlyDragged.moveToGridLocation(newGridLocation, true, true);
        }

        repaintLowLag();
        if (getParent() != null)
            getParent().revalidate();
        graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
    }

    public void repaintLowLag() {
        repaint(50);
        if (SystemUtils.IS_OS_LINUX) {
            Toolkit.getDefaultToolkit().sync();
        }
    }

    private void mouseDraggedConnectionDraggingTarget(MouseEvent mouseEvent) {
        // Mark this as actual dragging
        this.currentConnectionDragSourceDragged = true;

        JIPipeGraphNodeUI nodeUI = pickNodeUI(mouseEvent);
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
    }

    public void autoExpandLeftTop() {
        int minX = 0;
        int minY = 0;
        for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
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
        for (JIPipeGraphNodeUI value : nodeUIs.values()) {
            if (!currentlyDraggedOffsets.containsKey(value)) {
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
        for (JIPipeGraphNodeUI value : nodeUIs.values()) {
            if (!currentlyDraggedOffsets.containsKey(value)) {
                Point gridLocation = viewMode.realLocationToGrid(value.getLocation(), zoom);
                gridLocation.x += gridLeft;
                gridLocation.y += gridTop;
                value.moveToGridLocation(gridLocation, true, true);
            }
        }
        Point cursor = getGraphEditorCursor();
        if (cursor != null) {
            Point realLeftTop = viewMode.gridToRealLocation(new Point(gridLeft, gridTop), zoom);
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
        if (currentTool != null) {
            currentTool.mouseMoved(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
            defaultCursor = currentTool.getCursor();
        }

        // Resize handler
        if (currentResizeTarget != null && !currentResizeTarget.getNode().isUiLocked()) {
            for (Anchor anchor : Anchor.values()) {
                Rectangle rectangle = getCurrentResizeTargetAnchorArea(anchor);
                if (rectangle != null) {
                    if (rectangle.contains(mouseEvent.getPoint())) {
                        switch (anchor) {
                            case TopLeft:
                                setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                                break;
                            case TopCenter:
                                setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                                break;
                            case TopRight:
                                setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                                break;
                            case CenterLeft:
                                setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                                break;
                            case CenterRight:
                                setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                                break;
                            case BottomLeft:
                                setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                                break;
                            case BottomCenter:
                                setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                                break;
                            case BottomRight:
                                setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                                break;
                            default:
                                setCursor(defaultCursor);
                                break;
                        }
                        return;
                    }
                } else {
                    setCursor(defaultCursor);
                }
            }
        } else {
            setCursor(defaultCursor);
        }

        // Handling by node
        boolean changed = false;
        JIPipeGraphNodeUI nodeUI = pickNodeUI(mouseEvent);
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
            JIPipeNodeUIActiveArea currentActiveArea = currentlyMouseEnteredNode.getCurrentActiveArea();
            if (currentActiveArea != currentlyMouseEnteredNodeActiveArea) {
                currentlyMouseEnteredNodeActiveArea = currentActiveArea;
                changed = true;
            }
        }
        if (currentTool != null && settings.isShowToolInfo() && !(currentTool instanceof JIPipeDefaultGraphEditorTool)) {
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
        if (currentTool != null) {
            currentTool.mouseClicked(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }

        JIPipeGraphNodeUI ui = pickNodeUI(mouseEvent);

        if (ui != null) {
            ui.mouseClicked(mouseEvent);
            if (mouseEvent.isConsumed())
                return;
        }

        if (SwingUtilities.isLeftMouseButton(mouseEvent) && mouseEvent.getClickCount() == 2) {
            if (ui != null)
                defaultNodeUIActionRequestedEventEmitter.emit(new JIPipeGraphNodeUI.DefaultNodeUIActionRequestedEvent(ui));
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

        // Node partitioning menus
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            JIPipeRuntimePartitionConfiguration runtimePartitions = ((JIPipeProjectWorkbench) getWorkbench()).getProject().getRuntimePartitions();

            // Algorithms
            if (selection.stream().anyMatch(ui -> ui.getNode() instanceof JIPipeAlgorithm)) {
                menu.addSeparator();
                JMenu partitionMenu = new JMenu("Move to partition ...");
                for (JIPipeRuntimePartition runtimePartition : runtimePartitions.toList()) {
                    JMenuItem item = new JMenuItem(runtimePartitions.getFullName(runtimePartition), runtimePartitions.getIcon(runtimePartition));
                    item.addActionListener(e -> partitionSelectedAlgorithms(runtimePartition));
                    item.setToolTipText("Partitions all selected nodes to '" + runtimePartitions.getFullName(runtimePartition) + "'");
                    partitionMenu.add(item);
                }
                menu.add(partitionMenu);
            }

            // Compartments
            if (selection.stream().anyMatch(ui -> ui.getNode() instanceof JIPipeProjectCompartment)) {
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
        if (getWorkbench() instanceof JIPipeProjectWorkbench) {
            JIPipeRuntimePartitionConfiguration runtimePartitions = ((JIPipeProjectWorkbench) getWorkbench()).getProject().getRuntimePartitions();
            int newIndex = runtimePartitions.indexOf(runtimePartition);
            if (newIndex == -1) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "Unable to find selected partition!", "Partition nodes", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (JIPipeGraphNodeUI nodeUI : selection) {
                if (nodeUI.getNode() instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) nodeUI.getNode()).getRuntimePartition().setIndex(newIndex);
                    nodeUI.updateView(false, false, false);
                } else if (nodeUI.getNode() instanceof JIPipeProjectCompartment) {
                    UUID uuid = ((JIPipeProjectCompartment) nodeUI.getNode()).getProjectCompartmentUUID();
                    for (JIPipeGraphNode graphNode : ((JIPipeProjectWorkbench) getWorkbench()).getProject().getGraph().getGraphNodes()) {
                        if (graphNode instanceof JIPipeAlgorithm && Objects.equals(uuid, graphNode.getCompartmentUUIDInParentGraph())) {
                            ((JIPipeAlgorithm) graphNode).getRuntimePartition().setIndex(newIndex);
                            graphNode.getParameterChangedEventEmitter().emit(new JIPipeParameterCollection.ParameterChangedEvent(graphNode, "jipipe:algorithm:runtime-partition"));
                        }
                    }
                    nodeUI.updateView(false, false, false);
                }
            }
        }
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

            // Resize handling
            if (currentResizeTarget != null && !currentResizeTarget.getNode().isUiLocked()) {
                for (Anchor anchor : Anchor.values()) {
                    Rectangle rectangle = getCurrentResizeTargetAnchorArea(anchor);
                    if (rectangle != null) {
                        if (rectangle.contains(mouseEvent.getPoint())) {
                            JIPipeAnnotationGraphNode node = (JIPipeAnnotationGraphNode) currentResizeTarget.getNode();
                            Point gridLocation = node.getLocationWithin(StringUtils.nullToEmpty(getCompartment()), viewMode.name());
                            currentResizeOperationStartProperties = new Rectangle(gridLocation.x, gridLocation.y, node.getGridWidth(), node.getGridHeight());
                            currentResizeOperationAnchor = anchor;
                            return;
                        }
                    }
                }

            }

            // Node dragging
            if (currentlyDraggedOffsets.isEmpty()) {
                JIPipeGraphNodeUI ui = pickNodeUI(mouseEvent);
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
                            if (!startDragCurrentNodeSelection(mouseEvent)) {
                                selectionFirst = mouseEvent.getPoint();
                            }
                        }
                    } else {
                        // Dragging slots disabled by tools
                        if (!startDragCurrentNodeSelection(mouseEvent)) {
                            selectionFirst = mouseEvent.getPoint();
                        }
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

    private boolean startDragCurrentNodeSelection(MouseEvent mouseEvent) {
        if (currentToolAllowsNodeDragging()) {
            this.hasDragSnapshot = false;
            this.currentConnectionDragSourceDragged = false;
            for (JIPipeGraphNodeUI nodeUI : selection) {
                if (nodeUI.getNode().isUiLocked())
                    continue;
                Point offset = new Point();
                offset.x = nodeUI.getX() - mouseEvent.getX();
                offset.y = nodeUI.getY() - mouseEvent.getY();
                currentlyDraggedOffsets.put(nodeUI, offset);
            }
            return !currentlyDraggedOffsets.isEmpty();
        } else {
            stopAllDragging();
        }
        return false;
    }

    public JIPipeGraphNodeUI pickNodeUI(MouseEvent mouseEvent) {
        for (int i = 0; i < getComponentCount(); ++i) {
            Component component = getComponent(i);
            if (component.getBounds().contains(mouseEvent.getX(), mouseEvent.getY())) {
                if (component instanceof JIPipeGraphNodeUI) {
                    return (JIPipeGraphNodeUI) component;
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
        if (currentResizeOperationStartProperties != null) {
            stopAllResizing();
            return;
        }
        stopAllResizing();

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
                Set<JIPipeGraphNodeUI> newSelection = new HashSet<>();
                for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
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
                JIPipeGraphNodeUI ui = pickNodeUI(mouseEvent);
                if (ui == null) {
                    selectOnly(null);
                }
                selectionFirst = null;
                selectionSecond = null;
            }
        }
    }

    private void connectCreateNewSlot(JIPipeDataSlot sourceSlot, JIPipeGraphNodeUI nodeUI) {
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
        mouseIsEntered = true;

        // End resize operation
        stopAllResizing();

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
        mouseIsEntered = false;

        // End resize operation
        stopAllResizing();

        // Let the tool handle the event
        if (currentTool != null) {
            currentTool.mouseExited(mouseEvent);
            if (mouseEvent.isConsumed()) {
                return;
            }
        }
    }

    private void stopAllResizing() {
        currentResizeOperationStartProperties = null;
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

        for (JIPipeGraphNodeUI nodeUI : nodeUIs.values()) {
            int x = (int) (nodeUI.getX() * scale) + viewX;
            int y = (int) (nodeUI.getY() * scale) + viewY;
            int width = (int) (nodeUI.getWidth() * scale);
            int height = (int) (nodeUI.getHeight() * scale);

            nodeUI.paintMinimap(graphics2D, x, y, width, height, defaultStroke, selectedStroke, selection);
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

        final Stroke strokeDefault = getStrokeDefault();
        final Stroke strokeDefaultBorder = getStrokeDefaultBorder();
        final Stroke strokeHighlight = getStrokeHighlight();

        // Draw the annotations and shadows
        boolean finalDrawShadows = settings.isDrawNodeShadows();
        AffineTransform originalTransform = g.getTransform();
        for (int i = getComponentCount() - 1; i >= 0; i--) {
            Component component = getComponent(i);

            if (component instanceof JIPipeGraphNodeUI) {
                JIPipeGraphNodeUI ui = (JIPipeGraphNodeUI) component;

                // Draw shadow
                if (finalDrawShadows) {

                    // Set render settings (LQ)
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

                    if (ui.isDrawShadow()) {
                        DROP_SHADOW_BORDER.paint(g, ui.getX() - 3, ui.getY() - 3, ui.getWidth() + 8, ui.getHeight() + 8);
                    }
                    if (ui.getNode().isBookmarked()) {
                        BOOKMARK_SHADOW_BORDER.paint(g, ui.getX() - 12, ui.getY() - 12, ui.getWidth() + 24, ui.getHeight() + 24);
                    }
                }

                // Draw annotation
                if (component instanceof JIPipeAnnotationGraphNodeUI) {

                    // Set render settings (HQ)
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                    JIPipeAnnotationGraphNodeUI annotationGraphNodeUI = (JIPipeAnnotationGraphNodeUI) component;
                    g.translate(annotationGraphNodeUI.getX(), annotationGraphNodeUI.getY());
                    JIPipeAnnotationGraphNode node = (JIPipeAnnotationGraphNode) (annotationGraphNodeUI).getNode();
                    node.paintNode(g, annotationGraphNodeUI, zoom);
                    g.setTransform(originalTransform);
                }
            }
        }

        // Set render settings (HQ)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Below node paint
        if (currentTool != null) {
            currentTool.paintBelowNodesAndEdges(g);
        }

        if (renderOutsideEdges && getCompartment() != null && settings.isDrawOutsideEdges())
            paintOutsideEdges(g, false, Color.DARK_GRAY, strokeDefault, strokeDefaultBorder);

        // Main edge drawing
        lastDisplayedMainEdges = paintEdges(g,
                strokeDefault,
                strokeDefaultBorder,
                STROKE_COMMENT,
                false,
                false,
                1,
                0,
                0,
                true,
                isAutoMuteEdges(), false);

        // Outside edges drawing
        if (renderOutsideEdges && getCompartment() != null && settings.isDrawOutsideEdges()) {
            paintOutsideEdges(g, true, Color.DARK_GRAY, strokeHighlight, null);
        }

        // Selected edges drawing
        if (!selection.isEmpty()) {
            paintEdges(g,
                    strokeHighlight,
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
            g.setStroke(getStrokeHighlight());
            g.setColor(Color.RED);
            if (disconnectHighlight.getTarget().getSlot().isInput()) {
                Set<JIPipeDataSlot> sources = disconnectHighlight.getSources();
                for (JIPipeDataSlot source : sources) {
                    JIPipeDataSlot target = disconnectHighlight.getTarget().getSlot();
                    JIPipeGraphNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeGraphNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

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
                    JIPipeGraphNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeGraphNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

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
            g.setStroke(getStrokeHighlight());
            g.setColor(COLOR_HIGHLIGHT_GREEN);
            if (connectHighlight.getTarget().getSlot().isInput()) {
                JIPipeDataSlot source = connectHighlight.getSource().getSlot();
                JIPipeDataSlot target = connectHighlight.getTarget().getSlot();
                JIPipeGraphNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                JIPipeGraphNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

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
                JIPipeGraphNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                JIPipeGraphNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);
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
            g.setStroke(getStrokeHighlight());
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
                JIPipeGraphNodeUI nodeUI = currentConnectionDragTarget.getNodeUI();
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

        if (disposed) {
            return;
        }

        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHints(desktopRenderingHints);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Draw node selections & lock
        graphics2D.setStroke(STROKE_SELECTION);
        for (JIPipeGraphNodeUI ui : selection) {
            Rectangle bounds = ui.getBounds();
            bounds.x -= 4;
            bounds.y -= 4;
            bounds.width += 8;
            bounds.height += 8;
            g.setColor(ui.getBorderColor());
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

            // Lock icon
            if (ui.getNode().isUiLocked()) {
                g.fillRect(bounds.x - 1, bounds.y - 1, 22, 22);
                graphics2D.drawImage(lockIcon.getImage(), bounds.x + 2, bounds.y + 2, 16, 16, null);
            }

            // Layer Z (annotations)
            if (ui.getNode() instanceof JIPipeAnnotationGraphNode) {
                int zLayer = ((JIPipeAnnotationGraphNode) ui.getNode()).getzOrder();
                g.setFont(GRAPH_TOOL_CURSOR_FONT);
                FontMetrics fontMetrics = g.getFontMetrics();
                String text = "z " + zLayer;
                int rawStringWidth = fontMetrics.stringWidth(text);
                int indicatorWidth = rawStringWidth + 8;
                int xStart = bounds.x + bounds.width + 4 - indicatorWidth - 1 - 8 - 4;
                int yStart = bounds.y + bounds.height - 22 - 8;

                g.fillRoundRect(xStart, yStart, indicatorWidth, 22, 4, 4);
                g.setColor(Color.WHITE);

                g.drawString(text, xStart + indicatorWidth / 2 - rawStringWidth / 2, yStart + (fontMetrics.getAscent() - fontMetrics.getLeading()) + 22 / 2 - fontMetrics.getHeight() / 2);
            }
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
            paintNodeIOLabels(graphics2D, lastDisplayedMainEdges);
        }

        // Draw cursor over the components
        Point cursor = getGraphEditorCursor();
        if (cursor != null && renderCursor) {
            g.drawImage(cursorImage.getImage(),
                    cursor.x - cursorImage.getIconWidth() / 2,
                    cursor.y - cursorImage.getIconHeight() / 2,
                    null);
        }

        // Draw resize handles
        if (currentResizeTarget != null && !currentResizeTarget.getNode().isUiLocked()) {
            g.setColor(Color.GRAY);
            graphics2D.setStroke(STROKE_MARQUEE);
            g.drawRect(currentResizeTarget.getX() - RESIZE_HANDLE_DISTANCE, currentResizeTarget.getY() - RESIZE_HANDLE_DISTANCE, currentResizeTarget.getWidth() + RESIZE_HANDLE_DISTANCE * 2, currentResizeTarget.getHeight() + RESIZE_HANDLE_DISTANCE * 2);
            graphics2D.setStroke(STROKE_UNIT);

            for (Anchor anchor : Anchor.values()) {
                Rectangle rectangle = getCurrentResizeTargetAnchorArea(anchor);
                if (rectangle != null) {
                    g.setColor(COLOR_RESIZE_HANDLE_FILL);
                    g.fillOval(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                    g.setColor(COLOR_RESIZE_HANDLE_BORDER);
                    g.drawOval(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                }
            }
        }

        // Draw cursor info
        if (mouseIsEntered && lastMousePosition != null && currentTool != null && settings.isShowToolInfo() && !(currentTool instanceof JIPipeDefaultGraphEditorTool)) {
            currentTool.paintMouse(this, lastMousePosition, settings.getToolInfoDistance(), graphics2D);
        }
    }

    private Rectangle getCurrentResizeTargetAnchorArea(Anchor anchor) {
        if (currentResizeTarget != null) {
            switch (anchor) {
                case TopLeft:
                    return new Rectangle(currentResizeTarget.getX() - RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() - RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
                case TopCenter:
                    return new Rectangle(currentResizeTarget.getX() + currentResizeTarget.getWidth() / 2 - RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() - RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
                case TopRight:
                    return new Rectangle(currentResizeTarget.getRightX() + RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() - RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
                case CenterLeft:
                    return new Rectangle(currentResizeTarget.getX() - RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() + currentResizeTarget.getHeight() / 2 - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
                case CenterRight:
                    return new Rectangle(currentResizeTarget.getRightX() + RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getY() + currentResizeTarget.getHeight() / 2 - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
                case BottomLeft:
                    return new Rectangle(currentResizeTarget.getX() - RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getBottomY() + RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
                case BottomCenter:
                    return new Rectangle(currentResizeTarget.getX() + currentResizeTarget.getWidth() / 2 - RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getBottomY() + RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
                case BottomRight:
                    return new Rectangle(currentResizeTarget.getRightX() + RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, currentResizeTarget.getBottomY() + RESIZE_HANDLE_DISTANCE - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE);
                default:
                    return null;
            }
        } else {
            return null;
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
            JIPipeGraphNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
            JIPipeGraphNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

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
                    JIPipeGraphNodeUI sourceUI = nodeUIs.getOrDefault(source.getNode(), null);
                    JIPipeGraphNodeUI targetUI = nodeUIs.getOrDefault(target.getNode(), null);

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

    private void paintNodeIOLabels(Graphics2D g, List<DisplayedSlotEdge> displayedMainEdges) {

        // Modified to only show on hover, as input labels are not very helpful in complex graphs

        if (!settings.isDrawLabelsOnHover())
            return;

        FontMetrics slotFontMetrics = g.getFontMetrics(smartEdgeTooltipSlotFont);
//        FontMetrics nodeFontMetrics = g.getFontMetrics(smartEdgeTooltipNodeFont);

        // Collect labelled inputs based on pre-calculated
        // Target to sources
        Map<DisplayedSlotEdge, Integer> edgeIds = new IdentityHashMap<>();
        Multimap<JIPipeDataSlot, DisplayedSlotEdge> highlightedEdges = HashMultimap.create();

        // Find edges of interest
        JIPipeDataSlot targetSlot = null;
        if (settings.isDrawLabelsOnHover() && !isCurrentlyDraggingNode() && !isCurrentlyDraggingConnection() && lastMousePosition != null) {
            if (currentlyMouseEnteredNode != null && currentlyMouseEnteredNodeActiveArea instanceof JIPipeNodeUISlotActiveArea) {
                JIPipeNodeUISlotActiveArea slot = (JIPipeNodeUISlotActiveArea) currentlyMouseEnteredNodeActiveArea;
                targetSlot = slot.getSlot();
                for (DisplayedSlotEdge displayedSlotEdge : displayedMainEdges) {
                    if (slot.getSlot() == displayedSlotEdge.getTarget() || slot.getSlot() == displayedSlotEdge.getSource()) {

                        // Set ID
                        int id = edgeIds.getOrDefault(displayedSlotEdge, edgeIds.size() + 1);
                        edgeIds.put(displayedSlotEdge, id);

                        if (slot.getSlot() == displayedSlotEdge.getTarget()) {
                            highlightedEdges.put(displayedSlotEdge.getSource(), displayedSlotEdge);
                        }
                        else if(slot.getSlot() == displayedSlotEdge.getSource()) {
                            highlightedEdges.put(displayedSlotEdge.getTarget(), displayedSlotEdge);
                        }

                    }
                }
            }
        }

        // Cancel if there is only a single edge and not far away
        if(edgeIds.isEmpty()) {
            return;
        }
        if(edgeIds.size() == 1) {
            DisplayedSlotEdge edge = edgeIds.keySet().iterator().next();
            if(edge.getUIManhattanDistance() <= 2 * zoom * getViewMode().getGridHeight()) {
                return;
            }
        }


        // Draw edges
        for (JIPipeDataSlot currentSlot : highlightedEdges.keySet()) {
            List<DisplayedSlotEdge> connectedSlots = highlightedEdges.get(currentSlot).stream().sorted(Comparator.comparing(DisplayedSlotEdge::getUIManhattanDistance)).collect(Collectors.toList());
            JIPipeGraphNodeUI nodeUI = nodeUIs.get(currentSlot.getNode());

            JIPipeNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(currentSlot);
            if (slotActiveArea != null && slotActiveArea.getZoomedHitArea() != null) {

                final int baseHeight = 32;
                final int baseIconSize = 16;
                final int baseIconSpacing = 6;
                int tooltipHeight = (int) Math.round(zoom * connectedSlots.size() * baseHeight);
                int maxAvailableWidth = slotActiveArea.getZoomedHitArea().width;
                int tooltipWidth = 0;
                for (DisplayedSlotEdge slotEdge : connectedSlots) {
                    // JIPipeDataSlot sourceSlot = slotEdge.source;
                    int id = edgeIds.get(slotEdge);
                    tooltipWidth = Math.max(tooltipWidth, slotFontMetrics.stringWidth(String.valueOf(id)) + (int) Math.round(zoom * (baseIconSize + baseIconSpacing) + zoom * 8));
                }
                tooltipWidth = Math.min(maxAvailableWidth, tooltipWidth);

                // Generate tooltip
                final int spacing = (int) Math.round(zoom * viewMode.getGridHeight() / 4.0);

                int tooltipX, tooltipY;
                Polygon tooltipPolygon;

                tooltipX = nodeUI.getX() + slotActiveArea.getZoomedHitArea().x;

                if (currentSlot.isInput()) {
                    tooltipY = nodeUI.getY() - tooltipHeight - spacing - (int) Math.round(zoom * 4);

                    tooltipPolygon = new Polygon(new int[]{tooltipX, tooltipX, tooltipX + tooltipWidth, tooltipX + tooltipWidth, tooltipX + tooltipWidth / 2 + spacing, tooltipX + tooltipWidth / 2, tooltipX + tooltipWidth / 2 - spacing},
                            new int[]{tooltipY + tooltipHeight, tooltipY, tooltipY, tooltipY + tooltipHeight, tooltipY + tooltipHeight, tooltipY + tooltipHeight + spacing, tooltipY + tooltipHeight},
                            7);
                } else {
                    tooltipY = nodeUI.getY() + nodeUI.getHeight() + (int) Math.round(zoom * 4) + spacing;

                    tooltipPolygon = new Polygon(new int[]{tooltipX, tooltipX + tooltipWidth / 2 - spacing, tooltipX + tooltipWidth / 2, tooltipX + tooltipWidth / 2 + spacing, tooltipX + tooltipWidth, tooltipX + tooltipWidth, tooltipX},
                            new int[]{tooltipY, tooltipY, tooltipY - spacing, tooltipY, tooltipY, tooltipY + tooltipHeight, tooltipY + tooltipHeight},
                            7);
                }


                if(targetSlot != null) {
                    JIPipeNodeInfo info = targetSlot.getNode().getInfo();
                    g.setPaint(UIUtils.DARK_THEME ? info.getCategory().getDarkFillColor() : info.getCategory().getFillColor());
                }
                else {
                    g.setPaint(smartEdgeSlotBackground);
                }

                g.setStroke(new BasicStroke(2));
                g.fill(tooltipPolygon);
                g.setPaint(nodeUI.getBorderColor());
                g.draw(tooltipPolygon);

                int startY = tooltipY;
                int slotInfoHeight = (int) Math.round(zoom * baseHeight);

                g.setPaint(smartEdgeSlotForeground);


                for (DisplayedSlotEdge slotEdge : connectedSlots) {

                    int id = edgeIds.get(slotEdge);

                    // Draw icon
                    Image icon;

                    if(currentSlot.isInput()) {
                        icon = JIPipe.getDataTypes().getIconFor(slotEdge.getSource().getAcceptedDataType()).getImage();
                    }
                    else {
                        icon = JIPipe.getDataTypes().getIconFor(slotEdge.getTarget().getAcceptedDataType()).getImage();
                    }

                    int iconX = tooltipX + (int) Math.round(zoom * 3);
                    int iconSize = (int) Math.round(baseIconSize * zoom);
                    g.drawImage(icon, iconX, startY + slotInfoHeight / 2 - iconSize / 2, iconSize, iconSize, null);
//                    g.setPaint(new Color(Color.HSBtoRGB(1.0f * id / (edgeIds.size()), 0.5f, 0.8f)));
//                    g.fillOval(iconX, startY + slotInfoHeight / 2 - iconSize / 2, iconSize, iconSize);
//                    g.setPaint(smartEdgeSlotForeground);

                    // Draw text
                    double centerY = startY + slotInfoHeight / 2.0 + (slotFontMetrics.getAscent() - slotFontMetrics.getDescent()) / 2.0;
                    int textX = (int) (tooltipX + (int) Math.round(zoom * 2) + iconSize + baseIconSpacing * zoom);
                    int slotTextY = (int) Math.round(centerY);

                    g.setFont(smartEdgeTooltipSlotFont);
                    g.drawString(String.valueOf(id), textX, slotTextY);

                    startY += slotInfoHeight;
                }


            }
        }

    }

    private double calculateApproximateSlotManhattanDistance(JIPipeDataSlot source, JIPipeDataSlot target) {
        JIPipeGraphNodeUI sourceNode = nodeUIs.get(source.getNode());
        JIPipeGraphNodeUI targetNode = nodeUIs.get(target.getNode());
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
        JIPipeGraphNodeUI sourceUI = displayedSlotEdge.getSourceUI();
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
        for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
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
        JIPipeGraphNodeUI algorithmUI = nodeUIs.getOrDefault(slot.getNode(), null);
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

    public BiMap<JIPipeGraphNode, JIPipeGraphNodeUI> getNodeUIs() {
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
     * @return the set of selected {@link JIPipeGraphNodeUI}
     */
    public Set<JIPipeGraphNodeUI> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * @return the set of selected nodes
     */
    public Set<JIPipeGraphNode> getSelectedNodes() {
        return selection.stream().map(JIPipeGraphNodeUI::getNode).collect(Collectors.toSet());
    }

    /**
     * Clears the list of selected algorithms
     */
    public void clearSelection() {
        selection.clear();
        updateSelection();
    }

    private void updateSelection() {
        updateAnnotationNodeLayers();
        repaint();
        requestFocusInWindow();
        nodeSelectionChangedEventEmitter.emit(new NodeSelectionChangedEvent(this));

        // Update resize handles
        if (selection.size() == 1) {
            JIPipeGraphNodeUI nodeUI = selection.iterator().next();
            if (nodeUI instanceof JIPipeAnnotationGraphNodeUI) {
                currentResizeTarget = (JIPipeAnnotationGraphNodeUI) nodeUI;
            } else {
                currentResizeTarget = null;
            }
        } else {
            currentResizeTarget = null;
        }
    }

    /**
     * Selects only the specified algorithm
     *
     * @param ui The algorithm UI
     */
    public void selectOnly(JIPipeGraphNodeUI ui) {
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
    public void removeFromSelection(JIPipeGraphNodeUI ui) {
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
    public void addToSelection(JIPipeGraphNodeUI ui) {
        selection.add(ui);
        if (!(ui instanceof JIPipeAnnotationGraphNodeUI)) {
            if (getLayer(ui) < currentNodeLayer) {
                setLayer(ui, ++currentNodeLayer);
            }
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
        for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
            minX = Math.min(ui.getX(), minX);
            minY = Math.min(ui.getY(), minY);
        }
        boolean oldModified = getWorkbench().isProjectModified();
        for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
            ui.moveToClosestGridPoint(new Point(ui.getX() - minX + viewMode.getGridWidth(),
                    ui.getY() - minY + viewMode.getGridHeight()), true, save);
        }
        getWorkbench().setProjectModified(oldModified);
        setGraphEditCursor(viewMode.gridToRealLocation(new Point(1, 1), zoom));
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

    public List<NodeUIContextAction> getContextActions() {
        return contextActions;
    }

    public void setContextActions(List<NodeUIContextAction> contextActions) {
        this.contextActions = contextActions;
    }

    public <T extends JIPipeGraphNode> Set<JIPipeGraphNodeUI> getNodeUIsFor(Set<T> nodes) {
        Set<JIPipeGraphNodeUI> uis = new HashSet<>();
        for (T node : nodes) {
            JIPipeGraphNodeUI ui = nodeUIs.getOrDefault(node, null);
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

    public void setGraphEditCursor(Point graphEditCursor) {
        long stamp = stampedLock.writeLock();
        try {
            this.graphEditCursor = graphEditCursor != null ? new Point(graphEditCursor.x, graphEditCursor.y) : new Point();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    public void selectAll() {
        selection.addAll(nodeUIs.values());
        updateSelection();
    }

    public void invertSelection() {
        ImmutableSet<JIPipeGraphNodeUI> originalSelection = ImmutableSet.copyOf(selection);
        selection.clear();
        for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
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
        double oldZoom = this.zoom;
        this.zoom = zoom;

        // Recalculate assets
        updateAssets();

        // Zoom the cursor
        Point cursor = getGraphEditorCursor();
        double normalizedCursorX = cursor.x / oldZoom;
        double normalizedCursorY = cursor.y / oldZoom;
        setGraphEditCursor(new Point((int) Math.round(normalizedCursorX * zoom), (int) Math.round(normalizedCursorY * zoom)));

        // Zoom nodes

        zoomChangedEventEmitter.emit(new ZoomChangedEvent(this));
        for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
            ui.moveToStoredGridLocation(true);
            ui.setZoom(zoom);
        }
        graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
    }

    public GraphCanvasUpdatedEventEmitter getGraphCanvasUpdatedEventEmitter() {
        return graphCanvasUpdatedEventEmitter;
    }

    private void updateAssets() {
        smartEdgeTooltipSlotFont = new Font(Font.DIALOG, Font.BOLD, Math.max(1, (int) Math.round(14 * zoom)));
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
        setZoom(Math.max(0.1, zoom - 0.05));
    }

    public void zoomIn() {
        setZoom(Math.min(3, zoom + 0.05));
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

        if (disposed) {
            return;
        }

        // Update the location of existing nodes
        for (JIPipeGraphNodeUI ui : nodeUIs.values()) {
            ui.moveToStoredGridLocation(true);
        }
        removeOldNodes();
        addNewNodes(true);
        requestFocusInWindow();
    }

    @Override
    public void onNodeConnected(JIPipeGraph.NodeConnectedEvent event) {

        if (disposed) {
            return;
        }

        JIPipeGraphNodeUI sourceNode = nodeUIs.getOrDefault(event.getSource().getNode(), null);
        JIPipeGraphNodeUI targetNode = nodeUIs.getOrDefault(event.getTarget().getNode(), null);

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

            Point cursorBackup = getGraphEditorCursor();
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
    public void onNodeUIActionRequested(JIPipeGraphNodeUI.NodeUIActionRequestedEvent event) {

        if (disposed) {
            return;
        }

        if (event.getAction() instanceof OpenContextMenuAction) {
            if (event.getUi() != null) {
                openContextMenu(getLastMousePosition());
            }
        }
        nodeUIActionRequestedEventEmitter.emit(event);
    }

    public void moveSelection(int gridDx, int gridDy, boolean force) {

        if (selection.isEmpty())
            return;

        int negativeDx = 0;
        int negativeDy = 0;
        for (JIPipeGraphNodeUI nodeUI : selection) {

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

        if (negativeDx < 0 || negativeDy < 0) {
            // Negative expansion
            for (JIPipeGraphNodeUI value : nodeUIs.values()) {
                if (!currentlyDraggedOffsets.containsKey(value)) {
                    Point storedGridLocation = value.getStoredGridLocation();
                    value.moveToGridLocation(new Point(storedGridLocation.x - negativeDx, storedGridLocation.y - negativeDy), true, true);
                }
            }
        }

        for (JIPipeGraphNodeUI nodeUI : selection) {

            if (force || nodeUI.getNode().isUiLocked())
                continue;


            Point newGridLocation = new Point(nodeUI.getStoredGridLocation().x + gridDx, nodeUI.getStoredGridLocation().y + gridDy);

            if (!hasDragSnapshot) {
                // Check if something would change
                if (!Objects.equals(nodeUI.getStoredGridLocation(), newGridLocation)) {
                    createMoveSnapshotIfNeeded();
                }
            }

            nodeUI.moveToGridLocation(newGridLocation, true, true);
        }

        repaintLowLag();
        if (getParent() != null)
            getParent().revalidate();
        graphCanvasUpdatedEventEmitter.emit(new GraphCanvasUpdatedEvent(this));
    }

    public interface NodeUISelectedEventListener {
        void onNodeUISelected(NodeUISelectedEvent event);
    }

    public interface NodeSelectionChangedEventListener {
        void onGraphCanvasNodeSelectionChanged(NodeSelectionChangedEvent event);
    }

    public interface GraphCanvasUpdatedEventListener {
        void onGraphCanvasUpdated(GraphCanvasUpdatedEvent event);
    }

    public static class DisplayedSlotEdge implements Comparable<DisplayedSlotEdge> {
        private final JIPipeDataSlot source;
        private final JIPipeDataSlot target;
        private final JIPipeGraphEdge edge;

        private int multiColorIndex;

        private int multiColorMax;

        private JIPipeGraphNodeUI sourceUI;

        private JIPipeGraphNodeUI targetUI;

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

        public JIPipeGraphNodeUI getSourceUI() {
            return sourceUI;
        }

        public void setSourceUI(JIPipeGraphNodeUI sourceUI) {
            this.sourceUI = sourceUI;
        }

        public JIPipeGraphNodeUI getTargetUI() {
            return targetUI;
        }

        public void setTargetUI(JIPipeGraphNodeUI targetUI) {
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

        private final JIPipeGraphNodeUI nodeUI;
        private boolean addToSelection;

        /**
         * @param nodeUI         the algorithm UI
         * @param addToSelection if the algorithm should be added to the selection
         */
        public NodeUISelectedEvent(JIPipeGraphNodeUI nodeUI, boolean addToSelection) {
            super(nodeUI);
            this.nodeUI = nodeUI;
            this.addToSelection = addToSelection;
        }

        public JIPipeGraphNodeUI getNodeUI() {
            return nodeUI;
        }

        public boolean isAddToSelection() {
            return addToSelection;
        }
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
