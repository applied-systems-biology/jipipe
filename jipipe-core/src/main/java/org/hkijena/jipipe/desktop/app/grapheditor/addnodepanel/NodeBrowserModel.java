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

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabasePipelineVisibility;
import org.hkijena.jipipe.api.nodes.database.entries.CreateNewNodesByTemplateDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.entries.NodeEntryKind;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.nodes.taxonomy.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The non-Swing state of the adaptive node browser (Task 16).
 * <p>
 * This class is deliberately free of any Swing and {@code JIPipe} instance
 * dependency so it can be unit-tested in isolation (see
 * {@link NodeBrowserModelTest}). The {@link JIPipeDesktopNodeBrowser}
 * component owns a model instance and re-renders whenever one of its
 * facets changes.
 * <p>
 * The state splits into three independent groups, none of which resets
 * the others:
 * <ul>
 * <li><b>search text</b> — a non-empty search ({@link #isSearchActive()})
 * re-queries the list and highlights the matching tree leafs (the layout
 * itself never changes: tree + list are always shown);</li>
 * <li><b>the taxonomy facets</b> (framework / input+output data types /
 * tags) — the shared filter predicate {@link #matches(JIPipeNodeDatabaseEntry)};</li>
 * <li><b>the tree path and view toggles</b> (selected function path —
 * mutually exclusive with a selected template scope — pinned-only,
 * selection-only).</li>
 * </ul>
 */
public class NodeBrowserModel {

    /**
     * The compact tier: a single tree list of node leaves.
     */
    public static final int TIER_COMPACT = 0;

    /**
     * The comfortable tier: tree list + results list side by side.
     */
    public static final int TIER_COMFORTABLE = 1;

    /**
     * Width below which the compact tier is used (exclusive).
     */
    public static final int COMPACT_TIER_MAX_WIDTH = 380;

    private String search = "";
    private JIPipeNodeTaxonomyFramework framework;
    private Set<JIPipeNodeTaxonomySpatialDimension> spatial = Set.of();
    private Set<JIPipeNodeTaxonomyInputProfile> input = Set.of();
    private Set<String> tags = Set.of();
    /**
     * Selected input data types (by class); empty = no input-data-type filter.
     */
    private Set<Class<? extends JIPipeData>> inputDataTypes = Set.of();
    /**
     * Selected output data types (by class); empty = no output-data-type filter.
     */
    private Set<Class<? extends JIPipeData>> outputDataTypes = Set.of();
    private List<JIPipeNodeTaxonomyFunction> treePath = List.of();
    /**
     * The selected template scope of the synthetic {@code Templates /
     * <scope>} branch (spec §7.2). MUTUALLY EXCLUSIVE with {@link #treePath}
     * (setting one clears the other). {@code null} = no scope selected.
     */
    @Nullable
    private TemplateScope selectedScope;
    /**
     * The taxonomy function whose synthetic {@code <function>/Patterns} row
     * is selected (the per-function template grouping — the row lists the
     * template entries filed under that function). MUTUALLY EXCLUSIVE with
     * {@link #treePath} and {@link #selectedScope} (setting one clears the
     * other two). {@code null} = no Patterns row selected.
     */
    @Nullable
    private JIPipeNodeTaxonomyFunction patternsFunction;
    private boolean pinnedOnly;
    private boolean selectionOnly;
    /**
     * The selected result kinds (1340): plain nodes, node examples, and the
     * template kinds split by pattern/custom badge and system/user origin.
     * An empty set means "no kind filter" (show every entry kind). Entries
     * without a kind (existing nodes / compartments, new compartments)
     * always pass (see {@link #matchesKindFilter}).
     */
    private Set<NodeEntryKind> kindFilter = Set.of();

    public NodeBrowserModel() {
    }

    // ------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------

    /**
     * The ids of the given leaf and all of its descendants in the function
     * tree. A results row matches a selected leaf when the entry's
     * {@link JIPipeNodeDatabaseEntry#getFunctionIds() function ids} contain
     * one of these ids (an entry stores the leaf ids it appears under, so
     * descendants of the selected leaf are included as well).
     *
     * @param registry the function registry to walk
     * @param leaf     the selected leaf (or an intermediate node); {@code null}
     *                 yields an empty set
     * @return an unmodifiable set of function ids, always containing the leaf
     * itself
     */
    public static Set<String> descendantIds(@Nullable FunctionRegistry registry, @Nullable JIPipeNodeTaxonomyFunction leaf) {
        Set<String> ids = new LinkedHashSet<>();
        if (registry == null || leaf == null) {
            return Collections.unmodifiableSet(ids);
        }
        Deque<JIPipeNodeTaxonomyFunction> stack = new ArrayDeque<>();
        stack.push(leaf);
        while (!stack.isEmpty()) {
            JIPipeNodeTaxonomyFunction current = stack.pop();
            if (!ids.add(current.getId())) {
                continue;
            }
            for (JIPipeNodeTaxonomyFunction child : registry.children(current)) {
                stack.push(child);
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    /**
     * The ids of every taxonomy function under which at least one of the
     * given node infos has a node — used to prune the function tree so it
     * never shows empty categories (e.g. a plot sub-leaf no node is
     * assigned to, or an entire branch with no nodes).
     * <p>
     * Node infos (rather than node database entries) are the authoritative
     * source here: the database builds its entries asynchronously and may
     * still be empty when a tree is first rendered, while the registered
     * infos are complete as soon as the plugins are registered. A node is
     * counted only when it is visible for the given database role, so a
     * function whose only nodes are hidden in this editor is pruned too —
     * matching what the browser's query would show under it.
     * <p>
     * For each counted node the primary <em>and</em> all secondary function
     * ids are added, mirroring {@link JIPipeNodeDatabaseEntry#getFunctionIds()}
     * (a node appears under every function it declares).
     *
     * @param infos the node infos to inspect; {@code null} yields an empty
     *              set (the caller then keeps the full skeleton)
     * @param role  the visibility role to filter by ({@code Both} counts
     *              every node)
     * @return the populated function ids (possibly empty)
     */
    public static Set<String> populatedFunctionIds(@Nullable Iterable<? extends JIPipeNodeInfo> infos,
                                                   JIPipeNodeDatabasePipelineVisibility role) {
        Set<String> ids = new LinkedHashSet<>();
        if (infos == null) {
            return Collections.unmodifiableSet(ids);
        }
        for (JIPipeNodeInfo info : infos) {
            if (info == null) {
                continue;
            }
            JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
            if (primary == null) {
                continue; // unconverted / fn-null: never appears in the tree
            }
            if (role == JIPipeNodeDatabasePipelineVisibility.Pipeline && !primary.isVisibleInPipeline()) {
                continue;
            }
            if (role == JIPipeNodeDatabasePipelineVisibility.Compartments && !primary.isVisibleInCompartments()) {
                continue;
            }
            ids.add(primary.getId());
            for (JIPipeNodeTaxonomyFunction secondary : info.getSecondaryFunctions()) {
                if (secondary != null) {
                    ids.add(secondary.getId());
                }
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    public String getSearch() {
        return search;
    }

    // ------------------------------------------------------------------
    // Taxonomy facets
    // ------------------------------------------------------------------

    /**
     * @param search the search text; {@code null} is treated as empty
     */
    public void setSearch(@Nullable String search) {
        this.search = search == null ? "" : search;
    }

    /**
     * Whether a search is active: the trimmed search text is non-empty.
     * An active search re-queries the results list and highlights the
     * matching function tree leafs (the tree itself stays visible).
     */
    public boolean isSearchActive() {
        return search != null && !search.trim().isEmpty();
    }

    /**
     * @return the selected framework filter, or {@code null} for "no
     * framework filter"
     */
    @Nullable
    public JIPipeNodeTaxonomyFramework getFramework() {
        return framework;
    }

    /**
     * @param framework the framework to filter by, or {@code null} to clear
     *                  the framework filter (does not affect the other facets)
     */
    public void setFramework(@Nullable JIPipeNodeTaxonomyFramework framework) {
        this.framework = framework;
    }

    /**
     * @return the selected spatial dimensions; empty means "no spatial filter"
     */
    public Set<JIPipeNodeTaxonomySpatialDimension> getSpatial() {
        return spatial;
    }

    /**
     * @param spatial the spatial dimensions to filter by; an empty (or
     *                {@code null}) set clears the spatial filter without affecting the other
     *                facets
     */
    public void setSpatial(@Nullable Set<JIPipeNodeTaxonomySpatialDimension> spatial) {
        this.spatial = Collections.unmodifiableSet(spatial == null ? Set.of() : new LinkedHashSet<>(spatial));
    }

    /**
     * @return the selected input profiles; empty means "no input filter"
     */
    public Set<JIPipeNodeTaxonomyInputProfile> getInput() {
        return input;
    }

    /**
     * @param input the input profiles to filter by; an empty (or {@code null})
     *              set clears the input filter without affecting the other facets
     */
    public void setInput(@Nullable Set<JIPipeNodeTaxonomyInputProfile> input) {
        this.input = Collections.unmodifiableSet(input == null ? Set.of() : new LinkedHashSet<>(input));
    }

    /**
     * @return the selected tags; empty means "no tag filter"
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to filter by; an empty (or {@code null}) set
     *             clears the tag filter without affecting the other facets
     */
    public void setTags(@Nullable Set<String> tags) {
        this.tags = Collections.unmodifiableSet(tags == null ? Set.of() : new LinkedHashSet<>(tags));
    }

    /**
     * The input data types selected in the data-type facet; empty means
     * "no input data type filter" (the entry passes unconditionally).
     *
     * @return the selected input data type classes
     */
    public Set<Class<? extends JIPipeData>> getInputDataTypes() {
        return inputDataTypes;
    }

    /**
     * @param inputDataTypes the input data types to filter by; an entry
     *                       passes when at least one of its input slots has
     *                       one of the selected types. An empty (or
     *                       {@code null}) set clears the filter.
     */
    public void setInputDataTypes(@Nullable Set<Class<? extends JIPipeData>> inputDataTypes) {
        this.inputDataTypes = Collections.unmodifiableSet(
                inputDataTypes == null ? Set.of() : new LinkedHashSet<>(inputDataTypes));
    }

    /**
     * The output data types selected in the data-type facet; empty means
     * "no output data type filter" (the entry passes unconditionally).
     *
     * @return the selected output data type classes
     */
    public Set<Class<? extends JIPipeData>> getOutputDataTypes() {
        return outputDataTypes;
    }

    // ------------------------------------------------------------------
    // Tree path + view toggles
    // ------------------------------------------------------------------

    /**
     * @param outputDataTypes the output data types to filter by; an entry
     *                        passes when at least one of its output slots
     *                        has one of the selected types. An empty (or
     *                        {@code null}) set clears the filter.
     */
    public void setOutputDataTypes(@Nullable Set<Class<? extends JIPipeData>> outputDataTypes) {
        this.outputDataTypes = Collections.unmodifiableSet(
                outputDataTypes == null ? Set.of() : new LinkedHashSet<>(outputDataTypes));
    }

    /**
     * The shared facet predicate: whether the given entry matches the
     * currently selected framework / spatial / input / tags facets.
     * <p>
     * An empty facet set (or a {@code null} framework) means "no filter" on
     * that dimension. This is the same relation the node database's facet
     * query post-filter applies, so the tree/list render and the database
     * query agree on what "matches" means.
     *
     * @param entry the entry to test; must not be {@code null}
     * @return {@code true} if the entry satisfies every active facet
     */
    public boolean matches(JIPipeNodeDatabaseEntry entry) {
        if (framework != null && !framework.getId().equals(entry.getFramework().getId())) {
            return false;
        }
        if (!spatial.isEmpty() && Collections.disjoint(spatial, entry.getSpatialDimensions())) {
            return false;
        }
        if (!input.isEmpty() && !input.contains(entry.getInputProfile())) {
            return false;
        }
        Set<String> entryTags = entry.getTags();
        if (!tags.isEmpty() && Collections.disjoint(tags, entryTags == null ? Set.of() : entryTags)) {
            return false;
        }
        // Data-type facets: the entry passes the input filter iff at least
        // one of its input slots produces one of the selected types (a
        // slot's own type or a convertible type counts — the slot's data
        // class IS what the node accepts), and symmetrically for outputs.
        // An empty selection disables the respective check.
        if (!inputDataTypes.isEmpty()) {
            boolean passes = false;
            for (var slot : entry.getInputSlots().values()) {
                if (inputDataTypes.contains(slot.getDataClass())) {
                    passes = true;
                    break;
                }
            }
            if (!passes) {
                return false;
            }
        }
        if (!outputDataTypes.isEmpty()) {
            boolean passes = false;
            for (var slot : entry.getOutputSlots().values()) {
                if (outputDataTypes.contains(slot.getDataClass())) {
                    passes = true;
                    break;
                }
            }
            if (!passes) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the currently selected function tree path (root → leaf), empty
     * when nothing is selected
     */
    public List<JIPipeNodeTaxonomyFunction> getTreePath() {
        return treePath;
    }

    /**
     * @param treePath the selected function path (a single leaf, or a chain
     *                 ending at a leaf); an empty (or {@code null}) list clears
     *                 the selection
     */
    public void setTreePath(@Nullable List<JIPipeNodeTaxonomyFunction> treePath) {
        this.treePath = Collections.unmodifiableList(treePath == null ? List.of() : new ArrayList<>(treePath));
        // A function path, a template scope, and a <function>/Patterns row are
        // mutually exclusive selections (spec §7.2 + the Patterns grouping):
        // choosing one cancels the others.
        this.selectedScope = null;
        this.patternsFunction = null;
    }

    /**
     * @return the selected template scope of the synthetic
     * {@code Templates / <scope>} branch, or {@code null} when a
     * function path (or the root) is selected instead
     */
    @Nullable
    public TemplateScope getSelectedScope() {
        return selectedScope;
    }

    /**
     * @param selectedScope the template scope to select in the synthetic
     *                      branch; {@code null} clears the scope selection.
     *                      Mutually exclusive with {@link #setTreePath} and
     *                      {@link #setPatternsFunction}: selecting a scope
     *                      clears the function path and the Patterns row.
     */
    public void setSelectedScope(@Nullable TemplateScope selectedScope) {
        this.selectedScope = selectedScope;
        if (selectedScope != null) {
            this.treePath = List.of();
            this.patternsFunction = null;
        }
    }

    /**
     * @return the taxonomy function whose {@code <function>/Patterns} row is
     * selected, or {@code null} when a function path, a template
     * scope, or the root is selected instead
     */
    @Nullable
    public JIPipeNodeTaxonomyFunction getPatternsFunction() {
        return patternsFunction;
    }

    /**
     * @param patternsFunction the function whose {@code <function>/Patterns}
     *                         row is selected (the list then shows the
     *                         template entries filed under exactly that
     *                         function); {@code null} clears the selection.
     *                         Mutually exclusive with {@link #setTreePath}
     *                         and {@link #setSelectedScope}.
     */
    public void setPatternsFunction(@Nullable JIPipeNodeTaxonomyFunction patternsFunction) {
        this.patternsFunction = patternsFunction;
        if (patternsFunction != null) {
            this.treePath = List.of();
            this.selectedScope = null;
        }
    }

    /**
     * The shared SELECTION predicate with an explicitly supplied function
     * registry (spec §7.2): whether the given entry belongs in the results
     * list given the current tree selection — a template scope, a
     * {@code <function>/Patterns} row, else a function tree path, else the
     * root (all entries).
     * <p>
     * This is the single non-Swing, {@code JIPipe}-instance-free predicate
     * both node-browser surfaces (the docked
     * {@link JIPipeDesktopNodeBrowser} and the
     * {@code JIPipeDesktopNodeFinderDialogUI}) call after their query, so
     * their list filters agree. A template scope selection lists only
     * template entries of that scope; a {@code <function>/Patterns}
     * selection lists only the template entries filed under that exact
     * function; a function-path selection lists every entry (node or
     * template) filed under the selected leaf's subtree; the root lists
     * everything.
     *
     * @param entry    the entry to test; must not be {@code null}
     * @param registry the function registry used to resolve the function-path
     *                 selection to a descendant-id set ({@code null} disables
     *                 the function side, so only a scope or the root applies)
     * @return {@code true} if the entry belongs in the list for the current
     * selection
     */
    public boolean matchesSelection(JIPipeNodeDatabaseEntry entry, FunctionRegistry registry) {
        if (selectedScope != null) {
            return entry instanceof CreateNewNodesByTemplateDatabaseEntry template
                    && template.getScope() == selectedScope;
        }
        if (patternsFunction != null) {
            // The <function>/Patterns row: only the template entries filed
            // under that exact function (the row's own attach rule).
            return entry instanceof CreateNewNodesByTemplateDatabaseEntry
                    && entry.getFunctionIds().contains(patternsFunction.getId());
        }
        if (!treePath.isEmpty()) {
            JIPipeNodeTaxonomyFunction leaf = treePath.get(treePath.size() - 1);
            Set<String> ids = descendantIds(registry, leaf);
            for (String functionId : entry.getFunctionIds()) {
                if (ids.contains(functionId)) {
                    return true;
                }
            }
            return false;
        }
        // Root: every entry passes the selection (de-dup is inherent to the
        // single query-driven list, so a dually-placed template is listed once).
        return true;
    }

    public boolean isPinnedOnly() {
        return pinnedOnly;
    }

    public void setPinnedOnly(boolean pinnedOnly) {
        this.pinnedOnly = pinnedOnly;
    }

    /**
     * The ⊂ "selection-only" toggle: when set, only candidates whose input
     * slots can accept the output of a currently-selected canvas node are
     * shown (see {@link JIPipeDesktopNodeBrowser} for the connection-
     * compatibility rule).
     */
    public boolean isSelectionOnly() {
        return selectionOnly;
    }

    public void setSelectionOnly(boolean selectionOnly) {
        this.selectionOnly = selectionOnly;
    }

    /**
     * @return the selected result kinds (1340); empty means "no kind
     * filter" (every entry kind passes)
     */
    public Set<NodeEntryKind> getKindFilter() {
        return kindFilter;
    }

    // ------------------------------------------------------------------
    // Tier
    // ------------------------------------------------------------------

    /**
     * @param kindFilter the result kinds to show; an empty (or {@code null})
     *                   set clears the filter. Does not affect the other
     *                   facets or toggles.
     */
    public void setKindFilter(@Nullable Set<NodeEntryKind> kindFilter) {
        this.kindFilter = Collections.unmodifiableSet(kindFilter == null ? Set.of() : new LinkedHashSet<>(kindFilter));
    }

    // ------------------------------------------------------------------
    // Tree helpers
    // ------------------------------------------------------------------

    /**
     * The shared kind-filter predicate (1340): whether the given entry
     * passes the currently selected result kinds. Delegates to
     * {@link NodeEntryKind#matches(JIPipeNodeDatabaseEntry, Set)} — entries
     * without a kind (existing nodes / compartments, new compartments)
     * always pass, and an empty selection matches everything. Both node
     * browser surfaces (the docked {@link JIPipeDesktopNodeBrowser} and the
     * {@code JIPipeDesktopNodeFinderDialogUI}) apply this so they agree.
     *
     * @param entry the entry to test; must not be {@code null}
     * @return {@code true} if the entry passes the kind filter
     */
    public boolean matchesKindFilter(JIPipeNodeDatabaseEntry entry) {
        return NodeEntryKind.matches(entry, kindFilter);
    }

    /**
     * The layout tier for a given component width: a pure function of the
     * width, so it can be recomputed on every resize.
     *
     * @param widthPx the component width in pixels
     * @return {@link #TIER_COMPACT} when {@code widthPx < 380},
     * {@link #TIER_COMFORTABLE} otherwise
     */
    public int getTier(int widthPx) {
        return widthPx < COMPACT_TIER_MAX_WIDTH ? TIER_COMPACT : TIER_COMFORTABLE;
    }
}
