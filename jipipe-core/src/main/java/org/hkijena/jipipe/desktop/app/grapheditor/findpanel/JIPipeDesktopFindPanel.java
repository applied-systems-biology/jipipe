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
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUIUpdatedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUIUpdatedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.icons.SolidColorIcon;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.DefaultTextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeCellRenderer;
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
 * current-canvas groups. Canvas notes never appear as entries. Single click
 * only selects an entry (and updates the "In group" pane); double-click
 * navigates: nodes on the current canvas are selected and scrolled to, nodes
 * of other compartments open (or focus) their compartment's editor tab first.
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
    public static final int COMPACT_TIER_WIDTH = 380;
    /**
     * Neutral fill for the "Ungrouped" group and for groups whose note carries
     * no usable colors.
     */
    public static final Color UNGROUPED_FILL = new Color(0xe8e8e8);
    /**
     * Neutral border for the "Ungrouped" group and for groups whose note
     * carries no usable colors.
     */
    public static final Color UNGROUPED_BORDER = new Color(0xc4c4c4);

    /**
     * The search field placeholder.
     */
    private static final String SEARCH_PLACEHOLDER_TEXT = "Search by name, description or UUID";
    /**
     * Coalesces event storms (graph changes fire continuously during drags)
     * into one rebuild. Mirrors the reload timer of
     * {@link org.hkijena.jipipe.desktop.app.bookmarks.JIPipeDesktopBookmarkListPanel}.
     */
    private static final int REBUILD_DEBOUNCE_MS = 1000;
    /**
     * Debounce for the search field. Mirrors
     * {@code JIPipeDesktopSearchTextField.SEARCH_DEBOUNCE_DELAY}.
     */
    private static final int SEARCH_DEBOUNCE_MS = 150;
    /**
     * Number of leading UUID characters shown in a node row.
     */
    private static final int UUID_FRAGMENT_LENGTH = 4;
    /**
     * Group titles longer than this many characters are truncated for display.
     */
    private static final int GROUP_TITLE_MAX_CHARS = 40;
    /**
     * Side length of the colored group swatch.
     */
    private static final int GROUP_SWATCH_SIZE = 11;
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
     * Font size reduction of the UUID fragment relative to the row font.
     */
    private static final int UUID_FONT_SIZE_REDUCTION = 2;
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
    private final SearchTextField searchField = new SearchTextField(SEARCH_PLACEHOLDER_TEXT);
    private final JToggleButton pinnedOnlyButton = new JToggleButton("Pinned only");

    // --- Body (tree + "In group" pane, tier-aware) ---
    private final JPanel bodyPanel = new JPanel();
    private final DefaultTreeModel treeModel;
    private final JTree tree;
    private final JScrollPane treeScroll;
    private final JList<FindEntry> inGroupList = new JList<>();
    private final JScrollPane inGroupScroll;
    private final JPanel inGroupPanel = new JPanel();

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

        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new FindTreeCellRenderer());
        tree.setSelectionModel(new NodeRowOnlySelectionModel());
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
        treeScroll = new JScrollPane(tree);

        inGroupList.setCellRenderer(new InGroupListCellRenderer());
        inGroupScroll = new JScrollPane(inGroupList);
        inGroupPanel.setLayout(new BorderLayout());
        JLabel inGroupHeader = new JLabel("In group");
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

    private void initializeToolbar() {
        toolBar.setFloatable(false);
        toolBar.setBorder(null);
        toolBar.add(searchField);
        searchField.addActionListener(e -> rebuildTree());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleSearchRebuild();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleSearchRebuild();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleSearchRebuild();
            }
        });

        JButton clearSearchButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/edit-clear.png"));
        clearSearchButton.setToolTipText("Clear search");
        UIUtils.makeButtonFlatWithSize(clearSearchButton, TOOLBAR_BUTTON_SIZE);
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            rebuildTree();
        });
        toolBar.add(clearSearchButton);

        pinnedOnlyButton.setToolTipText("Show only pinned nodes");
        pinnedOnlyButton.addActionListener(e -> rebuildTree());
        toolBar.add(pinnedOnlyButton);
    }

    private void registerNodeEvents() {
        for (JIPipeGraphNode node : graphEditorUI.getGraph().getGraphNodes()) {
            node.getParameterChangedEventEmitter().subscribeWeak(this);
        }
        if (graphEditorUI.getGraph().getProject() != null) {
            for (JIPipeGraphNode node : graphEditorUI.getGraph().getProject().getCompartmentGraph().getGraphNodes()) {
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
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();

            JIPipeProject project = getProject();
            if (project != null) {
                String query = searchField.getText().trim();
                boolean searching = !query.isEmpty();
                boolean pinnedOnly = pinnedOnlyButton.isSelected();

                CanvasState canvasState = collectCanvasState(project);
                List<NoteGroupingCalculator.Group> groups = NoteGroupingCalculator.group(canvasState.nodeRefs(), canvasState.noteRefs());
                List<NoteGroupingCalculator.NodeRef> ungrouped = NoteGroupingCalculator.ungrouped(canvasState.nodeRefs(), canvasState.noteRefs());

                for (NoteGroupingCalculator.Group group : groups) {
                    addGroupRow(root, group.noteId(), group.title(), group.fill(), group.border(),
                            filterEntries(group.nodes().stream().map(NoteGroupingCalculator.NodeRef::entry).toList(), query, pinnedOnly),
                            false, searching);
                }
                // The "Ungrouped" group is last with the neutral colors; while
                // a search is active it is skipped when nothing matches (like
                // all other empty groups), otherwise it is always shown.
                addGroupRow(root, UNGROUPED_GROUP_ID, "Ungrouped", UNGROUPED_FILL, UNGROUPED_BORDER,
                        filterEntries(ungrouped.stream().map(NoteGroupingCalculator.NodeRef::entry).toList(), query, pinnedOnly),
                        false, searching);

                if (searching) {
                    addCompartmentGroups(root, project, canvasState.canvasNodeIds(), query, pinnedOnly);
                }
            }

            treeModel.setRoot(root);
            reapplyExpandedGroups();
            updateInGroupPane();
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
        Set<UUID> canvasNodeIds = new HashSet<>();
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
            nodeRefs.add(new NoteGroupingCalculator.NodeRef(uuidString(node), bounds, createEntry(node, project)));
        }
        return new CanvasState(nodeRefs, noteRefs, canvasNodeIds);
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
        return new FindEntry(node.getUUIDInParentGraph(), node.getName(), nodeDescription(node), uuidString(node), compartmentId, compartmentName, node.isBookmarked());
    }

    private static String nodeDescription(JIPipeGraphNode node) {
        HTMLText description = node.getInfo().getDescription();
        return description != null ? description.toPlainText() : "";
    }

    private static String uuidString(JIPipeGraphNode node) {
        UUID uuid = node.getUUIDInParentGraph();
        return uuid != null ? uuid.toString() : "";
    }

    private static List<FindEntry> filterEntries(List<FindEntry> entries, String query, boolean pinnedOnly) {
        List<FindEntry> result = new ArrayList<>();
        for (FindEntry entry : entries) {
            if (!entry.matches(query)) {
                continue;
            }
            if (pinnedOnly && !entry.isBookmarked()) {
                continue;
            }
            result.add(entry);
        }
        return result;
    }

    private void addGroupRow(DefaultMutableTreeNode root, String groupId, String title, Color fill, Color border, List<FindEntry> entries, boolean compartmentGroup, boolean hideWhenEmpty) {
        if (hideWhenEmpty && entries.isEmpty()) {
            return;
        }
        GroupRow groupRow = new GroupRow(groupId, displayTitle(title), fill, border, entries, compartmentGroup);
        groupRows.add(groupRow);
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(groupRow);
        for (FindEntry entry : entries) {
            groupNode.add(new DefaultMutableTreeNode(new NodeRow(entry, compartmentGroup)));
        }
        root.add(groupNode);
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
    private void addCompartmentGroups(DefaultMutableTreeNode root, JIPipeProject project, Set<UUID> canvasNodeIds, String query, boolean pinnedOnly) {
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
            if (entry.getCompartmentId() == null) {
                continue;
            }
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
        updateInGroupPane();
    }

    private DefaultMutableTreeNode selectedTreeNode() {
        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath == null) {
            return null;
        }
        return selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode node ? node : null;
    }

    private void updateInGroupPane() {
        DefaultListModel<FindEntry> listModel = new DefaultListModel<>();
        for (FindEntry entry : selectedEntryMembers()) {
            listModel.addElement(entry);
        }
        inGroupList.setModel(listModel);
    }

    /**
     * The members of the group that contains the current selection (the union
     * of the groups for a multi-membership node; the ungrouped members when
     * the selection is ungrouped). Empty when nothing node-like is selected
     * (e.g. the selection is a cross-compartment search result, which has no
     * current-canvas grouping).
     */
    private List<FindEntry> selectedEntryMembers() {
        DefaultMutableTreeNode treeNode = selectedTreeNode();
        if (treeNode == null || !(treeNode.getUserObject() instanceof NodeRow nodeRow)) {
            return List.of();
        }
        Map<UUID, FindEntry> members = new LinkedHashMap<>();
        boolean found = false;
        for (GroupRow groupRow : groupRows) {
            if (groupRow.compartmentGroup()) {
                continue;
            }
            boolean contains = false;
            for (FindEntry entry : groupRow.entries()) {
                if (entry.getNodeId().equals(nodeRow.entry().getNodeId())) {
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
     * render the compartment badge.
     */
    private record NodeRow(FindEntry entry, boolean crossCompartment) {
    }

    /**
     * The current canvas state used for one rebuild.
     */
    private record CanvasState(List<NoteGroupingCalculator.NodeRef> nodeRefs, List<NoteGroupingCalculator.NoteRef> noteRefs, Set<UUID> canvasNodeIds) {
    }

    // ------------------------------------------------------------------
    // Selection model (group rows are not selectable)
    // ------------------------------------------------------------------

    /**
     * A selection model that filters out all group rows: only node leaves can
     * be selected (group rows remain expandable/collapsible).
     */
    private static final class NodeRowOnlySelectionModel extends DefaultTreeSelectionModel {
        private static TreePath[] filterNodeRows(TreePath[] paths) {
            if (paths == null) {
                return null;
            }
            List<TreePath> nodeRows = new ArrayList<>();
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof NodeRow) {
                    nodeRows.add(path);
                }
            }
            return nodeRows.toArray(new TreePath[0]);
        }

        @Override
        public void setSelectionPaths(TreePath[] paths) {
            super.setSelectionPaths(filterNodeRows(paths));
        }

        @Override
        public void addSelectionPaths(TreePath[] paths) {
            super.addSelectionPaths(filterNodeRows(paths));
        }
    }

    // ------------------------------------------------------------------
    // Renderers
    // ------------------------------------------------------------------

    /**
     * Renders the group header rows (colored swatch, title, member count) and
     * the node leaves (pin glyph, name, compartment badge, UUID fragment).
     */
    private final class FindTreeCellRenderer implements TreeCellRenderer {
        private final SolidColorIcon groupIcon = new SolidColorIcon(GROUP_SWATCH_SIZE, GROUP_SWATCH_SIZE, UNGROUPED_FILL, UNGROUPED_BORDER);
        private final Icon pinIcon = JIPipe.RESOURCES.getIcon16("actions/window-pin.png");
        private final JPanel groupPanel = new JPanel(new BorderLayout());
        private final JPanel groupTitlePanel = new JPanel();
        private final JLabel groupSwatchLabel = new JLabel();
        private final JLabel groupTitleLabel = new JLabel();
        private final JLabel groupCountLabel = new JLabel();
        private final JPanel nodePanel = new JPanel(new BorderLayout());
        private final JPanel nodeTitlePanel = new JPanel();
        private final JLabel nodePinLabel = new JLabel();
        private final JLabel nodeNameLabel = new JLabel();
        private final CompartmentBadgeLabel nodeBadgeLabel = new CompartmentBadgeLabel();
        private final JLabel nodeUUIDLabel = new JLabel();

        private FindTreeCellRenderer() {
            groupTitlePanel.setLayout(new BoxLayout(groupTitlePanel, BoxLayout.X_AXIS));
            groupTitlePanel.setOpaque(false);
            groupSwatchLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            groupTitleLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            groupTitlePanel.add(Box.createHorizontalStrut(2));
            groupTitlePanel.add(groupSwatchLabel);
            groupTitlePanel.add(Box.createHorizontalStrut(5));
            groupTitlePanel.add(groupTitleLabel);
            groupPanel.setOpaque(true);
            groupPanel.add(groupTitlePanel, BorderLayout.WEST);
            groupCountLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            groupPanel.add(groupCountLabel, BorderLayout.EAST);

            nodeTitlePanel.setLayout(new BoxLayout(nodeTitlePanel, BoxLayout.X_AXIS));
            nodeTitlePanel.setOpaque(false);
            nodePinLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            nodeNameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            nodeBadgeLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            nodeTitlePanel.add(nodePinLabel);
            nodeTitlePanel.add(Box.createHorizontalStrut(2));
            nodeTitlePanel.add(nodeNameLabel);
            nodeTitlePanel.add(Box.createHorizontalStrut(4));
            nodeTitlePanel.add(nodeBadgeLabel);
            nodePanel.setOpaque(true);
            nodePanel.add(nodeTitlePanel, BorderLayout.WEST);
            nodePanel.add(nodeUUIDLabel, BorderLayout.EAST);

            groupTitleLabel.setFont(groupTitleLabel.getFont().deriveFont(Font.BOLD));
            groupCountLabel.setForeground(DIM_TEXT_COLOR);
            nodeUUIDLabel.setForeground(DIM_TEXT_COLOR);
            nodeUUIDLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, nodeUUIDLabel.getFont().getSize() - UUID_FONT_SIZE_REDUCTION));
            nodeBadgeLabel.setForeground(COMPARTMENT_TEXT_COLOR);

            groupPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 6));
            nodePanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 6));
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
            groupIcon.setFillColor(groupRow.fill());
            groupIcon.setBorderColor(groupRow.border());
            groupSwatchLabel.setIcon(groupIcon);
            groupTitleLabel.setText(groupRow.title());
            groupTitleLabel.setForeground(groupRow.compartmentGroup() && !selected ? COMPARTMENT_TEXT_COLOR : groupPanel.getForeground());
            groupCountLabel.setText(String.valueOf(groupRow.entries().size()));
            groupCountLabel.setForeground(selected ? groupPanel.getForeground() : DIM_TEXT_COLOR);
            return groupPanel;
        }

        private Component configureNodeRow(NodeRow nodeRow, JTree tree, boolean selected) {
            applySelectionColors(nodePanel, tree, selected);
            FindEntry entry = nodeRow.entry();
            nodePinLabel.setIcon(entry.isBookmarked() ? pinIcon : null);
            nodePinLabel.setVisible(entry.isBookmarked());
            nodePinLabel.setToolTipText(entry.isBookmarked() ? "Pinned" : null);
            nodeNameLabel.setText(entry.getName());
            nodeNameLabel.setForeground(nodePanel.getForeground());
            nodeBadgeLabel.setText(entry.getCompartmentName());
            nodeBadgeLabel.setVisible(nodeRow.crossCompartment());
            nodeUUIDLabel.setText(uuidFragment(entry.getUuidString()));
            nodeUUIDLabel.setForeground(selected ? nodePanel.getForeground() : DIM_TEXT_COLOR);
            return nodePanel;
        }

        private void applySelectionColors(JPanel panel, JTree tree, boolean selected) {
            Color background = selected ? UIManager.getColor("Tree.selectionBackground") : tree.getBackground();
            Color foreground = selected ? UIManager.getColor("Tree.selectionForeground") : tree.getForeground();
            if (background != null) {
                panel.setBackground(background);
            }
            if (foreground != null) {
                panel.setForeground(foreground);
            }
        }

        private String uuidFragment(String uuidString) {
            if (uuidString == null || uuidString.length() <= UUID_FRAGMENT_LENGTH) {
                return "";
            }
            return uuidString.substring(0, UUID_FRAGMENT_LENGTH) + "...";
        }
    }

    /**
     * Renders the "In group" member rows (pin glyph + name).
     */
    private static final class InGroupListCellRenderer extends JPanel implements ListCellRenderer<FindEntry> {
        private final Icon pinIcon = JIPipe.RESOURCES.getIcon16("actions/window-pin.png");
        private final JLabel pinLabel = new JLabel();
        private final JLabel nameLabel = new JLabel();

        private InGroupListCellRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            pinLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
            add(pinLabel);
            add(Box.createHorizontalStrut(2));
            add(nameLabel);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FindEntry> list, FindEntry entry, int index, boolean selected, boolean hasFocus) {
            Color background = selected ? UIManager.getColor("List.selectionBackground") : list.getBackground();
            Color foreground = selected ? UIManager.getColor("List.selectionForeground") : list.getForeground();
            if (background != null) {
                setBackground(background);
            }
            if (foreground != null) {
                setForeground(foreground);
            }
            pinLabel.setIcon(entry.isBookmarked() ? pinIcon : null);
            nameLabel.setText(entry.getName());
            nameLabel.setForeground(getForeground());
            return this;
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

    /**
     * A single-line text field that renders a placeholder while empty.
     */
    private static final class SearchTextField extends JTextField {
        private final String placeholder;

        private SearchTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (placeholder.isEmpty() || !getText().isEmpty()) {
                return;
            }
            Color color = UIManager.getColor("TextField.inactiveForeground");
            g.setColor(color != null ? color : DIM_TEXT_COLOR);
            FontMetrics fontMetrics = g.getFontMetrics();
            Insets insets = getInsets();
            int y = (getHeight() - fontMetrics.getHeight()) / 2 + fontMetrics.getAscent();
            g.drawString(placeholder, insets.left + 1, y);
        }
    }
}
