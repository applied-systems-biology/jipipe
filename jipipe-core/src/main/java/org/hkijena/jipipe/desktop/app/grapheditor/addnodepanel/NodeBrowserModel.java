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

import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * <li><b>search text</b> — a non-empty search switches the browser into
 * flat-results mode ({@link #isSearchActive()}), overriding the tree;</li>
 * <li><b>the taxonomy facets</b> (framework / spatial / input / tags) —
 * the shared filter predicate {@link #matches(JIPipeNodeDatabaseEntry)};</li>
 * <li><b>the tree path and view toggles</b> (selected function path,
 * pinned-only, selection-only).</li>
 * </ul>
 */
public class NodeBrowserModel {

    /** The compact tier: a single tree list of node leaves. */
    public static final int TIER_COMPACT = 0;

    /** The comfortable tier: tree list + results list side by side. */
    public static final int TIER_COMFORTABLE = 1;

    /** Width below which the compact tier is used (exclusive). */
    public static final int COMPACT_TIER_MAX_WIDTH = 380;

    private String search = "";
    private JIPipeNodeTaxonomyFramework framework;
    private Set<JIPipeNodeTaxonomySpatialDimension> spatial = Set.of();
    private Set<JIPipeNodeTaxonomyInputProfile> input = Set.of();
    private Set<String> tags = Set.of();
    private List<JIPipeNodeTaxonomyFunction> treePath = List.of();
    private boolean pinnedOnly;
    private boolean selectionOnly;

    public NodeBrowserModel() {
    }

    // ------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------

    public String getSearch() {
        return search;
    }

    /**
     * @param search the search text; {@code null} is treated as empty
     */
    public void setSearch(@Nullable String search) {
        this.search = search == null ? "" : search;
    }

    /**
     * Whether a search is active: the trimmed search text is non-empty.
     * An active search puts the browser into flat-results mode, overriding
     * the function tree.
     */
    public boolean isSearchActive() {
        return search != null && !search.trim().isEmpty();
    }

    // ------------------------------------------------------------------
    // Taxonomy facets
    // ------------------------------------------------------------------

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
     * the framework filter (does not affect the other facets)
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
     * {@code null}) set clears the spatial filter without affecting the other
     * facets
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
     * set clears the input filter without affecting the other facets
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
     * clears the tag filter without affecting the other facets
     */
    public void setTags(@Nullable Set<String> tags) {
        this.tags = Collections.unmodifiableSet(tags == null ? Set.of() : new LinkedHashSet<>(tags));
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
        return true;
    }

    // ------------------------------------------------------------------
    // Tree path + view toggles
    // ------------------------------------------------------------------

    /**
     * @return the currently selected function tree path (root → leaf), empty
     * when nothing is selected
     */
    public List<JIPipeNodeTaxonomyFunction> getTreePath() {
        return treePath;
    }

    /**
     * @param treePath the selected function path (a single leaf, or a chain
     * ending at a leaf); an empty (or {@code null}) list clears the selection
     */
    public void setTreePath(@Nullable List<JIPipeNodeTaxonomyFunction> treePath) {
        this.treePath = Collections.unmodifiableList(treePath == null ? List.of() : new ArrayList<>(treePath));
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

    // ------------------------------------------------------------------
    // Tier
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Tree helpers
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
}
