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
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The adaptive node browser (Task 16), replacing the old
 * {@link JIPipeDesktopAddNodesPanel} in a later task.
 * <p>
 * The browser is function-tree driven: a taxonomy function tree
 * (13 top-levels → 48 leaves) selects which entries are shown, and the
 * taxonomy facets (framework / spatial / input / tags) plus the search
 * text post-filter them. It is <em>tier-aware</em>: at narrow widths
 * (&lt; 380 px) it collapses to a single tree list of node leaves
 * ({@link NodeBrowserModel#TIER_COMPACT}), and at comfortable widths it
 * shows the tree and the results list side by side
 * ({@link NodeBrowserModel#TIER_COMFORTABLE}).
 * <p>
 * The non-Swing state lives in {@link NodeBrowserModel} (unit-tested);
 * this component renders it and reacts to its changes.
 * <p>
 * <b>Wiring note (Task 24):</b> this component is self-contained — it
 * reads from {@code JIPipe.getNodes()} and the (project's)
 * {@link JIPipeNodeDatabase} and holds no static state. It is mounted
 * into the graph editors by Task 24.
 */
public class JIPipeDesktopNodeBrowser extends JIPipeDesktopWorkbenchPanel implements JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener {

    /** The model holds all non-Swing browser state. */
    private final NodeBrowserModel model = new NodeBrowserModel();

    private final JIPipeNodeDatabase database;
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final boolean isCompartmentsEditor;

    // --- Toolbar (search + AI toggle lifted from JIPipeDesktopAddNodesPanel) ---
    private final JToolBar toolBar = new JToolBar();
    private final JIPipeEphemeralRunnableExecutor executor = new JIPipeEphemeralRunnableExecutor("Node browser");
    private final JIPipeDesktopSearchTextField searchField;
    private final JToggleButton aiSearchButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/ai.png"));
    private boolean aiSearch;

    // --- Filter bar ---
    private final JPanel filterBar = new JPanel();
    private final JComboBox<Object> frameworkCombo = new JComboBox<>();
    private final JCheckBox spatial2DBox = new JCheckBox("2D");
    private final JCheckBox spatial3DBox = new JCheckBox("3D");
    private final JCheckBox inputPlane2DBox = new JCheckBox(JIPipeNodeTaxonomyInputProfile.PLANE_2D.name());
    private final JCheckBox inputVolume3DBox = new JCheckBox(JIPipeNodeTaxonomyInputProfile.VOLUME_3D.name());
    private final JCheckBox inputAnyBox = new JCheckBox(JIPipeNodeTaxonomyInputProfile.ANY.name());
    private final JComboBox<Object> tagsCombo = new JComboBox<>();
    private final JToggleButton pinnedToggleButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));
    private final JToggleButton selectionOnlyButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/arrow-right.png"));

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

    // --- Layout containers (re-rendered per tier) ---
    private final JPanel bodyPanel = new JPanel();
    private int lastRenderedTier = Integer.MIN_VALUE;

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
        searchField.addActionListener(e -> model.setSearch(searchField.getText()));
        searchField.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume();
                    SwingUtilities.invokeLater(() -> {
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
        tree.setTransferHandler(new TreeFunctionTransferHandler(tree));
        tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tree.setCellRenderer(new FunctionTreeCellRenderer());
        tree.addTreeSelectionListener(e -> updateTreePathFromSelection());

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
        applyTierIfNeeded();

        // Recompute the layout tier on resize (compact vs. comfortable)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyTierIfNeeded();
            }
        });

        // Recompute the ⊂ selection-only rule on canvas selection change
        graphEditorUI.getSelectionManager().getNodeSelectionChangedEventEmitter().subscribe(this);

        applySelectionOnlyState();
        updateFollowUpStrip();
        reloadList();
    }

    // ------------------------------------------------------------------
    // Toolbar (lifted from JIPipeDesktopAddNodesPanel.initializeToolbar, L615-689)
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

        // Framework combo (first item = "all frameworks" sentinel)
        frameworkCombo.addItem(new AllFrameworksSentinel());
        for (JIPipeNodeTaxonomyFramework framework : JIPipe.getNodes().getFrameworks().all()) {
            frameworkCombo.addItem(framework);
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
        filterBar.add(Box.createHorizontalStrut(8));

        // Spatial 2D/3D checkboxes
        spatial2DBox.setToolTipText("Show only nodes operating in 2D");
        spatial3DBox.setToolTipText("Show only nodes operating in 3D");
        spatial2DBox.addActionListener(e -> updateSpatialModel());
        spatial3DBox.addActionListener(e -> updateSpatialModel());
        filterBar.add(spatial2DBox);
        filterBar.add(spatial3DBox);
        filterBar.add(Box.createHorizontalStrut(8));

        // Input profile checkboxes
        inputPlane2DBox.setToolTipText("Show only nodes consuming 2D spatial data");
        inputVolume3DBox.setToolTipText("Show only nodes consuming 3D spatial data");
        inputAnyBox.setToolTipText("Show only nodes consuming any input");
        inputPlane2DBox.addActionListener(e -> updateInputModel());
        inputVolume3DBox.addActionListener(e -> updateInputModel());
        inputAnyBox.addActionListener(e -> updateInputModel());
        filterBar.add(inputPlane2DBox);
        filterBar.add(inputVolume3DBox);
        filterBar.add(inputAnyBox);
        filterBar.add(Box.createHorizontalStrut(8));

        // Tags combo (first item = "all tags" sentinel)
        tagsCombo.addItem(new AllTagsSentinel());
        Set<String> allTags = new LinkedHashSet<>();
        for (JIPipeNodeTaxonomyFunction function : JIPipe.getNodes().getFunctions().all()) {
            allTags.addAll(function.getTags());
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

    private void updateSpatialModel() {
        Set<JIPipeNodeTaxonomySpatialDimension> spatial = new LinkedHashSet<>();
        if (spatial2DBox.isSelected()) {
            spatial.add(JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D);
        }
        if (spatial3DBox.isSelected()) {
            spatial.add(JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D);
        }
        model.setSpatial(spatial);
        reloadList();
    }

    private void updateInputModel() {
        Set<JIPipeNodeTaxonomyInputProfile> input = new LinkedHashSet<>();
        if (inputPlane2DBox.isSelected()) {
            input.add(JIPipeNodeTaxonomyInputProfile.PLANE_2D);
        }
        if (inputVolume3DBox.isSelected()) {
            input.add(JIPipeNodeTaxonomyInputProfile.VOLUME_3D);
        }
        if (inputAnyBox.isSelected()) {
            input.add(JIPipeNodeTaxonomyInputProfile.ANY);
        }
        model.setInput(input);
        reloadList();
    }

    // ------------------------------------------------------------------
    // Body (tree + results, tier-aware)
    // ------------------------------------------------------------------

    private void initializeBody() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);

        // The body holds the filter bar (top) + the tier-dependent center;
        // applyTierIfNeeded() (called from the constructor and the resize
        // listener) packs it per the current tier.
        bodyPanel.setLayout(new BorderLayout());
        add(bodyPanel, BorderLayout.CENTER);
        add(followUpStrip, BorderLayout.SOUTH);
    }

    /**
     * Applies the layout tier for the current width: compact shows only the
     * results list (the tree keeps its selection but is hidden), comfortable
     * shows the tree and results side by side. It is a no-op when the tier is
     * unchanged since the last application, so it is safe to call from the
     * component's resize listener on every resize.
     */
    private void applyTierIfNeeded() {
        int tier = model.getTier(getWidth());
        if (tier == lastRenderedTier) {
            return;
        }
        lastRenderedTier = tier;
        bodyPanel.removeAll();
        bodyPanel.add(filterBar, BorderLayout.NORTH);
        if (tier == NodeBrowserModel.TIER_COMPACT) {
            // Compact: single list of node leaves. The tree remains the
            // source of the function filter but is not shown.
            bodyPanel.add(resultScroll, BorderLayout.CENTER);
        } else {
            // Comfortable: tree + results side by side
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, resultScroll);
            split.setResizeWeight(0.35);
            split.setContinuousLayout(true);
            bodyPanel.add(split, BorderLayout.CENTER);
        }
        bodyPanel.revalidate();
        bodyPanel.repaint();
    }

    // ------------------------------------------------------------------
    // Tree path + selection
    // ------------------------------------------------------------------

    private void updateTreePathFromSelection() {
        TreePath path = tree.getSelectionPath();
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
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();
        for (JIPipeNodeTaxonomyFunction top : registry.topLevel()) {
            root.add(buildTreeNode(top, registry));
        }
        treeModel.reload();
    }

    private DefaultMutableTreeNode buildTreeNode(JIPipeNodeTaxonomyFunction function, FunctionRegistry registry) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(function);
        for (JIPipeNodeTaxonomyFunction child : registry.children(function)) {
            node.add(buildTreeNode(child, registry));
        }
        return node;
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

        DefaultListModel<JIPipeNodeDatabaseEntry> listModel = new DefaultListModel<>();
        for (JIPipeNodeDatabaseEntry entry : filtered) {
            listModel.addElement(entry);
        }
        resultList.setModel(listModel);
        if (!listModel.isEmpty()) {
            resultList.setSelectedIndex(0);
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
        JIPipeNodeDatabaseEntry entry = resultList.getSelectedValue();
        if (entry == null && resultList.getModel().getSize() > 0) {
            entry = resultList.getModel().getElementAt(0);
        }
        if (entry != null) {
            insertAtCursor(entry);
        }
    }

    private void insertAtCursor(JIPipeNodeDatabaseEntry entry) {
        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(getDesktopWorkbench())) {
            return;
        }
        // entry.addToGraph(...) snapshots the history internally and adds the
        // node(s) to the canvas (mirrors JIPipeDesktopAddNodesPanel.insertAtCursor,
        // L787-790); then select the added node UIs.
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
                        followUpContent.add(new JLabel(JIPipe.RESOURCES.getIcon16("actions/arrow-right.png")));
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
     * {@link JIPipeDesktopAddNodeTransferHandler}), so a selected function
     * leaf is resolved to the first database entry that appears under it (or
     * one of its descendants) and delegated to that handler — the same
     * payload the results list produces. Dragging a leaf with no matching
     * entry (e.g. an unconverted branch) produces no droppable data.
     */
    private class TreeFunctionTransferHandler extends TransferHandler {
        private final JTree tree;
        private final JIPipeDesktopAddNodeTransferHandler delegate = new JIPipeDesktopAddNodeTransferHandler();

        private TreeFunctionTransferHandler(JTree tree) {
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
            JIPipeNodeTaxonomyFunction leaf = null;
            for (Object o : path.getPath()) {
                if (o instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction fn) {
                    leaf = fn;
                }
            }
            if (leaf == null) {
                return null;
            }
            Set<String> ids = NodeBrowserModel.descendantIds(JIPipe.getNodes().getFunctions(), leaf);
            JIPipeNodeDatabaseEntry entry = null;
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
     * Tree cell renderer: shows the function's name and icon; top-level
     * functions have icons, leaves return {@code null} and fall back to a
     * generic node icon (as the old panel did for its entries).
     */
    private static class FunctionTreeCellRenderer implements TreeCellRenderer {

        private static final Icon GENERIC_NODE_ICON = JIPipe.RESOURCES.getIcon16("actions/tag.png");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            JLabel label = new JLabel();
            label.setOpaque(true);
            if (node.getUserObject() instanceof JIPipeNodeTaxonomyFunction function) {
                label.setText(function.getName());
                Icon icon = function.getIcon();
                label.setIcon(icon != null ? icon : GENERIC_NODE_ICON);
                label.setToolTipText(function.getDescription());
            } else {
                // The root node
                label.setText("All functions");
                label.setIcon(GENERIC_NODE_ICON);
            }
            Color selectionBackground = UIManager.getColor("Tree.selectionBackground");
            Color selectionForeground = UIManager.getColor("Tree.selectionForeground");
            label.setForeground(selected && selectionForeground != null ? selectionForeground : tree.getForeground());
            label.setBackground(selected && selectionBackground != null ? selectionBackground : tree.getBackground());
            return label;
        }
    }
}
