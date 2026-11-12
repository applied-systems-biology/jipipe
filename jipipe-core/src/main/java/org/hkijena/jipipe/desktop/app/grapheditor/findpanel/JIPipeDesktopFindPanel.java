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

package org.hkijena.jipipe.desktop.app.grapheditor.findpanel;

import com.google.common.collect.BiMap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.api.nodes.database.entries.ExistingPipelineNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUIUpdatedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUIUpdatedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.icons.ColoredCircleIcon;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopNodeDatabaseEntryListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.DefaultTextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * The Find dock panel (MR 2 of the navigator overhaul, #803): a searchable
 * tree of all project nodes, grouped by the canvas notes that claim them,
 * replacing the Bookmarks panel.
 * <p>
 * The main area is adaptive (a pure function of the dock width), mirroring
 * {@link org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.JIPipeDesktopNodeBrowser}:
 * <ul>
 * <li>compact (below {@link #COMPACT_TIER_WIDTH}): the group tree only;</li>
 * <li>comfortable: the group tree | "In group" member list side by side. The
 * right pane lists the members of the group that contains the current tree
 * selection (the ungrouped members when the selection is ungrouped).</li>
 * </ul>
 * Tree content (spec section 3): canvas notes
 * ({@code DefaultTextBoxGraphCanvasNote}) form groups ordered by their
 * top-left canvas position, "Ungrouped" last with neutral colors, and - while
 * a search is active - one group per other compartment appended after the
 * current-canvas groups. A synthetic "All" group (every entry of the tree)
 * sits on top and is pre-selected when no other selection can be restored.
 * Canvas notes never appear as entries. Single click only selects a row - a
 * group shows its entries in the "In group" pane, a node shows the members of
 * its containing group; double-click navigates: nodes on the current canvas
 * are selected and scrolled to, nodes of other compartments open (or focus)
 * their compartment's editor tab first.
 * <p>
 * The grouping geometry comes from {@link NoteGroupingCalculator} (unit
 * tested); this component only renders it and reacts to graph, parameter and
 * canvas events (subscriptions mirror
 * {@link org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel}).
 * Event-driven rebuilds are debounced and only run while the panel is showing;
 * becoming visible triggers one rebuild so the tree is never stale from the
 * hidden period.
 */
public class JIPipeDesktopFindPanel extends JIPipeDesktopWorkbenchPanel implements JIPipeGraph.GraphChangedEventListener, JIPipeParameterCollection.ParameterChangedEventListener, JIPipeDesktopGraphCanvasUIUpdatedEventListener {

    /**
     * Panels narrower than this are in the compact tier (tree only). Mirrors
     * {@code NodeBrowserModel.COMPACT_TIER_MAX_WIDTH}.
     */
    static final int COMPACT_TIER_WIDTH = 380;
    /**
     * Neutral fill for the "Ungrouped" group and for groups whose note carries
     * no usable colors.
     */
    static final Color UNGROUPED_FILL = new Color(0xe8e8e8);
    /**
     * Neutral border for the "Ungrouped" group and for groups whose note
     * carries no usable colors.
     */
    static final Color UNGROUPED_BORDER = new Color(0xc4c4c4);

    /**
     * Coalesces event storms (graph changes fire continuously during drags)
     * into one rebuild. Mirrors the reload timer of
     * {@link org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel}.
     */
    private static final int REBUILD_DEBOUNCE_MS = 1000;
    /**
     * Debounce for the filter bar changes (the search field debounces
     * internally, mirroring
     * {@code JIPipeDesktopSearchTextField.SEARCH_DEBOUNCE_DELAY}).
     */
    private static final int SEARCH_DEBOUNCE_MS = 150;
    /**
     * Group titles longer than this many characters are truncated for display.
     */
    private static final int GROUP_TITLE_MAX_CHARS = 40;
    /**
     * Corner radius of the framework / spatial chips (mirrors the Add-nodes
     * tree renderer).
     */
    private static final int CHIP_ARC = 6;
    /**
     * Font size of the framework / spatial chips (mirrors the Add-nodes tree
     * renderer).
     */
    private static final float CHIP_FONT_SIZE = 9f;
    /**
     * Flat size of the toolbar buttons.
     */
    private static final int TOOLBAR_BUTTON_SIZE = 28;
    /**
     * Corner radius of the compartment badge.
     */
    private static final int BADGE_CORNER_RADIUS = 8;
    /**
     * Font size reduction of the compartment badge relative to the row font.
     */
    private static final float BADGE_FONT_SIZE_REDUCTION = 2f;
    /**
     * Minimum font size of the compartment badge.
     */
    private static final float BADGE_MIN_FONT_SIZE = 9f;
    /**
     * Font size of the "In group" pane header.
     */
    private static final float IN_GROUP_HEADER_FONT_SIZE = 10f;
    /**
     * Fill of the compartment group styling (prototype: purple badge).
     */
    private static final Color COMPARTMENT_GROUP_FILL = new Color(0xf0e8fa);
    /**
     * Border of the compartment group styling (prototype: purple badge).
     */
    private static final Color COMPARTMENT_GROUP_BORDER = new Color(0xd8c6f2);
    /**
     * Text color of the compartment group styling and compartment badges
     * (prototype: purple badge).
     */
    private static final Color COMPARTMENT_TEXT_COLOR = new Color(0x6b4fa0);
    /**
     * Dimmed text color (UUID fragments, member counts).
     */
    private static final Color DIM_TEXT_COLOR = new Color(0x9a9a9a);
    /**
     * ID of the "Ungrouped" group row (in-memory only; groups the expansion
     * state across rebuilds).
     */
    private static final String UNGROUPED_GROUP_ID = "__ungrouped";
    /**
     * ID of the synthetic "All" group row (in-memory only; groups the
     * expansion and selection state across rebuilds).
     */
    private static final String GROUP_ID_ALL = "__all";
    /**
     * Gap between a group's title and its member count.
     */
    private static final int GROUP_COUNT_GAP = 8;
    private static final int TIER_COMPACT = 0;
    private static final int TIER_COMFORTABLE = 1;
    /**
     * Divider weight of the comfortable tier's split pane (mirrors the Add
     * nodes panel).
     */
    private static final double COMFORTABLE_RESIZE_WEIGHT = 0.35;

    private final JIPipeDesktopGraphEditorUI graphEditorUI;

    // --- Toolbar ---
    private final JToolBar toolBar = new JToolBar();
    private final JIPipeDesktopSearchTextField searchField = new JIPipeDesktopSearchTextField();

    // --- Filter bar (the Add-nodes filter facets; empty selection = disabled) ---
    private final JPanel filterBar = new JPanel();
    private final JComboBox<Object> frameworkCombo = new JComboBox<>();
    private JIPipeDesktopRichSelectionBox dataTypeBox;
    private JIPipeDesktopRichSelectionBox tagsBox;
    private final JToggleButton pinnedOnlyButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));
    private String filterFrameworkId;
    private final Set<Class<? extends JIPipeData>> filterInputDataTypes = new LinkedHashSet<>();
    private final Set<Class<? extends JIPipeData>> filterOutputDataTypes = new LinkedHashSet<>();
    private final Set<String> filterTags = new LinkedHashSet<>();

    // --- Body (tree + "In group" pane, tier-aware) ---
    private final JPanel bodyPanel = new JPanel();
    private final DefaultTreeModel treeModel;
    private final JTree tree;
    private final JScrollPane treeScroll;
    private final JList<ExistingPipelineNodeDatabaseEntry> inGroupList = new JList<>();
    private final JScrollPane inGroupScroll;
    private final JPanel inGroupPanel = new JPanel();
    /**
     * The header label of the "In group" pane (reads "In group (matching)"
     * while a restricting state is active).
     */
    private final JLabel inGroupHeader = new JLabel("In group");

    /**
     * The debounce timer for event-driven rebuilds (restarted by
     * {@link #scheduleRebuild()}).
     */
    private final javax.swing.Timer rebuildTimer;
    /**
     * The debounce timer for search input.
     */
    private final javax.swing.Timer searchTimer;
    /**
     * Group ids the user expanded (in-memory only); reapplied after rebuilds
     * so regrouping does not collapse the tree. The panel itself never
     * auto-expands groups.
     */
    private final Set<String> expandedGroupIds = new HashSet<>();
    /**
     * The group rows of the current tree (the source for the "In group"
     * pane). Freshly built on every rebuild.
     */
    private final List<GroupRow> groupRows = new ArrayList<>();
    /**
     * The nodes of the current tree by node id (canvas nodes and
     * cross-compartment search results). Rebuilt on every rebuild; resolves
     * the tree rows' node infos and the "In group" pane's wrapped entries.
     */
    private final Map<UUID, JIPipeGraphNode> nodesById = new HashMap<>();
    /**
     * The node ids visible on the current canvas (the current compartment's
     * nodes in the pipeline flavor, the compartments in the compartments
     * flavor). Rebuilt on every rebuild; excludes the canvas nodes from the
     * cross-compartment search results and drives the compartment badge of
     * the "All" group's node rows.
     */
    private final Set<UUID> canvasNodeIds = new HashSet<>();
    /**
     * The nodes whose parameter events this panel is subscribed to. The
     * subscriptions are weak, but {@code subscribeWeak} does not dedupe, so
     * without this set every graph-changed event would stack another
     * subscriber per node (and each parameter event would dispatch once per
     * stacked subscription). See {@link #registerNodeEvents()}.
     */
    private final Set<JIPipeGraphNode> parameterEventNodes = new HashSet<>();
    /**
     * The tier at the last view render ({@code -1} = none yet).
     */
    private int lastRenderedTier = -1;
    /**
     * Whether a tree rebuild is in flight - the tree selection and expansion
     * listeners must not react to the rebuild's transient events.
     */
    private boolean rebuilding;

    /**
     * Creates a new Find panel for the given workbench and graph editor.
     *
     * @param workbench     the desktop workbench
     * @param graphEditorUI the graph editor UI (provides the canvas + event sources)
     */
    public JIPipeDesktopFindPanel(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(workbench);
        this.graphEditorUI = graphEditorUI;

        rebuildTimer = new javax.swing.Timer(REBUILD_DEBOUNCE_MS, e -> {
            if (isShowing()) {
                rebuildTree();
            }
        });
        rebuildTimer.setRepeats(false);
        searchTimer = new javax.swing.Timer(SEARCH_DEBOUNCE_MS, e -> {
            if (isShowing()) {
                rebuildTree();
            }
        });
        searchTimer.setRepeats(false);

        initializeToolbar();
        initializeFilterBar();

        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new FindTreeCellRenderer());
        tree.setSelectionModel(new RowSelectionModel());
        tree.addTreeSelectionListener(e -> onTreeSelectionChanged());
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                recordExpansion(event);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                recordExpansion(event);
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    navigateToSelectedEntry();
                }
            }
        });
        // JTree surfaces the renderer components' tooltips (row UUID, chip and
        // pin tooltips) only when the tree is registered with the tooltip
        // manager
        ToolTipManager.sharedInstance().registerComponent(tree);
        treeScroll = new JScrollPane(tree);

        inGroupList.setCellRenderer(new JIPipeDesktopNodeDatabaseEntryListCellRenderer());
        // Double-click navigates like the tree rows (the Add nodes results
        // list resolves the selected value the same way)
        inGroupList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    navigateToSelectedPaneEntry();
                }
            }
        });
        inGroupScroll = new JScrollPane(inGroupList);
        inGroupPanel.setLayout(new BorderLayout());
        inGroupHeader.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        inGroupHeader.setForeground(DIM_TEXT_COLOR);
        inGroupHeader.setFont(inGroupHeader.getFont().deriveFont(Font.BOLD, IN_GROUP_HEADER_FONT_SIZE));
        inGroupPanel.add(inGroupHeader, BorderLayout.NORTH);
        inGroupPanel.add(inGroupScroll, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        bodyPanel.setLayout(new BorderLayout());
        add(bodyPanel, BorderLayout.CENTER);
        applyViewIfNeeded();

        // The layout is tier-adaptive (a pure function of the width);
        // re-applying is a no-op while the tier is unchanged. Becoming
        // visible triggers one rebuild (the panel is usually created hidden
        // and misses the events of the hidden period).
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyViewIfNeeded();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                rebuildTree();
            }
        });

        // Event subscriptions, mirroring JIPipeDesktopBookmarkListPanel:
        // graph structure changes (both the editor graph and the compartment
        // graph), per-node parameter changes (bookmark toggles, renames,
        // descriptions), and canvas updates (node/note positions).
        graphEditorUI.getGraph().getGraphChangedEventEmitter().subscribeWeak(this);
        if (graphEditorUI.getGraph().getProject() != null) {
            graphEditorUI.getGraph().getProject().getCompartmentGraph().getGraphChangedEventEmitter().subscribeWeak(this);
        }
        registerNodeEvents();
        graphEditorUI.getCanvasUI().getGraphCanvasUpdatedEventEmitter().subscribeWeak(this);

        // Initial content
        rebuildTree();
    }

    /**
     * The toolbar: the shared Add-nodes search field (the component already
     * debounces its per-keystroke notifications, so the listener rebuilds
     * directly instead of stacking the panel's own debounce).
     */
    private void initializeToolbar() {
        toolBar.setFloatable(false);
        toolBar.setBorder(null);
        searchField.setToolTipText("Search by name, description or UUID");
        searchField.addActionListener(e -> rebuildTree());
        toolBar.add(searchField);
    }

    /**
     * The Add-nodes-style filter bar (without the kinds box - existing nodes
     * have no kind): framework combo, data types box, tags box, and the pinned
     * toggle. Every change triggers the same debounced rebuild path the search
     * uses.
     */
    private void initializeFilterBar() {
        filterBar.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        filterBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Framework combo (first item = "all frameworks" sentinel): only
        // frameworks with at least one node visible in THIS editor role are
        // listed - a framework nobody's nodes use (e.g. Comet) would just be
        // an empty, confusing filter option
        frameworkCombo.addItem(new AllFrameworksSentinel());
        Set<String> populatedFrameworks = new LinkedHashSet<>();
        boolean compartmentsEditor = graphEditorUI instanceof JIPipeDesktopCompartmentsGraphEditorUI;
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            TaxonomyView.BehaviorFlags flags = TaxonomyView.behaviorFlags(info);
            boolean visibleInRole = compartmentsEditor ? flags.isVisibleInCompartments() : flags.isVisibleInPipeline();
            if (!visibleInRole) {
                continue;
            }
            populatedFrameworks.add(info.getFramework().getId());
        }
        for (JIPipeNodeTaxonomyFramework framework : JIPipe.getNodes().getFrameworks().all()) {
            if (populatedFrameworks.contains(framework.getId())) {
                frameworkCombo.addItem(framework);
            }
        }
        frameworkCombo.addActionListener(e -> {
            Object selected = frameworkCombo.getSelectedItem();
            filterFrameworkId = selected instanceof JIPipeNodeTaxonomyFramework framework ? framework.getId() : null;
            scheduleSearchRebuild();
        });
        frameworkCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof JIPipeNodeTaxonomyFramework framework) {
                    setText(framework.getName());
                }
                return this;
            }
        });
        filterBar.add(frameworkCombo);

        // Data types (input + output, multi): one option per unhidden
        // registered data type, split into the Input and Output groups, each
        // carrying the data type's icon
        dataTypeBox = new JIPipeDesktopRichSelectionBox("All data", "Data", true, true, this::createDataTypeOptions, selection -> {
            filterInputDataTypes.clear();
            filterOutputDataTypes.clear();
            for (JIPipeDesktopRichSelectionBox.Option<?> option : selection) {
                if (option.getValue() instanceof TypedDataValue typed) {
                    if (typed.input()) {
                        filterInputDataTypes.add(typed.dataClass());
                    } else {
                        filterOutputDataTypes.add(typed.dataClass());
                    }
                }
            }
            scheduleSearchRebuild();
        });
        dataTypeBox.setToolTipText("Filter by input and/or output data type (select one or more)");
        filterBar.add(dataTypeBox);

        // Tags (multi): the merged tag vocabulary (node-level + function-path
        // tags of all registered nodes) - the same set the filter predicate
        // matches against
        tagsBox = new JIPipeDesktopRichSelectionBox("All tags", "Tags", true, this::createTagOptions, selection -> {
            filterTags.clear();
            for (JIPipeDesktopRichSelectionBox.Option<?> option : selection) {
                if (option.getValue() instanceof String tag) {
                    filterTags.add(tag);
                }
            }
            scheduleSearchRebuild();
        });
        filterBar.add(tagsBox);
        filterBar.add(Box.createHorizontalStrut(8));

        // The pinned toggle (icon-only, like the Add nodes filter bar)
        pinnedOnlyButton.setToolTipText("Show only pinned nodes");
        UIUtils.makeButtonFlatWithSize(pinnedOnlyButton, TOOLBAR_BUTTON_SIZE);
        pinnedOnlyButton.addActionListener(e -> scheduleSearchRebuild());
        filterBar.add(pinnedOnlyButton);
    }

    /**
     * The data-type options of the "Data" rich selection box: one per
     * unhidden registered data type, sorted by display name, split into the
     * "Input" and "Output" groups, each carrying the data type's icon.
     */
    private List<JIPipeDesktopRichSelectionBox.Option<?>> createDataTypeOptions() {
        List<JIPipeDesktopRichSelectionBox.Option<?>> options = new ArrayList<>();
        Map<String, Class<? extends JIPipeData>> dataTypes = JIPipe.getDataTypes().getUnhiddenRegisteredDataTypes();
        List<String> sortedIds = new ArrayList<>(dataTypes.keySet());
        sortedIds.sort(Comparator.comparing(id -> JIPipeDataInfo.getInstance(dataTypes.get(id)).getName()));
        for (String id : sortedIds) {
            Class<? extends JIPipeData> dataClass = dataTypes.get(id);
            options.add(new JIPipeDesktopRichSelectionBox.Option<>(new TypedDataValue(dataClass, true),
                    JIPipeDataInfo.getInstance(dataClass).getName(), JIPipe.getDataTypes().getIconFor(dataClass), "Input"));
        }
        for (String id : sortedIds) {
            Class<? extends JIPipeData> dataClass = dataTypes.get(id);
            options.add(new JIPipeDesktopRichSelectionBox.Option<>(new TypedDataValue(dataClass, false),
                    JIPipeDataInfo.getInstance(dataClass).getName(), JIPipe.getDataTypes().getIconFor(dataClass), "Output"));
        }
        return options;
    }

    /**
     * The tag options of the "Tags" rich selection box: the merged tag
     * vocabulary (node-level + function-path tags of all nodes), sorted.
     */
    private List<JIPipeDesktopRichSelectionBox.Option<?>> createTagOptions() {
        Set<String> allTags = new LinkedHashSet<>();
        for (JIPipeNodeTaxonomyFunction function : JIPipe.getNodes().getFunctions().all()) {
            allTags.addAll(function.getTags());
        }
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            allTags.addAll(info.getMergedTags());
        }
        List<JIPipeDesktopRichSelectionBox.Option<?>> options = new ArrayList<>();
        for (String tag : allTags.stream().sorted(Comparator.naturalOrder()).toList()) {
            options.add(new JIPipeDesktopRichSelectionBox.Option<>(tag, tag, JIPipe.RESOURCES.getIcon16("actions/tag.png"), null));
        }
        return options;
    }

    /**
     * Whether any facet filter restricts the tree (framework, data types or
     * tags).
     */
    private boolean hasActiveFacetFilters() {
        return filterFrameworkId != null || !filterTags.isEmpty() || !filterInputDataTypes.isEmpty() || !filterOutputDataTypes.isEmpty();
    }

    /**
     * Whether any restricting state is currently active (a non-blank search,
     * the pinned filter or an active facet filter): empty groups are hidden
     * and the "In group" pane marks its member list as filtered.
     */
    private boolean isRestricting() {
        return !searchField.getText().trim().isEmpty() || pinnedOnlyButton.isSelected() || hasActiveFacetFilters();
    }

    private void registerNodeEvents() {
        JIPipeGraph graph = graphEditorUI.getGraph();
        Set<JIPipeGraphNode> liveNodes = new HashSet<>();
        subscribeNodeEvents(graph.getGraphNodes(), liveNodes);
        if (graph.getProject() != null) {
            subscribeNodeEvents(graph.getProject().getCompartmentGraph().getGraphNodes(), liveNodes);
        }
        // Nodes removed from the graphs: unsubscribe explicitly and drop
        // them from the tracking set (the set must not retain dead nodes).
        // Without the unsubscribe, undo (remove + re-add of the SAME node
        // instance) re-subscribes while the old weak subscription is still
        // alive on the live instance, so parameter events would dispatch
        // twice.
        for (JIPipeGraphNode removed : new ArrayList<>(parameterEventNodes)) {
            if (!liveNodes.contains(removed)) {
                removed.getParameterChangedEventEmitter().unsubscribe(this);
                parameterEventNodes.remove(removed);
            }
        }
    }

    private void subscribeNodeEvents(Set<JIPipeGraphNode> nodes, Set<JIPipeGraphNode> liveNodes) {
        for (JIPipeGraphNode node : nodes) {
            liveNodes.add(node);
            if (parameterEventNodes.add(node)) {
                // Newcomer: subscribe exactly once (the set already contains
                // the previously subscribed nodes)
                node.getParameterChangedEventEmitter().subscribeWeak(this);
            }
        }
    }

    // ------------------------------------------------------------------
    // Event reactions
    // ------------------------------------------------------------------

    @Override
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        registerNodeEvents();
        scheduleRebuild();
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("jipipe:node:bookmarked".equals(event.getKey())) {
            scheduleRebuild();
        } else if ("jipipe:node:description".equals(event.getKey())) {
            scheduleRebuild();
        } else if ("jipipe:node:name".equals(event.getKey())) {
            scheduleRebuild();
        }
    }

    @Override
    public void onGraphCanvasUpdated(JIPipeDesktopGraphCanvasUIUpdatedEvent event) {
        scheduleRebuild();
    }

    /**
     * Requests a debounced rebuild. Only schedules while the panel is showing:
     * a hidden panel is stale by design and rebuilds once on
     * {@code componentShown}.
     */
    private void scheduleRebuild() {
        if (!isShowing()) {
            return;
        }
        rebuildTimer.restart();
    }

    private void scheduleSearchRebuild() {
        if (!isShowing()) {
            return;
        }
        searchTimer.restart();
    }

    // ------------------------------------------------------------------
    // Tier-adaptive layout (mirrors JIPipeDesktopNodeBrowser.applyViewIfNeeded)
    // ------------------------------------------------------------------

    private int getTier() {
        return getWidth() < COMPACT_TIER_WIDTH ? TIER_COMPACT : TIER_COMFORTABLE;
    }

    /**
     * Applies the main-area layout: the compact tier (narrow dock) shows the
     * tree only; the comfortable tier shows the tree | "In group" pane side by
     * side. Idempotent: a no-op while the tier is unchanged, so it is safe to
     * call from the resize listener on every resize.
     */
    private void applyViewIfNeeded() {
        int tier = getTier();
        if (tier == lastRenderedTier) {
            return;
        }
        lastRenderedTier = tier;
        bodyPanel.removeAll();
        // The filter bar sits at the top of the body (below the search
        // toolbar, above the tree/list), like the Add nodes panel
        bodyPanel.add(filterBar, BorderLayout.NORTH);
        if (tier == TIER_COMPACT) {
            bodyPanel.add(treeScroll, BorderLayout.CENTER);
        } else {
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, inGroupPanel);
            splitPane.setResizeWeight(COMFORTABLE_RESIZE_WEIGHT);
            splitPane.setContinuousLayout(true);
            bodyPanel.add(splitPane, BorderLayout.CENTER);
        }
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    // ------------------------------------------------------------------
    // Tree rebuild
    // ------------------------------------------------------------------

    private JIPipeProject getProject() {
        if (getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench projectWorkbench) {
            return projectWorkbench.getProject();
        }
        return null;
    }

    /**
     * Rebuilds the tree from the live canvas and project state. Runs
     * unconditionally (the callers guard visibility themselves), but not
     * re-entrantly: the {@link #rebuilding} flag keeps the rebuild's transient
     * tree events from re-entering the selection/expansion handling.
     */
    private void rebuildTree() {
        rebuilding = true;
        try {
            groupRows.clear();
            nodesById.clear();
            canvasNodeIds.clear();
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();

            // Remember the selected row across the rebuild (the model reload
            // drops the tree selection); restored below, mirroring
            // reapplyExpandedGroups(). When nothing can be restored (first
            // build, or the selected row no longer exists), the synthetic
            // "All" group is pre-selected.
            UUID selectedNodeId = selectedEntryNodeId();
            String selectedGroupId = selectedGroupId();

            boolean restricting = false;
            JIPipeProject project = getProject();
            if (project != null) {
                String query = searchField.getText().trim();
                boolean searching = !query.isEmpty();
                boolean pinnedOnly = pinnedOnlyButton.isSelected();
                // Empty groups are hidden whenever ANY restricting state is
                // active; with no restricting state they stay visible
                restricting = searching || pinnedOnly || hasActiveFacetFilters();

                CanvasState canvasState = collectCanvasState(project);
                List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(canvasState.nodeRefs(), canvasState.noteRefs());
                List<NoteGroupingCalculator.NodeRef> ungrouped = NoteGroupingCalculator.ungrouped(canvasState.nodeRefs(), canvasState.noteRefs());

                for (NoteGroupingCalculator.Group group : groups) {
                    addGroupRow(root, group.noteId(), group.title(), group.fill(), group.border(),
                            filterEntries(group.nodes().stream().map(NoteGroupingCalculator.NodeRef::entry).toList(), query, pinnedOnly),
                            false, restricting);
                }
                // The "Ungrouped" group is last with the neutral colors; while
                // any restricting state is active it is skipped when nothing
                // matches (like all other empty groups), otherwise it is
                // always shown.
                addGroupRow(root, UNGROUPED_GROUP_ID, "Ungrouped", UNGROUPED_FILL, UNGROUPED_BORDER,
                        filterEntries(ungrouped.stream().map(NoteGroupingCalculator.NodeRef::entry).toList(), query, pinnedOnly),
                        false, restricting);

                if (searching) {
                    addCompartmentGroups(root, project, query, pinnedOnly);
                }

                // The synthetic "All" group goes first: every entry currently
                // in the tree (post-filter, including the cross-compartment
                // search results while searching), deduplicated by node id
                List<FindEntry> allEntries = new ArrayList<>();
                for (GroupRow groupRow : groupRows) {
                    for (FindEntry entry : groupRow.entries()) {
                        if (allEntries.stream().noneMatch(existing -> existing.getNodeId().equals(entry.getNodeId()))) {
                            allEntries.add(entry);
                        }
                    }
                }
                // The All row follows the same hide-when-empty rule as the
                // other groups: it stays visible on an empty project (nothing
                // restricts the tree) and hides while restricted and empty
                DefaultMutableTreeNode allNode = addGroupRow(root, GROUP_ID_ALL, "All", UNGROUPED_FILL, UNGROUPED_BORDER, allEntries, false, restricting);
                if (allNode != null && root.getChildCount() > 1) {
                    root.insert(allNode, 0);
                }
            }

            treeModel.setRoot(root);
            reapplyExpandedGroups();
            if (!reapplySelection(selectedGroupId, selectedNodeId)) {
                selectAllRow();
            }
            updateInGroupPane(restricting);
        } finally {
            rebuilding = false;
        }
    }

    /**
     * Collects the current canvas state: node rects + entries (notes excluded)
     * and note rects + titles + colors (only group-forming notes).
     */
    private CanvasState collectCanvasState(JIPipeProject project) {
        List<NoteGroupingCalculator.NodeRef> nodeRefs = new ArrayList<>();
        List<NoteGroupingCalculator.NoteRef> noteRefs = new ArrayList<>();
        BiMap<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> nodeUIs = graphEditorUI.getCanvasUI().getNodeUIs();
        for (Map.Entry<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> nodeUI : nodeUIs.entrySet()) {
            JIPipeGraphNode node = nodeUI.getKey();
            Rectangle bounds = nodeUI.getValue().getBounds();
            if (node instanceof JIPipeGraphCanvasNote) {
                // Canvas notes never appear as entries; only titled text box
                // notes form groups
                if (node instanceof DefaultTextBoxGraphCanvasNote textBoxNote) {
                    noteRefs.add(new NoteGroupingCalculator.NoteRef(uuidString(node), bounds, textBoxNote.getTextTitle(), noteFillColor(textBoxNote), noteBorderColor(textBoxNote)));
                }
                continue;
            }
            UUID nodeId = node.getUUIDInParentGraph();
            if (nodeId == null) {
                continue;
            }
            canvasNodeIds.add(nodeId);
            nodesById.put(nodeId, node);
            nodeRefs.add(new NoteGroupingCalculator.NodeRef(uuidString(node), bounds, createEntry(node, project)));
        }
        return new CanvasState(nodeRefs, noteRefs);
    }

    /**
     * The fill color for a note's group swatch: the note's fill color if it is
     * a text box note with an enabled fill color, otherwise the neutral
     * ungrouped fill (non-text-box notes carry no shape colors).
     */
    private static Color noteFillColor(JIPipeGraphCanvasNote note) {
        if (note instanceof TextBoxGraphCanvasNote textBox) {
            OptionalColorParameter fillParameter = textBox.getShapeParameters().getFillColor();
            if (fillParameter.isEnabled()) {
                return fillParameter.getContent();
            }
        }
        return UNGROUPED_FILL;
    }

    /**
     * The border color for a note's group swatch: the note's border color if
     * it is a text box note, otherwise the neutral ungrouped border.
     */
    private static Color noteBorderColor(JIPipeGraphCanvasNote note) {
        if (note instanceof TextBoxGraphCanvasNote textBox) {
            return textBox.getShapeParameters().getBorderColor();
        }
        return UNGROUPED_BORDER;
    }

    /**
     * Creates the Find panel entry of a graph node.
     */
    private static FindEntry createEntry(JIPipeGraphNode node, JIPipeProject project) {
        UUID compartmentId = null;
        String compartmentName = null;
        if (node instanceof JIPipeProjectCompartment compartment) {
            compartmentId = compartment.getProjectCompartmentUUID();
            compartmentName = compartment.getName();
        } else {
            UUID compartmentUUID = node.getCompartmentUUIDInParentGraph();
            if (compartmentUUID != null) {
                JIPipeProjectCompartment compartment = project.getCompartments().get(compartmentUUID);
                if (compartment != null) {
                    compartmentId = compartmentUUID;
                    compartmentName = compartment.getName();
                }
            }
        }
        return new FindEntry(node.getUUIDInParentGraph(), nodeDisplayTitle(node), nodeDescription(node), uuidString(node), compartmentId, compartmentName, node.isBookmarked(),
                node.getInfo().getFramework().getId(), node.getInfo().getMergedTags(), nodeInputDataTypes(node), nodeOutputDataTypes(node));
    }

    /**
     * The data classes of the node's input slots (the input facet of the
     * filter bar).
     */
    private static Set<Class<? extends JIPipeData>> nodeInputDataTypes(JIPipeGraphNode node) {
        Set<Class<? extends JIPipeData>> dataTypes = new LinkedHashSet<>();
        for (JIPipeInputDataSlot slot : node.getInputSlotMap().values()) {
            dataTypes.add(slot.getInfo().getDataClass());
        }
        return dataTypes;
    }

    /**
     * The data classes of the node's output slots (the output facet of the
     * filter bar).
     */
    private static Set<Class<? extends JIPipeData>> nodeOutputDataTypes(JIPipeGraphNode node) {
        Set<Class<? extends JIPipeData>> dataTypes = new LinkedHashSet<>();
        for (JIPipeOutputDataSlot slot : node.getOutputSlotMap().values()) {
            dataTypes.add(slot.getInfo().getDataClass());
        }
        return dataTypes;
    }

    /**
     * The display title of a node in the Find panel: tunnels are prefixed so
     * their two node kinds are distinguishable (the canvas shows them by
     * their algorithm name only). Other nodes keep their plain name.
     */
    private static String nodeDisplayTitle(JIPipeGraphNode node) {
        if (node instanceof JIPipeDataFlowTunnelEntrance) {
            return "Tunnel entrance: " + node.getName();
        }
        if (node instanceof JIPipeDataFlowTunnelExit) {
            return "Tunnel exit: " + node.getName();
        }
        return node.getName();
    }

    private static String nodeDescription(JIPipeGraphNode node) {
        HTMLText description = node.getInfo().getDescription();
        return description != null ? description.toPlainText() : "";
    }

    private static String uuidString(JIPipeGraphNode node) {
        UUID uuid = node.getUUIDInParentGraph();
        return uuid != null ? uuid.toString() : "";
    }

    /**
     * Filters the entries by the query, the pinned filter and the facet
     * filters. {@code pinnedOnly} is the snapshot taken by the caller (the
     * rebuild reads the toggle once so every consumer sees the same value).
     */
    private List<FindEntry> filterEntries(List<FindEntry> entries, String query, boolean pinnedOnly) {
        List<FindEntry> result = new ArrayList<>();
        for (FindEntry entry : entries) {
            if (!entry.matches(query)) {
                continue;
            }
            if (pinnedOnly && !entry.isBookmarked()) {
                continue;
            }
            // The Add-nodes filter facets (framework, data types, tags); an
            // empty selection disables the respective check
            if (!entry.matchesFacets(filterFrameworkId, filterTags, filterInputDataTypes, filterOutputDataTypes)) {
                continue;
            }
            result.add(entry);
        }
        return result;
    }

    private DefaultMutableTreeNode addGroupRow(DefaultMutableTreeNode root, String groupId, String title, Color fill, Color border, List<FindEntry> entries, boolean compartmentGroup, boolean hideWhenEmpty) {
        if (hideWhenEmpty && entries.isEmpty()) {
            return null;
        }
        GroupRow groupRow = new GroupRow(groupId, displayTitle(title), fill, border, entries, compartmentGroup);
        groupRows.add(groupRow);
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(groupRow);
        for (FindEntry entry : entries) {
            JIPipeGraphNode node = nodesById.get(entry.getNodeId());
            // The "All" group mixes canvas and cross-compartment entries: the
            // badge follows each entry, not the group
            boolean crossCompartment = GROUP_ID_ALL.equals(groupId) ? !canvasNodeIds.contains(entry.getNodeId()) : compartmentGroup;
            groupNode.add(new DefaultMutableTreeNode(new NodeRow(entry, crossCompartment, node != null ? node.getInfo() : null)));
        }
        root.add(groupNode);
        return groupNode;
    }

    private static String displayTitle(String title) {
        if (title == null || title.length() <= GROUP_TITLE_MAX_CHARS) {
            return title;
        }
        return title.substring(0, GROUP_TITLE_MAX_CHARS) + "...";
    }

    /**
     * Adds one group per other compartment holding matching nodes (search
     * only), ordered by compartment name, appended after the current-canvas
     * groups.
     */
    private void addCompartmentGroups(DefaultMutableTreeNode root, JIPipeProject project, String query, boolean pinnedOnly) {
        Map<UUID, List<FindEntry>> entriesByCompartment = new HashMap<>();
        for (JIPipeGraphNode node : project.getGraph().getGraphNodes()) {
            if (node instanceof JIPipeGraphCanvasNote) {
                continue;
            }
            UUID nodeId = node.getUUIDInParentGraph();
            if (nodeId == null || canvasNodeIds.contains(nodeId)) {
                continue;
            }
            FindEntry entry = createEntry(node, project);
            if (!entry.matches(query)) {
                continue;
            }
            if (pinnedOnly && !entry.isBookmarked()) {
                continue;
            }
            if (!entry.matchesFacets(filterFrameworkId, filterTags, filterInputDataTypes, filterOutputDataTypes)) {
                continue;
            }
            if (entry.getCompartmentId() == null) {
                continue;
            }
            nodesById.put(nodeId, node);
            entriesByCompartment.computeIfAbsent(entry.getCompartmentId(), key -> new ArrayList<>()).add(entry);
        }
        List<UUID> compartmentIds = new ArrayList<>(entriesByCompartment.keySet());
        compartmentIds.sort(Comparator.comparing(id -> compartmentName(project, id), String.CASE_INSENSITIVE_ORDER));
        for (UUID compartmentId : compartmentIds) {
            List<FindEntry> entries = entriesByCompartment.get(compartmentId);
            entries.sort(Comparator.comparing(FindEntry::getName, String.CASE_INSENSITIVE_ORDER));
            addGroupRow(root, compartmentId.toString(), compartmentName(project, compartmentId), COMPARTMENT_GROUP_FILL, COMPARTMENT_GROUP_BORDER, entries, true, true);
        }
    }

    private static String compartmentName(JIPipeProject project, UUID compartmentId) {
        JIPipeProjectCompartment compartment = project.getCompartments().get(compartmentId);
        return compartment != null ? compartment.getName() : "";
    }

    /**
     * Re-expands the groups the user had expanded. The panel never
     * auto-expands groups, so only previously expanded ones open.
     */
    private void reapplyExpandedGroups() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        for (Enumeration<? extends TreeNode> children = root.children(); children.hasMoreElements(); ) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (child.getUserObject() instanceof GroupRow groupRow && expandedGroupIds.contains(groupRow.groupId())) {
                tree.expandPath(new TreePath(child.getPath()));
            }
        }
    }

    /**
     * The node id of the currently selected tree entry, or null when no node
     * row is selected.
     */
    private UUID selectedEntryNodeId() {
        DefaultMutableTreeNode treeNode = selectedTreeNode();
        if (treeNode == null || !(treeNode.getUserObject() instanceof NodeRow nodeRow)) {
            return null;
        }
        return nodeRow.entry().getNodeId();
    }

    /**
     * The group id of the currently selected tree row, or null when no group
     * row is selected.
     */
    private String selectedGroupId() {
        DefaultMutableTreeNode treeNode = selectedTreeNode();
        return treeNode != null && treeNode.getUserObject() instanceof GroupRow groupRow ? groupRow.groupId() : null;
    }

    /**
     * Reselects the previously selected row after a rebuild, mirroring
     * {@link #reapplyExpandedGroups()}: the model reload drops the tree
     * selection, which would otherwise empty the "In group" pane about a
     * second after ordinary canvas use (zoom, drop, paste). Restores a
     * selected group (by group id) or node (by node id); returns false when
     * neither exists, so the caller pre-selects the "All" group.
     */
    private boolean reapplySelection(String groupId, UUID nodeId) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        if (groupId != null) {
            for (Enumeration<? extends TreeNode> children = root.children(); children.hasMoreElements(); ) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                if (child.getUserObject() instanceof GroupRow groupRow && groupId.equals(groupRow.groupId())) {
                    TreePath path = new TreePath(child.getPath());
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                    return true;
                }
            }
            return false;
        }
        if (nodeId == null) {
            return false;
        }
        for (Enumeration<? extends TreeNode> groupChildren = root.children(); groupChildren.hasMoreElements(); ) {
            DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) groupChildren.nextElement();
            for (Enumeration<? extends TreeNode> leafChildren = groupNode.children(); leafChildren.hasMoreElements(); ) {
                DefaultMutableTreeNode leafNode = (DefaultMutableTreeNode) leafChildren.nextElement();
                if (leafNode.getUserObject() instanceof NodeRow nodeRow && nodeId.equals(nodeRow.entry().getNodeId())) {
                    TreePath path = new TreePath(leafNode.getPath());
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Pre-selects the synthetic "All" group row (the first tree row) when no
     * selection could be restored, so the "In group" pane lists all entries.
     */
    private void selectAllRow() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        for (Enumeration<? extends TreeNode> children = root.children(); children.hasMoreElements(); ) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (child.getUserObject() instanceof GroupRow groupRow && GROUP_ID_ALL.equals(groupRow.groupId())) {
                TreePath path = new TreePath(child.getPath());
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                return;
            }
        }
    }

    private void recordExpansion(TreeExpansionEvent event) {
        if (rebuilding) {
            return;
        }
        if (event.getPath().getLastPathComponent() instanceof DefaultMutableTreeNode node
                && node.getUserObject() instanceof GroupRow groupRow) {
            if (tree.isExpanded(event.getPath())) {
                expandedGroupIds.add(groupRow.groupId());
            } else {
                expandedGroupIds.remove(groupRow.groupId());
            }
        }
    }

    // ------------------------------------------------------------------
    // Selection + "In group" pane
    // ------------------------------------------------------------------

    private void onTreeSelectionChanged() {
        if (rebuilding) {
            return;
        }
        updateInGroupPane(isRestricting());
    }

    private DefaultMutableTreeNode selectedTreeNode() {
        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath == null) {
            return null;
        }
        return selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode node ? node : null;
    }

    /**
     * Repopulates the "In group" pane from the current tree selection. While
     * a restricting state is active (search, pinned filter or facets) the
     * header reads "In group (matching)" so the post-filter member list is
     * self-explanatory.
     */
    private void updateInGroupPane(boolean restricting) {
        inGroupHeader.setText(restricting ? "In group (matching)" : "In group");
        DefaultListModel<ExistingPipelineNodeDatabaseEntry> listModel = new DefaultListModel<>();
        for (FindEntry entry : selectedRowMembers()) {
            JIPipeGraphNode node = nodesById.get(entry.getNodeId());
            if (node == null) {
                continue;
            }
            // The Add-nodes results-list row for this member: the entry wraps
            // the live node (getGraphNode() maps back for the double-click
            // navigation)
            listModel.addElement(createPaneEntry(entry, node));
        }
        inGroupList.setModel(listModel);
    }

    /**
     * The existing-node entry for the "In group" pane: tunnel nodes keep the
     * display name override of the tree rows (the base class would show the
     * raw node name); every other node uses the stock entry.
     */
    private ExistingPipelineNodeDatabaseEntry createPaneEntry(FindEntry entry, JIPipeGraphNode node) {
        if (node instanceof JIPipeDataFlowTunnelEntrance || node instanceof JIPipeDataFlowTunnelExit) {
            return new TunnelDisplayEntry(entry.getUuidString(), node, nodeDisplayTitle(node));
        }
        return new ExistingPipelineNodeDatabaseEntry(entry.getUuidString(), node);
    }

    /**
     * The members shown in the "In group" pane for the current selection: a
     * selected group row lists its entries (the post-filter list the row
     * displays); a selected node row lists the members of the group
     * containing that node (the union of the groups for a multi-membership
     * node; the ungrouped members when the selection is ungrouped). Empty
     * when nothing row-like is selected (e.g. a node of another compartment,
     * which has no current-canvas grouping).
     */
    private List<FindEntry> selectedRowMembers() {
        DefaultMutableTreeNode treeNode = selectedTreeNode();
        if (treeNode == null) {
            return List.of();
        }
        if (treeNode.getUserObject() instanceof GroupRow groupRow) {
            return new ArrayList<>(groupRow.entries());
        }
        if (treeNode.getUserObject() instanceof NodeRow nodeRow) {
            return nodeRowMembers(nodeRow.entry());
        }
        return List.of();
    }

    private List<FindEntry> nodeRowMembers(FindEntry selected) {
        Map<UUID, FindEntry> members = new LinkedHashMap<>();
        boolean found = false;
        for (GroupRow groupRow : groupRows) {
            // The synthetic "All" group and the compartment groups are not
            // canvas groups: they must not leak into a node row's member list
            if (groupRow.compartmentGroup() || GROUP_ID_ALL.equals(groupRow.groupId())) {
                continue;
            }
            boolean contains = false;
            for (FindEntry entry : groupRow.entries()) {
                if (entry.getNodeId().equals(selected.getNodeId())) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                continue;
            }
            found = true;
            for (FindEntry entry : groupRow.entries()) {
                members.putIfAbsent(entry.getNodeId(), entry);
            }
        }
        if (!found) {
            return List.of();
        }
        return new ArrayList<>(members.values());
    }

    // ------------------------------------------------------------------
    // Navigation (double-click)
    // ------------------------------------------------------------------

    private void navigateToSelectedEntry() {
        DefaultMutableTreeNode treeNode = selectedTreeNode();
        if (treeNode == null || !(treeNode.getUserObject() instanceof NodeRow nodeRow)) {
            return;
        }
        navigateTo(nodeRow.entry());
    }

    /**
     * Navigates to the entry double-clicked in the "In group" pane: the pane
     * entry wraps the live node; the matching FindEntry of the current pane
     * population (the selected row's post-filter entries) provides the
     * navigation data (node id, compartment id).
     */
    private void navigateToSelectedPaneEntry() {
        ExistingPipelineNodeDatabaseEntry paneEntry = inGroupList.getSelectedValue();
        JIPipeGraphNode node = paneEntry != null ? paneEntry.getGraphNode() : null;
        UUID nodeId = node != null ? node.getUUIDInParentGraph() : null;
        if (nodeId == null) {
            return;
        }
        for (FindEntry entry : selectedRowMembers()) {
            if (entry.getNodeId().equals(nodeId)) {
                navigateTo(entry);
                return;
            }
        }
    }

    /**
     * Navigates to the entry's node: on the current canvas it is selected and
     * scrolled to; in another compartment its editor tab is opened (or
     * focused) first, then the node is selected and scrolled to there.
     */
    private void navigateTo(FindEntry entry) {
        JIPipeProject project = getProject();
        if (project == null) {
            return;
        }

        // The node is visible on the current canvas: select + scroll here
        JIPipeGraphNode canvasNode = null;
        for (JIPipeGraphNode node : graphEditorUI.getCanvasUI().getNodeUIs().keySet()) {
            if (entry.getNodeId().equals(node.getUUIDInParentGraph())) {
                canvasNode = node;
                break;
            }
        }
        if (canvasNode != null) {
            JIPipeDesktopGraphNodeUI nodeUI = graphEditorUI.getCanvasUI().getNodeUIs().get(canvasNode);
            if (nodeUI == null) {
                return;
            }
            graphEditorUI.getSelectionManager().setSelection(Collections.singleton(nodeUI));
            graphEditorUI.scrollToAlgorithm(nodeUI);
            return;
        }

        // The node lives in another compartment: open (or focus) its editor
        // tab, then select + scroll there (deferred, like the bookmark panel)
        if (!(getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench projectWorkbench)) {
            return;
        }
        JIPipeProjectCompartment compartment = entry.getCompartmentId() != null ? project.getCompartments().get(entry.getCompartmentId()) : null;
        if (compartment == null) {
            return;
        }
        JIPipeDesktopTabPane.DocumentTab tab = projectWorkbench.getOrOpenPipelineEditorTab(compartment, true);
        SwingUtilities.invokeLater(() -> {
            if (!(tab.getContent() instanceof JIPipeDesktopPipelineGraphEditorUI editorUI)) {
                return;
            }
            JIPipeDesktopGraphNodeUI nodeUI = findNodeUI(editorUI, entry.getNodeId());
            if (nodeUI == null) {
                getDesktopWorkbench().sendStatusBarText("Unable to navigate to node");
                return;
            }
            editorUI.getSelectionManager().setSelection(Collections.singleton(nodeUI));
            editorUI.scrollToAlgorithm(nodeUI);
        });
    }

    private static JIPipeDesktopGraphNodeUI findNodeUI(JIPipeDesktopGraphEditorUI editorUI, UUID nodeId) {
        for (Map.Entry<JIPipeGraphNode, JIPipeDesktopGraphNodeUI> nodeUI : editorUI.getCanvasUI().getNodeUIs().entrySet()) {
            if (nodeId.equals(nodeUI.getKey().getUUIDInParentGraph())) {
                return nodeUI.getValue();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Row types
    // ------------------------------------------------------------------

    /**
     * A group header row (a canvas note's group, the "Ungrouped" group, or a
     * compartment group of search results). Not selectable.
     */
    private record GroupRow(String groupId, String title, Color fill, Color border, List<FindEntry> entries, boolean compartmentGroup) {
    }

    /**
     * A node leaf row. {@code crossCompartment} marks entries that do not live
     * on the current canvas (search results from other compartments); they
     * render the compartment badge. {@code info} is the node's info (for the
     * Add-nodes entry style: colors, framework pattern, chips, location path);
     * null falls back to the simple row (defensive - every entry is built from
     * a live node).
     */
    private record NodeRow(FindEntry entry, boolean crossCompartment, JIPipeNodeInfo info) {
    }

    /**
     * The current canvas state used for one rebuild.
     */
    private record CanvasState(List<NoteGroupingCalculator.NodeRef> nodeRefs, List<NoteGroupingCalculator.NoteRef> noteRefs) {
    }

    /**
     * The existing-node entry for the "In group" pane of a tunnel node: only
     * the display name is overridden so the pane shows the same tunnel title
     * as the tree rows ("Tunnel entrance: ..." / "Tunnel exit: ...");
     * everything else (icon, colors, location line) is inherited.
     */
    private static final class TunnelDisplayEntry extends ExistingPipelineNodeDatabaseEntry {
        private final String displayName;

        private TunnelDisplayEntry(String id, JIPipeGraphNode graphNode, String displayName) {
            super(id, graphNode);
            this.displayName = displayName;
        }

        @Override
        public String getName() {
            return displayName;
        }
    }

    /**
     * The framework filter's sentinel combo item ("all frameworks").
     */
    private static final class AllFrameworksSentinel {
        @Override
        public String toString() {
            return "All frameworks";
        }
    }

    /**
     * A data-type filter option: the data class plus the slot direction it
     * applies to.
     */
    private record TypedDataValue(Class<? extends JIPipeData> dataClass, boolean input) {
    }

    // ------------------------------------------------------------------
    // Selection model (the invisible root is not selectable)
    // ------------------------------------------------------------------

    /**
     * A selection model that makes the group rows and the node rows
     * selectable while keeping the invisible root unselectable. A request
     * that contains only unselectable rows keeps the previous selection
     * instead of clearing it (clicking a non-row must not deselect).
     * <p>
     * Single selection only: the pane and the selection restore follow one
     * lead row, so ctrl-click cannot mix group and node selections.
     */
    private static final class RowSelectionModel extends DefaultTreeSelectionModel {
        private RowSelectionModel() {
            setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }

        private TreePath[] filterSelectableRows(TreePath[] paths) {
            if (paths == null) {
                return null;
            }
            List<TreePath> rows = new ArrayList<>();
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof DefaultMutableTreeNode node
                        && (node.getUserObject() instanceof GroupRow || node.getUserObject() instanceof NodeRow)) {
                    rows.add(path);
                }
            }
            if (rows.isEmpty() && paths.length > 0) {
                return getSelectionPaths();
            }
            return rows.toArray(new TreePath[0]);
        }

        @Override
        public void setSelectionPaths(TreePath[] paths) {
            super.setSelectionPaths(filterSelectableRows(paths));
        }

        @Override
        public void addSelectionPaths(TreePath[] paths) {
            super.addSelectionPaths(filterSelectableRows(paths));
        }
    }

    /**
     * Renders the group header rows (colored circle, bold title, member count
     * - the exact function-row look of the Add nodes tree) and the node
     * leaves (the single-line node-leaf look of the Add-nodes tree: pin
     * glyph, 16px node icon, name, framework abbreviation chip, spatial
     * chips, and the compartment badge for cross-compartment rows; the full
     * UUID lives in the row tooltip).
     */
    private final class FindTreeCellRenderer implements TreeCellRenderer {
        private final Icon pinIcon = JIPipe.RESOURCES.getIcon16("actions/window-pin.png");
        private final Icon genericNodeIcon = JIPipe.RESOURCES.getIcon16("actions/tag.png");
        // --- Group rows: the Add-nodes function-row look (the colored circle
        // doubles as the color legend for the note's fill)
        private final ColoredCircleIcon groupCircleIcon = new ColoredCircleIcon(null, UNGROUPED_FILL, UNGROUPED_BORDER);
        private final JPanel groupPanel = new JPanel(new BorderLayout());
        private final JPanel groupTitlePanel = new JPanel();
        private final JLabel groupCircleLabel = new JLabel();
        private final JLabel groupTitleLabel = new JLabel();
        private final JLabel groupCountLabel = new JLabel();
        // --- Node rows: the single-line node-leaf look of the Add-nodes tree
        private final JPanel nodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        private final JLabel nodePinLabel = new JLabel();
        private final JLabel nodeNameLabel = new JLabel();
        private final CompartmentBadgeLabel nodeBadgeLabel = new CompartmentBadgeLabel();
        private final JLabel frameworkChipLabel = new JLabel();
        private final JPanel frameworkChip = createFrameworkChip();
        private final JPanel spatialChip2D = createSpatialChip();
        private final JPanel spatialChip3D = createSpatialChip();
        private final JLabel spatialChip2DLabel = new JLabel("2D");
        private final JLabel spatialChip3DLabel = new JLabel("3D");
        // The framework chip paints this row's framework color (renderer
        // components are reused across rows)
        private JIPipeNodeTaxonomyFramework currentFramework;
        // --- Fallback (no node info; should not happen): plain name row
        private final JLabel fallbackLabel = new JLabel();

        private FindTreeCellRenderer() {
            // Group rows: colored circle + bold title + count badge
            groupTitlePanel.setLayout(new BoxLayout(groupTitlePanel, BoxLayout.X_AXIS));
            groupTitlePanel.setOpaque(false);
            groupCircleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            groupTitleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            groupTitlePanel.add(groupCircleLabel);
            groupTitlePanel.add(Box.createHorizontalStrut(5));
            groupTitlePanel.add(groupTitleLabel);
            groupTitlePanel.add(Box.createHorizontalStrut(GROUP_COUNT_GAP));
            groupTitlePanel.add(groupCountLabel);
            groupPanel.setOpaque(true);
            groupPanel.add(groupTitlePanel, BorderLayout.WEST);
            groupTitleLabel.setFont(groupTitleLabel.getFont().deriveFont(Font.BOLD));
            groupCountLabel.setForeground(DIM_TEXT_COLOR);
            groupPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 6));

            // Node rows: pin + node icon + name + framework chip + spatial
            // chips + compartment badge - the exact node-leaf pattern of the
            // Add-nodes tree renderer (FunctionTreeCellRenderer)
            nodePanel.setOpaque(true);
            nodePanel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
            frameworkChipLabel.setFont(frameworkChipLabel.getFont().deriveFont(Font.BOLD, CHIP_FONT_SIZE));
            frameworkChip.setOpaque(false);
            frameworkChip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            frameworkChip.add(frameworkChipLabel);
            nodePanel.add(nodePinLabel);
            nodePanel.add(nodeNameLabel);
            nodePanel.add(frameworkChip);
            nodePanel.add(spatialChip2D);
            nodePanel.add(spatialChip3D);
            nodePanel.add(nodeBadgeLabel);
            nodeBadgeLabel.setForeground(COMPARTMENT_TEXT_COLOR);
            initSpatialChip(spatialChip2D, spatialChip2DLabel, "Operates in 2D");
            initSpatialChip(spatialChip3D, spatialChip3DLabel, "Operates in 3D");

            // Fallback row (no node info): a plain name label
            fallbackLabel.setOpaque(true);
            fallbackLabel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object userObject = value instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
            if (userObject instanceof GroupRow groupRow) {
                return configureGroupRow(groupRow, tree, selected);
            }
            if (userObject instanceof NodeRow nodeRow) {
                return configureNodeRow(nodeRow, tree, selected);
            }
            return new JLabel();
        }

        private Component configureGroupRow(GroupRow groupRow, JTree tree, boolean selected) {
            applySelectionColors(groupPanel, tree, selected);
            // The colored circle doubles as the color legend for the note's
            // fill (the exact function-row look of the Add nodes tree)
            groupCircleIcon.setFillColor(groupRow.fill());
            groupCircleIcon.setBorderColor(groupRow.border());
            groupCircleLabel.setIcon(groupCircleIcon);
            groupTitleLabel.setText(groupRow.title());
            groupTitleLabel.setForeground(groupRow.compartmentGroup() && !selected ? COMPARTMENT_TEXT_COLOR : groupPanel.getForeground());
            groupCountLabel.setText(String.valueOf(groupRow.entries().size()));
            groupCountLabel.setForeground(selected ? groupPanel.getForeground() : DIM_TEXT_COLOR);
            // Force a synchronous re-layout with freshly measured sizes:
            // JLabel.setText only queues an async revalidate, and
            // CellRendererPane's validate() no-ops on a still-valid
            // container - without this explicit invalidate+validate the
            // title panel renders with the PREVIOUS row's text bounds (an
            // assumed text size instead of the measured one)
            groupTitlePanel.invalidate();
            groupTitlePanel.validate();
            return groupPanel;
        }

        private Component configureNodeRow(NodeRow nodeRow, JTree tree, boolean selected) {
            applySelectionColors(nodePanel, tree, selected);
            FindEntry entry = nodeRow.entry();
            JIPipeNodeInfo info = nodeRow.info();
            if (info == null) {
                // Defensive: every FindEntry is built from a live node, so
                // this should not happen - render a plain name row rather
                // than crash
                applySelectionColors(fallbackLabel, tree, selected);
                fallbackLabel.setText(entry.getName());
                return fallbackLabel;
            }
            nodePinLabel.setIcon(entry.isBookmarked() ? pinIcon : null);
            nodePinLabel.setVisible(entry.isBookmarked());
            nodePinLabel.setToolTipText(entry.isBookmarked() ? "Pinned" : null);
            // The 16px node icon with the generic fallback (the Add-nodes
            // tree pattern)
            Icon nodeIcon = info.getIcon();
            nodeNameLabel.setIcon(nodeIcon != null ? nodeIcon : genericNodeIcon);
            nodeNameLabel.setText(entry.getName());
            nodeNameLabel.setForeground(nodePanel.getForeground());
            // The framework abbreviation chip (exact Add-nodes tree pattern:
            // colored chip, bold white abbreviation, gray for NoFramework)
            currentFramework = info.getFramework();
            boolean noFramework = NoFramework.ID.equals(currentFramework.getId());
            frameworkChipLabel.setText(currentFramework.getAbbreviation());
            frameworkChipLabel.setForeground(noFramework ? Color.GRAY : Color.WHITE);
            frameworkChip.setToolTipText(noFramework ? "No external framework" : "Framework: " + currentFramework.getName());
            // The spatial chips ("2D" / "3D", exact Add-nodes tree pattern)
            Set<JIPipeNodeTaxonomySpatialDimension> spatial = info.getSpatialDimensions();
            spatialChip2DLabel.setForeground(nodePanel.getForeground());
            spatialChip3DLabel.setForeground(nodePanel.getForeground());
            spatialChip2D.setVisible(spatial.contains(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D));
            spatialChip3D.setVisible(spatial.contains(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D));
            // The badge abbreviates the compartment name to its first word;
            // the full name is surfaced in the row tooltip (JTree tooltips
            // only consult the top-level renderer component, not child
            // components, so the badge itself cannot carry it)
            nodeBadgeLabel.setText(firstWord(entry.getCompartmentName()));
            nodeBadgeLabel.setVisible(nodeRow.crossCompartment());
            // The row tooltip carries the full UUID, and for cross-compartment
            // rows also the full compartment name the badge abbreviates
            String rowTooltip = "UUID: " + entry.getUuidString();
            if (nodeRow.crossCompartment()) {
                rowTooltip += "\nCompartment: " + entry.getCompartmentName();
            }
            nodePanel.setToolTipText(rowTooltip);
            // Force a synchronous re-layout with freshly measured sizes:
            // JLabel.setText only queues an async revalidate, and
            // CellRendererPane's validate() no-ops on a still-valid
            // container - without this explicit invalidate+validate the row
            // renders with the PREVIOUS row's text bounds (an assumed text
            // size instead of the measured one)
            nodePanel.invalidate();
            nodePanel.validate();
            return nodePanel;
        }

        /**
         * The framework chip of the Add-nodes tree renderer: a rounded chip
         * painted in the framework's color (nothing painted for NoFramework).
         */
        private JPanel createFrameworkChip() {
            return new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    JIPipeNodeTaxonomyFramework framework = currentFramework;
                    if (framework == null || NoFramework.ID.equals(framework.getId())) {
                        return;
                    }
                    Graphics2D graphics2D = (Graphics2D) g.create();
                    try {
                        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        graphics2D.setColor(framework.getColor());
                        graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CHIP_ARC, CHIP_ARC);
                    } finally {
                        graphics2D.dispose();
                    }
                }
            };
        }

        /**
         * The outlined spatial chip of the Add-nodes tree renderer.
         */
        private static JPanel createSpatialChip() {
            JPanel chip = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D graphics2D = (Graphics2D) g.create();
                    try {
                        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        graphics2D.setColor(Color.GRAY);
                        graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CHIP_ARC, CHIP_ARC);
                    } finally {
                        graphics2D.dispose();
                    }
                }
            };
            chip.setOpaque(false);
            chip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            return chip;
        }

        private static void initSpatialChip(JPanel chip, JLabel chipLabel, String tooltip) {
            chipLabel.setFont(chipLabel.getFont().deriveFont(Font.PLAIN, CHIP_FONT_SIZE));
            chip.setToolTipText(tooltip);
            chip.add(chipLabel);
        }

        private void applySelectionColors(JComponent component, JTree tree, boolean selected) {
            Color background = selected ? UIManager.getColor("Tree.selectionBackground") : tree.getBackground();
            Color foreground = selected ? UIManager.getColor("Tree.selectionForeground") : tree.getForeground();
            if (background != null) {
                component.setBackground(background);
            }
            if (foreground != null) {
                component.setForeground(foreground);
            }
        }

        /**
         * The first whitespace-separated word of the compartment name (the
         * badge abbreviates to the first word; the full name lives in the
         * badge tooltip).
         */
        private static String firstWord(String text) {
            if (text == null) {
                return null;
            }
            String trimmed = text.trim();
            for (int i = 0; i < trimmed.length(); i++) {
                if (Character.isWhitespace(trimmed.charAt(i))) {
                    return trimmed.substring(0, i);
                }
            }
            return trimmed;
        }
    }

    /**
     * A small rounded badge (the purple compartment badge of the prototype).
     */
    private static final class CompartmentBadgeLabel extends JLabel {
        private CompartmentBadgeLabel() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            setFont(getFont().deriveFont(Math.max(BADGE_MIN_FONT_SIZE, getFont().getSize2D() - BADGE_FONT_SIZE_REDUCTION)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D graphics2D = (Graphics2D) g.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(COMPARTMENT_GROUP_FILL);
                graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, BADGE_CORNER_RADIUS, BADGE_CORNER_RADIUS);
                graphics2D.setColor(COMPARTMENT_GROUP_BORDER);
                graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, BADGE_CORNER_RADIUS, BADGE_CORNER_RADIUS);
            } finally {
                graphics2D.dispose();
            }
            super.paintComponent(g);
        }
    }
}
