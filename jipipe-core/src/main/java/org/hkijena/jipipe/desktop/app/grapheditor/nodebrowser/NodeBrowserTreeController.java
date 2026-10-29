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

package org.hkijena.jipipe.desktop.app.grapheditor.nodebrowser;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.NodeBrowserModel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * The shared function-tree component of the node browser (add-node panel)
 * and the node finder dialog.
 * <p>
 * Before this class both UIs carried their own copy of the tree machinery -
 * the "All functions" tree model, the empty-branch pruning, the collapse-all
 * reset, the "select the reference node in the tree" crosshairs action, the
 * search-match highlighting and the tree cell renderer. Those copies had
 * drifted apart; this component is the single implementation, wrapped in a
 * panel that adds the slim control bar (collapse-all + crosshairs) under the
 * tree, so the two call sites can drop the tree into their layouts as one
 * component.
 * <p>
 * Call-site specifics stay with the call sites: the browser owns its
 * selection listener (branch filtering, node-leaf insert targets, the
 * {@code selectionSuppressed} guard it passes in), the finder owns its own
 * (list re-query, the {@code programmaticUpdate} guard). The reference entry
 * for the crosshairs action is provided via
 * {@link #setReferenceEntrySupplier(java.util.function.Supplier)}.
 */
public class NodeBrowserTreeController extends JPanel {

    private final JTree tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("All functions")));
    private final DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();

    /**
     * The "select the reference node in the tree" crosshairs button of the
     * slim control bar (visible by default - the node browser uses it; the
     * modal find-node dialog hides it via
     * {@link #setCrosshairsButtonVisible}).
     */
    private JButton crosshairsButton;

    /**
     * Function-leaf tree rows to highlight because a search matched a node
     * under them (bold in the cell renderer); empty when no search is
     * active.
     */
    private volatile Set<TreePath> searchHighlightedLeafPaths = Set.of();

    /**
     * Supplies the entry the crosshairs button should reveal in the tree
     * (e.g. the first selected graph node, or the first result row). May
     * return {@code null} - the button is then a no-op.
     */
    private Supplier<JIPipeNodeDatabaseEntry> referenceEntrySupplier = () -> null;

    public NodeBrowserTreeController() {
        setLayout(new BorderLayout());

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tree.setCellRenderer(new FunctionTreeCellRenderer(this));
        tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Slim control bar under the tree (no border, minimal height): a
        // collapse-all reset and a "select the reference node in the tree"
        // crosshairs button (hideable via setCrosshairsButtonVisible - the
        // modal find-node dialog hides it, see its javadoc).
        JPanel treeToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        treeToolbar.setBorder(null);
        JButton collapseAllButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/collapse-all.png"));
        UIUtils.makeButtonFlatWithSize(collapseAllButton, 26);
        collapseAllButton.setToolTipText("Collapse the function tree");
        collapseAllButton.addActionListener(e -> collapseToRoot());
        treeToolbar.add(collapseAllButton);
        crosshairsButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/crosshairs.png"));
        UIUtils.makeButtonFlatWithSize(crosshairsButton, 26);
        crosshairsButton.setToolTipText("Select the reference node in the tree");
        crosshairsButton.addActionListener(e -> selectReferenceNodeInTree());
        treeToolbar.add(crosshairsButton);

        add(treeScroll, BorderLayout.CENTER);
        add(treeToolbar, BorderLayout.SOUTH);
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
     * The primary taxonomy function of the given entry, or {@code null}.
     * <p>
     * {@link JIPipeNodeDatabaseEntry#getFunctionIds()} is a {@link LinkedHashSet}
     * with the primary id added first (see
     * {@code CreateNewNodeByInfoDatabaseEntry.initializeTaxonomyFacets}), so
     * the first element is the primary function.
     */
    public static JIPipeNodeTaxonomyFunction primaryFunctionOf(JIPipeNodeDatabaseEntry entry) {
        Set<String> ids = entry.getFunctionIds();
        if (ids.isEmpty()) {
            return null;
        }
        return JIPipe.getNodes().getFunctions().byId(ids.iterator().next());
    }

    public JTree getTree() {
        return tree;
    }

    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * @return the function-leaf rows highlighted by the last search, empty
     * when no search is active (read by the cell renderer)
     */
    public Set<TreePath> getSearchHighlightedLeafPaths() {
        return searchHighlightedLeafPaths;
    }

    /**
     * @param referenceEntrySupplier the supplier for the crosshairs button's
     *                               reference entry (see the field's doc)
     */
    public void setReferenceEntrySupplier(Supplier<JIPipeNodeDatabaseEntry> referenceEntrySupplier) {
        this.referenceEntrySupplier = referenceEntrySupplier;
    }

    /**
     * Shows or hides the "select the reference node in the tree" crosshairs
     * button of the slim control bar (visible by default - the node browser
     * uses it). The modal find-node dialog hides it: a canvas node
     * selection the button would reveal is unlikely behind a modal, so the
     * button was a frequent no-op there. (The reveal ACTION itself,
     * {@link #selectReferenceNodeInTree}, stays available programmatically.)
     *
     * @param visible {@code true} to show the button (default)
     */
    public void setCrosshairsButtonVisible(boolean visible) {
        crosshairsButton.setVisible(visible);
    }

    /**
     * Populates the tree with the live function registry, pruning branches
     * that carry no node visible for the given role (so the tree never
     * shows empty categories). The root stays expanded. No node leaves are
     * attached - call {@link #rebuildNodeLeaves(List)} (or the 2-arg
     * overload) to add them.
     *
     * @param role the visibility role of the calling editor
     */
    public void populateTree(JIPipeNodeDatabasePipelineVisibility role) {
        populateTree(role, List.of());
    }

    // ------------------------------------------------------------------
    // Expansion-state persistence (call site decides WHEN to save/restore;
    // the ids are the stable taxonomy function ids - safe to persist, see
    // the project's stable-ID rule)
    // ------------------------------------------------------------------

    /**
     * Populates the tree with the live function registry and attaches the
     * given entries as node leaves (the full current result set - see
     * {@link #rebuildNodeLeaves(List)}). The branches are pruned by the
     * entries' own function ids when the list is non-empty (a tree that
     * carries leaves is a full navigator, so its pruning follows the same
     * set the leaves follow); when the list is empty, pruning falls back to
     * the role-visible registered nodes.
     *
     * @param role    the visibility role of the calling editor
     * @param entries the entries to attach as node leaves (may be empty)
     */
    public void populateTree(JIPipeNodeDatabasePipelineVisibility role, List<JIPipeNodeDatabaseEntry> entries) {
        FunctionRegistry registry = JIPipe.getNodes().getFunctions();
        Set<String> populated;
        if (entries.isEmpty()) {
            populated = NodeBrowserModel.populatedFunctionIds(
                    JIPipe.getNodes().getRegisteredNodeInfos().values(), role);
        } else {
            // Prune by the entries themselves: a function branch stays when
            // any entry is filed under it (primary or secondary). This keeps
            // the tree and the leaves on the exact same set, so no leaf is
            // ever orphaned and no populated branch is hidden.
            populated = new LinkedHashSet<>();
            for (JIPipeNodeDatabaseEntry entry : entries) {
                populated.addAll(entry.getFunctionIds());
            }
        }
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();
        for (JIPipeNodeTaxonomyFunction top : registry.topLevel()) {
            DefaultMutableTreeNode node = buildTreeNode(top, registry, populated);
            if (node != null) {
                root.add(node);
            }
        }
        // The synthetic "Templates" branch (spec §7.2): a UI-only scope
        // grouping, appended AFTER the registry branches so it is never
        // pruned by the node-info logic. It carries all three scopes
        // (System/Global/Project) regardless of population.
        DefaultMutableTreeNode templatesBranch =
                new DefaultMutableTreeNode(TemplateScopeTreeNode.ROOT);
        for (TemplateScope scope : TemplateScope.values()) {
            templatesBranch.add(new DefaultMutableTreeNode(TemplateScopeTreeNode.forScope(scope)));
        }
        root.add(templatesBranch);
        attachNodeLeaves(root, entries);
        treeModel.reload();
        tree.expandPath(new TreePath(new Object[]{root}));
    }

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

    /**
     * The stable ids of all currently expanded function branches (function
     * rows only - the root's expansion is implicit and entry leaves are
     * never persisted). The call site decides how long to keep the list
     * (the node browser holds it in memory for the current run only).
     */
    public List<String> expandedFunctionIds() {
        List<String> ids = new ArrayList<>();
        collectExpandedFunctionIds((DefaultMutableTreeNode) treeModel.getRoot(), ids);
        return ids;
    }

    private void collectExpandedFunctionIds(DefaultMutableTreeNode node, List<String> ids) {
        if (node.getUserObject() instanceof JIPipeNodeTaxonomyFunction function
                && node.getPath() != null
                && tree.isExpanded(new TreePath(node.getPath()))) {
            ids.add(function.getId());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof DefaultMutableTreeNode child) {
                collectExpandedFunctionIds(child, ids);
            }
        }
    }

    /**
     * Expands every branch whose stable function id is in the given list
     * (top-down, so an ancestor listed after its child still expands).
     * Unknown ids (a function removed or pruned from the current view) are
     * ignored. Call after {@link #populateTree} /
     * {@link #rebuildNodeLeaves(List)}.
     *
     * @param functionIds the persisted expanded function ids
     */
    public void restoreExpandedFunctionIds(Collection<String> functionIds) {
        if (functionIds == null || functionIds.isEmpty()) {
            return;
        }
        Set<String> wanted = new HashSet<>(functionIds);
        restoreExpandedFunctionIds((DefaultMutableTreeNode) treeModel.getRoot(), wanted);
    }

    // ------------------------------------------------------------------
    // Node leaves (compact single-pane of the node browser)
    // ------------------------------------------------------------------

    private void restoreExpandedFunctionIds(DefaultMutableTreeNode node, Set<String> wanted) {
        if (node.getPath() != null
                && node.getUserObject() instanceof JIPipeNodeTaxonomyFunction function
                && wanted.remove(function.getId())) {
            // JTree.expandPath expands every node on the path, so the
            // root/ancestors follow along automatically.
            tree.expandPath(new TreePath(node.getPath()));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof DefaultMutableTreeNode child) {
                restoreExpandedFunctionIds(child, wanted);
            }
        }
    }

    /**
     * Whether any function branch currently carries entry (node-leaf)
     * children.
     */
    public boolean hasNodeLeaves() {
        return branchHasEntryChildren((DefaultMutableTreeNode) treeModel.getRoot());
    }

    private boolean branchHasEntryChildren(DefaultMutableTreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode mtn) {
                if (mtn.getUserObject() instanceof JIPipeNodeDatabaseEntry) {
                    return true;
                }
                if (mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction && branchHasEntryChildren(mtn)) {
                    return true;
                }
                // A synthetic Templates/<scope> row (or the header) also
                // carries entry leaves - recurse so its expandability is
                // treated like a function branch with entry children.
                if (mtn.getUserObject() instanceof TemplateScopeTreeNode && branchHasEntryChildren(mtn)) {
                    return true;
                }
                // A synthetic <function>/Patterns row carries the function's
                // template leaves - recurse the same way.
                if (mtn.getUserObject() instanceof PatternsTreeNode && branchHasEntryChildren(mtn)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * (Re)builds the node leaves of the function tree from the given
     * entries, preserving the expanded function rows and the (function-row)
     * selection across the model reload.
     * <p>
     * Callers are responsible for the "should I rebuild at all?" decision
     * (tier, search state) and for suppressing their own selection listeners
     * (the {@code selectionSuppressed}/{@code programmaticUpdate} guards)
     * around this call - see the call sites.
     *
     * @param entries the post-filtered entries to attach as node leaves
     */
    public void rebuildNodeLeaves(List<JIPipeNodeDatabaseEntry> entries) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        boolean hadLeaves = hasNodeLeaves();
        boolean willHaveLeaves = !entries.isEmpty();
        if (!hadLeaves && !willHaveLeaves) {
            return;
        }

        // Capture the expanded function paths + the selection so they
        // survive the reload below.
        Set<TreePath> expandedFunctionPaths = new LinkedHashSet<>();
        for (int i = 0; i < tree.getRowCount(); i++) {
            TreePath p = tree.getPathForRow(i);
            if (p != null && tree.isExpanded(p) && entryFromPath(p) == null) {
                expandedFunctionPaths.add(p);
            }
        }
        TreePath selectionPath = tree.getSelectionPath();

        attachNodeLeaves(root, entries);
        treeModel.reload();

        // Restore the captured navigation state (only function rows -
        // entry rows were re-created by attachNodeLeaves)
        for (TreePath p : expandedFunctionPaths) {
            tree.expandPath(p);
        }
        if (selectionPath != null && entryFromPath(selectionPath) == null) {
            tree.setSelectionPath(selectionPath);
        } else {
            tree.clearSelection();
        }
    }

    // ------------------------------------------------------------------
    // Tree path + selection helpers (shared by the crosshairs action, the
    // search highlights and the call sites' selection listeners)
    // ------------------------------------------------------------------

    /**
     * Recursively attaches the entries that list each function node's own
     * id as direct children of that node. The tree ROOT carries a String
     * user object ("All functions"), not a function - it only forwards the
     * walk to its function children (the round-3 ClassCastException was
     * exactly this missing guard).
     */
    private void attachNodeLeaves(DefaultMutableTreeNode functionNode, List<JIPipeNodeDatabaseEntry> entries) {
        // Remove any previously attached node leaves AND any stale
        // <function>/Patterns row (reverse order keeps the indices stable
        // while removing). The Patterns row is fully rebuilt below when the
        // function carries template entries, so a rebuild WITHOUT templates
        // must drop the old row - otherwise an empty "Patterns" entry
        // survives (and its stale leaves would leak into the counts).
        for (int i = functionNode.getChildCount() - 1; i >= 0; i--) {
            Object child = functionNode.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode mtn
                    && (mtn.getUserObject() instanceof JIPipeNodeDatabaseEntry
                    || mtn.getUserObject() instanceof PatternsTreeNode)) {
                functionNode.remove(i);
            }
        }
        Object userObject = functionNode.getUserObject();
        if (userObject instanceof JIPipeNodeTaxonomyFunction function) {
            // A function row: attach the entries that list this function
            // (own id - rows appear at the level the node declares). This is
            // the dual-placement "taxonomy" side; it is free for templates
            // too (their getFunctionIds() returns the function id).
            // Templates, however, do NOT sit at the function's own level:
            // they spill into the node lists and were the source of the
            // "functions full of templates" clutter. They attach one level
            // below, under a synthetic "Patterns" child (only created when
            // the function actually carries template entries), so the
            // function level shows only its real nodes. The top-level
            // Templates/<scope> branch (spec §7.2) is kept as the
            // scope-organized dual view.
            String functionId = function.getId();
            boolean hasTemplate = false;
            for (JIPipeNodeDatabaseEntry entry : entries) {
                if (!entry.getFunctionIds().contains(functionId)) {
                    continue;
                }
                if (entry instanceof CreateNewNodesByTemplateDatabaseEntry) {
                    hasTemplate = true;
                } else {
                    functionNode.add(new DefaultMutableTreeNode(entry));
                }
            }
            if (hasTemplate) {
                DefaultMutableTreeNode patternsChild =
                        new DefaultMutableTreeNode(new PatternsTreeNode(function));
                for (JIPipeNodeDatabaseEntry entry : entries) {
                    if (entry instanceof CreateNewNodesByTemplateDatabaseEntry
                            && entry.getFunctionIds().contains(functionId)) {
                        patternsChild.add(new DefaultMutableTreeNode(entry));
                    }
                }
                functionNode.add(patternsChild);
            }
        } else if (userObject instanceof TemplateScopeTreeNode scopeNode && !scopeNode.isRoot()) {
            // The "scope placement": a synthetic Templates/<scope> row lists
            // the template entries of exactly that scope (guaranteed home).
            // The ROOT header is a pure grouping row - it forwards only.
            TemplateScope scope = scopeNode.getScope();
            for (JIPipeNodeDatabaseEntry entry : entries) {
                if (entry instanceof CreateNewNodesByTemplateDatabaseEntry template
                        && template.getScope() == scope) {
                    functionNode.add(new DefaultMutableTreeNode(entry));
                }
            }
        }
        // Forward to the function children (the taxonomy side) ...
        for (int i = 0; i < functionNode.getChildCount(); i++) {
            Object child = functionNode.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction) {
                attachNodeLeaves(mtn, entries);
            }
        }
        // ... and to the scope children of the Templates header (the scope
        // side). Scope rows carry no further function/scope children. The
        // tree ROOT (a String user object) also carries the Templates header
        // as a direct child - forward there too, or the scope rows never
        // receive their leaves (the round-2 review bug: the root only ran
        // the function forwarding, so Templates/System was empty in every
        // tier).
        if (userObject instanceof TemplateScopeTreeNode
                || userObject instanceof String) {
            for (int i = 0; i < functionNode.getChildCount(); i++) {
                Object child = functionNode.getChildAt(i);
                if (child instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof TemplateScopeTreeNode) {
                    attachNodeLeaves(mtn, entries);
                }
            }
        }
    }

    /**
     * The {@link TreePath} to the first node leaf (depth-first, in tree
     * order) that carries a database entry, or {@code null} when the tree
     * has none (the compact single-pane's "first hit").
     */
    public TreePath findFirstNodeLeafPath() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        TreePath rootPath = new TreePath(new Object[]{root});
        for (int i = 0; i < root.getChildCount(); i++) {
            Object child = root.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode mtn) {
                TreePath found = findFirstNodeLeafPath(mtn, rootPath);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private TreePath findFirstNodeLeafPath(DefaultMutableTreeNode node, TreePath parentPath) {
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode mtn) {
                TreePath path = parentPath.pathByAddingChild(mtn);
                if (mtn.getUserObject() instanceof JIPipeNodeDatabaseEntry) {
                    return path;
                }
                // Recurse into function branches, the synthetic scope
                // branches (templates attached under Templates/<scope>) and
                // the synthetic <function>/Patterns rows.
                if (mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction
                        || mtn.getUserObject() instanceof TemplateScopeTreeNode
                        || mtn.getUserObject() instanceof PatternsTreeNode) {
                    TreePath found = findFirstNodeLeafPath(mtn, path);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    /**
     * The database entry carried by the last node of the given tree path, or
     * {@code null} when the path is empty or ends on a function/root row
     * (those are not insertable nodes).
     */
    public JIPipeNodeDatabaseEntry entryFromPath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeDatabaseEntry entry) {
            return entry;
        }
        return null;
    }

    /**
     * The taxonomy function carried by the last node of the given tree path,
     * or {@code null} when the path is empty or ends on a node-leaf/root row.
     */
    public JIPipeNodeTaxonomyFunction functionFromTreePath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last instanceof DefaultMutableTreeNode mtn && mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction fn) {
            return fn;
        }
        return null;
    }

    /**
     * The template scope carried by the last node of the given tree path, or
     * {@code null} when the path is empty or ends on a row that is not a
     * non-root synthetic {@code Templates / <scope>} row (i.e. a function,
     * node-leaf, root, or the "Templates" header row).
     */
    public TemplateScope scopeFromTreePath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last instanceof DefaultMutableTreeNode mtn
                && mtn.getUserObject() instanceof TemplateScopeTreeNode scopeNode
                && !scopeNode.isRoot()) {
            return scopeNode.getScope();
        }
        return null;
    }

    /**
     * The taxonomy function a synthetic {@code <function>/Patterns} row
     * groups, or {@code null} when the path is empty or does not end on such
     * a row. The row's own {@link PatternsTreeNode#getFunction()} carries the
     * answer directly (the row is created per function), so no ancestor walk
     * is needed.
     */
    public JIPipeNodeTaxonomyFunction functionOfPatternsPath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last instanceof DefaultMutableTreeNode mtn
                && mtn.getUserObject() instanceof PatternsTreeNode patterns) {
            return patterns.getFunction();
        }
        return null;
    }

    /**
     * The {@link TreePath} of the tree row carrying the given function, or
     * {@code null} when the function is not part of the visible tree.
     */
    public TreePath functionRow(JIPipeNodeTaxonomyFunction function) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        return findFunctionRow(root, new TreePath(new Object[]{root}), function);
    }

    private TreePath findFunctionRow(DefaultMutableTreeNode node, TreePath parentPath,
                                     JIPipeNodeTaxonomyFunction function) {
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            if (!(child instanceof DefaultMutableTreeNode mtn)) {
                continue;
            }
            TreePath path = parentPath.pathByAddingChild(mtn);
            if (mtn.getUserObject() == function) {
                return path;
            }
            if (mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction) {
                TreePath found = findFunctionRow(mtn, path, function);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Expands the given path's ancestors (so the row becomes visible) and
     * returns the row index, or {@code -1} if the path does not exist.
     */
    public int expandAndReturnRow(TreePath path) {
        Object[] components = path.getPath();
        for (int i = 1; i < components.length; i++) {
            tree.expandPath(new TreePath(Arrays.copyOfRange(components, 0, i)));
        }
        return tree.getRowForPath(path);
    }

    /**
     * Selects the given tree row, making it visible.
     */
    public void selectRow(TreePath path) {
        int row = expandAndReturnRow(path);
        if (row >= 0) {
            tree.setSelectionRow(row);
            tree.makeVisible(path);
        }
    }

    /**
     * Collapses every expanded function branch, leaving only the "All
     * functions" root open - the tree's default depth. Call sites wrap the
     * call in their selection guard (the collapse itself does not change
     * the selection, the guard protects the restored root expansion from
     * any listener side effect).
     */
    public void collapseToRoot() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        TreePath rootPath = new TreePath(new Object[]{root});
        // Collect the expanded paths first - collapsing changes the row
        // count, so iterating the rows while collapsing is not safe.
        List<TreePath> expandedPaths = new ArrayList<>();
        for (int i = 0; i < tree.getRowCount(); i++) {
            TreePath p = tree.getPathForRow(i);
            if (p != null && tree.isExpanded(p) && p.getPath().length > 1) {
                expandedPaths.add(p);
            }
        }
        for (TreePath p : expandedPaths) {
            tree.collapsePath(p);
        }
        tree.expandPath(rootPath);
        tree.scrollPathToVisible(rootPath);
    }

    /**
     * The crosshairs action: reveals the reference entry (see
     * {@link #setReferenceEntrySupplier}) in the tree. When the tree carries
     * node leaves (the browser's compact single-pane) the entry's own leaf
     * is selected; otherwise the entry's primary function row is selected
     * (the finder's functions-only tree, and the browser's two-pane).
     */
    public void selectReferenceNodeInTree() {
        JIPipeNodeDatabaseEntry reference = referenceEntrySupplier == null ? null : referenceEntrySupplier.get();
        if (reference == null) {
            return;
        }
        TreePath target = null;
        if (hasNodeLeaves()) {
            // Node leaves are present: the reference entry may have its own
            // leaf (it is in the current result view). Find it.
            target = nodeLeafRow(reference);
            if (target == null) {
                // The reference is not in the current view - fall back to
                // its primary function row.
                JIPipeNodeTaxonomyFunction primary = primaryFunctionOf(reference);
                target = primary != null ? functionRow(primary) : null;
            }
        } else {
            JIPipeNodeTaxonomyFunction primary = primaryFunctionOf(reference);
            target = primary != null ? functionRow(primary) : null;
        }
        if (target == null) {
            return;
        }
        selectRow(target);
    }

    /**
     * The {@link TreePath} of the tree row carrying the given entry's node
     * leaf, or {@code null} when the entry has no leaf in the current tree
     * (no leaves attached, or the entry is not in the attached set).
     */
    public TreePath nodeLeafRow(JIPipeNodeDatabaseEntry entry) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        return findNodeLeafRow(root, new TreePath(new Object[]{root}), entry);
    }

    private TreePath findNodeLeafRow(DefaultMutableTreeNode node, TreePath parentPath, JIPipeNodeDatabaseEntry entry) {
        for (int i = 0; i < node.getChildCount(); i++) {
            Object child = node.getChildAt(i);
            if (!(child instanceof DefaultMutableTreeNode mtn)) {
                continue;
            }
            TreePath path = parentPath.pathByAddingChild(mtn);
            if (mtn.getUserObject() == entry) {
                return path;
            }
            // Recurse into function branches, the synthetic scope branches
            // (templates attached under Templates/<scope>) and the synthetic
            // <function>/Patterns rows.
            if (mtn.getUserObject() instanceof JIPipeNodeTaxonomyFunction
                    || mtn.getUserObject() instanceof TemplateScopeTreeNode
                    || mtn.getUserObject() instanceof PatternsTreeNode) {
                TreePath found = findNodeLeafRow(mtn, path, entry);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Search highlighting
    // ------------------------------------------------------------------

    /**
     * Highlights (in bold) the function rows that a search matched - i.e.
     * the rows under which at least one of the given result entries is
     * filed - and expands their ancestors so the matches are visible even
     * when their branches were collapsed. An empty or inactive search
     * clears the highlight.
     *
     * @param searchActive whether a search is currently active
     * @param results      the current search results
     */
    public void updateSearchHighlights(boolean searchActive, List<JIPipeNodeDatabaseEntry> results) {
        if (!searchActive || results == null || results.isEmpty()) {
            if (!searchHighlightedLeafPaths.isEmpty()) {
                searchHighlightedLeafPaths = Set.of();
                tree.repaint();
            }
            return;
        }
        Set<String> matchedFunctionIds = new HashSet<>();
        for (JIPipeNodeDatabaseEntry entry : results) {
            matchedFunctionIds.addAll(entry.getFunctionIds());
        }
        Set<TreePath> highlighted = new HashSet<>();
        markMatchedRows((DefaultMutableTreeNode) treeModel.getRoot(), matchedFunctionIds, highlighted);
        searchHighlightedLeafPaths = highlighted;
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
                // nodes. Plain branches (no direct entries) are not bolded -
                // that would highlight nearly every branch for any search.
                if (matchedFunctionIds.contains(fn.getId())) {
                    highlighted.add(new TreePath(mtn.getPath()));
                }
                markMatchedRows(mtn, matchedFunctionIds, highlighted);
            }
        }
    }
}
