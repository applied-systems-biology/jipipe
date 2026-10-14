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

package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DataSourcesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.run.JIPipeEphemeralRunnableExecutor;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.nodebrowser.NodeBrowserTreeController;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.ai.JIPipeDesktopAISetupDialog;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormHelpPanel;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopNodeDatabaseEntryListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeStateChips;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The adaptive node browser (Task 16), which replaces the old add-nodes
 * panel (Task 24: mounted in both graph editors, old panel deleted).
 * <p>
 * The browser is function-tree driven: a taxonomy function tree
 * (top-levels → leaves) selects which entries are shown, and the
 * taxonomy facets (framework / input+output data types / tags) plus the
 * search text post-filter them. The main area is <em>adaptive</em>
 * (a pure function of the dock width):
 * <ul>
 * <li>compact (&lt; 380 px, {@link NodeBrowserModel#TIER_COMPACT}): the
 * <b>single function tree with node leaves</b> — the tree takes over the
 * node-view role, so the panel stays usable in a narrow dock;</li>
 * <li>comfortable ({@link NodeBrowserModel#TIER_COMFORTABLE}): the
 * <b>function tree | filtered node list</b> side by side.</li>
 * </ul>
 * An active search never changes the layout: the list re-queries in place
 * while the tree <b>highlights the matching leafs</b> (bold, see
 * {@link NodeBrowserTreeController#updateSearchHighlights(boolean, List)}).
 * <p>
 * The non-Swing state lives in {@link NodeBrowserModel} (unit-tested);
 * this component renders it and reacts to its changes.
 * <p>
 * <b>Wiring note (Task 24):</b> this component is self-contained — it
 * reads from {@code JIPipe.getNodes()} and the (project's)
 * {@link JIPipeNodeDatabase} and holds no static state. It is mounted in
 * both graph editors' {@code DOCK_ADD_NODES} dock (Task 24b).
 */
public class JIPipeDesktopNodeBrowser extends JIPipeDesktopWorkbenchPanel implements JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener {

    /** The model holds all non-Swing browser state. */
    private final NodeBrowserModel model = new NodeBrowserModel();

    private final JIPipeNodeDatabase database;
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final boolean isCompartmentsEditor;

    // --- Toolbar (search + AI toggle lifted from the old add-nodes panel) ---
    private final JToolBar toolBar = new JToolBar();
    private final JIPipeEphemeralRunnableExecutor executor = new JIPipeEphemeralRunnableExecutor("Node browser");
    private final JIPipeDesktopSearchTextField searchField;
    private final JToggleButton aiSearchButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/ai.png"));
    private boolean aiSearch;

    // --- Filter bar ---
    private final JPanel filterBar = new JPanel();
    private final JComboBox<Object> frameworkCombo = new JComboBox<>();
    private JIPipeDesktopRichSelectionBox tagsBox;
    private JIPipeDesktopRichSelectionBox dataTypeBox;
    private final JToggleButton pinnedToggleButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));
    private final JToggleButton selectionOnlyButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/diagram-next.png"));

    // --- Tree (shared controller; see the nodebrowser package) ---
    private final NodeBrowserTreeController tree;

    // --- Results list ---
    private final JList<JIPipeNodeDatabaseEntry> resultList;
    private final JScrollPane resultScroll;

    // --- Follow-up strip ---
    private final JPanel followUpStrip = new JPanel();
    private final JPanel followUpContent = new JPanel();

    // --- Layout containers (re-rendered per view) ---
    private final JPanel bodyPanel = new JPanel();
    /** The tier at the last view render ({@code -1} = none yet). */
    private int lastRenderedTier = -1;

    /**
     * Whether the tree-structure changes of {@link #rebuildNodeLeaves()} are
     * in flight — the selection listener must not re-query (and not wipe the
     * branch filter) while the node leaves are being rebuilt (see
     * {@link NodeBrowserTreeController#rebuildNodeLeaves(List)}).
     */
    private boolean selectionSuppressed;

    // --- Last post-filtered results (the single source of truth for the
    // compact node leaves and the search highlights; see applyQueryResult) ---
    private List<JIPipeNodeDatabaseEntry> lastFiltered = List.of();

    /**
     * Creates a new node browser for the given workbench and graph editor.
     *
     * @param workbench     the desktop workbench
     * @param graphEditorUI the graph editor UI (provides the canvas + dock panel)
     */
    public JIPipeDesktopNodeBrowser(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(workbench);
        this.graphEditorUI = graphEditorUI;
        this.database = workbench instanceof JIPipeDesktopProjectWorkbench ?
            ((JIPipeDesktopProjectWorkbench) workbench).getNodeDatabase() : JIPipeNodeDatabase.getInstance();
        this.isCompartmentsEditor = graphEditorUI instanceof JIPipeDesktopCompartmentsGraphEditorUI;

        searchField = new JIPipeDesktopSearchTextField();
        searchField.addActionListener(e -> {
            model.setSearch(searchField.getText());
            applyViewIfNeeded(); // no-op while the tier is unchanged (the search never changes the layout)
            reloadList();
        });
        searchField.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume();
                    SwingUtilities.invokeLater(() -> {
                        if (!isCompartmentsEditor
                            && model.getTier(getWidth()) == NodeBrowserModel.TIER_COMPACT) {
                            // Compact single-pane: move the selection to the
                            // first node leaf of the tree, expanding its
                            // branches to reveal it
                            TreePath firstLeaf = tree.findFirstNodeLeafPath();
                            if (firstLeaf != null) {
                                selectionSuppressed = true;
                                try {
                                    tree.selectRow(firstLeaf);
                                } finally {
                                    selectionSuppressed = false;
                                }
                                tree.getTree().requestFocusInWindow();
                            }
                        } else {
                            // Two-pane (or compartments): move the selection
                            // to the node list's first row
                            resultList.requestFocusInWindow();
                            resultList.setSelectedIndex(0);
                        }
                    });
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    SwingUtilities.invokeLater(() -> {
                        insertFirstAtCursor();
                        graphEditorUI.getCanvasUI().requestFocus();
                    });
                }
            }
        });

        // Function tree (shared controller: the tree, the slim control bar
        // under it, the search-match highlighting, the collapse-all and
        // crosshairs actions).
        this.tree = new NodeBrowserTreeController();
        tree.getTree().setDragEnabled(true);
        tree.getTree().setTransferHandler(new TreeNodeTransferHandler(tree));
        tree.getTree().addTreeSelectionListener(e -> updateTreePathFromSelection());
        tree.setReferenceEntrySupplier(this::referenceEntry);
        tree.getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    JIPipeNodeDatabaseEntry entry = tree.entryFromPath(tree.getTree().getSelectionPath());
                    if (entry != null) {
                        insertAtCursor(entry);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handleTreePopupMenuTrigger(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleTreePopupMenuTrigger(e);
            }
        });

        // Results list
        resultList = new JList<>();
        resultList.setModel(new DefaultListModel<>());
        resultList.setCellRenderer(new JIPipeDesktopNodeDatabaseEntryListCellRenderer());
        resultList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resultList.setOpaque(false);
        resultList.setDragEnabled(true);
        resultList.setTransferHandler(new JIPipeDesktopAddNodeTransferHandler());
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    JIPipeNodeDatabaseEntry entry = resultList.getSelectedValue();
                    if (entry != null) {
                        insertAtCursor(entry);
                    }
                }
            }
        });
        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    SwingUtilities.invokeLater(() -> {
                        JIPipeNodeDatabaseEntry entry = resultList.getSelectedValue();
                        if (entry != null) {
                            insertAtCursor(entry);
                            graphEditorUI.getCanvasUI().requestFocus();
                        }
                    });
                } else if (e.getKeyCode() == KeyEvent.VK_UP && resultList.getSelectedIndex() <= 0) {
                    e.consume();
                    SwingUtilities.invokeLater(() -> {
                        searchField.getTextField().requestFocusInWindow();
                        searchField.getTextField().selectAll();
                    });
                }
            }
        });
        initializeResultListContextMenu();
        resultScroll = new JScrollPane(resultList);
        resultScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Follow-up strip
        followUpStrip.setLayout(new BorderLayout());
        followUpStrip.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtils.getControlBorderColor()));
        followUpContent.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        followUpStrip.add(followUpContent, BorderLayout.CENTER);

        initializeToolbar();
        initializeFilterBar();
        initializeBody();
        // Only the root ("All functions") is expanded by default: the
        // top-level function categories stay collapsed, so the tree opens
        // to a short overview, not a wall of categories (the controller's
        // populateTree expands exactly the root).
        tree.populateTree(isCompartmentsEditor ?
            JIPipeNodeDatabasePipelineVisibility.Compartments :
            JIPipeNodeDatabasePipelineVisibility.Pipeline);
        applyViewIfNeeded();

        // The layout is tier-adaptive (a pure function of the width);
        // re-applying is a no-op while the tier is unchanged.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyViewIfNeeded();
            }
        });

        // Recompute the ⊂ selection-only rule on canvas selection change
        graphEditorUI.getSelectionManager().getNodeSelectionChangedEventEmitter().subscribe(this);

        applySelectionOnlyState();
        updateFollowUpStrip();
        reloadList();
    }

    // ------------------------------------------------------------------
    // Toolbar (lifted from the old add-nodes panel's toolbar)
    // ------------------------------------------------------------------

    private void initializeToolbar() {
        toolBar.setFloatable(false);
        toolBar.setBorder(null);
        toolBar.add(searchField);

        // AI-based search toggle (lifted behavior: ensure setup, then reload)
        UIUtils.makeButtonFlatWithSize(aiSearchButton, 36);
        aiSearchButton.setSelected(aiSearch);
        aiSearchButton.setToolTipText("Use AI-based node search using an embedding model");
        aiSearchButton.addActionListener(e -> {
            aiSearch = aiSearchButton.isSelected();
            if (aiSearch) {
                if (JIPipeDesktopAISetupDialog.checkFirstTimeSetup(getDesktopWorkbench())) {
                    // Spin up the embedding model already
                    JIPipe.getInstance().getAiService().tryStartEmbeddingModel();
                    // Clear list to indicate AI switch, then reload
                    resultList.setModel(new DefaultListModel<>());
                    reloadList();
                }
            } else {
                reloadList();
            }
        });
        if (AIApplicationSettings.getInstance().isEnableAI()) {
            toolBar.add(aiSearchButton);
        }
    }

    // ------------------------------------------------------------------
    // Filter bar
    // ------------------------------------------------------------------

    private void initializeFilterBar() {
        filterBar.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        filterBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Framework combo (first item = "all frameworks" sentinel). Only
        // frameworks with at least one node visible in THIS editor role are
        // listed — a framework nobody's nodes use (e.g. Comet) would just be
        // an empty, confusing filter option.
        frameworkCombo.addItem(new AllFrameworksSentinel());
        Set<String> populatedFrameworks = new LinkedHashSet<>();
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            TaxonomyView.BehaviorFlags flags = TaxonomyView.behaviorFlags(info);
            boolean visibleInRole = isCompartmentsEditor ? flags.isVisibleInCompartments() : flags.isVisibleInPipeline();
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
            model.setFramework(selected instanceof JIPipeNodeTaxonomyFramework fw ? fw : null);
            reloadList();
        });
        frameworkCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof JIPipeNodeTaxonomyFramework fw) {
                    setText(fw.getName());
                }
                return this;
            }
        });
        filterBar.add(frameworkCombo);

        // Data types (input + output, multi) — a rich selection box with a
        // search bar and the data types' icons (replaces the former
        // "Spatial" + "Input" buttons and their checkbox popup)
        dataTypeBox = new JIPipeDesktopRichSelectionBox("Data", true, true, this::dataTypeOptions, selection -> {
            Set<Class<? extends JIPipeData>> inputs = new LinkedHashSet<>();
            Set<Class<? extends JIPipeData>> outputs = new LinkedHashSet<>();
            for (JIPipeDesktopRichSelectionBox.Option<?> option : selection) {
                if (option.getValue() instanceof TypedDataValue typed) {
                    if (typed.input()) {
                        inputs.add(typed.dataClass());
                    } else {
                        outputs.add(typed.dataClass());
                    }
                }
            }
            model.setInputDataTypes(inputs);
            model.setOutputDataTypes(outputs);
            reloadList();
        });
        dataTypeBox.setToolTipText("Filter by input and/or output data type (select one or more)");
        filterBar.add(dataTypeBox);

        // Tags (multi) — a rich selection box with a search bar (the merged
        // tag vocabulary is far too long for a plain combo). Populated from
        // the merged tags (node-level + function-path tags) — the same set
        // the filter predicate matches against.
        tagsBox = new JIPipeDesktopRichSelectionBox("Tags", true, this::tagOptions, selection -> {
            Set<String> selectedTags = new LinkedHashSet<>();
            for (JIPipeDesktopRichSelectionBox.Option<?> option : selection) {
                if (option.getValue() instanceof String tag) {
                    selectedTags.add(tag);
                }
            }
            model.setTags(selectedTags);
            reloadList();
        });
        filterBar.add(tagsBox);
        filterBar.add(Box.createHorizontalStrut(8));

        // ★ pinned toggle
        pinnedToggleButton.setToolTipText("Show only pinned nodes");
        UIUtils.makeButtonFlatWithSize(pinnedToggleButton, 28);
        pinnedToggleButton.addActionListener(e -> {
            model.setPinnedOnly(pinnedToggleButton.isSelected());
            reloadList();
        });
        filterBar.add(pinnedToggleButton);

        // ⊂ selection-only toggle
        selectionOnlyButton.setToolTipText("Show only nodes compatible with the current selection's output");
        UIUtils.makeButtonFlatWithSize(selectionOnlyButton, 28);
        selectionOnlyButton.addActionListener(e -> {
            model.setSelectionOnly(selectionOnlyButton.isSelected());
            applySelectionOnlyState();
            reloadList();
        });
        filterBar.add(selectionOnlyButton);
    }

    /** A data-type filter option value: (data class, input-side or output-side). */
    private record TypedDataValue(Class<? extends JIPipeData> dataClass, boolean input) {
    }

    /**
     * The data-type options of the "Data" rich selection box: one per
     * unhidden registered data type, split into the "Input" and "Output"
     * groups, each carrying the data type's icon.
     */
    private List<JIPipeDesktopRichSelectionBox.Option<?>> dataTypeOptions() {
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
    private List<JIPipeDesktopRichSelectionBox.Option<?>> tagOptions() {
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

    // ------------------------------------------------------------------
    // Body (tree + results, tier- and search-aware)
    // ------------------------------------------------------------------

    private void initializeBody() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);

        // The body holds the filter bar (top) + the view-dependent center;
        // applyViewIfNeeded() (called from the constructor, the resize
        // listener, and the search listener) packs it per the current view.
        bodyPanel.setLayout(new BorderLayout());
        add(bodyPanel, BorderLayout.CENTER);
        add(followUpStrip, BorderLayout.SOUTH);
    }

    /**
     * Applies the main-area layout. Unlike the earlier tier-based design,
     * the pipeline view is <em>always</em> the function tree | filtered node
     * list side by side — it no longer collapses to a tree-only pane when
     * the dock narrows, and an active search no longer hides the tree (it
     * re-queries the list in place and highlights the matching tree leafs,
     * see {@link NodeBrowserTreeController#updateSearchHighlights(boolean, List)}). This keeps the panel usable
     * at any width: there is always a node list to pick from.
     * <p>
     * The compartments editor keeps its special flat-list layout (its node
     * set has no taxonomy). Both branches are idempotent, so it is safe to
     * call from the resize listener on every resize, from the search
     * listener, and from {@link #applyQueryResult}.
     */
    private void applyViewIfNeeded() {
        // The compartments graph has a special node set ("Graph compartment"
        // + canvas notes); the taxonomy function tree and its facets do not
        // apply to it. The browser is therefore a plain search box + flat
        // node list, mirroring the legacy add-nodes panel's "Compartments
        // have no categories". Idempotent: no-op when the flat list is
        // already the only body child.
        if (isCompartmentsEditor) {
            if (bodyPanel.getComponentCount() != 1
                || bodyPanel.getComponent(0) != resultScroll) {
                bodyPanel.removeAll();
                bodyPanel.add(resultScroll, BorderLayout.CENTER);
                bodyPanel.revalidate();
                bodyPanel.repaint();
            }
            return;
        }

        // Pipeline editor: the adaptive design switches between the
        // compact single-pane (a narrow dock, < 380 px: the function tree
        // WITH node leaves — the tree takes over the node-view role) and the
        // comfortable two-pane (the function tree | filtered node list side
        // by side). An active search never changes the layout: the list
        // re-queries in place and the tree highlights the matches.
        // Idempotent: a no-op while the tier is unchanged.
        int tier = model.getTier(getWidth());
        if (tier == lastRenderedTier) {
            return;
        }
        lastRenderedTier = tier;
        bodyPanel.removeAll();
        bodyPanel.add(filterBar, BorderLayout.NORTH);
        if (tier == NodeBrowserModel.TIER_COMPACT) {
            bodyPanel.add(tree, BorderLayout.CENTER);
        } else {
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, resultScroll);
            split.setResizeWeight(0.35);
            split.setContinuousLayout(true);
            bodyPanel.add(split, BorderLayout.CENTER);
        }
        // Keep the compact node leaves in sync with the view
        rebuildNodeLeaves();
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    // ------------------------------------------------------------------
    // Compact node leaves (children of the function tree)
    // ------------------------------------------------------------------

    /**
     * Syncs the controller's node leaves with the current view tier: only
     * the compact single-pane shows the results as node leaves (the tree
     * takes over the node-view role there). The {@code selectionSuppressed}
     * guard keeps the leaf rebuild's model reload from tripping the tree
     * selection listener (which would re-query the list).
     */
    private void rebuildNodeLeaves() {
        if (isCompartmentsEditor
            || model.getTier(getWidth()) != NodeBrowserModel.TIER_COMPACT) {
            return;
        }
        selectionSuppressed = true;
        try {
            tree.rebuildNodeLeaves(lastFiltered);
        } finally {
            selectionSuppressed = false;
        }
    }

    // ------------------------------------------------------------------
    // Tree path + selection
    // ------------------------------------------------------------------

    private void updateTreePathFromSelection() {
        if (selectionSuppressed) {
            return;
        }
        TreePath path = tree.getTree().getSelectionPath();
        // A node leaf (compact view) is an insert/drag target, not a branch
        // filter — selecting it must not re-query.
        if (path != null && tree.entryFromPath(path) != null) {
            return;
        }
        // Function rows filter by their path; the root row ("All
        // functions") and an empty selection both clear the branch filter
        // (the root IS "all nodes", so selecting it must not be a no-op).
        List<JIPipeNodeTaxonomyFunction> pathFunctions = new ArrayList<>();
        if (path != null) {
            for (Object node : path.getPath()) {
                if (node instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction fn) {
                    pathFunctions.add(fn);
                }
            }
        }
        model.setTreePath(pathFunctions);
        reloadList();
    }


    // ------------------------------------------------------------------
    // Results list (post-filter via queryWithFacets + model.matches + tree)
    // ------------------------------------------------------------------

    private void reloadList() {
        // Query on a worker thread (the AI search in particular must not
        // block the EDT); the UI-side filters and the model update happen on
        // the EDT in applyQueryResult.
        model.setSearch(searchField.getText());
        executor.cancelAll();
        executor.enqueue(new ReloadRun(this));
    }

    private void applyQueryResult(List<JIPipeNodeDatabaseEntry> queryResult, Set<String> pinnedIds) {
        // Keep the model in sync with the field (the debounced search field
        // listener does this as well)
        model.setSearch(searchField.getText());

        List<JIPipeNodeDatabaseEntry> filtered = new ArrayList<>();
        for (JIPipeNodeDatabaseEntry entry : queryResult) {
            if (model.isPinnedOnly() && !pinnedIds.contains(entry.getId())) {
                continue;
            }
            // Selection-only (⊂) rule: candidate must be connectable to the
            // current canvas selection's output
            if (model.isSelectionOnly() && !passesSelectionOnlyRule(entry)) {
                continue;
            }
            filtered.add(entry);
        }

        // Tree filter: when not in search mode and a leaf is selected, only
        // entries whose function ids intersect the selected leaf (+descendants)
        if (!model.isSearchActive() && !model.getTreePath().isEmpty()) {
            JIPipeNodeTaxonomyFunction leaf = model.getTreePath().get(model.getTreePath().size() - 1);
            Set<String> descendantIds = NodeBrowserModel.descendantIds(JIPipe.getNodes().getFunctions(), leaf);
            filtered.removeIf(entry -> {
                Set<String> functionIds = entry.getFunctionIds();
                for (String fid : functionIds) {
                    if (descendantIds.contains(fid)) {
                        return false;
                    }
                }
                return true;
            });
        }

        // Down-ranked nodes (auto-imported, deprecated, unstable — the
        // generalized state classifiers, see JIPipeNodeStateChips) come
        // after the standard curated nodes; the query's own ranking is
        // preserved within each group (stable partition).
        List<JIPipeNodeDatabaseEntry> ordered = new ArrayList<>(filtered);
        ordered.sort(Comparator.comparingInt(e -> JIPipeNodeStateChips.isDownRanked(e) ? 1 : 0));
        lastFiltered = ordered;

        DefaultListModel<JIPipeNodeDatabaseEntry> listModel = new DefaultListModel<>();
        for (JIPipeNodeDatabaseEntry entry : ordered) {
            listModel.addElement(entry);
        }
        resultList.setModel(listModel);
        if (!listModel.isEmpty()) {
            resultList.setSelectedIndex(0);
        }
        // Highlight the tree leafs the search matched (no-op without search).
        tree.updateSearchHighlights(model.isSearchActive(), lastFiltered);
        // Re-apply the layout (idempotent; the tier may have flipped) and
        // rebuild the compact node leaves from this same `ordered` list
        // (single source of truth) so they always match the flat list.
        applyViewIfNeeded();
        rebuildNodeLeaves();
    }

    // ------------------------------------------------------------------
    // ⊂ selection-only rule (R1)
    // ------------------------------------------------------------------

    /**
     * The ⊂ selection-only rule: a candidate passes iff the canvas
     * selection is non-empty and for at least one selected node's output
     * slot there exists a candidate input slot whose data type is
     * compatible with the output's data type
     * ({@code JIPipe.getDataTypes().isConvertible(outputClass, inputClass)}).
     * Candidates with no input slots are excluded.
     */
    private boolean passesSelectionOnlyRule(JIPipeNodeDatabaseEntry candidate) {
        JIPipeDesktopGraphCanvasUI canvasUI = graphEditorUI.getCanvasUI();
        Set<? extends JIPipeDesktopGraphInteractiveObjectUI> selection = canvasUI.getSelectionManager().getSelection();
        if (selection.isEmpty()) {
            return false;
        }
        // Collect the output data classes of all selected nodes
        Set<Class<? extends JIPipeData>> selectedOutputClasses = new HashSet<>();
        for (JIPipeDesktopGraphInteractiveObjectUI object : selection) {
            if (object instanceof JIPipeDesktopGraphNodeUI nodeUI) {
                for (JIPipeOutputDataSlot outSlot : nodeUI.getNode().getOutputSlots()) {
                    selectedOutputClasses.add(outSlot.getInfo().getDataClass());
                }
            }
        }
        if (selectedOutputClasses.isEmpty()) {
            return false;
        }
        // Candidate must have at least one input slot
        Map<String, JIPipeDataSlotInfo> candidateInputs = candidate.getInputSlots();
        if (candidateInputs.isEmpty()) {
            return false;
        }
        // A candidate passes iff ∃ selected output O, ∃ candidate input I
        // with isConvertible(O's class, I's class)
        for (Class<? extends JIPipeData> outClass : selectedOutputClasses) {
            for (JIPipeDataSlotInfo inputSlot : candidateInputs.values()) {
                if (JIPipe.getDataTypes().isConvertible(outClass, inputSlot.getDataClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Applies the selection-only UI state: the ⊂ button is only enabled
     * when the canvas selection is non-empty (the rule can only be
     * meaningful then). Called on selection change and on init.
     */
    private void applySelectionOnlyState() {
        boolean hasSelection = !graphEditorUI.getCanvasUI().getSelectionManager().getSelection().isEmpty();
        selectionOnlyButton.setEnabled(hasSelection);
        if (!hasSelection) {
            // If the selection is cleared, force the toggle off and reload
            if (model.isSelectionOnly()) {
                model.setSelectionOnly(false);
                selectionOnlyButton.setSelected(false);
                reloadList();
            }
        }
    }

    @Override
    public void onGraphCanvasNodeSelectionChanged(JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent event) {
        applySelectionOnlyState();
        if (model.isSelectionOnly()) {
            reloadList();
        }
        updateFollowUpStrip();
    }

    // ------------------------------------------------------------------
    // Insertion (double-click / Enter ⇒ entry.addToGraph(canvasUI))
    // ------------------------------------------------------------------

    private void insertFirstAtCursor() {
        if (!isCompartmentsEditor
            && model.getTier(getWidth()) == NodeBrowserModel.TIER_COMPACT) {
            // Compact single-pane: the current view is the tree, so the
            // first hit is its first node leaf (depth-first in tree order)
            TreePath firstLeaf = tree.findFirstNodeLeafPath();
            if (firstLeaf != null) {
                JIPipeNodeDatabaseEntry entry = tree.entryFromPath(firstLeaf);
                if (entry != null) {
                    insertAtCursor(entry);
                }
            }
            return;
        }
        // Two-pane (or compartments): the first hit is list row 0
        JIPipeNodeDatabaseEntry entry = resultList.getModel().getSize() > 0
            ? resultList.getModel().getElementAt(0) : null;
        if (entry != null) {
            insertAtCursor(entry);
        }
    }

    /**
     * The reference entry the tree's crosshairs action reveals: the first
     * node selected in the graph, when there is one (the first element of
     * the canvas selection), otherwise the first result row.
     */
    private JIPipeNodeDatabaseEntry referenceEntry() {
        Set<? extends JIPipeDesktopGraphInteractiveObjectUI> selection =
            graphEditorUI.getCanvasUI().getSelectionManager().getSelection();
        if (!selection.isEmpty()) {
            JIPipeDesktopGraphInteractiveObjectUI first = selection.iterator().next();
            if (first instanceof JIPipeDesktopGraphNodeUI nodeUI) {
                JIPipeNodeDatabaseEntry entry = entryByOriginNodeId(nodeUI.getNode().getInfo().getId());
                if (entry != null) {
                    return entry;
                }
            }
        }
        return lastFiltered.isEmpty() ? null : lastFiltered.get(0);
    }

    private void insertAtCursor(JIPipeNodeDatabaseEntry entry) {
        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(getDesktopWorkbench())) {
            return;
        }
        // entry.addToGraph(...) snapshots the history internally and adds the
        // node(s) to the canvas (mirrors the old add-nodes panel's
        // insertAtCursor); then select the added node UIs.
        Set<JIPipeDesktopGraphNodeUI> nodeUIs = entry.addToGraph(graphEditorUI.getCanvasUI());
        graphEditorUI.getCanvasUI().getSelectionManager().setSelection(nodeUIs);
    }

    // ------------------------------------------------------------------
    // Pinned (lifted settings key: "node-db" / "pinned-node-ids")
    // ------------------------------------------------------------------

    public Set<String> getPinnedNodeDatabaseEntries() {
        return new HashSet<>(JIPipe.getSettings().getListFromRegistry("node-db", JIPipePathMetadataStore.key("pinned-node-ids"), String.class, true));
    }

    private void pinNodes(List<JIPipeNodeDatabaseEntry> entries) {
        List<String> ids = JIPipe.getSettings().getListFromRegistry("node-db", JIPipePathMetadataStore.key("pinned-node-ids"), String.class, true);
        for (JIPipeNodeDatabaseEntry entry : entries) {
            if (!ids.contains(entry.getId())) {
                ids.add(entry.getId());
            }
        }
        JIPipe.getSettings().save();
        reloadList();
    }

    private void unpinNodes(List<JIPipeNodeDatabaseEntry> entries) {
        List<String> ids = JIPipe.getSettings().getListFromRegistry("node-db", JIPipePathMetadataStore.key("pinned-node-ids"), String.class, true);
        for (JIPipeNodeDatabaseEntry entry : entries) {
            ids.remove(entry.getId());
        }
        JIPipe.getSettings().save();
        reloadList();
    }

    // ------------------------------------------------------------------
    // Data-source menu (ported from the old add-nodes panel, which is
    // deleted in this task; the per-category menus are not ported — the
    // tree takes over the "see what's available" role)
    // ------------------------------------------------------------------

    /**
     * Initializes the data-source quick menu: one submenu per data type
     * (grouped by the data types' menu paths), each listing the data source
     * nodes that can produce that type. Ported from the old add-nodes panel's
     * {@code initializeAddDataSourceMenu} — same {@code getDataTypesByMenuPaths()}
     * iteration, same {@code getMenuDataSourcesFor(dataClass)} lookup, same
     * hidden data-type / node skips, same {@code newInstance()} +
     * {@code snapshotBeforeAddNode} + {@code insertNode} (the old method's
     * {@code addedAlgorithms} accumulation had no user-visible effect, so it
     * is dropped).
     */
    private void initializeAddDataSourceMenu(JPopupMenu menu) {
        JIPipeGraph algorithmGraph = graphEditorUI.getGraph();
        Map<String, Set<Class<? extends JIPipeData>>> dataTypesByMenuPaths = JIPipe.getDataTypes().getDataTypesByMenuPaths();
        Map<String, JComponent> menuTree = UIUtils.createMenuTree(menu, dataTypesByMenuPaths.keySet());

        for (Map.Entry<String, Set<Class<? extends JIPipeData>>> entry : dataTypesByMenuPaths.entrySet()) {
            JComponent subMenu = menuTree.get(entry.getKey());
            for (Class<? extends JIPipeData> dataTypeClass : JIPipeData.getSortedList(entry.getValue())) {
                if (JIPipeData.isHidden(dataTypeClass))
                    continue;
                Set<JIPipeNodeInfo> dataSources = JIPipe.getNodes().getMenuDataSourcesFor(dataTypeClass);
                boolean isEmpty = true;
                Icon icon = JIPipe.getDataTypes().getIconFor(dataTypeClass);
                JMenu dataMenu = new JMenu(JIPipeData.getNameOf(dataTypeClass));
                dataMenu.setIcon(icon);

                for (JIPipeNodeInfo info : dataSources) {
                    if (info.isHidden())
                        continue;
                    JMenuItem addItem = new JMenuItem(info.getName(), JIPipe.getNodes().getIconFor(info));
                    addItem.setToolTipText(TooltipUtils.getAlgorithmTooltip(info));
                    addItem.addActionListener(e -> {
                        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(graphEditorUI.getDesktopWorkbench()))
                            return;
                        JIPipeGraphNode node = info.newInstance();
                        graphEditorUI.getHistoryJournal().snapshotBeforeAddNode(node, graphEditorUI.getCompartment());
                        algorithmGraph.insertNode(node, graphEditorUI.getCompartment());
                    });
                    dataMenu.add(addItem);
                    isEmpty = false;
                }

                subMenu.add(dataMenu);
                if (isEmpty)
                    dataMenu.setVisible(false);
            }
        }

        // Remove empty menus
        boolean changed;
        Set<JComponent> invisible = new HashSet<>();
        do {
            changed = false;
            for (JComponent item : menuTree.values()) {
                if (invisible.contains(item)) {
                    continue;
                }
                boolean hasVisible = false;
                for (int i = 0; i < UIUtils.getMenuItemCount(item); i++) {
                    if (UIUtils.getMenuItem(item, i).isVisible()) {
                        hasVisible = true;
                        break;
                    }
                }
                if (!hasVisible) {
                    item.setVisible(false);
                    invisible.add(item);
                    changed = true;
                }
            }
        }
        while (changed);
    }

    /**
     * Right-click trigger for the function tree (the {@code isPopupTrigger}
     * check on both press and release covers all platforms). When the clicked
     * row is on the {@code DataSourcesFunction} branch or one of its
     * {@code *DataFunction} leaf branches, the full data-source menu is shown
     * — rebuilt on each show (reloaded-on-show, like the old panel's
     * reloadable pattern). Right-clicks on any other tree row (or on a node
     * leaf row) are ignored: the old per-category {@code menuPath} popups are
     * removed by the redesign. The full menu is shown on the branch <em>and</em>
     * its leaves (a verifiable per-leaf data-type pre-scope is not available).
     */
    private void handleTreePopupMenuTrigger(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        TreePath path = tree.getTree().getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }
        JIPipeNodeTaxonomyFunction branch = tree.functionFromTreePath(path);
        if (branch == null || !isUnderDataSources(branch)) {
            return;
        }
        JPopupMenu popupMenu = new JPopupMenu();
        initializeAddDataSourceMenu(popupMenu);
        if (popupMenu.getComponentCount() > 0) {
            popupMenu.revalidate();
            popupMenu.show(tree, e.getX(), e.getY());
        }
    }

    /**
     * Whether the given function is {@code DataSourcesFunction} itself or a
     * descendant of it (walks the function's {@link JIPipeNodeTaxonomyFunction#
     * getParent()} chain up to the root).
     */
    private boolean isUnderDataSources(JIPipeNodeTaxonomyFunction function) {
        JIPipeNodeTaxonomyFunction current = function;
        while (current != null) {
            if (current instanceof DataSourcesFunction) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Context menu (lifted from old panel)
    // ------------------------------------------------------------------

    private void initializeResultListContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        UIUtils.addRightClickPopupMenuToJList(resultList, contextMenu, () -> {
            contextMenu.removeAll();
            List<JIPipeNodeDatabaseEntry> selected = resultList.getSelectedValuesList();
            if (!selected.isEmpty()) {
                Set<String> pinned = getPinnedNodeDatabaseEntries();
                contextMenu.add(UIUtils.createMenuItem("Insert at cursor", "Inserts the node at the cursor",
                    JIPipe.RESOURCES.getIcon16("actions/add.png"), () -> insertAtCursor(selected.get(0))));
                UIUtils.addSeparatorIfNeeded(contextMenu);
                if (selected.stream().anyMatch(entry -> !pinned.contains(entry.getId()))) {
                    contextMenu.add(UIUtils.createMenuItem("Pin", "Adds the item to the list of pinned items",
                        JIPipe.RESOURCES.getIcon16("actions/window-pin.png"), () -> pinNodes(selected)));
                }
                if (selected.stream().anyMatch(entry -> pinned.contains(entry.getId()))) {
                    contextMenu.add(UIUtils.createMenuItem("Unpin", "Removes the item from the list of pinned items",
                        JIPipe.RESOURCES.getIcon16("actions/window-unpin.png"), () -> unpinNodes(selected)));
                }
                contextMenu.addSeparator();
                contextMenu.add(UIUtils.createMenuItem("Show documentation", "Displays the documentation of this node",
                    JIPipe.RESOURCES.getIcon16("actions/help.png"), () -> showDocumentation(selected.get(0))));
            }
        });
    }

    private void showDocumentation(JIPipeNodeDatabaseEntry entry) {
        JIPipeDesktopDockPanel dockPanel = graphEditorUI.getDockPanel();
        JIPipeDesktopFormHelpPanel helpPanel = dockPanel.getPanelComponent(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_HELP, JIPipeDesktopFormHelpPanel.class);
        if (helpPanel == null) {
            helpPanel = new JIPipeDesktopFormHelpPanel();
            dockPanel.addDockPanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_HELP,
                "Documentation",
                JIPipe.RESOURCES.getIcon24("actions/help-question.png"),
                JIPipeDesktopDockPanel.PanelLocation.BottomRight,
                true, 0, helpPanel);
        }
        dockPanel.activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_NODE_CONTEXT_HELP, false);
        helpPanel.showContent(entry.getName(), new MarkdownText("# " + entry.getName() + "\n\n" + entry.getDescription().getBody()));
    }

    // ------------------------------------------------------------------
    // Follow-up strip
    // ------------------------------------------------------------------

    /**
     * Populates (or collapses) the follow-up strip based on the current
     * canvas selection: when exactly one node is selected and its entry
     * has follow-ups, the strip shows them as clickable buttons.
     */
    private void updateFollowUpStrip() {
        followUpContent.removeAll();
        boolean show = false;
        Set<? extends JIPipeDesktopGraphInteractiveObjectUI> selection = graphEditorUI.getCanvasUI().getSelectionManager().getSelection();
        if (selection.size() == 1) {
            JIPipeDesktopGraphInteractiveObjectUI selected = selection.iterator().next();
            if (selected instanceof JIPipeDesktopGraphNodeUI nodeUI) {
                JIPipeGraphNode node = nodeUI.getNode();
                String nodeId = node.getInfo().getId();
                JIPipeNodeDatabaseEntry entry = entryByOriginNodeId(nodeId);
                if (entry != null) {
                    // Follow-ups first (the workflow successors), then related
                    // nodes (same-family / same-framework / shared-tag) that
                    // are not already follow-ups, de-duplicated by entry id.
                    List<JIPipeNodeDatabaseEntry> followUps = database.followUps(entry);
                    Set<String> added = new HashSet<>();
                    for (JIPipeNodeDatabaseEntry followUp : followUps) {
                        added.add(followUp.getId());
                    }
                    List<JIPipeNodeDatabaseEntry> suggestions = new ArrayList<>(followUps);
                    for (JIPipeNodeDatabaseEntry related : database.related(entry, 6)) {
                        if (added.add(related.getId())) {
                            suggestions.add(related);
                        }
                    }
                    if (!suggestions.isEmpty()) {
                        show = true;
                        followUpContent.add(new JLabel(JIPipe.RESOURCES.getIcon16("actions/diagram-next.png")));
                        for (JIPipeNodeDatabaseEntry suggestion : suggestions) {
                            JButton button = new JButton(suggestion.getName(), suggestion.getIcon());
                            button.setToolTipText("Suggested next step for the selected node");
                            button.setBorder(UIUtils.createControlBorder());
                            button.addActionListener(e -> insertAtCursor(suggestion));
                            followUpContent.add(button);
                        }
                    }
                }
            }
        }
        followUpStrip.setVisible(show);
        followUpStrip.revalidate();
        followUpStrip.repaint();
    }

    private JIPipeNodeDatabaseEntry entryByOriginNodeId(String originNodeId) {
        for (JIPipeNodeDatabaseEntry entry : database.getEntries()) {
            if (originNodeId.equals(entry.getOriginNodeId())) {
                return entry;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Accessors (for Task 24 wiring)
    // ------------------------------------------------------------------

    public NodeBrowserModel getModel() {
        return model;
    }

    public JIPipeNodeDatabase getDatabase() {
        return database;
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    public void focusSearchBar() {
        JTextField textField = searchField.getTextField();
        SwingUtilities.invokeLater(() -> {
            textField.selectAll();
            textField.grabFocus();
            textField.requestFocus();
            searchField.grabAttentionAnimation();
        });
    }

    // ------------------------------------------------------------------
    // Combo sentinels
    // ------------------------------------------------------------------

    /**
     * Worker that queries the node database off the EDT (the AI search in
     * particular must not block the EDT) and hands the raw result back to the
     * EDT, where the UI-side filters (pinned-only, selection-only, tree) and
     * the list render are applied. Mirrors the old panel's {@code ReloadListRun}.
     */
    private static class ReloadRun extends DefaultJIPipeRunnable {

        private final JIPipeDesktopNodeBrowser browser;

        private ReloadRun(JIPipeDesktopNodeBrowser browser) {
            this.browser = browser;
        }

        @Override
        public String getTaskLabel() {
            return "Reload node browser";
        }

        @Override
        public void run() {
            JIPipeNodeDatabasePipelineVisibility role = browser.isCompartmentsEditor ?
                JIPipeNodeDatabasePipelineVisibility.Compartments : JIPipeNodeDatabasePipelineVisibility.Pipeline;
            Set<String> pinnedIds = browser.getPinnedNodeDatabaseEntries();
            boolean aiSearch = browser.aiSearch;
            NodeBrowserModel model = browser.model;

            List<JIPipeNodeDatabaseEntry> queryResult = browser.database.queryWithFacets(
                browser.searchField.getText(),
                role,
                false,
                true,
                pinnedIds,
                model.getFramework(),
                model.getSpatial(),
                model.getInput(),
                model.getTags(),
                aiSearch ? JIPipeNodeDatabase.FLAG_USE_AI_SEARCH : null);

            SwingUtilities.invokeLater(() -> browser.applyQueryResult(queryResult, pinnedIds));
        }
    }

    /**
     * DnD source for the function tree. The canvas only accepts node-graph
     * JSON (the transfer data produced by
     * {@link JIPipeDesktopAddNodeTransferHandler}), so the selected tree row
     * is resolved to a database entry and delegated to that handler — the
     * same payload the results list produces:
     * <ul>
     * <li>a <em>node leaf</em> row (compact view) drags exactly that entry;
     * <li>a <em>function</em> row (branch) is resolved to the first database
     * entry that appears under it (or one of its descendants) — unchanged
     * from before node leaves existed; dragging a branch with no matching
     * entry (e.g. an unconverted branch) produces no droppable data.
     * </ul>
     * The root row is not draggable.
     */
    private class TreeNodeTransferHandler extends TransferHandler {
        private final NodeBrowserTreeController treeController;
        private final JIPipeDesktopAddNodeTransferHandler delegate = new JIPipeDesktopAddNodeTransferHandler();

        private TreeNodeTransferHandler(NodeBrowserTreeController treeController) {
            this.treeController = treeController;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            TreePath path = treeController.getTree().getSelectionPath();
            if (path == null) {
                return null;
            }
            JIPipeNodeDatabaseEntry entry = treeController.entryFromPath(path);
            JIPipeNodeTaxonomyFunction branch = null;
            for (Object o : path.getPath()) {
                if (o instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction fn) {
                    branch = fn;
                }
            }
            if (entry == null) {
                // A function branch row: resolve to the first database entry
                // under it (or one of its descendants)
                if (branch == null) {
                    return null;
                }
                Set<String> ids = NodeBrowserModel.descendantIds(JIPipe.getNodes().getFunctions(), branch);
                for (JIPipeNodeDatabaseEntry candidate : database.getEntries()) {
                    for (String fid : candidate.getFunctionIds()) {
                        if (ids.contains(fid)) {
                            entry = candidate;
                            break;
                        }
                    }
                    if (entry != null) {
                        break;
                    }
                }
            }
            if (entry == null) {
                return null;
            }
            // Build a single-row list view of the entry and reuse the
            // add-node transfer handler's graph-JSON payload
            DefaultListModel<JIPipeNodeDatabaseEntry> model = new DefaultListModel<>();
            model.addElement(entry);
            JList<JIPipeNodeDatabaseEntry> list = new JList<>(model);
            list.setSelectedIndex(0);
            return delegate.createTransferable(list);
        }
    }

    /** Sentinel for "no framework filter" in the framework combo. */
    private static class AllFrameworksSentinel {
        @Override
        public String toString() {
            return "All frameworks";
        }
    }


}
