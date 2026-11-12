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

package org.hkijena.jipipe.desktop.app.grapheditor.relationspanel;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Relations dock panel (MR 3 of the navigator overhaul, #1358): shows a
 * FOCUSED node (the node the displayed layers are built around) centrally
 * with its upstream (above) and downstream (below) nodes arranged in BFS
 * layers, plus a back/forward visitation trail whose history is visualized
 * as an opacity ramp on the cards.
 * <p>
 * Navigation (card click, back/forward, up/down) updates only the CURRENT
 * node - the trail, the canvas selection and the per-card opacity/border
 * repaint; the row layout does not re-position. The "Refocus" toolbar button
 * rebuilds the layers around the current node (focused := current) and is
 * the only user action that re-positions rows; a selection arriving while the
 * panel is empty populates it the same way. The body is one custom-laid-out
 * component without scrollbars: the focused center row is pinned to the
 * vertical middle, rows fill the full width (leftover width distributed
 * proportionally to the cards' preferred widths), rows beyond the body
 * height collapse into "N more above"/"N more below" edge tiles that page the
 * visible window, and full-width horizontal guide lines in the theme border
 * color structure the empty lanes.
 * <p>
 * The graph logic is pure and headless ({@link RelationLayerCalculator},
 * {@link RelationTrail}) and the cards are lightweight node representations
 * ({@link RelationCard}). Recomputes run on graph structure changes (only
 * while the panel is showing; becoming visible catches up).
 */
public class JIPipeDesktopRelationsPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeGraph.GraphChangedEventListener, JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener {

    /**
     * How many nodes the visitation trail remembers.
     */
    public static final int TRAIL_LENGTH = 10;
    /**
     * How many steps back the opacity ramp spans (must match
     * {@link RelationTrail#OPACITY_RAMP_LENGTH}).
     */
    public static final int RAMP_STEPS = 5;
    /**
     * How many cards a collapsed layer row shows before the "+N" tile.
     */
    public static final int MAX_CARDS_PER_ROW = 4;

    /**
     * Opacity of the current cards.
     */
    public static final float OPACITY_CURRENT = 1.0f;
    /**
     * Opacity of the first trail step back (rank 1).
     */
    public static final float OPACITY_TRAIL_FIRST = 0.85f;
    /**
     * Opacity decrease per further trail step.
     */
    public static final float OPACITY_TRAIL_STEP = 0.05f;
    /**
     * Opacity of related (unranked) cards.
     */
    public static final float OPACITY_RELATED = 0.60f;

    /**
     * Card width minimum (the preferred width is measured from the name).
     */
    public static final int CARD_MIN_WIDTH = 90;
    /**
     * Card height minimum (a layout minimum, not a text assumption).
     */
    public static final int CARD_HEIGHT = 58;
    /**
     * "+N" tile width minimum (the preferred width is measured from the
     * label).
     */
    public static final int MORE_TILE_MIN_WIDTH = 44;

    /**
     * Gap between the lanes of the body.
     */
    private static final int ROW_GAP = 8;
    /**
     * Gap between the cards of one row.
     */
    private static final int CARD_GAP = 8;
    /**
     * The lane pitch of the center-pinned body: the vertical distance between
     * the tops of consecutive rows.
     */
    private static final int ROW_PITCH = CARD_HEIGHT + ROW_GAP;
    /**
     * Padding around the body content.
     */
    private static final int BODY_PADDING = 10;
    /**
     * Toolbar flat button size (as the graph editor toolbar buttons).
     */
    private static final int TOOLBAR_BUTTON_SIZE = 32;
    /**
     * Debounce for graph-changed rebuilds (mirrors the Find panel).
     */
    private static final int REBUILD_DEBOUNCE_MS = 1000;

    private static final String EMPTY_SELECTION_PLACEHOLDER = "Select a node to see its relations";
    private static final String NO_RELATIONS_HINT_SINGULAR = "The selected node has no related nodes.";
    private static final String NO_RELATIONS_HINT_PLURAL = "The selected nodes have no related nodes.";
    private static final String MORE_ABOVE_SUFFIX = " more above";
    private static final String MORE_BELOW_SUFFIX = " more below";
    private static final Color FALLBACK_BORDER_COLOR = new Color(0xcfcfcf);

    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final RelationTrail trail = new RelationTrail(TRAIL_LENGTH);
    private final JToolBar toolBar = new JToolBar();
    private final JButton backButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-previous.png"));
    private final JButton forwardButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-next.png"));
    private final JButton refocusButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/target.png"));
    private final JButton upButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-up.png"));
    private final JButton downButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-down.png"));
    private final BodyPanel body = new BodyPanel();
    /**
     * Debounced rebuild for graph changes (bursts of events, e.g. during
     * pipeline edits, collapse into one rebuild).
     */
    private final Timer structureRebuildTimer = new Timer(REBUILD_DEBOUNCE_MS, e -> rebuildStructureIfShowing());

    /**
     * The node ids the displayed layers are built around (the focused
     * centers).
     */
    private Set<String> focusedIds = new LinkedHashSet<>();
    private RelationLayerCalculator.Result focusedResult;
    /**
     * The ids of the currently selected node UIs (usually zero or one; the
     * 2px border and 100% opacity follow these).
     */
    private Set<String> currentIds = new LinkedHashSet<>();
    /**
     * The visible window's displacement in lanes from the center-pinned
     * position: positive reveals more upstream rows, negative more downstream
     * rows. Only edge tile clicks and Refocus change it.
     */
    private int windowShift;
    /**
     * The canvas nodes of the last computation, keyed by id (resolves cards).
     */
    private Map<String, JIPipeGraphNode> nodesById = new HashMap<>();
    /**
     * The row keys ("upstream-N" / "downstream-N") the user expanded
     * horizontally.
     */
    private final Set<String> expandedLayers = new LinkedHashSet<>();
    /**
     * The upstream/downstream level counts of the previous rebuild: a changed
     * count means a structural change added or removed a layer, so the
     * index-based expansion keys shifted and the view state is reset.
     */
    private int previousUpstreamLevelCount = -1;
    private int previousDownstreamLevelCount = -1;
    /**
     * True while the panel applies its own canvas selection: the selection
     * manager emits a transient event for the cleared selection BEFORE the
     * new one, and both must not reach the handler (the transient empty
     * event previously bounced back/forward navigation, see the task report).
     */
    private boolean updatingSelection;

    /**
     * Creates a new Relations panel for the given workbench and graph editor.
     *
     * @param workbench     the desktop workbench
     * @param graphEditorUI the graph editor UI (provides the canvas + event sources)
     */
    public JIPipeDesktopRelationsPanel(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(workbench);
        this.graphEditorUI = graphEditorUI;

        structureRebuildTimer.setRepeats(false);
        initializeToolbar();
        initializeBody();

        // Event subscriptions: graph structure changes (also covers slot and
        // parameter-structure changes; debounced) and selection changes
        // (guarded against the panel's own selection changes). Per-node
        // parameter events are not needed here.
        graphEditorUI.getGraph().getGraphChangedEventEmitter().subscribeWeak(this);
        graphEditorUI.getSelectionManager().getNodeSelectionChangedEventEmitter().subscribeWeak(this);

        // The panel is usually created hidden and misses the events of the
        // hidden period: becoming visible catches up with the current
        // selection.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshFromSelection();
            }
        });

        // Initial content (for the created-visible case)
        refreshFromSelection();
    }

    /**
     * The toolbar: back/forward (the visitation trail), Refocus (re-position
     * the rows around the current node), then up/down (the nearest node in
     * the focused layout's adjacent layers).
     */
    private void initializeToolbar() {
        toolBar.setFloatable(false);
        toolBar.setBorder(null);

        backButton.setToolTipText("Navigate back");
        UIUtils.makeButtonFlatWithSize(backButton, TOOLBAR_BUTTON_SIZE);
        backButton.addActionListener(e -> navigateBack());
        toolBar.add(backButton);

        forwardButton.setToolTipText("Navigate forward");
        UIUtils.makeButtonFlatWithSize(forwardButton, TOOLBAR_BUTTON_SIZE);
        forwardButton.addActionListener(e -> navigateForward());
        toolBar.add(forwardButton);

        refocusButton.setToolTipText("Refocus the panel to the current node");
        UIUtils.makeButtonFlatWithSize(refocusButton, TOOLBAR_BUTTON_SIZE);
        refocusButton.addActionListener(e -> refocus());
        toolBar.add(refocusButton);

        toolBar.addSeparator();

        upButton.setToolTipText("Go to nearest upstream node");
        UIUtils.makeButtonFlatWithSize(upButton, TOOLBAR_BUTTON_SIZE);
        upButton.addActionListener(e -> navigateToNearestUpstream());
        toolBar.add(upButton);

        downButton.setToolTipText("Go to nearest downstream node");
        UIUtils.makeButtonFlatWithSize(downButton, TOOLBAR_BUTTON_SIZE);
        downButton.addActionListener(e -> navigateToNearestDownstream());
        toolBar.add(downButton);
    }

    private void initializeBody() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        // The center-pinned layout depends on the body size: re-layout (and
        // re-sync the edge tiles) on resize.
        body.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutBody();
            }
        });
    }

    // ------------------------------------------------------------------
    // Navigation (updates the CURRENT node only; rows do not re-position)
    // ------------------------------------------------------------------

    /**
     * Navigates to a node (card click, up/down buttons): appends to the
     * visitation trail (no consecutive duplicates) and shows the node as the
     * current node. The rows do not re-position.
     *
     * @param nodeId the node to navigate to
     */
    private void navigateTo(String nodeId) {
        if (nodeId == null) {
            return;
        }
        if (!Objects.equals(nodeId, trail.current())) {
            // No consecutive duplicate: a reveal of the current node (card
            // re-click) keeps the trail untouched so back is never a dead step
            trail.navigateTo(nodeId);
        }
        showNode(nodeId);
    }

    /**
     * Moves one step back in the visitation trail (the trail move is the
     * navigation; nothing is appended). The toolbar is updated
     * unconditionally: also when the target node no longer exists (the show
     * is skipped, but the trail state changed).
     */
    private void navigateBack() {
        showNode(trail.goBack());
        updateToolbar();
    }

    /**
     * Moves one step forward in the visitation trail (the trail move is the
     * navigation; nothing is appended). The toolbar is updated
     * unconditionally: also when the target node no longer exists (the show
     * is skipped, but the trail state changed).
     */
    private void navigateForward() {
        showNode(trail.goForward());
        updateToolbar();
    }

    /**
     * Jumps to the nearest node of the layer adjacent to the focused center
     * on the upstream side. The upstream levels are ordered furthest first,
     * so the adjacent level is the LAST one; within it, the first entry is
     * the physically nearest node.
     */
    private void navigateToNearestUpstream() {
        if (focusedIds.size() != 1 || focusedResult == null || focusedResult.upstream().isEmpty()) {
            return; // the button is disabled in these states
        }
        navigateTo(focusedResult.upstream().get(focusedResult.upstream().size() - 1).get(0).id());
    }

    /**
     * Jumps to the nearest node of the layer adjacent to the focused center
     * on the downstream side. The downstream levels are ordered nearest
     * first, so the adjacent level is the FIRST one; within it, the first
     * entry is the physically nearest node.
     */
    private void navigateToNearestDownstream() {
        if (focusedIds.size() != 1 || focusedResult == null || focusedResult.downstream().isEmpty()) {
            return; // the button is disabled in these states
        }
        navigateTo(focusedResult.downstream().get(0).get(0).id());
    }

    /**
     * Shows one node as the current node: updates the current set, selects +
     * scrolls to it on the canvas and repaints the card states. Does not
     * touch the rows' layout.
     *
     * @param nodeId the node to show (null and gone nodes are ignored)
     */
    private void showNode(String nodeId) {
        JIPipeDesktopGraphNodeUI nodeUI = nodeId != null ? findNodeUI(nodeId) : null;
        if (nodeUI == null) {
            return;
        }
        updateCurrent(Set.of(nodeId));
        setCanvasSelection(nodeUI);
        graphEditorUI.scrollToAlgorithm(nodeUI);
    }

    /**
     * Applies the panel's own canvas selection. The selection manager emits
     * a selection-changed event for the cleared selection BEFORE the new one;
     * both are swallowed by the reentrancy guard.
     *
     * @param nodeUI the node to select
     */
    private void setCanvasSelection(JIPipeDesktopGraphNodeUI nodeUI) {
        updatingSelection = true;
        try {
            graphEditorUI.getSelectionManager().setSelection(Set.of(nodeUI));
        } finally {
            updatingSelection = false;
        }
    }

    // ------------------------------------------------------------------
    // Focus (the only actions that re-position the rows)
    // ------------------------------------------------------------------

    /**
     * Refocuses the panel to the current node(s): rebuilds the layers around
     * them and re-centers the visible window.
     */
    private void refocus() {
        if (currentIds.isEmpty()) {
            return; // the button is disabled in this state
        }
        refocusAround(currentIds);
    }

    private void refocusAround(Collection<String> ids) {
        focusedIds = new LinkedHashSet<>(ids);
        windowShift = 0;
        expandedLayers.clear();
        recomputeFocused();
    }

    /**
     * Recomputes the layers around the focused centers and rebuilds the rows.
     */
    private void recomputeFocused() {
        focusedResult = computeResult(focusedIds);
        int upstreamCount = focusedResult.upstream().size();
        int downstreamCount = focusedResult.downstream().size();
        if (upstreamCount != previousUpstreamLevelCount || downstreamCount != previousDownstreamLevelCount) {
            // The lane indexes shifted: reset the view state that refers to
            // lanes
            expandedLayers.clear();
            windowShift = 0;
        }
        previousUpstreamLevelCount = upstreamCount;
        previousDownstreamLevelCount = downstreamCount;
        rebuildRows();
        updateToolbar();
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /**
     * Updates the current node set and repaints the card states (opacity +
     * current border). Does not rebuild the rows.
     *
     * @param ids the new current ids
     */
    private void updateCurrent(Collection<String> ids) {
        currentIds = new LinkedHashSet<>(ids);
        refreshCardStates();
        updateToolbar();
    }

    /**
     * Applies the current opacity/border to the existing cards without
     * rebuilding them.
     */
    private void refreshCardStates() {
        for (Component component : body.getComponents()) {
            if (component instanceof RelationCard) {
                RelationCard card = (RelationCard) component;
                card.setCurrent(currentIds.contains(card.getNodeId()));
                card.setOpacity(opacityFor(card.getNodeId()));
            }
        }
    }

    /**
     * Catch-up path (panel shown / created): mirrors the canvas selection.
     * A single-node selection the panel missed while hidden is a navigation
     * (appended to the trail); multi-selections update the current set
     * without appending. An empty panel populates around the selection.
     */
    private void refreshFromSelection() {
        Set<String> selectionIds = currentSelectionIds();
        if (selectionIds.size() == 1 && !selectionIds.equals(currentIds)) {
            trail.navigateTo(selectionIds.iterator().next());
        }
        updateCurrent(selectionIds);
        if (focusedIds.isEmpty()) {
            if (!selectionIds.isEmpty()) {
                refocusAround(currentIds);
            }
        } else {
            recomputeFocused();
        }
    }

    /**
     * Computes the layering around the given centers from the canvas:
     * positions are the node UI centers, the adjacency comes from the graph's
     * slot edges (source slot feeds target slot). Annotation nodes are
     * skipped; the calculator traverses through ids without positions and
     * never lists them.
     *
     * @param centerIdSet the centers
     * @return the layering
     */
    private RelationLayerCalculator.Result computeResult(Set<String> centerIdSet) {
        JIPipeGraph graph = graphEditorUI.getGraph();
        Map<String, RelationLayerCalculator.NodePos> nodes = new HashMap<>();
        nodesById = new HashMap<>();
        for (Map.Entry<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> entry : graphEditorUI.getCanvasUI().getNodeUIs().entrySet()) {
            JIPipeGraphNode node = entry.getKey();
            if (node instanceof JIPipeGraphCanvasNote) {
                continue;
            }
            String id = node.getUUIDInParentGraph().toString();
            JIPipeDesktopGraphNodeUI nodeUI = entry.getValue();
            nodes.put(id, new RelationLayerCalculator.NodePos(id,
                    nodeUI.getX() + nodeUI.getWidth() / 2.0,
                    nodeUI.getY() + nodeUI.getHeight() / 2.0));
            nodesById.put(id, node);
        }
        Map<String, List<String>> downstreamAdjacency = new HashMap<>();
        Map<String, List<String>> upstreamAdjacency = new HashMap<>();
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
            JIPipeGraphNode source = edge.getKey().getNode();
            JIPipeGraphNode target = edge.getValue().getNode();
            if (source == null || target == null) {
                continue;
            }
            String sourceId = source.getUUIDInParentGraph().toString();
            String targetId = target.getUUIDInParentGraph().toString();
            downstreamAdjacency.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(targetId);
            upstreamAdjacency.computeIfAbsent(targetId, k -> new ArrayList<>()).add(sourceId);
        }
        return RelationLayerCalculator.compute(centerIdSet, nodes, downstreamAdjacency, upstreamAdjacency);
    }

    /**
     * The ids of the currently selected node UIs (the selection may contain
     * other object types, which are filtered out).
     */
    private Set<String> currentSelectionIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (JIPipeDesktopGraphInteractiveObjectUI objectUI : graphEditorUI.getSelectionManager().getSelection()) {
            if (objectUI instanceof JIPipeDesktopGraphNodeUI) {
                ids.add(((JIPipeDesktopGraphNodeUI) objectUI).getNode().getUUIDInParentGraph().toString());
            }
        }
        return ids;
    }

    private JIPipeDesktopGraphNodeUI findNodeUI(String nodeId) {
        for (Map.Entry<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> entry : graphEditorUI.getCanvasUI().getNodeUIs().entrySet()) {
            if (entry.getKey().getUUIDInParentGraph().toString().equals(nodeId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * The ids that still exist on the canvas (used to drop stale ids after
     * structural changes).
     */
    private Set<String> retainExisting(Collection<String> ids) {
        Set<String> result = new LinkedHashSet<>();
        for (String id : ids) {
            if (findNodeUI(id) != null) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * The opacity of one card: the current cards are fully opaque, the trail
     * ranks 1 to {@link #RAMP_STEPS} form the ramp, everything else is the
     * related level.
     */
    private float opacityFor(String nodeId) {
        if (currentIds.contains(nodeId)) {
            return OPACITY_CURRENT;
        }
        int rank = trail.opacityRank(nodeId);
        if (rank >= 1 && rank <= RAMP_STEPS) {
            return OPACITY_TRAIL_FIRST - OPACITY_TRAIL_STEP * (rank - 1);
        }
        return OPACITY_RELATED;
    }

    private void updateToolbar() {
        backButton.setEnabled(trail.canGoBack());
        forwardButton.setEnabled(trail.canGoForward());
        refocusButton.setEnabled(!currentIds.isEmpty());
        boolean singleFocus = focusedIds.size() == 1;
        upButton.setEnabled(singleFocus && focusedResult != null && !focusedResult.upstream().isEmpty());
        downButton.setEnabled(singleFocus && focusedResult != null && !focusedResult.downstream().isEmpty());
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void rebuildRows() {
        body.setRows(buildRowModel());
        relayoutBody();
    }

    /**
     * Builds the row model of the focused layout: upstream levels (furthest
     * first) as negative slots, the focused center row at slot 0, downstream
     * levels (nearest first) as positive slots.
     */
    private List<BodyRow> buildRowModel() {
        List<BodyRow> rows = new ArrayList<>();
        if (focusedResult == null) {
            return rows;
        }
        List<List<RelationLayerCalculator.Layer>> upstream = focusedResult.upstream();
        for (int i = 0; i < upstream.size(); ++i) {
            rows.add(new BodyRow(i - upstream.size(), createRowItems(upstream.get(i), "upstream-" + i)));
        }
        rows.add(new BodyRow(0, createCenterItems()));
        List<List<RelationLayerCalculator.Layer>> downstream = focusedResult.downstream();
        for (int i = 0; i < downstream.size(); ++i) {
            rows.add(new BodyRow(i + 1, createRowItems(downstream.get(i), "downstream-" + i)));
        }
        return rows;
    }

    /**
     * One layer row's items: the first {@link #MAX_CARDS_PER_ROW} cards (the
     * calculator already ordered them by ascending physical distance) and,
     * when the layer is larger, a "+N" tile that expands the layer in place.
     */
    private List<JComponent> createRowItems(List<RelationLayerCalculator.Layer> level, String key) {
        List<JComponent> items = new ArrayList<>();
        boolean expanded = expandedLayers.contains(key);
        int visibleCount = expanded ? level.size() : Math.min(level.size(), MAX_CARDS_PER_ROW);
        for (int i = 0; i < visibleCount; ++i) {
            RelationCard card = createCard(level.get(i).id());
            if (card != null) {
                items.add(card);
            }
        }
        if (!expanded && level.size() > MAX_CARDS_PER_ROW) {
            int hiddenCount = level.size() - MAX_CARDS_PER_ROW;
            DashTile tile = new DashTile("+ " + hiddenCount + " more", () -> {
                expandedLayers.add(key);
                rebuildRows();
            });
            tile.setToolTipText("Show " + hiddenCount + " more node" + (hiddenCount == 1 ? "" : "s"));
            items.add(tile);
        }
        return items;
    }

    private List<JComponent> createCenterItems() {
        List<JComponent> items = new ArrayList<>();
        for (String id : focusedIds) {
            RelationCard card = createCard(id);
            if (card != null) {
                items.add(card);
            }
        }
        return items;
    }

    private RelationCard createCard(String nodeId) {
        JIPipeGraphNode node = nodesById.get(nodeId);
        if (node == null) {
            return null;
        }
        return new RelationCard(nodeId, node.getName(), node.getInfo(),
                RelationCardAppearance.of(node.getInfo()),
                opacityFor(nodeId), currentIds.contains(nodeId),
                this::navigateTo);
    }

    /**
     * Re-syncs the edge tiles with the geometry and re-runs the body layout.
     */
    private void relayoutBody() {
        body.syncEdgeTiles();
        body.revalidate();
        body.repaint();
    }

    private Color mutedColor() {
        JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
        if (style != null) {
            return style.getTextMuted();
        }
        Color color = UIManager.getColor("Label.disabledForeground");
        return color != null ? color : new Color(0x9a9a9a);
    }

    // ------------------------------------------------------------------
    // Event reactions
    // ------------------------------------------------------------------

    @Override
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        if (!isShowing()) {
            return; // caught up on componentShown
        }
        structureRebuildTimer.restart();
    }

    @Override
    public void onGraphCanvasNodeSelectionChanged(JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent event) {
        if (updatingSelection) {
            // The panel's own selection change (including the transient
            // empty-selection emission of setSelection): no echo, no churn
            return;
        }
        if (!isShowing()) {
            return; // caught up on componentShown
        }
        Set<String> selectionIds = currentSelectionIds();
        if (selectionIds.equals(currentIds)) {
            // No change for the current node set: no trail churn
            return;
        }
        boolean wasEmpty = focusedIds.isEmpty();
        updateCurrent(selectionIds);
        if (selectionIds.size() == 1) {
            // A canvas-driven selection of a single node is a navigation;
            // multi-selections do not append (the trail tracks single centers)
            trail.navigateTo(selectionIds.iterator().next());
        }
        if (wasEmpty && !selectionIds.isEmpty()) {
            // The initial populate: focus the current selection (not a
            // navigation of an existing layout)
            refocusAround(currentIds);
        }
    }

    private void rebuildStructureIfShowing() {
        if (!isShowing()) {
            return;
        }
        currentIds = retainExisting(currentIds);
        if (focusedIds.isEmpty()) {
            refreshCardStates();
            updateToolbar();
            return;
        }
        if (retainExisting(focusedIds).size() != focusedIds.size()) {
            // The focused node disappeared: rebuild around the current node
            // if possible, else clear to the empty state
            if (!currentIds.isEmpty()) {
                refocusAround(currentIds);
            } else {
                focusedIds = new LinkedHashSet<>();
                focusedResult = null;
                rebuildRows();
                updateToolbar();
            }
            return;
        }
        recomputeFocused();
    }

    // ------------------------------------------------------------------
    // The dashed tiles ("+N" per row and the body edge tiles)
    // ------------------------------------------------------------------

    /**
     * A dashed-outline tile with a measured label (repo policy: never assume
     * text sizes): the per-row "+N more" expansion tile and the body's "N
     * more above/below" paging tiles share this component.
     */
    private static final class DashTile extends JComponent {

        private static final float[] DASH_PATTERN = {4f, 4f};
        private static final int HORIZONTAL_PADDING = 8;
        private static final int VERTICAL_PADDING = 6;
        private static final Color FALLBACK_MUTED_COLOR = new Color(0xa0a0a0);
        private static final Color FALLBACK_TILE_BACKGROUND = new Color(0xf2f2f2);

        private String label;
        private final Runnable clickCallback;

        /**
         * @param label         the measured label
         * @param clickCallback run when the tile is clicked
         */
        DashTile(String label, Runnable clickCallback) {
            this.label = label;
            this.clickCallback = clickCallback;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        clickCallback.run();
                    }
                }
            });
        }

        /**
         * Sets the label and repaints. The caller revalidates the container
         * (the preferred size depends on the measured label).
         *
         * @param label the new label
         */
        void setLabel(String label) {
            this.label = label;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());
            return new Dimension(
                    Math.max(JIPipeDesktopRelationsPanel.MORE_TILE_MIN_WIDTH,
                            2 * HORIZONTAL_PADDING + metrics.stringWidth(label)),
                    Math.max(JIPipeDesktopRelationsPanel.CARD_HEIGHT,
                            metrics.getHeight() + 2 * VERTICAL_PADDING));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int width = getWidth();
                int height = getHeight();
                JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
                Color muted = style != null ? style.getTextMuted() : FALLBACK_MUTED_COLOR;
                Color background = style != null ? style.getPanelBackground() : FALLBACK_TILE_BACKGROUND;
                g2.setPaint(background);
                g2.fillRect(0, 0, width, height);
                g2.setPaint(muted);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10f, DASH_PATTERN, 0f));
                g2.drawRect(0, 0, width - 1, height - 1);
                FontMetrics metrics = g2.getFontMetrics();
                UIUtils.drawStringVerticallyCentered(g2, label, (width - metrics.stringWidth(label)) / 2,
                        height / 2, metrics);
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------
    // The center-pinned body
    // ------------------------------------------------------------------

    /**
     * One row of the center-pinned layout: its lane slot relative to the
     * focused center row (0 = center row, negative = upstream, positive =
     * downstream) and its items (cards + optional "+N" tile).
     */
    private static final class BodyRow {
        private final int slot;
        private final List<JComponent> items;

        private BodyRow(int slot, List<JComponent> items) {
            this.slot = slot;
            this.items = items;
        }
    }

    /**
     * The geometry of the center-pinned layout for one layout pass.
     */
    private record BodyGeometry(int centerTop, boolean topTile, boolean bottomTile,
                                int hiddenAbove, int hiddenBelow) {
    }

    /**
     * The custom-laid-out body: the focused center row is pinned to the
     * vertical middle, upstream rows stack upward and downstream rows
     * downward with a fixed lane pitch, rows fill the full width (leftover
     * width distributed proportionally to the cards' preferred widths, tiles
     * keep their measured width), and rows beyond the body height collapse
     * into the edge tiles. No scrollbars: the edge tiles page the visible
     * window. paintComponent draws the horizontal guide lines (theme border
     * color) under the cards and the placeholder/hint texts.
     */
    private final class BodyPanel extends JComponent {

        private final List<BodyRow> rows = new ArrayList<>();
        private DashTile topEdgeTile;
        private DashTile bottomEdgeTile;

        /**
         * Replaces the row model (and its children).
         *
         * @param newRows the new rows
         */
        void setRows(List<BodyRow> newRows) {
            rows.clear();
            removeAll();
            for (BodyRow row : newRows) {
                rows.add(row);
                for (JComponent item : row.items) {
                    add(item);
                }
            }
        }

        /**
         * The top y of the focused center row for the current window shift:
         * the centered position plus the shift in lanes (a positive shift
         * moves the content down, revealing more upstream rows).
         */
        private int centerTop() {
            int centeredTop = Math.max(0, (getHeight() - CARD_HEIGHT) / 2);
            return centeredTop + windowShift * ROW_PITCH;
        }

        /**
         * The geometry of the current state: which edge tiles are needed and
         * how many rows they hide (including rows displaced by the tiles'
         * slots).
         */
        private BodyGeometry geometry() {
            int centerTop = centerTop();
            int height = getHeight();
            boolean topTile = false;
            boolean bottomTile = false;
            for (BodyRow row : rows) {
                int y = centerTop + row.slot * ROW_PITCH;
                if (y < 0 || y + CARD_HEIGHT > height) {
                    if (row.slot < 0) {
                        topTile = true;
                    } else if (row.slot > 0) {
                        bottomTile = true;
                    }
                }
            }
            int hiddenAbove = 0;
            int hiddenBelow = 0;
            if (topTile) {
                // Rows out of view or overlapping the top tile's slot
                for (BodyRow row : rows) {
                    if (row.slot >= 0) {
                        continue;
                    }
                    int y = centerTop + row.slot * ROW_PITCH;
                    if (y < CARD_HEIGHT) {
                        ++hiddenAbove;
                    }
                }
            }
            if (bottomTile) {
                // Rows out of view or overlapping the bottom tile's slot
                for (BodyRow row : rows) {
                    if (row.slot <= 0) {
                        continue;
                    }
                    int y = centerTop + row.slot * ROW_PITCH;
                    if (y + CARD_HEIGHT > height - CARD_HEIGHT) {
                        ++hiddenBelow;
                    }
                }
            }
            return new BodyGeometry(centerTop, topTile, bottomTile, hiddenAbove, hiddenBelow);
        }

        /**
         * Creates, updates or removes the edge tiles for the current
         * geometry (called before every revalidation).
         */
        private void syncEdgeTiles() {
            BodyGeometry geo = geometry();
            if (geo.topTile()) {
                if (topEdgeTile == null) {
                    topEdgeTile = new DashTile("", () -> shiftWindow(1));
                    add(topEdgeTile);
                }
                topEdgeTile.setLabel(geo.hiddenAbove() + MORE_ABOVE_SUFFIX);
            } else if (topEdgeTile != null) {
                remove(topEdgeTile);
                topEdgeTile = null;
            }
            if (geo.bottomTile()) {
                if (bottomEdgeTile == null) {
                    bottomEdgeTile = new DashTile("", () -> shiftWindow(-1));
                    add(bottomEdgeTile);
                }
                bottomEdgeTile.setLabel(geo.hiddenBelow() + MORE_BELOW_SUFFIX);
            } else if (bottomEdgeTile != null) {
                remove(bottomEdgeTile);
                bottomEdgeTile = null;
            }
        }

        /**
         * Slides the visible window by one lane in the given direction
         * (positive reveals more upstream rows) and re-layouts.
         *
         * @param delta the lane delta
         */
        private void shiftWindow(int delta) {
            windowShift += delta;
            relayoutBody();
        }

        @Override
        public Dimension getPreferredSize() {
            // The body fills the dock panel area (no scrolling)
            return new Dimension(CARD_MIN_WIDTH, 2 * ROW_PITCH + CARD_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                paintGuideLines(g2);
                if (focusedIds.isEmpty()) {
                    paintCenteredText(g2, EMPTY_SELECTION_PLACEHOLDER);
                } else if (focusedResult != null && focusedResult.upstream().isEmpty()
                        && focusedResult.downstream().isEmpty()) {
                    paintNoRelationsHint(g2);
                }
            } finally {
                g2.dispose();
            }
        }

        /**
         * The horizontal guide lines: one 1px line per lane pitch across the
         * visible height, in the theme's border color, moving with the
         * center-pinned layout (painted under the cards).
         */
        private void paintGuideLines(Graphics2D g2) {
            JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
            g2.setPaint(style != null ? style.getBorderColor() : FALLBACK_BORDER_COLOR);
            int height = getHeight();
            // The lane separators sit midway in the gap between consecutive
            // lanes
            double firstLine = centerTop() - ROW_GAP / 2.0;
            int pitch = ROW_PITCH;
            int minK = (int) Math.ceil((0 - firstLine) / pitch);
            int maxK = (int) Math.floor((height - firstLine) / pitch);
            for (int k = minK; k <= maxK; ++k) {
                int y = (int) Math.round(firstLine + k * (double) pitch);
                g2.fillRect(0, y, getWidth(), 1);
            }
        }

        private void paintCenteredText(Graphics2D g2, String text) {
            FontMetrics metrics = g2.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(text)) / 2;
            g2.setPaint(mutedColor());
            UIUtils.drawStringVerticallyCentered(g2, text, x, getHeight() / 2, metrics);
        }

        /**
         * The "no related nodes" hint: painted into the first lane below the
         * focused center row (that lane is free when there are no related
         * nodes).
         */
        private void paintNoRelationsHint(Graphics2D g2) {
            String text = focusedIds.size() == 1 ? NO_RELATIONS_HINT_SINGULAR : NO_RELATIONS_HINT_PLURAL;
            int y = centerTop() + ROW_PITCH;
            if (y < 0 || y + CARD_HEIGHT > getHeight()) {
                return;
            }
            FontMetrics metrics = g2.getFontMetrics();
            g2.setPaint(mutedColor());
            UIUtils.drawStringVerticallyCentered(g2, text, BODY_PADDING,
                    y + CARD_HEIGHT / 2, metrics);
        }

        @Override
        public void doLayout() {
            BodyGeometry geo = geometry();
            int height = getHeight();
            int innerX = BODY_PADDING;
            int innerWidth = Math.max(0, getWidth() - 2 * BODY_PADDING);
            for (BodyRow row : rows) {
                int y = geo.centerTop() + row.slot * ROW_PITCH;
                boolean visible = y >= 0 && y + CARD_HEIGHT <= height
                        && !(geo.topTile() && y < CARD_HEIGHT)
                        && !(geo.bottomTile() && y + CARD_HEIGHT > height - CARD_HEIGHT);
                if (!visible) {
                    // Hidden rows (or rows displaced by an edge tile) paint
                    // nowhere; the edge tile represents them
                    for (JComponent item : row.items) {
                        item.setBounds(0, 0, 0, 0);
                    }
                    continue;
                }
                layoutRow(row.items, y, innerX, innerWidth);
            }
            if (topEdgeTile != null) {
                topEdgeTile.setBounds(innerX, 0, innerWidth, CARD_HEIGHT);
            }
            if (bottomEdgeTile != null) {
                bottomEdgeTile.setBounds(innerX, height - CARD_HEIGHT, innerWidth, CARD_HEIGHT);
            }
        }

        /**
         * The masonry row layout: every card gets at least its measured
         * preferred width and the leftover (or missing) width is distributed
         * proportionally to the cards' preferred widths, so the row always
         * fills the full body width without a horizontal scrollbar. Tiles
         * keep their own measured width (they do not stretch).
         *
         * @param items the row items (cards + optional tile)
         * @param y     the row's top y
         * @param x     the row's left x
         * @param width the available row width
         */
        private void layoutRow(List<JComponent> items, int y, int x, int width) {
            int count = items.size();
            if (count == 0 || width <= 0) {
                return;
            }
            int[] preferredWidths = new int[count];
            int stretchableCount = 0;
            long stretchableTotal = 0;
            long fixedTotal = 0;
            for (int i = 0; i < count; ++i) {
                preferredWidths[i] = items.get(i).getPreferredSize().width;
                if (items.get(i) instanceof RelationCard) {
                    ++stretchableCount;
                    stretchableTotal += preferredWidths[i];
                } else {
                    fixedTotal += preferredWidths[i];
                }
            }
            int available = width - (count - 1) * CARD_GAP - (int) fixedTotal;
            int xCursor = x;
            for (int i = 0; i < count; ++i) {
                JComponent item = items.get(i);
                int itemWidth;
                if (item instanceof RelationCard) {
                    if (stretchableTotal > 0) {
                        itemWidth = (int) Math.round(available * (double) preferredWidths[i] / stretchableTotal);
                    } else {
                        itemWidth = available / Math.max(1, stretchableCount);
                    }
                    itemWidth = Math.max(1, itemWidth);
                } else {
                    itemWidth = preferredWidths[i];
                }
                int itemHeight = item.getPreferredSize().height;
                item.setBounds(xCursor, y, itemWidth, itemHeight);
                xCursor += itemWidth + CARD_GAP;
            }
        }
    }
}
