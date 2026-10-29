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
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.NodeEntryKind;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DataSourcesFunction;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeEphemeralRunnableExecutor;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.NodeEntryContextMenu;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.nodebrowser.NodeBrowserTreeController;
import org.hkijena.jipipe.desktop.app.grapheditor.templatemanage.TemplateManagementController;
import org.hkijena.jipipe.desktop.app.grapheditor.templatemanage.TemplateManagementDialog;
import org.hkijena.jipipe.desktop.commons.components.ai.JIPipeDesktopAISetupDialog;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormHelpPanel;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopNodeDatabaseEntryListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeStateChips;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.selection.JIPipeDesktopRichSelectionBox;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.CustomStateLessToggleButton;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.*;
import java.util.List;

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
 * <b>single function tree with node leaves</b> - the tree takes over the
 * node-view role, so the panel stays usable in a narrow dock;</li>
 * <li>comfortable ({@link NodeBrowserModel#TIER_COMFORTABLE}): the
 * <b>function tree (with the same node leaves) | filtered node list</b> side
 * by side - the tree is a stable navigator of the full result set; the list
 * applies the selection (function path / template scope) on top of it.</li>
 * </ul>
 * An active search never changes the layout: the list re-queries in place
 * while the tree <b>highlights the matching leafs</b> (bold, see
 * {@link NodeBrowserTreeController#updateSearchHighlights(boolean, List)}).
 * <p>
 * The non-Swing state lives in {@link NodeBrowserModel} (unit-tested);
 * this component renders it and reacts to its changes.
 * <p>
 * <b>Wiring note (Task 24):</b> this component is self-contained - it
 * reads from {@code JIPipe.getNodes()} and the (project's)
 * {@link JIPipeNodeDatabase} and holds no static state. It is mounted in
 * both graph editors' {@code DOCK_ADD_NODES} dock (Task 24b).
 */
public class JIPipeDesktopNodeBrowser extends JIPipeDesktopWorkbenchPanel implements JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener {

    /**
     * Cap for the follow-up suggestion area in the "Add nodes" panel
     * (1337): the wrapping list is unlimited in height otherwise and can
     * crowd out the node list. Above this it scrolls internally.
     */
    private static final int MAX_FOLLOW_UP_BODY_PX = 110;
    /**
     * The expanded function branches as of the last save. Kept in memory
     * only (the current run): it survives browser instances being
     * recreated (panel close/reopen, editor tab recreation) - which is
     * where the state was lost - without the brittleness of a
     * settings-registry round-trip (JSON serialization, autosave timing,
     * stale ids across versions).
     */
    private static volatile List<String> sessionExpandedFunctionIds = List.of();
    /**
     * The model holds all non-Swing browser state.
     */
    private final NodeBrowserModel model = new NodeBrowserModel();
    private final TemplateManagementController templateController = new TemplateManagementController();
    private final JIPipeNodeDatabase database;
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final boolean isCompartmentsEditor;
    // --- Toolbar (search + AI toggle lifted from the old add-nodes panel) ---
    private final JToolBar toolBar = new JToolBar();
    private final JIPipeEphemeralRunnableExecutor executor = new JIPipeEphemeralRunnableExecutor("Node browser");
    private final JIPipeDesktopSearchTextField searchField;
    private final JToggleButton aiSearchButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/ai.png"));
    // --- Filter bar ---
    private final JPanel filterBar = new JPanel();
    private final JComboBox<Object> frameworkCombo = new JComboBox<>();
    private final JToggleButton pinnedToggleButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));
    private final JToggleButton selectionOnlyButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/diagram-next.png"));
    // --- Tree (shared controller; see the nodebrowser package) ---
    private final NodeBrowserTreeController tree;
    // --- Results list ---
    private final JList<JIPipeNodeDatabaseEntry> resultList;
    private final JScrollPane resultScroll;
    // --- Follow-up strip ---
    private final JPanel followUpStrip = new JPanel();
    private final CustomStateLessToggleButton followUpToggle;
    private final JPanel followUpContent = new JPanel();
    private final JScrollPane followUpScroll;
    // --- Layout containers (re-rendered per view) ---
    private final JPanel bodyPanel = new JPanel();
    private boolean aiSearch;
    private JIPipeDesktopRichSelectionBox tagsBox;
    private JIPipeDesktopRichSelectionBox dataTypeBox;
    /**
     * The result-kind filter box (1340); see {@link #initializeFilterBar()}.
     */
    private JIPipeDesktopRichSelectionBox kindsBox;
    /**
     * Mirrors the persisted {@code follow-up-nodes-expanded} setting: applied
     * on creation, updated on every toggle.
     */
    private boolean followUpExpanded = true;
    /**
     * The tier at the last view render ({@code -1} = none yet).
     */
    private int lastRenderedTier = -1;
    /**
     * Whether the tree-structure changes of {@link #rebuildNodeLeaves()} are
     * in flight - the selection listener must not re-query (and not wipe the
     * branch filter) while the node leaves are being rebuilt (see
     * {@link NodeBrowserTreeController#rebuildNodeLeaves(List)}).
     */
    private boolean selectionSuppressed;
    /**
     * Suspends the tree-expansion persistence while the browser applies
     * programmatic model changes (populate, state restore, node-leaf
     * rebuild) - their transient collapse/expand events must not overwrite
     * the user's saved state.
     */
    private boolean treeStateSavingSuspended;
    // --- Last search + facet filtered results (before the tree selection
    // filter): the single source of truth for the tree's node leaves (all
    // tiers) and the search highlights; see applyQueryResult ---
    private List<JIPipeNodeDatabaseEntry> lastFacets = List.of();
    // --- Last fully filtered results (also the tree selection filter):
    // the results list's model + the reference-entry fallback ---
    private List<JIPipeNodeDatabaseEntry> lastFiltered = List.of();

    // ------------------------------------------------------------------
    // Toolbar (lifted from the old add-nodes panel's toolbar)
    // ------------------------------------------------------------------

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
        // Persist the user's expand/collapse state (function ids) for the
        // current run (in-memory, shared across all node-browser
        // instances) so it survives the panel being recreated (new
        // browser instance). The guard keeps the programmatic model
        // reloads (populate, node-leaf rebuild) from clobbering the state
        // with their transient intermediate expansion.
        tree.getTree().addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent e) {
                if (!treeStateSavingSuspended) {
                    saveTreeExpansionState();
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent e) {
                if (!treeStateSavingSuspended) {
                    saveTreeExpansionState();
                }
            }
        });
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
        resultScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Follow-up strip: a compact toggleable header above a scrolling
        // body. The wrap layout grows without bound as more suggestions
        // match, so the body is capped (MAX_FOLLOW_UP_BODY_PX) and scrolls
        // vertically instead; the header collapses the body to a thin bar.
        followUpStrip.setLayout(new BorderLayout());
        followUpStrip.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        followUpContent.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        followUpScroll = new JScrollPane(followUpContent,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        followUpScroll.setBorder(null);
        followUpScroll.getVerticalScrollBar().setUnitIncrement(16);

        followUpToggle = new CustomStateLessToggleButton("Follow-ups",
                JIPipe.RESOURCES.getIcon8("actions/caret-right.png"),
                "Follow-ups",
                JIPipe.RESOURCES.getIcon8("actions/caret-down.png"),
                true);
        followUpToggle.setBorder(
                BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 0, 3, 0),
                        BorderFactory.createMatteBorder(1, 0, 1, 0, ThemeUtils.getCurrentStyle().getBorderColor())));

        followUpToggle.setHorizontalAlignment(SwingConstants.LEFT);

        followUpStrip.add(followUpToggle, BorderLayout.NORTH);
        followUpStrip.add(followUpScroll, BorderLayout.CENTER);

        followUpToggle.setOpaque(false);
        followUpToggle.setFocusPainted(false);

        // Apply the persisted collapse state (setState does not fire the
        // toggled event - only a user click does - so this cannot write the
        // setting back).
        boolean followUpInitiallyExpanded =
                JIPipeGraphEditorUIApplicationSettings.getInstance().isFollowUpNodesExpanded();
        followUpExpanded = followUpInitiallyExpanded;
        followUpToggle.setState(followUpInitiallyExpanded);
        followUpScroll.setVisible(followUpInitiallyExpanded);
        followUpToggle.setToolTipText(followUpInitiallyExpanded ? "Collapse the follow-up nodes" : "Expand the follow-up nodes");

        followUpToggle.getToggledEventEmitter().subscribeLambda((emitter, event) -> {
            boolean expanded = followUpToggle.getState();
            followUpExpanded = expanded;
            JIPipeGraphEditorUIApplicationSettings.getInstance().setFollowUpNodesExpanded(expanded);
            followUpScroll.setVisible(expanded);
            followUpToggle.setToolTipText(expanded ? "Collapse the follow-up nodes" : "Expand the follow-up nodes");
            followUpStrip.revalidate();
            followUpStrip.repaint();
        });

        initializeToolbar();
        initializeFilterBar();
        initializeBody();
        // Only the root ("All functions") is expanded by default: the
        // top-level function categories stay collapsed, so the tree opens
        // to a short overview, not a wall of categories (the controller's
        // populateTree expands exactly the root). The user's previous
        // expand/collapse state (persisted function ids) is restored right
        // after the populate - suspended, so the populate's collapse
        // events do not overwrite the saved state first.
        treeStateSavingSuspended = true;
        try {
            tree.populateTree(isCompartmentsEditor ?
                    JIPipeNodeDatabasePipelineVisibility.Compartments :
                    JIPipeNodeDatabasePipelineVisibility.Pipeline);
            tree.restoreExpandedFunctionIds(loadTreeExpansionState());
        } finally {
            treeStateSavingSuspended = false;
        }
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

        // The "Configure node templates" manager is a non-blocking (non-modal)
        // window, so while it is open in the background the workbench stays
        // interactive. When the store changes there (copy / delete / edit /
        // import / download), refresh this browser so the node list and the
        // template tree leaves reflect it immediately.
        JIPipe.getNodeTemplates().getNodeTemplatesRefreshedEventEmitter()
                .subscribeWeak(event -> reloadList());
    }

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

        // "Configure templates" gear (spec §8.3): opens the TemplateManagement
        // modal on Global by default (or the selected entry's scope when a
        // template entry is selected - see openTemplateManagementFromToolbar).
        JButton configureTemplatesButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        configureTemplatesButton.setToolTipText("Configure node templates");
        UIUtils.makeButtonFlatWithSize(configureTemplatesButton, 28);
        configureTemplatesButton.addActionListener(e -> openTemplateManagementFromToolbar());
        toolBar.add(configureTemplatesButton);
    }

    // ------------------------------------------------------------------
    // Filter bar
    // ------------------------------------------------------------------

    /**
     * The gear's entry point (spec §8.3): opens the "Configure templates" modal
     * on the scope tab of the currently selected template entry, falling back
     * to Global when the selection is empty or a node entry.
     */
    private void openTemplateManagementFromToolbar() {
        TemplateScope scope = null;
        JIPipeNodeDatabaseEntry selected = resultList.getSelectedValue();
        if (selected instanceof CreateNewNodesByTemplateDatabaseEntry templateEntry) {
            scope = templateEntry.getScope();
        }
        openTemplateManagement(scope == null ? TemplateScope.Global : scope);
    }

    private void initializeFilterBar() {
        filterBar.setLayout(new JIPipeDesktopWrapLayout(FlowLayout.LEFT));
        filterBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // Result-kind filter (1340): plain nodes, node examples, and the
        // template kinds split by Pattern/Custom badge and system/user
        // origin. The selection is a PERSISTED, shared setting (the node
        // finder dialog reads the same value), so restore it into the model
        // before the constructor's initial reloadList() - an empty selection
        // means "show all kinds".
        kindsBox = new JIPipeDesktopRichSelectionBox("All kinds", "Kinds", true, NodeEntryKind::options, selection -> {
            Set<NodeEntryKind> kinds = new LinkedHashSet<>();
            for (JIPipeDesktopRichSelectionBox.Option<?> option : selection) {
                if (option.getValue() instanceof NodeEntryKind kind) {
                    kinds.add(kind);
                }
            }
            model.setKindFilter(kinds);
            NodeEntryKind.saveKinds(kinds);
            reloadList();
        });
        kindsBox.setToolTipText("Filter the results by entry kind (select one or more; empty shows all)");
        Set<NodeEntryKind> persistedKinds = NodeEntryKind.loadKinds();
        model.setKindFilter(persistedKinds);
        kindsBox.setSelectedValues(persistedKinds);
        filterBar.add(kindsBox);

        // Framework combo (first item = "all frameworks" sentinel). Only
        // frameworks with at least one node visible in THIS editor role are
        // listed - a framework nobody's nodes use (e.g. Comet) would just be
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

        // Data types (input + output, multi) - a rich selection box with a
        // search bar and the data types' icons (replaces the former
        // "Spatial" + "Input" buttons and their checkbox popup)
        dataTypeBox = new JIPipeDesktopRichSelectionBox("All data", "Data", true, true, this::dataTypeOptions, selection -> {
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

        // Tags (multi) - a rich selection box with a search bar (the merged
        // tag vocabulary is far too long for a plain combo). Populated from
        // the merged tags (node-level + function-path tags) - the same set
        // the filter predicate matches against.
        tagsBox = new JIPipeDesktopRichSelectionBox("All tags", "Tags", true, this::tagOptions, selection -> {
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
     * list side by side - it no longer collapses to a tree-only pane when
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
        // WITH node leaves - the tree takes over the node-view role) and the
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
        // Keep the node leaves in sync with the view (the full entry set -
        // see rebuildNodeLeaves); in the comfortable tier the tree carries
        // them alongside the results list.
        rebuildNodeLeaves();
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    // ------------------------------------------------------------------
    // Node leaves (children of the function tree - every tier)
    // ------------------------------------------------------------------

    /**
     * Syncs the controller's node leaves with the current results. The
     * leaves are attached from the FULL search+facet filtered set (see
     * {@link #lastFacets}) - NOT from the selection-filtered list - so the
     * tree is a stable full navigator in every tier: every branch is
     * expandable and every {@code Templates / <scope>} group shows its
     * entries regardless of what is currently selected (selecting a branch
     * filters the results list and highlights the tree; it does not add or
     * remove the tree's leaves). In the compact tier the tree doubles as
     * the node view; in the comfortable tier it carries the same leaves
     * alongside the (selection-filtered) results list.
     * <p>
     * The {@code selectionSuppressed}/{@code treeStateSavingSuspended}
     * guards keep the leaf rebuild's model reload from tripping the tree
     * selection listener (which would re-query the list).
     */
    private void rebuildNodeLeaves() {
        if (isCompartmentsEditor) {
            return;
        }
        selectionSuppressed = true;
        treeStateSavingSuspended = true;
        try {
            tree.rebuildNodeLeaves(lastFacets);
        } finally {
            selectionSuppressed = false;
            treeStateSavingSuspended = false;
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
        // filter - selecting it must not re-query.
        if (path != null && tree.entryFromPath(path) != null) {
            return;
        }
        // A synthetic Templates/<scope> row selects a template scope (spec
        // §7.2). It is MUTUALLY EXCLUSIVE with the function path: choosing a
        // scope clears the function path (setSelectedScope does this) and a
        // function path clears the scope (setTreePath does this). The
        // "Templates" header (ROOT) and the "All functions" root both fall
        // through to the function-path branch below and reset to "everything".
        TemplateScope scope = tree.scopeFromTreePath(path);
        if (scope != null) {
            model.setSelectedScope(scope);
            reloadList();
            return;
        }
        // A synthetic <function>/Patterns row selects the templates filed
        // under exactly that function (mutually exclusive with the function
        // path and the scope - the model setters cancel the others). Must be
        // checked BEFORE the function-path fallback: the row's path contains
        // its ancestor function, which the fallback would misread as a
        // branch filter (listing real nodes too).
        JIPipeNodeTaxonomyFunction patternsFunction = tree.functionOfPatternsPath(path);
        if (patternsFunction != null) {
            model.setPatternsFunction(patternsFunction);
            reloadList();
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
            // Result-kind filter (1340): shared NodeBrowserModel predicate -
            // entries without a kind (existing nodes/compartments, new
            // compartments) always pass.
            if (!model.matchesKindFilter(entry)) {
                continue;
            }
            // Taxonomy facets (framework / spatial / input profile / tags /
            // input+output data types): the shared NodeBrowserModel
            // predicate. The framework / spatial / input-profile / tags
            // sides are already applied by queryWithFacets' post-filter
            // (identical semantics), so this call is a no-op for them and
            // adds the DATA-TYPE facet, which the query does not cover.
            if (!model.matches(entry)) {
                continue;
            }
            filtered.add(entry);
        }
        // The tree's node leaves use the FULL filtered set (all tiers - the
        // tree is a stable navigator; the selection below must not add or
        // remove its leaves). Search mode: the flat results override the
        // tree, so no leaves (matching the old compact-only behavior).
        lastFacets = model.isSearchActive() ? List.of() : new ArrayList<>(filtered);

        // Selection filter (spec §7.2, shared predicate): when not in search
        // mode, only entries matching the current selection - a template
        // scope, a function path, or the root - are listed. This replaces the
        // old inline function-path filter and covers the new scope selection;
        // both surfaces call the same NodeBrowserModel#matchesSelection so
        // they agree. (In search mode the flat results override the tree, so
        // the selection filter is skipped, as before.)
        if (!model.isSearchActive()) {
            FunctionRegistry registry = JIPipe.getNodes().getFunctions();
            filtered.removeIf(entry -> !model.matchesSelection(entry, registry));
        }

        // Down-ranked nodes (auto-imported, deprecated, unstable - the
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
                updateFollowUpStrip(); // the strip tracks the ⊂ toggle
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
    // Tree expansion state (current run only - in-memory, shared by all
    // node-browser instances in this process; deliberately NOT persisted
    // to settings)
    // ------------------------------------------------------------------

    /**
     * The expanded function ids saved during this run, empty on first use.
     */
    private List<String> loadTreeExpansionState() {
        return sessionExpandedFunctionIds;
    }

    /**
     * Remembers the tree's current expanded function branches (called on
     * every user expansion/collapse; cheap - a reference swap).
     */
    private void saveTreeExpansionState() {
        sessionExpandedFunctionIds = List.copyOf(tree.expandedFunctionIds());
    }

    /**
     * Initializes the data-source quick menu: one submenu per data type
     * (grouped by the data types' menu paths), each listing the data source
     * nodes that can produce that type. Ported from the old add-nodes panel's
     * {@code initializeAddDataSourceMenu} - same {@code getDataTypesByMenuPaths()}
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

    // ------------------------------------------------------------------
    // Data-source menu (ported from the old add-nodes panel, which is
    // deleted in this task; the per-category menus are not ported - the
    // tree takes over the "see what's available" role)
    // ------------------------------------------------------------------

    /**
     * Right-click trigger for the function tree (the {@code isPopupTrigger}
     * check on both press and release covers all platforms).
     * <p>
     * When the clicked row is a <b>node leaf</b> (a
     * {@link JIPipeNodeDatabaseEntry} user object - what the compact
     * single-pane shows, the tree having taken over the node-list role),
     * the shared node-entry menu is shown - the SAME
     * {@link NodeEntryContextMenu#specs} +
     * {@link NodeEntryContextMenu#buildJPopupMenu} +
     * {@link #entryActionsMap} the node list uses (spec §7.3, the 1-pane
     * context-menu bug fix: before, the tree leaves had NO context menu, so
     * Insert at cursor / Pin / Unpin / Show documentation were unreachable).
     * <p>
     * When the clicked row is on the {@code DataSourcesFunction} branch or
     * one of its {@code *DataFunction} leaf branches, the full data-source
     * menu is shown - rebuilt on each show (reloaded-on-show, like the old
     * panel's reloadable pattern). Right-clicks on any other tree row are
     * ignored: the old per-category {@code menuPath} popups are removed by
     * the redesign. The full menu is shown on the branch <em>and</em> its
     * leaves (a verifiable per-leaf data-type pre-scope is not available).
     */
    private void handleTreePopupMenuTrigger(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        TreePath path = tree.getTree().getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }
        // Node leaf (the compact single-pane's insert target): the shared
        // node-entry menu - the SAME builder + actions map the node list
        // uses, so a tree leaf and a list row of the same entry render the
        // identical menu (spec §7.3).
        JIPipeNodeDatabaseEntry entry = tree.entryFromPath(path);
        if (entry != null) {
            // Select the right-clicked row so the menu can be related to its
            // entry by eye (a right-click on a JTree row does not select it
            // the way a left-click does). Safe: the selection listener treats
            // node-leaf rows as insert/drag targets only (no list re-query).
            tree.getTree().setSelectionPath(path);
            Set<String> pinned = getPinnedNodeDatabaseEntries();
            List<JIPipeNodeDatabaseEntry> selected = List.of(entry);
            JPopupMenu popupMenu = NodeEntryContextMenu.buildJPopupMenu(
                    NodeEntryContextMenu.specs(entry, pinned),
                    entryActionsMap(selected, pinned));
            if (popupMenu.getComponentCount() > 0) {
                popupMenu.revalidate();
                // The event coords are relative to the inner JTree (the listener
                // is attached to tree.getTree()), so the popup invoker MUST be the
                // JTree, not the NodeBrowserTreeController panel that wraps it.
                // JPopupMenu.show() resolves (x,y) against the invoker's origin;
                // using the panel offsets the menu by the tree's on-screen
                // position inside its scrollpane (which grows with scroll), so
                // the lower the clicked entry, the further below the cursor the
                // menu landed.
                popupMenu.show(tree.getTree(), e.getX(), e.getY());
            }
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
            // Invoker = the JTree (see the node-leaf branch above): the event
            // coords are tree-relative, so the panel would offset the menu.
            popupMenu.show(tree.getTree(), e.getX(), e.getY());
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

    private void initializeResultListContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        UIUtils.addRightClickPopupMenuToJList(resultList, contextMenu, () -> {
            contextMenu.removeAll();
            List<JIPipeNodeDatabaseEntry> selected = resultList.getSelectedValuesList();
            if (!selected.isEmpty()) {
                // Shared menu builder (spec §7.3): the specs are the single
                // source of truth for WHICH items the selection gets - the
                // same specs + buildJPopupMenu the compact-mode tree leaves
                // use, so a list row and a tree leaf of the same entry render
                // the identical menu (structural parity, not copy-paste).
                // specsForSelection delegates to specs(first, pinned) for a
                // single-entry selection (== the tree's path) and only
                // restores BOTH pin+unpin for a MIXED multi-selection (the
                // pre-refactor list-menu display). The built menu's
                // components are re-parented into the (single) reloadable
                // contextMenu that addRightClickPopupMenuToJList shows.
                Set<String> pinned = getPinnedNodeDatabaseEntries();
                JPopupMenu built = NodeEntryContextMenu.buildJPopupMenu(
                        NodeEntryContextMenu.specsForSelection(selected, pinned),
                        entryActionsMap(selected, pinned));
                for (Component component : built.getComponents()) {
                    contextMenu.add(component);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Context menu (lifted from old panel)
    // ------------------------------------------------------------------

    /**
     * The shared actions map for the node-entry context menu (spec §7.3):
     * the list menu supplier and the compact-mode tree-leaf handler BOTH
     * build their menu from {@link NodeEntryContextMenu#specs} +
     * {@link NodeEntryContextMenu#buildJPopupMenu} with the map from this
     * helper, so the two surfaces cannot diverge.
     * <p>
     * Multi-selection (list rows): pin/unpin acts on the WHOLE selection
     * (as before); insert-at-cursor and show-documentation act on the first
     * selected entry (as before). The tree selection is single, so there the
     * selection list is always exactly one entry.
     * <p>
     * The template quick actions (edit / copy-to-global / copy-to-project /
     * delete / configure-templates) are supplied ONLY when the first selected
     * entry is a {@link CreateNewNodesByTemplateDatabaseEntry} (the five ids
     * that {@code specs()} declared for that scope). A node entry supplies none
     * of them, so its menu is unchanged; and because
     * {@link NodeEntryContextMenu#buildJPopupMenu} skips any spec id that has
     * no action, the scope-gated omission in {@code specs()} (e.g. System has
     * no edit/delete) is what actually controls what renders.
     *
     * @param selected the selected entries (the list's selection, or the
     *                 single tree-leaf entry)
     * @param pinned   the currently pinned node ids
     */
    private Map<String, Runnable> entryActionsMap(List<JIPipeNodeDatabaseEntry> selected, Set<String> pinned) {
        Map<String, Runnable> actions = new HashMap<>();
        actions.put(NodeEntryContextMenu.INSERT_AT_CURSOR, () -> insertAtCursor(selected.get(0)));
        // Pin and Unpin are BOTH always supplied - the menu shows whichever
        // one specs() (driven by the first selected entry) includes, and the
        // action acts on the WHOLE selection (pinNodes/unpinNodes walk every
        // selected entry, as before). Supplying both keeps this helper
        // uniform for the single-selection tree case and the multi-selection
        // list case; the specs + builder decide what actually renders.
        actions.put(NodeEntryContextMenu.PIN, () -> pinNodes(selected));
        actions.put(NodeEntryContextMenu.UNPIN, () -> unpinNodes(selected));
        actions.put(NodeEntryContextMenu.SHOW_DOC, () -> showDocumentation(selected.get(0)));

        // Template quick actions (spec §8.2/§8.3): only supplied when the FIRST
        // selected entry is a template entry. The scope-gated ids that specs()
        // declared for that scope are supplied here; any template id NOT in the
        // first entry's scope (and every node entry, which has none) is simply
        // absent, so buildJPopupMenu skips it. This is what makes a node entry's
        // menu unchanged (node entries' specs() carry no template ids).
        if (selected.get(0) instanceof CreateNewNodesByTemplateDatabaseEntry templateEntry) {
            TemplateScope scope = templateEntry.getScope();
            actions.put(NodeEntryContextMenu.EDIT, () -> templateEdit(templateEntry));
            actions.put(NodeEntryContextMenu.COPY_TO_GLOBAL, () -> templateCopyToGlobal(templateEntry));
            actions.put(NodeEntryContextMenu.COPY_TO_PROJECT, () -> templateCopyToProject(templateEntry));
            actions.put(NodeEntryContextMenu.DELETE, () -> templateDelete(templateEntry));
            actions.put(NodeEntryContextMenu.CONFIGURE_TEMPLATES, () -> openTemplateManagement(scope));
        }
        return actions;
    }

    /**
     * The current project (from the workbench), or {@code null} when the browser
     * is not hosted in a project workbench (e.g. the compartments editor).
     */
    private JIPipeProject currentProject() {
        return getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench projectWorkbench ?
                projectWorkbench.getProject() : null;
    }

    /**
     * Opens the "Configure templates" modal on the given scope tab (the
     * context-menu "Configure templates" item, spec §8.3).
     */
    private void openTemplateManagement(TemplateScope scope) {
        // Unique, non-blocking window: at most one instance per owner window;
        // opening again brings the existing one to the front.
        TemplateManagementDialog.show(getDesktopWorkbench(), scope);
        reloadList();
    }

    /**
     * The Edit action: opens the template editor dialog on a COPY (exactly as
     * the old template box's edit action did) - because
     * {@code taxonomyFunction} is a registered {@code @JIPipeParameter}, the
     * form automatically shows the taxonomy-function picker (spec §8.2).
     * Restricted to Global/Project by specs() (System omits edit).
     */
    private void templateEdit(CreateNewNodesByTemplateDatabaseEntry entry) {
        JIPipeNodeTemplate template = entry.getTemplate();
        if (entry.getScope() == TemplateScope.System || entry.getScope() == TemplateScope.Downloaded) {
            // System (bundled) and Downloaded (repository artifact) templates
            // are read-only: the escape hatch is a copy into the user's own
            // templates (specs() omits edit for both).
            return;
        }
        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(template);
        if (JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), copy,
                new MarkdownText("# Node templates\n\nUse this user interface to modify node templates."),
                "Edit template",
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING
                        | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR
                        | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION)) {
            JIPipe.getNodeTemplates().editTemplate(template, copy, currentProject());
            reloadList();
        }
    }

    /**
     * The Copy-to-Global action (spec §8.2): copies the template into the global
     * store via the controller (the "Copy to Global" escape hatch, valid from
     * System/Project). The controller emits the refresh event; reloadList()
     * makes the browser list/tree update immediately.
     */
    private void templateCopyToGlobal(CreateNewNodesByTemplateDatabaseEntry entry) {
        if (templateController.copyTo(currentProject(), entry.getScope(), TemplateScope.Global, entry.getTemplate())) {
            reloadList();
        }
    }

    /**
     * The Copy-to-Project action (spec §8.2): copies the template into the
     * current project's metadata via the controller (valid from System/Global).
     */
    private void templateCopyToProject(CreateNewNodesByTemplateDatabaseEntry entry) {
        JIPipeProject project = currentProject();
        if (project == null) {
            return;
        }
        if (templateController.copyTo(project, entry.getScope(), TemplateScope.Project, entry.getTemplate())) {
            reloadList();
        }
    }

    /**
     * The Delete action (spec §8.2): removes the template from its scope via the
     * controller after confirmation (System is read-only, so specs() omits
     * delete there). A refresh is emitted on success.
     */
    private void templateDelete(CreateNewNodesByTemplateDatabaseEntry entry) {
        JIPipeNodeTemplate template = entry.getTemplate();
        TemplateScope scope = entry.getScope();
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the template \"" + template.getName() + "\"?",
                "Delete template", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        if (templateController.delete(currentProject(), scope, template)) {
            reloadList();
        }
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

    /**
     * Populates (or collapses) the relation strip based on the current
     * canvas selection: when exactly one node is selected, the strip shows
     * its workflow relations as clickable buttons - the "next steps" first
     * (the workflow successors, as before), then the "previous steps", then
     * the "similar" nodes, de-duplicated by entry id.
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
                    // Next steps first (the workflow successors, as before),
                    // then previous steps, then similar nodes - de-duplicated
                    // by entry id. The three groups come from the one
                    // relations() call; the database caches it per entry.
                    var relations = database.relations(entry, 6);
                    Set<String> added = new HashSet<>();
                    List<JIPipeNodeDatabaseEntry> suggestions = new ArrayList<>();
                    for (var group : List.of(relations.next(), relations.previous(), relations.similar())) {
                        for (JIPipeNodeDatabaseEntry suggestion : group) {
                            if (added.add(suggestion.getId())) {
                                suggestions.add(suggestion);
                            }
                        }
                    }
                    if (!suggestions.isEmpty()) {
                        show = true;
                        for (JIPipeNodeDatabaseEntry suggestion : suggestions) {
                            JButton button = new JButton(suggestion.getName(), suggestion.getIcon());
                            button.setToolTipText("Suggested related step for the selected node");
                            button.setBorder(UIUtils.createControlBorder());
                            button.addActionListener(e -> insertAtCursor(suggestion));
                            followUpContent.add(button);
                        }
                    }
                }
            }
        }
        followUpStrip.setVisible(show);
        if (show) {
            // The wrap layout's preferred size is width-dependent (it walks
            // up the parent chain for the width) and counts only effectively
            // visible buttons - so measure while the body is effectively
            // visible: temporarily re-show it if the user had it collapsed
            // (synchronous, nothing repaints in between). Then cap the
            // height so the body scrolls instead of growing without bound
            // (1337).
            boolean wasVisible = followUpScroll.isVisible();
            followUpScroll.setVisible(true);
            followUpScroll.getViewport().revalidate();
            followUpScroll.revalidate();
            Dimension size = followUpScroll.getPreferredSize();
            followUpScroll.setPreferredSize(new Dimension(size.width, Math.min(size.height, MAX_FOLLOW_UP_BODY_PX)));
            followUpScroll.setVisible(wasVisible);
        }
        followUpStrip.revalidate();
        followUpStrip.repaint();
    }

    // ------------------------------------------------------------------
    // Relation strip (next / previous / similar)
    // ------------------------------------------------------------------

    private JIPipeNodeDatabaseEntry entryByOriginNodeId(String originNodeId) {
        for (JIPipeNodeDatabaseEntry entry : database.getEntries()) {
            if (originNodeId.equals(entry.getOriginNodeId())) {
                return entry;
            }
        }
        return null;
    }

    public NodeBrowserModel getModel() {
        return model;
    }

    // ------------------------------------------------------------------
    // Accessors (for Task 24 wiring)
    // ------------------------------------------------------------------

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

    /**
     * A data-type filter option value: (data class, input-side or output-side).
     */
    private record TypedDataValue(Class<? extends JIPipeData> dataClass, boolean input) {
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
     * Sentinel for "no framework filter" in the framework combo.
     */
    private static class AllFrameworksSentinel {
        @Override
        public String toString() {
            return "All frameworks";
        }
    }

    /**
     * DnD source for the function tree. The canvas only accepts node-graph
     * JSON (the transfer data produced by
     * {@link JIPipeDesktopAddNodeTransferHandler}), so the selected tree row
     * is resolved to a database entry and delegated to that handler - the
     * same payload the results list produces:
     * <ul>
     * <li>a <em>node leaf</em> row (compact view) drags exactly that entry;
     * <li>a <em>function</em> row (branch) is resolved to the first database
     * entry that appears under it (or one of its descendants) - unchanged
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


}
