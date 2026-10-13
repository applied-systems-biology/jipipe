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
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DataSourcesFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
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
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.ai.JIPipeDesktopAISetupDialog;
import org.hkijena.jipipe.desktop.commons.components.layouts.JIPipeDesktopWrapLayout;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormHelpPanel;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeDesktopNodeDatabaseEntryListCellRenderer;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
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
 * search text post-filter them. The main area is <em>always</em> the
 * function tree | filtered node list side by side (at any dock width —
 * the panel must stay usable when the dock is narrow), and an active
 * search re-queries the list in place while <b>highlighting the tree
 * leafs</b> the search matched (bold, see {@link #updateTreeSearchHighlights}).
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
    private GroupedMultiSelectFilterButton dataTypeFilter;
    private final JComboBox<Object> tagsCombo = new JComboBox<>();
    private final JToggleButton pinnedToggleButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));
    private final JToggleButton selectionOnlyButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/diagram-next.png"));

    // --- Tree ---
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final JScrollPane treeScroll;

    // --- Results list ---
    private final JList<JIPipeNodeDatabaseEntry> resultList;
    private final JScrollPane resultScroll;

    // --- Follow-up strip ---
    private final JPanel followUpStrip = new JPanel();
    private final JPanel followUpContent = new JPanel();

    // --- Layout containers (re-rendered per view) ---
    private final JPanel bodyPanel = new JPanel();

    /**
     * Function-leaf tree rows to highlight because a search matched a node
     * under them (see {@link #updateTreeSearchHighlights}); empty when no
     * search is active.
     */
    private Set<TreePath> searchHighlightedLeafPaths = Set.of();

    // --- Last post-filtered results (single source of truth for the search
    // highlights; see applyQueryResult) ---
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
            applyViewIfNeeded(); // the layout is stable; the list re-queries in place
            reloadList();
        });
        searchField.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume();
                    SwingUtilities.invokeLater(() -> {
                        // The node list is always visible (pipeline and
                        // compartments alike): move the selection to its first
                        // row.
                        resultList.requestFocusInWindow();
                        resultList.setSelectedIndex(0);
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

        // Function tree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("All functions");
        this.tree = new JTree(new DefaultTreeModel(root));
        this.treeModel = (DefaultTreeModel) tree.getModel();
        this.treeScroll = new JScrollPane(tree);
        this.treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tree.setDragEnabled(true);
        tree.setTransferHandler(new TreeNodeTransferHandler(tree));
        tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tree.setCellRenderer(new FunctionTreeCellRenderer());
        tree.addTreeSelectionListener(e -> updateTreePathFromSelection());
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    JIPipeNodeDatabaseEntry entry = entryFromPath(tree.getSelectionPath());
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
        populateTree();
        // Open to the "main function level" by default (root + the top-level
        // branches): populateTree()'s reload() collapses the tree to the root
        // only, so without this the tree would show no branches.
        expandToMainFunctionLevel((DefaultMutableTreeNode) treeModel.getRoot());
        applyViewIfNeeded();

        // The layout is stable (tree | list at any width); re-applying is
        // idempotent, so it is safe to run on every resize.
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

        // Data types (input + output, multi) — one button, one popup with
        // the two checkbox groups (replaces the former "Spatial" + "Input"
        // buttons)
        dataTypeFilter = new GroupedMultiSelectFilterButton("Data", this::applyDataTypeFilter);
        Map<String, Class<? extends JIPipeData>> dataTypes = JIPipe.getDataTypes().getUnhiddenRegisteredDataTypes();
        List<String> sortedIds = dataTypes.keySet().stream()
            .sorted(Comparator.comparing(id -> JIPipeDataInfo.getInstance(dataTypes.get(id)).getName()))
            .toList();
        dataTypeFilter.addSectionHeader("Input");
        for (String id : sortedIds) {
            dataTypeFilter.addOption("in:" + id, JIPipeDataInfo.getInstance(dataTypes.get(id)).getName());
        }
        dataTypeFilter.addSectionHeader("Output");
        for (String id : sortedIds) {
            dataTypeFilter.addOption("out:" + id, JIPipeDataInfo.getInstance(dataTypes.get(id)).getName());
        }
        dataTypeFilter.setToolTipText("Filter by input and/or output data type (select one or more)");
        filterBar.add(dataTypeFilter);

        // Tags combo (first item = "all tags" sentinel). Populated from the
        // merged tags (node-level + function-path tags) — the same set the
        // filter predicate matches against.
        tagsCombo.addItem(new AllTagsSentinel());
        Set<String> allTags = new LinkedHashSet<>();
        for (JIPipeNodeTaxonomyFunction function : JIPipe.getNodes().getFunctions().all()) {
            allTags.addAll(function.getTags());
        }
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            allTags.addAll(info.getMergedTags());
        }
        for (String tag : allTags.stream().sorted(Comparator.naturalOrder()).toList()) {
            tagsCombo.addItem(tag);
        }
        tagsCombo.addActionListener(e -> {
            Object selected = tagsCombo.getSelectedItem();
            model.setTags(selected instanceof String s ? Set.of(s) : Set.of());
            reloadList();
        });
        filterBar.add(tagsCombo);
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
     * Translates the data-type multiselect button's selected value set into
     * the model's input/output data-type filters. Values are encoded as
     * {@code in:<data-class-name>} / {@code out:<data-class-name>}; an empty
     * selection clears both.
     *
     * @param values the selected option values (empty = no data-type filter)
     */
    private void applyDataTypeFilter(Set<String> values) {
        Set<Class<? extends JIPipeData>> inputs = new LinkedHashSet<>();
        Set<Class<? extends JIPipeData>> outputs = new LinkedHashSet<>();
        for (String value : values) {
            if (value.startsWith("in:")) {
                inputs.add(JIPipe.getDataTypes().getById(value.substring(3)));
            } else if (value.startsWith("out:")) {
                outputs.add(JIPipe.getDataTypes().getById(value.substring(4)));
            }
        }
        model.setInputDataTypes(inputs);
        model.setOutputDataTypes(outputs);
        reloadList();
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
     * see {@link #updateTreeSearchHighlights}). This keeps the panel usable
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

        // Pipeline editor: the function tree + filtered node list are always
        // shown side by side, at any dock width (the split is user-adjustable).
        // Idempotent: rebuild the split only when the center is not the split.
        boolean isSplit = bodyPanel.getComponentCount() == 2
            && bodyPanel.getComponent(1) instanceof JSplitPane;
        if (isSplit) {
            return;
        }
        bodyPanel.removeAll();
        bodyPanel.add(filterBar, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, resultScroll);
        split.setResizeWeight(0.35);
        split.setContinuousLayout(true);
        bodyPanel.add(split, BorderLayout.CENTER);
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    // ------------------------------------------------------------------
    // Tree path + selection
    // ------------------------------------------------------------------

    private void updateTreePathFromSelection() {
        TreePath path = tree.getSelectionPath();
        // Selecting a non-function row does not change the active branch
        // filter and does not re-query: only function-branch selections
        // filter (spec: "selecting a branch filters").
        if (path != null) {
            Object last = path.getLastPathComponent();
            if (!(last instanceof DefaultMutableTreeNode mtn)
                || !(mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction)) {
                return;
            }
        }
        if (path == null) {
            model.setTreePath(List.of());
        } else {
            List<JIPipeNodeTaxonomyFunction> pathFunctions = new ArrayList<>();
            for (Object node : path.getPath()) {
                if (node instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction fn) {
                    pathFunctions.add(fn);
                }
            }
            model.setTreePath(pathFunctions);
        }
        reloadList();
    }

    private void populateTree() {
        FunctionRegistry registry = JIPipe.getNodes().getFunctions();
        // Prune function branches that carry no (visible) nodes so the tree
        // never shows empty categories (e.g. a plot sub-leaf no node is
        // assigned to). The registered node infos are the synchronous source
        // of truth (the database builds its entries asynchronously and may
        // still be empty at this point); the visibility role matches what
        // the browser's query shows under each branch.
        JIPipeNodeDatabasePipelineVisibility role = isCompartmentsEditor ?
            JIPipeNodeDatabasePipelineVisibility.Compartments :
            JIPipeNodeDatabasePipelineVisibility.Pipeline;
        Set<String> populated = NodeBrowserModel.populatedFunctionIds(
            JIPipe.getNodes().getRegisteredNodeInfos().values(), role);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();
        for (JIPipeNodeTaxonomyFunction top : registry.topLevel()) {
            DefaultMutableTreeNode node = buildTreeNode(top, registry, populated);
            if (node != null) {
                root.add(node);
            }
        }
        treeModel.reload();
    }

    /**
     * Builds the function-tree subtree rooted at the given function, pruning
     * child functions that have no node in their subtree (a function "has a
     * node" when its id — or a descendant's — is in {@code populated}).
     *
     * @param function the branch to build
     * @param registry the live function registry
     * @param populated the ids of all functions under which a node exists
     *                  (see {@link NodeBrowserModel#populatedFunctionIds})
     * @return the subtree root, or {@code null} when the function (and all
     * its descendants) carry no node
     */
    private DefaultMutableTreeNode buildTreeNode(JIPipeNodeTaxonomyFunction function, FunctionRegistry registry, Set<String> populated) {
        if (!populated.isEmpty() && !hasNodesUnder(function, registry, populated)) {
            return null;
        }
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(function);
        for (JIPipeNodeTaxonomyFunction child : registry.children(function)) {
            DefaultMutableTreeNode childNode = buildTreeNode(child, registry, populated);
            if (childNode != null) {
                node.add(childNode);
            }
        }
        return node;
    }

    private static boolean hasNodesUnder(JIPipeNodeTaxonomyFunction function, FunctionRegistry registry, Set<String> populated) {
        if (populated.contains(function.getId())) {
            return true;
        }
        for (JIPipeNodeTaxonomyFunction child : registry.children(function)) {
            if (hasNodesUnder(child, registry, populated)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Expands the tree to the "main function level": the root and all of its
     * top-level branches. {@code populateTree()}'s reload() collapses the
     * tree to the root, so without this the user would see a single row.
     */
    private void expandToMainFunctionLevel(DefaultMutableTreeNode root) {
        TreePath rootPath = new TreePath(new Object[]{root});
        tree.expandPath(rootPath);
        for (int i = 0; i < root.getChildCount(); i++) {
            Object child = root.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode mtn) {
                tree.expandPath(rootPath.pathByAddingChild(mtn));
            }
        }
    }

    /**
     * The database entry carried by the last node of the given tree path, or
     * {@code null} when the selection is empty or ends on a function/root row
     * (those are not insertable nodes). Used by the tree's double-click
     * handler and (as the drag source) by the transfer handler.
     */
    private JIPipeNodeDatabaseEntry entryFromPath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeDatabaseEntry entry) {
            return entry;
        }
        return null;
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

        // Curated (non-auto-imported) nodes first, auto-discovered IJ2/CLIJ2
        // nodes at the bottom (stable within each group, so the query's
        // ranking — pinned, AI relevance — is preserved).
        List<JIPipeNodeDatabaseEntry> ordered = new ArrayList<>(filtered);
        ordered.sort(Comparator.comparingInt(e -> isAutoImportedEntry(e) ? 1 : 0));
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
        updateTreeSearchHighlights();
        // Re-apply the layout (idempotent; needed after compartments toggles).
        applyViewIfNeeded();
    }

    /**
     * Whether the entry is an auto-discovered (not curated) node: the
     * auto-imported ImageJ2 ops ({@code ij2:op:...}) and CLIJ2 commands
     * ({@code clij:plugin:...}). Used to keep the curated JIPipe nodes at
     * the top of the result list.
     */
    private static boolean isAutoImportedEntry(JIPipeNodeDatabaseEntry entry) {
        String id = entry.getOriginNodeId();
        return id != null && (id.startsWith("ij2:") || id.startsWith("clij:"));
    }

    /**
     * Marks the function-leaf tree rows whose descendant entries appear in
     * the current results (and their ancestors, for context) as
     * search-highlighted — bold in the tree renderer. Only while a search
     * term is active (a facet filter alone would highlight nearly every
     * leaf, which is noise); otherwise clears any previous highlights.
     */
    private void updateTreeSearchHighlights() {
        if (!model.isSearchActive() || lastFiltered.isEmpty()) {
            if (!searchHighlightedLeafPaths.isEmpty()) {
                searchHighlightedLeafPaths = Set.of();
                tree.repaint();
            }
            return;
        }
        // Collect the function ids (primary + secondary) of all results.
        Set<String> matchedFunctionIds = new HashSet<>();
        for (JIPipeNodeDatabaseEntry entry : lastFiltered) {
            matchedFunctionIds.addAll(entry.getFunctionIds());
        }
        Set<TreePath> highlighted = new HashSet<>();
        markMatchedRows((DefaultMutableTreeNode) treeModel.getRoot(), matchedFunctionIds, highlighted);
        searchHighlightedLeafPaths = highlighted;
        // Expand the ancestors of the highlighted rows so the matches are
        // visible even when their branches were collapsed (the user's
        // expansion of non-matching branches is left untouched).
        for (TreePath path : highlighted) {
            Object[] components = path.getPath();
            for (int i = 1; i < components.length; i++) {
                tree.expandPath(new TreePath(Arrays.copyOfRange(components, 0, i)));
            }
        }
        tree.repaint();
    }

    private void markMatchedRows(DefaultMutableTreeNode node, Set<String> matchedFunctionIds, Set<TreePath> highlighted) {
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction fn) {
                // A row is highlighted when a result entry is filed under its
                // own function (primary or secondary): for leaf rows that is
                // their nodes, and for branches that carry direct nodes AND
                // sub-branches (e.g. Scripting, Segmentation) their direct
                // nodes. Plain branches (no direct entries) are not bolded —
                // that would highlight nearly every branch for any search.
                if (matchedFunctionIds.contains(fn.getId())) {
                    highlighted.add(new TreePath(mtn.getPath()));
                }
                markMatchedRows(mtn, matchedFunctionIds, highlighted);
            }
        }
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
        // The node list is the always-visible result view: the first hit is
        // list row 0.
        JIPipeNodeDatabaseEntry entry = resultList.getModel().getSize() > 0
            ? resultList.getModel().getElementAt(0) : null;
        if (entry != null) {
            insertAtCursor(entry);
        }
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
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }
        JIPipeNodeTaxonomyFunction branch = functionFromTreePath(path);
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

    /**
     * The function carried by the last node of the given tree path (the row
     * the user interacted with), or {@code null} when the path is empty, on
     * the root, or on a node-leaf (database-entry) row.
     */
    private JIPipeNodeTaxonomyFunction functionFromTreePath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction fn) {
            return fn;
        }
        return null;
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
        private final JTree tree;
        private final JIPipeDesktopAddNodeTransferHandler delegate = new JIPipeDesktopAddNodeTransferHandler();

        private TreeNodeTransferHandler(JTree tree) {
            this.tree = tree;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            TreePath path = tree.getSelectionPath();
            if (path == null) {
                return null;
            }
            JIPipeNodeDatabaseEntry entry = entryFromPath(path);
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

    /** Sentinel for "no tag filter" in the tags combo. */
    private static class AllTagsSentinel {
        @Override
        public String toString() {
            return "All tags";
        }
    }

    /**
     * Tree cell renderer. Function branch rows keep the original look
     * (name + icon, generic icon for icon-less leaves); node-leaf rows
     * (compact view) show the entry's icon, name, a framework badge (a
     * rounded chip in the framework's color, mirroring the node finder's
     * detail rail), and spatial chips ("2D" / "3D").
     */
    private class FunctionTreeCellRenderer implements TreeCellRenderer {

        private static final Icon GENERIC_NODE_ICON = JIPipe.RESOURCES.getIcon16("actions/tag.png");
        private static final int CHIP_ARC = 6;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            Color selectionBackground = UIManager.getColor("Tree.selectionBackground");
            Color selectionForeground = UIManager.getColor("Tree.selectionForeground");
            Color background = selected && selectionBackground != null ? selectionBackground : tree.getBackground();
            Color foreground = selected && selectionForeground != null ? selectionForeground : tree.getForeground();

            if (userObject instanceof JIPipeNodeDatabaseEntry entry) {
                // Node leaf row: icon + name + framework badge + spatial chips
                JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                rowPanel.setOpaque(true);
                rowPanel.setBackground(background);
                rowPanel.setForeground(foreground);
                rowPanel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));

                JLabel nameLabel = new JLabel();
                nameLabel.setText(entry.getName());
                nameLabel.setIcon(entry.getIcon() != null ? entry.getIcon() : GENERIC_NODE_ICON);
                nameLabel.setForeground(foreground);
                rowPanel.add(nameLabel);

                JIPipeNodeTaxonomyFramework framework = entry.getFramework();
                boolean noFramework = NoFramework.ID.equals(framework.getId());
                JPanel frameworkChip = new JPanel(new GridBagLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (!noFramework) {
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2d.setColor(framework.getColor());
                            g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CHIP_ARC, CHIP_ARC);
                            g2d.dispose();
                        }
                    }
                };
                frameworkChip.setOpaque(false);
                frameworkChip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                frameworkChip.setToolTipText(noFramework ? "No external framework" : "Framework: " + framework.getName());
                JLabel frameworkLabel = new JLabel(framework.getAbbreviation());
                frameworkLabel.setForeground(noFramework ? Color.GRAY : Color.WHITE);
                frameworkLabel.setFont(frameworkLabel.getFont().deriveFont(Font.BOLD, 9f));
                frameworkChip.add(frameworkLabel);
                rowPanel.add(frameworkChip);

                Set<JIPipeNodeTaxonomySpatialDimension> spatial = entry.getSpatialDimensions();
                for (JIPipeNodeTaxonomySpatialDimension dimension : spatial) {
                    String text = dimension == JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D ? "2D" : "3D";
                    JPanel chip = new JPanel(new GridBagLayout()) {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2d.setColor(Color.GRAY);
                            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CHIP_ARC, CHIP_ARC);
                            g2d.dispose();
                        }
                    };
                    chip.setOpaque(false);
                    chip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
                    chip.setToolTipText(dimension == JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D
                        ? "Operates in 2D" : "Operates in 3D");
                    JLabel chipLabel = new JLabel(text);
                    chipLabel.setForeground(foreground);
                    chipLabel.setFont(chipLabel.getFont().deriveFont(Font.PLAIN, 9f));
                    chip.add(chipLabel);
                    rowPanel.add(chip);
                }
                return rowPanel;
            }

            JLabel label = new JLabel();
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
            if (userObject instanceof JIPipeNodeTaxonomyFunction function) {
                label.setText(function.getName());
                Icon icon = function.getIcon();
                label.setIcon(icon != null ? icon : GENERIC_NODE_ICON);
                label.setToolTipText(function.getDescription());
                // Search highlight: the search matched a node filed under this
                // leaf (or one of its descendant leaves)
                if (searchHighlightedLeafPaths.contains(new TreePath(((DefaultMutableTreeNode) value).getPath()))) {
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                }
            } else {
                // The root node
                label.setText("All functions");
                label.setIcon(GENERIC_NODE_ICON);
            }
            label.setForeground(foreground);
            label.setBackground(background);
            return label;
        }
    }
}
