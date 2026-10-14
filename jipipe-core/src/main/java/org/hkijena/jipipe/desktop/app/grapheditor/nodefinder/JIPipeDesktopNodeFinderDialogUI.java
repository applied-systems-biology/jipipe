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

package org.hkijena.jipipe.desktop.app.grapheditor.nodefinder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.entries.ExistingCompartmentDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.ExistingPipelineNodeDatabaseEntry;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.run.JIPipeEphemeralRunnableExecutor;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.NodeBrowserModel;
import org.hkijena.jipipe.desktop.app.grapheditor.nodebrowser.NodeBrowserTreeController;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.ai.JIPipeDesktopAISetupDialog;
import org.hkijena.jipipe.desktop.commons.components.renderers.JIPipeNodeStateChips;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.components.slots.JIPipeDesktopAddAlgorithmSlotPanel;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "Find node" dialog (Ctrl+A / context action). Rebuilt in Task 17 into a
 * permanent three-zone layout:
 * <ol>
 * <li><b>left</b> — the taxonomy function tree (13 top-levels → leaves, from
 * the live {@link FunctionRegistry}); selecting a node filters the center
 * list to entries whose {@link JIPipeNodeDatabaseEntry#getFunctionIds()
 * function ids} include that node or one of its descendants
 * ({@link NodeBrowserModel#descendantIds(FunctionRegistry, JIPipeNodeTaxonomyFunction)}),</li>
 * <li><b>center</b> — the results {@link JList} with the existing search
 * field, find-existing / create-nodes toggles, AI toggle and list behavior
 * (preserved from the pre-rebuild dialog — the "see what's available"
 * contract),</li>
 * <li><b>right</b> — the {@link NodeFinderDetailRail} (node facets +
 * Documentation / Related / Follow-ups tabs; see the rail's javadoc for the
 * deferred template-based action follow-up).</li>
 * </ol>
 * <p>
 * The launch contract is frozen: the constructor
 * {@link #JIPipeDesktopNodeFinderDialogUI(JIPipeDesktopGraphCanvasUI, JIPipeDataSlot)}
 * and its call sites ({@code AddNewNodeUIContextAction} passes
 * {@code (canvasUI, null)}; the slot-manager and pipeline-editor callers pass
 * a slot) keep working unchanged. When {@code querySlot != null} the title
 * says "Find matching source/target" and the results are the slot-targeted
 * query; the {@code null} case is the generic "Find node".
 * <p>
 * In search-active mode (non-empty search text) the flat query results
 * override the function tree: the tree stays visible, enabled and
 * navigable, but its branch filter is not applied to the flat results
 * (the stored branch is dropped on the transition into search and, once
 * the search is cleared, re-synced from the tree's current selection),
 * while the list and rail keep working.
 */
public class JIPipeDesktopNodeFinderDialogUI extends JDialog {
    private static boolean AI_SEARCH = false;
    private static String LAST_SEARCH = "";
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeDataSlot querySlot;
    private final JToggleButton findExistingNodesToggle = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/find.png"));
    private final JToggleButton createNodesToggle = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/add.png"));
    private final JList<JIPipeNodeDatabaseEntry> nodeList = new JList<>();
    private final JScrollPane scrollPane = new JScrollPane(nodeList);
    private final JIPipeEphemeralRunnableExecutor executor = new JIPipeEphemeralRunnableExecutor("Node finder");
    private final NodeBrowserModel model = new NodeBrowserModel();

    /**
     * Function-leaf tree rows to highlight because a search matched a node
     * under them (bold in the tree renderer); empty when no search is active.
     */
    private final JToggleButton aiSearchButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/ai.png"));
    private JIPipeDesktopSearchTextField searchField;

    // --- Three-zone layout components ---
    /** The shared function tree (tree + slim control bar under it). */
    private final NodeBrowserTreeController tree;
    private final NodeFinderDetailRail detailRail;
    private final JSplitPane leftSplit;
    private final JSplitPane rightSplit;

    /**
     * The entry the user clicked in the detail rail's related/follow-up
     * lists, which may not be present in the current (filtered) list view.
     * Set by {@link #selectEntryFromRail(JIPipeNodeDatabaseEntry)} and consumed
     * by {@link #applyResult(List, Set)} once the broadened view has loaded.
     */
    private JIPipeNodeDatabaseEntry pendingRailSelection;
    /**
     * The reload generation that {@link #pendingRailSelection} was set for (see
     * {@link #reloadGeneration}). {@link #applyResult} only consumes the pending
     * selection for the run whose generation matches, so a superseded broaden
     * reload cannot leave a stale pending selection for a later reload.
     */
    private long pendingRailSelectionGeneration = -1;
    /**
     * Guards the search / tree listeners while the rail's entry selection
     * programmatically broadens the view, so those listeners do not queue
     * redundant reloads.
     */
    private boolean programmaticUpdate;
    /**
     * True for the short window after {@link #selectEntryFromRail} has cleared
     * an active search in order to broaden the view. The search field's
     * debounced echo of that programmatic clear would otherwise re-apply the
     * list ~150ms later and reset the selection to row 0, clobbering the
     * entry the user just clicked in the rail. Set only when a non-empty
     * search was actually cleared (a no-op clear schedules no debounce, so
     * the flag would otherwise go stale).
     */
    private boolean broadening;
    /**
     * The search-active state of the last {@link #applySearchActiveState()}
     * call, so the branch filter is dropped exactly once per inactive→active
     * transition (see its javadoc) instead of on every debounced keystroke.
     */
    private boolean searchWasActive;
    /**
     * Monotonic counter bumped on every {@link #reloadList()}. A
     * {@link ReloadListRun} only applies its result if this value is still
     * current when the result is handed back on the EDT, so a stale in-flight
     * query (superseded by a newer reload) cannot clobber the list — in
     * particular it cannot reset the selection after
     * {@link #selectEntryFromRail} has broadened the view.
     */
    private long reloadGeneration;

    public JIPipeDesktopNodeFinderDialogUI(JIPipeDesktopGraphCanvasUI canvasUI, JIPipeDataSlot querySlot) {
        this.canvasUI = canvasUI;
        this.querySlot = querySlot;

        // Function tree (left zone; shared controller: the tree, the slim
        // control bar under it, the search-match highlighting, the
        // collapse-all and crosshairs actions)
        this.tree = new NodeBrowserTreeController();
        tree.setPreferredSize(new Dimension(240, 400));
        tree.getTree().addTreeSelectionListener(e -> {
            if (programmaticUpdate) {
                return;
            }
            updateModelTreePathFromSelection();
            reloadList();
        });
        tree.setReferenceEntrySupplier(this::referenceEntry);

        // Detail rail (right zone)
        this.detailRail = new NodeFinderDetailRail(resolveDatabase(), this::selectEntryFromRail);
        detailRail.setPreferredSize(new Dimension(300, 400));
        detailRail.setMinimumSize(new Dimension(180, 0));

        // Center list
        initializeList();

        // Splitters (draggable; the rail can be narrowed down to 180px)
        this.rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, detailRail);
        this.rightSplit.setResizeWeight(0.75);
        this.rightSplit.setContinuousLayout(true);
        this.leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, rightSplit);
        this.leftSplit.setResizeWeight(0.25);
        this.leftSplit.setContinuousLayout(true);

        initialize();
        // The compartments graph ⇒ compartments role, everything else ⇒
        // pipeline role (mirrors the list query's visibility role).
        tree.populateTree(canvasUI.getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench workbench
            && canvasUI.getGraph() == workbench.getProject().getCompartmentGraph() ?
            JIPipeNodeDatabasePipelineVisibility.Compartments :
            JIPipeNodeDatabasePipelineVisibility.Pipeline);
        reloadList();
    }

    /**
     * The reference entry the tree's crosshairs action reveals: the first
     * node selected in the graph, when there is one (the first element of
     * the canvas selection), otherwise the first center-list row.
     */
    private JIPipeNodeDatabaseEntry referenceEntry() {
        // The canvas selection is a LinkedHashSet, so the first element is
        // the first selected node
        Set<? extends JIPipeDesktopGraphInteractiveObjectUI> selection = canvasUI.getSelectionManager().getSelection();
        if (!selection.isEmpty()
            && selection.iterator().next() instanceof JIPipeDesktopGraphNodeUI nodeUI) {
            String nodeId = nodeUI.getNode().getInfo().getId();
            for (JIPipeNodeDatabaseEntry entry : resolveDatabase().getEntries()) {
                if (nodeId.equals(entry.getOriginNodeId())) {
                    return entry;
                }
            }
        }
        int size = nodeList.getModel().getSize();
        return size > 0 ? nodeList.getModel().getElementAt(0) : null;
    }

    private void initialize() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        setContentPane(contentPanel);

        initializeToolBar();
        contentPanel.add(leftSplit, BorderLayout.CENTER);

        setIconImage(UIUtils.getJIPipeIcon128());
        if (querySlot != null) {
            setTitle("Find matching " + (querySlot.isInput() ? "source" : "target"));
        } else {
            setTitle("Find node");
        }
        pack();
        setSize(1000, 640);
        setLocationRelativeTo(SwingUtilities.getWindowAncestor(canvasUI));
        setModal(true);
        UIUtils.addEscapeListener(this);
    }

    private void initializeList() {
        nodeList.setOpaque(false);
        nodeList.setCellRenderer(new JIPipeDesktopNodeFinderDatasetListCellRenderer(this));
        nodeList.setFixedCellHeight(JIPipeDesktopNodeFinderDatasetListCellRenderer.CELL_HEIGHT);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Keep the detail rail in sync with the selected list entry
        nodeList.addListSelectionListener(e -> detailRail.setEntry(nodeList.getSelectedValue()));

        nodeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
                    onItemClicked(e);
                }
            }
        });
    }

    public Set<String> getPinnedNodeDatabaseEntries() {
        return new HashSet<>(JIPipe.getSettings().getListFromRegistry("node-db", JIPipePathMetadataStore.key("pinned-node-ids"), String.class, true));
    }

    public JIPipeDataSlot getQuerySlot() {
        return querySlot;
    }

    private JIPipeNodeDatabase resolveDatabase() {
        if (canvasUI.getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
            return ((JIPipeDesktopProjectWorkbench) canvasUI.getDesktopWorkbench()).getNodeDatabase();
        }
        return JIPipeNodeDatabase.getInstance();
    }

    private void onItemClicked(MouseEvent e) {
        int index = nodeList.locationToIndex(e.getPoint());
        if (index >= 0) {
            JIPipeNodeDatabaseEntry entry = nodeList.getModel().getElementAt(index);
            nodeList.setSelectedIndex(index);
            openEntryMenu(entry, e.getPoint(), false);
        }
    }

    private void openEntryMenu(JIPipeNodeDatabaseEntry entry, Point location, boolean fromKeyboard) {
        JPopupMenu menu = new JPopupMenu();
        if (!entry.exists()) {
            menu.add(UIUtils.createMenuItem("Add new to graph", "Adds the selected node", JIPipe.RESOURCES.getIcon16("actions/node-add.png"), () -> addEntryToGraph(entry)));
        }
        if (querySlot != null) {
            if (querySlot.isInput()) {
                if ((!entry.getInputSlots().isEmpty() || entry.canAddOutputSlots()) && menu.getComponentCount() > 0) {
                    menu.addSeparator();
                }
                for (Map.Entry<String, JIPipeDataSlotInfo> infoEntry : entry.getOutputSlots().entrySet()) {
                    if (JIPipe.getDataTypes().isConvertible(infoEntry.getValue().getDataClass(), querySlot.getAcceptedDataType())) {
                        menu.add(UIUtils.createMenuItem("Connect to " + infoEntry.getKey(),
                                "Connect to the specified slot. Add the node if required.",
                                JIPipe.getDataTypes().getIconFor(infoEntry.getValue().getDataClass()),
                                () -> addAndConnectEntry(entry, infoEntry.getValue())));
                    }
                }
                if (entry.canAddOutputSlots()) {
                    menu.addSeparator();
                    menu.add(UIUtils.createMenuItem("Connect to new slot",
                            "Connect to a new slot. Add the node if required.",
                            JIPipe.RESOURCES.getIcon16("actions/add.png"),
                            () -> addAndConnectEntry(entry, null)));
                }
            } else {
                if ((!entry.getInputSlots().isEmpty() || entry.canAddInputSlots()) && menu.getComponentCount() > 0) {
                    menu.addSeparator();
                }
                for (Map.Entry<String, JIPipeDataSlotInfo> infoEntry : entry.getInputSlots().entrySet()) {
                    if (JIPipe.getDataTypes().isConvertible(querySlot.getAcceptedDataType(), infoEntry.getValue().getDataClass())) {
                        menu.add(UIUtils.createMenuItem("Connect to " + infoEntry.getKey(),
                                "Connect to the specified slot. Add the node if required.",
                                JIPipe.getDataTypes().getIconFor(infoEntry.getValue().getDataClass()),
                                () -> addAndConnectEntry(entry, infoEntry.getValue())));
                    }
                }
                if (entry.canAddInputSlots()) {
                    menu.addSeparator();
                    menu.add(UIUtils.createMenuItem("Connect to new slot",
                            "Connect to a new slot. Add the node if required.",
                            JIPipe.RESOURCES.getIcon16("actions/add.png"),
                            () -> addAndConnectEntry(entry, null)));
                }
            }

        }

        menu.show(nodeList, location.x, location.y);

        if (fromKeyboard) {
            JMenuItem firstItem = (JMenuItem) menu.getComponent(0);

            SwingUtilities.invokeLater(() -> {
                MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{menu, firstItem});
            });
        }
    }

    private void addAndConnectEntry(JIPipeNodeDatabaseEntry entry, JIPipeDataSlotInfo slotInfo) {
        JIPipeDesktopGraphNodeUI nodeUI = entry.addToGraph(canvasUI).iterator().next();
        JIPipeGraphNode node = nodeUI.getNode();
        if (querySlot != null) {
            if (querySlot.isInput()) {
                if (slotInfo != null) {
                    JIPipeDataSlot targetSlot = node.getOutputSlot(slotInfo.getName());
                    if (targetSlot != null) {
                        canvasUI.getGraph().connect(targetSlot, querySlot);
                    }
                } else {
                    JIPipeDesktopAddAlgorithmSlotPanel panel = JIPipeDesktopAddAlgorithmSlotPanel.showDialog(SwingUtilities.getWindowAncestor(canvasUI),
                            canvasUI.getHistoryJournal(),
                            node,
                            JIPipeSlotType.Output);
                    if (!panel.getAddedSlots().isEmpty()) {
                        canvasUI.getGraph().connect(panel.getAddedSlots().get(0), querySlot);
                    }
                }
            } else {
                if (slotInfo != null) {
                    JIPipeDataSlot targetSlot = node.getInputSlot(slotInfo.getName());
                    if (targetSlot != null) {
                        canvasUI.getGraph().connect(querySlot, targetSlot);
                    }
                } else {
                    JIPipeDesktopAddAlgorithmSlotPanel panel = JIPipeDesktopAddAlgorithmSlotPanel.showDialog(SwingUtilities.getWindowAncestor(canvasUI),
                            canvasUI.getHistoryJournal(),
                            node,
                            JIPipeSlotType.Input);
                    if (!panel.getAddedSlots().isEmpty()) {
                        canvasUI.getGraph().connect(querySlot, panel.getAddedSlots().get(0));
                    }
                }
            }
        }
        LAST_SEARCH = searchField.getText();
        setVisible(false);
    }

    private void addEntryToGraph(JIPipeNodeDatabaseEntry entry) {
        entry.addToGraph(canvasUI);
        LAST_SEARCH = searchField.getText();
        setVisible(false);
    }

    private void initializeToolBar() {

        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));

        // Info toolbar
        if (querySlot != null) {
            JIPipeGraphNode queryNode = querySlot.getNode();
            JToolBar infoToolbar = new JToolBar();
            infoToolbar.setFloatable(false);
            infoToolbar.add(new JLabel("Selected slot"));
            infoToolbar.add(Box.createRigidArea(new Dimension(8, 32)));
            JLabel algorithmNameLabel = new JLabel(queryNode.getName(), JIPipe.getNodes().getIconFor(queryNode.getInfo()), JLabel.LEFT);
            algorithmNameLabel.setToolTipText(TooltipUtils.getAlgorithmTooltip(queryNode.getInfo()));
            infoToolbar.add(algorithmNameLabel);
            JLabel arrowLabel = new JLabel(JIPipe.RESOURCES.getIcon16("actions/caret-right.png"));
            infoToolbar.add(arrowLabel);
            JLabel slotNameLabel = new JLabel(querySlot.getName(), JIPipe.getDataTypes().getIconFor(querySlot.getAcceptedDataType()), JLabel.LEFT);
            slotNameLabel.setToolTipText(TooltipUtils.getDataTableTooltip(querySlot));
            infoToolbar.add(slotNameLabel);
            toolbarPanel.add(infoToolbar);
        }

        // Main toolbar
        JToolBar mainToolBar = new JToolBar();
        mainToolBar.setFloatable(false);
        mainToolBar.add(Box.createHorizontalStrut(8));

        searchField = new JIPipeDesktopSearchTextField(executor);
        searchField.setText(LAST_SEARCH);
        searchField.addActionListener(e -> {
            if (!programmaticUpdate) {
                // Swallow the debounced echo of the programmatic search-clear
                // performed by selectEntryFromRail (broadening): a real
                // keystroke into the empty field is indistinguishable from
                // it at this point, but the broadened view is exactly what
                // the user asked for, so re-applying it is unnecessary. Any
                // search event that is NOT the empty clear-echo also
                // invalidates the pending broadening flag so it cannot go
                // stale and swallow a later, genuine clear.
                if (broadening && searchField.getText().isEmpty()) {
                    broadening = false;
                    return;
                }
                broadening = false;
                reloadList();
            }
        });
        searchField.getTextField().selectAll();
        searchField.getTextField().addActionListener(e -> openFirstEntryMenu());
        mainToolBar.add(searchField);

        // AI-based search
        UIUtils.makeButtonFlatWithSize(aiSearchButton, 36);
        aiSearchButton.setSelected(AI_SEARCH);
        aiSearchButton.setToolTipText("Use AI-based node search using an embedding model");
        aiSearchButton.addActionListener(e -> {
            AI_SEARCH = aiSearchButton.isSelected();
            if (aiSearchButton.isSelected()) {
                // Ensure that AI is set up
                if (JIPipeDesktopAISetupDialog.checkFirstTimeSetup(canvasUI.getDesktopWorkbench())) {
                    // Spin up the embedding model already
                    JIPipe.getInstance().getAiService().tryStartEmbeddingModel();

                    // Clear list to indicate AI switch
                    nodeList.setModel(new DefaultListModel<>());

                    // Trigger refresh
                    reloadList();
                }
            } else {
                // Switched AI off — reload with standard search
                reloadList();
            }
        });
        if (AIApplicationSettings.getInstance().isEnableAI()) {
            mainToolBar.add(aiSearchButton);
        }

        if (querySlot != null) {
            findExistingNodesToggle.setText("Find existing");
            findExistingNodesToggle.setSelected(JIPipeGraphEditorUIApplicationSettings.getInstance().getNodeSearchSettings().isSearchFindExistingNodes());
            createNodesToggle.setText("Create new");
            createNodesToggle.setSelected(JIPipeGraphEditorUIApplicationSettings.getInstance().getNodeSearchSettings().isSearchFindNewNodes());
            findExistingNodesToggle.addActionListener(e -> {
                JIPipeGraphEditorUIApplicationSettings.getInstance().getNodeSearchSettings().setSearchFindExistingNodes(findExistingNodesToggle.isSelected());
                JIPipe.autoSaveSettings();
                reloadList();
            });
            createNodesToggle.addActionListener(e -> {
                JIPipeGraphEditorUIApplicationSettings.getInstance().getNodeSearchSettings().setSearchFindNewNodes(createNodesToggle.isSelected());
                JIPipe.autoSaveSettings();
                reloadList();
            });
            mainToolBar.add(findExistingNodesToggle);
            mainToolBar.add(createNodesToggle);
        }
        mainToolBar.add(Box.createHorizontalStrut(8));

        toolbarPanel.add(mainToolBar);

        getContentPane().add(toolbarPanel, BorderLayout.NORTH);
    }

    private void openFirstEntryMenu() {
        if (nodeList.getModel().getSize() > 0) {
            nodeList.setSelectedIndex(0);
            Point point = nodeList.indexToLocation(0);
            point.x += nodeList.getWidth() - 32;
            point.y += JIPipeDesktopNodeFinderDatasetListCellRenderer.CELL_HEIGHT;
            openEntryMenu(nodeList.getModel().getElementAt(0), point, true);
        }
    }

    // ------------------------------------------------------------------
    // Function tree (left zone)
    // ------------------------------------------------------------------

    /**
     * Reads the selected tree path into the shared {@link NodeBrowserModel}.
     */
    private void updateModelTreePathFromSelection() {
        TreePath path = tree.getTree().getSelectionPath();
        if (path == null) {
            model.setTreePath(List.of());
            return;
        }
        List<JIPipeNodeTaxonomyFunction> pathFunctions = new ArrayList<>();
        for (Object node : path.getPath()) {
            if (node instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction fn) {
                pathFunctions.add(fn);
            }
        }
        model.setTreePath(pathFunctions);
    }

    /**
     * Keeps the stored tree branch filter consistent with the search
     * state. When a search becomes active the flat query results override
     * the function tree, so the stored branch filter is dropped ONCE (on
     * the inactive→active transition). When the search is cleared again,
     * the filter is re-synced from the tree's current selection ONCE
     * (active→inactive) — the user may have navigated the tree while
     * searching, and that branch then applies to the (no longer flat)
     * results. The tree itself is left alone at all times — it stays
     * enabled and navigable (dimming/disabling it blocked all interaction,
     * which the evaluation flagged).
     */
    private void applySearchActiveState() {
        boolean searchActive = model.isSearchActive();
        if (searchActive && !searchWasActive) {
            model.setTreePath(List.of());
        } else if (!searchActive && searchWasActive) {
            updateModelTreePathFromSelection();
        }
        searchWasActive = searchActive;
    }

    // ------------------------------------------------------------------
    // Rail → center list entry selection (with recursion guard)
    // ------------------------------------------------------------------

    /**
     * Called by the detail rail when the user clicks a related / follow-up
     * entry. If the entry is already visible in the center list (matched by
     * id — see {@link #indexOfEntry}) it is selected directly (which, via
     * the list's selection listener, updates the rail — one hop, and the
     * rail's {@code setEntry} does not call the selector again, so there is
     * no loop). Otherwise the view is broadened (search cleared, tree reset
     * to the entry's function branch) and the entry is selected once the
     * reloaded list is applied by {@link #applyResult(List, Set)}.
     */
    private void selectEntryFromRail(JIPipeNodeDatabaseEntry entry) {
        int index = indexOfEntry(nodeList.getModel(), entry);
        if (index >= 0) {
            nodeList.setSelectedIndex(index);
            return;
        }
        // The entry is not in the current filtered view. Broaden the view so
        // the reloaded list contains it, then select it in applyResult.
        pendingRailSelection = entry;
        programmaticUpdate = true;
        try {
            // Clearing a non-empty search schedules the search field's
            // debounced echo; mark broadening so that echo does not re-apply
            // the list (and reset the selection) after our reload finishes.
            if (!searchField.getText().trim().isEmpty()) {
                broadening = true;
            }
            searchField.setText("");
            // Reveal the entry's function branch in the tree (instead of
            // resetting to "All functions"): expand it and select its row,
            // so the broadened view is visibly scoped to the clicked entry.
            // The tree filter is NOT applied (the list keeps the full,
            // broadened result — the entry must be in it).
            JIPipeNodeTaxonomyFunction primaryFunction = NodeBrowserTreeController.primaryFunctionOf(entry);
            TreePath functionRow = primaryFunction != null ? tree.functionRow(primaryFunction) : null;
            if (functionRow != null) {
                tree.selectRow(functionRow);
            } else {
                tree.getTree().setSelectionRow(0);
            }
        } finally {
            programmaticUpdate = false;
        }
        model.setSearch("");
        // The tree now shows the clicked entry's branch — make the stored
        // branch filter match what the tree displays, so list and tree
        // agree (the reloaded list is scoped to that branch, which
        // contains the clicked entry by definition). The applyResult
        // pending-rail selection still lands on the entry even though the
        // list is branch-scoped.
        updateModelTreePathFromSelection();
        applySearchActiveState();
        long gen = reloadList();
        pendingRailSelectionGeneration = gen;
    }

    // ------------------------------------------------------------------
    // List reload (shared async pattern)
    // ------------------------------------------------------------------

    /**
     * @return the reload generation the new {@link ReloadListRun} was enqueued
     * under (see {@link #reloadGeneration}); a run only applies its result if
     * it is still the latest generation when it finishes.
     */
    private long reloadList() {
        model.setSearch(searchField.getText());
        applySearchActiveState();
        reloadGeneration++;
        executor.enqueue(new ReloadListRun(this));
        return reloadGeneration;
    }

    /**
     * Applies a query result (already off the EDT) to the center list:
     * post-filters by the selected function tree (unless a search is active),
     * renders the list and (re)selects the first row or the pending rail
     * selection.
     *
     * @param queryResult       the raw node-database query result
     * @param treeDescendantIds  when non-null and non-empty, the function ids
     *                           of the selected tree node + its descendants
     *                           (the tree filter); {@code null} means "no
     *                           tree filter"
     */
    private void applyResult(List<JIPipeNodeDatabaseEntry> queryResult, Set<String> treeDescendantIds) {
        List<JIPipeNodeDatabaseEntry> filtered = new ArrayList<>(queryResult);
        if (treeDescendantIds != null && !treeDescendantIds.isEmpty()) {
            filtered.removeIf(entry -> {
                for (String fid : entry.getFunctionIds()) {
                    if (treeDescendantIds.contains(fid)) {
                        return false;
                    }
                }
                return true;
            });
        }
        // Down-ranked nodes (auto-imported, deprecated, unstable — the
        // generalized state classifiers, see JIPipeNodeStateChips) come
        // after the standard curated nodes (same rule as the node browser).
        filtered.sort(Comparator.comparingInt(entry ->
            JIPipeNodeStateChips.isDownRanked(entry) ? 1 : 0));
        // Highlight the tree leafs the search matched (same rule as the node
        // browser; no-op without a search term)
        tree.updateSearchHighlights(model.isSearchActive(), filtered);
        DefaultListModel<JIPipeNodeDatabaseEntry> listModel = new DefaultListModel<>();
        for (JIPipeNodeDatabaseEntry entry : filtered) {
            listModel.addElement(entry);
        }
        nodeList.setModel(listModel);
        scrollPane.getVerticalScrollBar().setValue(0);

        // This run only reaches here if it is still the latest generation, so
        // reloadGeneration == this run's generation. Consume a pending rail
        // selection only if it was requested for this (current) reload — a
        // pending selection left over from a superseded broaden reload must
        // not be applied to an unrelated later reload.
        boolean pendingMatches = pendingRailSelection != null && pendingRailSelectionGeneration == reloadGeneration;
        if (pendingMatches) {
            int index = indexOfEntry(listModel, pendingRailSelection);
            if (index >= 0) {
                nodeList.setSelectedIndex(index);
                // Make the clicked rail entry visible (the list was just
                // re-created and the scrollbar reset to the top).
                nodeList.ensureIndexIsVisible(index);
            }
            pendingRailSelection = null;
            pendingRailSelectionGeneration = -1;
        } else {
            // Clear a stale pending selection (from a superseded broaden
            // reload) so it cannot linger; select the first row otherwise.
            pendingRailSelection = null;
            pendingRailSelectionGeneration = -1;
            if (!listModel.isEmpty()) {
                nodeList.setSelectedIndex(0);
                nodeList.ensureIndexIsVisible(0);
            }
        }
    }

    /**
     * The index of the first entry with the same id in the given list
     * model, or -1. Entries carry no {@code equals()} override (reference
     * identity only), and related / follow-up lookups resolve entries
     * through a different code path than the list query, so the id — not
     * the instance — is the stable identity (the {@link ListModel}
     * interface does not expose a lookup by value either way).
     */
    private static int indexOfEntry(ListModel<? extends JIPipeNodeDatabaseEntry> model, JIPipeNodeDatabaseEntry entry) {
        if (entry == null) {
            return -1;
        }
        String entryId = entry.getId();
        for (int i = 0; i < model.getSize(); i++) {
            if (entryId.equals(model.getElementAt(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    public static class ReloadListRun extends DefaultJIPipeRunnable {

        private final JIPipeDesktopNodeFinderDialogUI dialogUI;
        /** The reload generation this run was enqueued under (see {@link #reloadGeneration}). */
        private final long generation;

        public ReloadListRun(JIPipeDesktopNodeFinderDialogUI dialogUI) {
            this.dialogUI = dialogUI;
            this.generation = dialogUI.reloadGeneration;
        }

        @Override
        public String getTaskLabel() {
            return "Reload list";
        }

        @Override
        public void run() {
            JIPipeDesktopGraphCanvasUI canvasUI = dialogUI.canvasUI;
            JIPipeDataSlot querySlot = dialogUI.querySlot;
            JIPipeNodeDatabase nodeDatabase;
            if (canvasUI.getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                nodeDatabase = ((JIPipeDesktopProjectWorkbench) canvasUI.getDesktopWorkbench()).getNodeDatabase();
            } else {
                nodeDatabase = JIPipeNodeDatabase.getInstance();
            }

            Set<String> pinnedNodeDatabaseEntries = dialogUI.getPinnedNodeDatabaseEntries();

            JIPipeNodeDatabasePipelineVisibility role;
            if (canvasUI.getDesktopWorkbench() instanceof JIPipeDesktopProjectWorkbench) {
                if (canvasUI.getGraph() == canvasUI.getDesktopWorkbench().getProject().getCompartmentGraph()) {
                    role = JIPipeNodeDatabasePipelineVisibility.Compartments;
                } else {
                    role = JIPipeNodeDatabasePipelineVisibility.Pipeline;
                }
            } else {
                role = JIPipeNodeDatabasePipelineVisibility.Pipeline;
            }
            boolean allowExisting, allowNew;
            if (querySlot != null) {
                allowExisting = dialogUI.findExistingNodesToggle.isSelected();
                allowNew = dialogUI.createNodesToggle.isSelected();
            } else {
                allowExisting = false;
                allowNew = true;
            }

            // The slot-targeted / generic query is preserved verbatim. The
            // function tree only ADDS a post-filter (never removes the slot
            // query), so it is captured here and applied on the EDT.
            List<JIPipeNodeDatabaseEntry> queryResult = new ArrayList<>();
            if (querySlot != null) {
                for (JIPipeNodeDatabaseEntry entry : nodeDatabase.query(dialogUI.searchField.getText(),
                        role,
                        allowExisting,
                        allowNew,
                        querySlot.getSlotType(),
                        querySlot.getInfo().getDataClass(),
                        AI_SEARCH ? JIPipeNodeDatabase.FLAG_USE_AI_SEARCH : null)) {
                    JIPipeGraphNode existingNode = null;
                    if (entry instanceof ExistingPipelineNodeDatabaseEntry) {
                        existingNode = ((ExistingPipelineNodeDatabaseEntry) entry).getGraphNode();
                    }
                    if (entry instanceof ExistingCompartmentDatabaseEntry) {
                        existingNode = ((ExistingCompartmentDatabaseEntry) entry).getCompartment();
                    }
                    if (existingNode == querySlot.getNode()) {
                        continue;
                    }
                    queryResult.add(entry);
                }
            } else {
                for (JIPipeNodeDatabaseEntry entry : nodeDatabase.query(dialogUI.searchField.getText(), role, allowExisting, allowNew, pinnedNodeDatabaseEntries, AI_SEARCH ? JIPipeNodeDatabase.FLAG_USE_AI_SEARCH : null)) {
                    queryResult.add(entry);
                }
            }

            // Capture the tree filter at query time (search-active ⇒ no tree
            // filter; otherwise the selected node + its descendants).
            // Computed as a single expression so the local is effectively
            // final for the lambda below.
            Set<String> treeDescendantIds = (!dialogUI.model.isSearchActive() && !dialogUI.model.getTreePath().isEmpty())
                ? NodeBrowserModel.descendantIds(JIPipe.getNodes().getFunctions(),
                    dialogUI.model.getTreePath().get(dialogUI.model.getTreePath().size() - 1))
                : null;

            final long runGeneration = generation;
            SwingUtilities.invokeLater(() -> {
                // Discard a stale result: a newer reload was enqueued after
                // this one, so applying ours would clobber the (newer) list
                // and reset the selection (e.g. after selectEntryFromRail).
                if (runGeneration != dialogUI.reloadGeneration) {
                    return;
                }
                dialogUI.applyResult(queryResult, treeDescendantIds);
            });
        }
    }

}
