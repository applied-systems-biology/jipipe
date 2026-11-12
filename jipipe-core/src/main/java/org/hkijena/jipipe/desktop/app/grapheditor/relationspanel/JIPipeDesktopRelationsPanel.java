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
 * The Relations dock panel (MR 3 of the navigator overhaul, #1358): shows the
 * current selection centrally with its upstream (above) and downstream
 * (below) nodes arranged in BFS layers, navigable by card click, with a
 * back/forward visitation trail whose history is visualized as an opacity
 * ramp on the cards.
 * <p>
 * The graph logic is pure and headless ({@link RelationLayerCalculator},
 * {@link RelationTrail}) and the cards are lightweight node representations
 * ({@link RelationCard}); the panel's only visual factor is the per-card
 * opacity. Rebuilds run on selection-changed events (guarded against the
 * panel's own navigation, so canvas-driven re-centers happen without a loop)
 * and on graph structure changes, and only while the panel is showing;
 * becoming visible catches up with the current selection.
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
     * Opacity of the current-center cards.
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
     * Gap between the rows of the body.
     */
    private static final int ROW_GAP = 8;
    /**
     * Gap between the cards of one row.
     */
    private static final int CARD_GAP = 8;
    /**
     * Padding around the row stack.
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

    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final RelationTrail trail = new RelationTrail(TRAIL_LENGTH);
    private final JToolBar toolBar = new JToolBar();
    private final JButton backButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-previous.png"));
    private final JButton forwardButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-next.png"));
    private final JButton upButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-up.png"));
    private final JButton downButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-down.png"));
    private final JPanel rowsPanel = new JPanel();
    private final JScrollPane bodyScroll = new JScrollPane(rowsPanel);
    private final JPanel placeholderPanel = new JPanel(new GridBagLayout());
    private final JLabel placeholderLabel = new JLabel(EMPTY_SELECTION_PLACEHOLDER);
    /**
     * Debounced rebuild for graph changes (bursts of events, e.g. during
     * pipeline edits, collapse into one rebuild).
     */
    private final Timer structureRebuildTimer = new Timer(REBUILD_DEBOUNCE_MS, e -> rebuildStructureIfShowing());

    /**
     * The node ids the panel currently shows as centers (the canvas
     * selection).
     */
    private Set<String> centerIds = new LinkedHashSet<>();
    private RelationLayerCalculator.Result result;
    /**
     * The canvas nodes of the last computation, keyed by id (resolves cards).
     */
    private Map<String, JIPipeGraphNode> nodesById = new HashMap<>();
    /**
     * The layer keys ("upstream-N" / "downstream-N") the user expanded.
     */
    private final Set<String> expandedLayers = new LinkedHashSet<>();
    /**
     * The upstream/downstream level counts of the previous rebuild: a changed
     * count means a structural change added or removed a layer, so the
     * index-based expansion keys shifted and the expansion state is reset
     * instead of re-attaching to other layers.
     */
    private int previousUpstreamLevelCount = -1;
    private int previousDownstreamLevelCount = -1;

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
        // (guarded against the panel's own navigation). Per-node parameter
        // events are not needed here.
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
     * The toolbar: back/forward (the visitation trail), then up/down (the
     * nearest node in the adjacent layer).
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
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.PAGE_AXIS));
        rowsPanel.setBorder(BorderFactory.createEmptyBorder(BODY_PADDING, BODY_PADDING, BODY_PADDING, BODY_PADDING));
        rowsPanel.setOpaque(false);

        placeholderLabel.setForeground(mutedColor());
        placeholderPanel.add(placeholderLabel);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(bodyScroll, BorderLayout.CENTER);
        applyViewState();
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    /**
     * Navigates to a node (card click, up/down buttons): appends to the
     * visitation trail, then shows the node as the single center and selects
     * + scrolls to it on the canvas. The selection-changed event this fires
     * is recognized by the guard (the centers are already updated).
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
        reveal(nodeId);
    }

    /**
     * Moves one step back in the visitation trail (the trail move is the
     * navigation; nothing is appended). The toolbar is updated
     * unconditionally: also when the target node no longer exists (the
     * reveal is skipped, but the trail state changed).
     */
    private void navigateBack() {
        String nodeId = trail.goBack();
        reveal(nodeId);
        updateToolbar();
    }

    /**
     * Moves one step forward in the visitation trail (the trail move is the
     * navigation; nothing is appended). The toolbar is updated
     * unconditionally: also when the target node no longer exists (the
     * reveal is skipped, but the trail state changed).
     */
    private void navigateForward() {
        String nodeId = trail.goForward();
        reveal(nodeId);
        updateToolbar();
    }

    /**
     * Jumps to the nearest node of the layer adjacent to the center on the
     * upstream side. The upstream levels are ordered furthest first, so the
     * adjacent level is the LAST one; within it, the first entry is the
     * physically nearest node.
     */
    private void navigateToNearestUpstream() {
        if (centerIds.size() != 1 || result == null || result.upstream().isEmpty()) {
            return; // the button is disabled in these states
        }
        navigateTo(result.upstream().get(result.upstream().size() - 1).get(0).id());
    }

    /**
     * Jumps to the nearest node of the layer adjacent to the center on the
     * downstream side. The downstream levels are ordered nearest first, so
     * the adjacent level is the FIRST one; within it, the first entry is the
     * physically nearest node.
     */
    private void navigateToNearestDownstream() {
        if (centerIds.size() != 1 || result == null || result.downstream().isEmpty()) {
            return; // the button is disabled in these states
        }
        navigateTo(result.downstream().get(0).get(0).id());
    }

    /**
     * Shows one node as the single center: the caller has already moved the
     * trail. Recenters the panel first (so the selection-changed event fired
     * below is recognized as the panel's own), then selects + scrolls to the
     * node on the canvas.
     *
     * @param nodeId the node to show (null and gone nodes are ignored)
     */
    private void reveal(String nodeId) {
        JIPipeDesktopGraphNodeUI nodeUI = nodeId != null ? findNodeUI(nodeId) : null;
        if (nodeUI == null) {
            return;
        }
        recenter(Set.of(nodeId));
        graphEditorUI.getSelectionManager().setSelection(Set.of(nodeUI));
        graphEditorUI.scrollToAlgorithm(nodeUI);
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /**
     * Recenters the panel to the given centers: recomputes the layers and
     * rebuilds the rows. Does not touch the trail.
     *
     * @param centerIdCollection the new centers
     */
    private void recenter(Collection<String> centerIdCollection) {
        centerIds = new LinkedHashSet<>(centerIdCollection);
        result = computeResult(centerIds);
        expandedLayers.clear();
        rebuildRows();
        updateToolbar();
    }

    /**
     * Catch-up path (panel shown / created): mirrors the canvas selection.
     * A single-node selection the panel missed while hidden is a navigation
     * (appended to the trail); multi-selections re-center without appending.
     */
    private void refreshFromSelection() {
        Set<String> selectionIds = currentSelectionIds();
        if (selectionIds.size() == 1 && !selectionIds.equals(centerIds)) {
            trail.navigateTo(selectionIds.iterator().next());
        }
        recenter(selectionIds);
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
     * The opacity of one card: the current-center cards are fully opaque,
     * the trail ranks 1 to {@link #RAMP_STEPS} form the ramp, everything
     * else is the related level.
     */
    private float opacityFor(String nodeId) {
        if (centerIds.contains(nodeId)) {
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
        boolean singleCenter = centerIds.size() == 1;
        upButton.setEnabled(singleCenter && result != null && !result.upstream().isEmpty());
        downButton.setEnabled(singleCenter && result != null && !result.downstream().isEmpty());
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void rebuildRows() {
        rowsPanel.removeAll();
        if (result != null) {
            List<List<RelationLayerCalculator.Layer>> upstream = result.upstream();
            List<List<RelationLayerCalculator.Layer>> downstream = result.downstream();
            if (upstream.size() != previousUpstreamLevelCount || downstream.size() != previousDownstreamLevelCount) {
                // The index-based expansion keys refer to other layers now
                expandedLayers.clear();
            }
            previousUpstreamLevelCount = upstream.size();
            previousDownstreamLevelCount = downstream.size();
            for (int i = 0; i < upstream.size(); ++i) {
                addRow(createLayerRow(upstream.get(i), "upstream-" + i));
            }
            addRow(createCenterRow());
            for (int i = 0; i < downstream.size(); ++i) {
                addRow(createLayerRow(downstream.get(i), "downstream-" + i));
            }
            if (upstream.isEmpty() && downstream.isEmpty()) {
                addRow(createNoRelationsHint());
            }
        } else {
            previousUpstreamLevelCount = -1;
            previousDownstreamLevelCount = -1;
        }
        applyViewState();
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private void addRow(JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowsPanel.add(row);
        rowsPanel.add(Box.createVerticalStrut(ROW_GAP));
    }

    /**
     * One layer row: the first {@link #MAX_CARDS_PER_ROW} cards (the
     * calculator already ordered them by ascending physical distance) and,
     * when the layer is larger, a "+N" tile that expands the layer in place.
     */
    private JComponent createLayerRow(List<RelationLayerCalculator.Layer> level, String key) {
        JPanel row = createRowPanel();
        boolean expanded = expandedLayers.contains(key);
        int visibleCount = expanded ? level.size() : Math.min(level.size(), MAX_CARDS_PER_ROW);
        for (int i = 0; i < visibleCount; ++i) {
            RelationCard card = createCard(level.get(i).id());
            if (card != null) {
                row.add(card);
            }
        }
        if (!expanded && level.size() > MAX_CARDS_PER_ROW) {
            row.add(new MoreTile(level.size() - MAX_CARDS_PER_ROW, () -> {
                expandedLayers.add(key);
                rebuildRows();
            }));
        }
        return row;
    }

    private JComponent createCenterRow() {
        JPanel row = createRowPanel();
        for (String id : centerIds) {
            RelationCard card = createCard(id);
            if (card != null) {
                row.add(card);
            }
        }
        return row;
    }

    private JComponent createNoRelationsHint() {
        JLabel label = new JLabel(centerIds.size() == 1 ? NO_RELATIONS_HINT_SINGULAR : NO_RELATIONS_HINT_PLURAL);
        label.setForeground(mutedColor());
        JPanel row = createRowPanel();
        row.add(label);
        return row;
    }

    private JPanel createRowPanel() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, CARD_GAP, 0));
        row.setOpaque(false);
        return row;
    }

    private RelationCard createCard(String nodeId) {
        JIPipeGraphNode node = nodesById.get(nodeId);
        if (node == null) {
            return null;
        }
        return new RelationCard(nodeId, node.getName(), node.getInfo(),
                RelationCardAppearance.of(node.getInfo()),
                opacityFor(nodeId), centerIds.contains(nodeId),
                this::navigateTo);
    }

    /**
     * The no-selection placeholder replaces the rows while nothing is
     * selected.
     */
    private void applyViewState() {
        if (centerIds.isEmpty()) {
            bodyScroll.setViewportView(placeholderPanel);
        } else {
            bodyScroll.setViewportView(rowsPanel);
        }
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
        if (!isShowing()) {
            return; // caught up on componentShown
        }
        Set<String> selectionIds = currentSelectionIds();
        if (selectionIds.equals(centerIds)) {
            // The panel's own navigation fired this event (it already
            // re-centered the panel): do nothing, no loop.
            return;
        }
        if (selectionIds.size() == 1) {
            // A canvas-driven re-center is a navigation; multi-selection
            // re-centers do not append (the trail tracks single centers)
            trail.navigateTo(selectionIds.iterator().next());
        }
        recenter(selectionIds);
    }

    private void rebuildStructureIfShowing() {
        if (!isShowing()) {
            return;
        }
        Set<String> selectionIds = currentSelectionIds();
        if (selectionIds.equals(centerIds)) {
            // The centers are unchanged: refresh the layers (positions and
            // edges may have changed) without touching the trail or the
            // expanded layers.
            result = computeResult(centerIds);
            rebuildRows();
            updateToolbar();
        } else {
            recenter(selectionIds);
        }
    }

    // ------------------------------------------------------------------
    // The "+N" tile
    // ------------------------------------------------------------------

    /**
     * The "+N" tile that expands a collapsed layer in place: a
     * dashed-outline tile with a measured label (repo policy: never assume
     * text sizes). Tiles are recreated on every rebuild, so a changed label
     * always comes with a fresh layout.
     */
    private static final class MoreTile extends JComponent {

        private static final float[] DASH_PATTERN = {4f, 4f};
        private static final int HORIZONTAL_PADDING = 8;
        private static final int VERTICAL_PADDING = 6;

        private final int hiddenCount;
        private final Runnable expandCallback;

        /**
         * @param hiddenCount    how many nodes of the layer are hidden
         * @param expandCallback run when the tile is clicked
         */
        MoreTile(int hiddenCount, Runnable expandCallback) {
            this.hiddenCount = hiddenCount;
            this.expandCallback = expandCallback;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Show " + hiddenCount + " more node" + (hiddenCount == 1 ? "" : "s"));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        expandCallback.run();
                    }
                }
            });
        }

        private String label() {
            return "+ " + hiddenCount + " more";
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(getFont());
            return new Dimension(
                    Math.max(JIPipeDesktopRelationsPanel.MORE_TILE_MIN_WIDTH,
                            2 * HORIZONTAL_PADDING + metrics.stringWidth(label())),
                    Math.max(JIPipeDesktopRelationsPanel.CARD_HEIGHT,
                            metrics.getHeight() + 2 * VERTICAL_PADDING));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int width = getWidth();
                int height = getHeight();
                JIPipeDesktopModernThemeStyle style = ThemeUtils.getCurrentStyle();
                Color muted = style != null ? style.getTextMuted() : new Color(0xa0a0a0);
                Color background = style != null ? style.getPanelBackground() : new Color(0xf2f2f2);
                g2.setPaint(background);
                g2.fillRect(0, 0, width, height);
                g2.setPaint(muted);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10f, DASH_PATTERN, 0f));
                g2.drawRect(0, 0, width - 1, height - 1);
                FontMetrics metrics = g2.getFontMetrics();
                String text = label();
                UIUtils.drawStringVerticallyCentered(g2, text, (width - metrics.stringWidth(text)) / 2,
                        height / 2, metrics);
            } finally {
                g2.dispose();
            }
        }
    }
}
