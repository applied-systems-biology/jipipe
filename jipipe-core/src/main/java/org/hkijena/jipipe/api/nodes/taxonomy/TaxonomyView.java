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

package org.hkijena.jipipe.api.nodes.taxonomy;

import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure transition view over a node's taxonomy, built for the
 * node-taxonomy conversion (Phase 1b).
 * <p>
 * After the 1b conversions every converted node's
 * {@link JIPipeNodeInfo#getCategory() category} resolves to an
 * {@code InternalNodeTypeCategory}, so legacy-reading consumers would see
 * the generic "Internal" hue/flags/name for all of them. This class is the
 * <em>dual read</em>: every accessor prefers the node's
 * {@link JIPipeNodeInfo#getPrimaryFunction() primary taxonomy function} when
 * present (a converted node) and otherwise falls back to the legacy category
 * byte-identically (an unconverted node). It is a set of static, stateless
 * methods over the {@link JIPipeNodeInfo} — no {@code JIPipe} instance or
 * registry is required, because the function objects carry their own parent
 * chain and behavior flags.
 * <p>
 * All methods are null-safe: a missing category, a missing menu path, or an
 * empty secondary-function set never throws.
 */
public final class TaxonomyView {

    private TaxonomyView() {
    }

    /**
     * The four behavior flags that gate where and how a node appears and
     * can be manipulated, read from either the primary taxonomy function
     * (converted) or the legacy category (unconverted).
     *
     * @param isVisibleInPipeline    whether the node shows in the pipeline editor
     * @param isVisibleInCompartments whether the node shows in the compartment editor
     * @param canExtract             whether the node can be extracted (copied) from a graph
     * @param isRunnable             whether the node can be run
     */
    public record BehaviorFlags(boolean isVisibleInPipeline,
                                boolean isVisibleInCompartments,
                                boolean canExtract,
                                boolean isRunnable) {
    }

    /**
     * The display name for the node's function/category.
     * <p>
     * Converted: the primary function's name. Unconverted: the legacy
     * category's name, or {@code "Internal"} if the category is null.
     *
     * @param info the node info
     * @return the display name, never {@code null}
     */
    public static String functionDisplayName(JIPipeNodeInfo info) {
        JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
        if (primary != null) {
            return primary.getName();
        }
        JIPipeNodeTypeCategory category = info.getCategory();
        return category != null ? category.getName() : "Internal";
    }

    /**
     * The human-readable location path of the node.
     * <p>
     * Converted: the function chain from the root down to the primary
     * function, joined by {@code " > "}, followed by the node name — e.g.
     * {@code "Images > Filtering > Gaussian Blur"}.
     * Unconverted: the legacy shape — the category name, plus the legacy
     * menu path (each {@code \n}-separated entry joined by {@code " > "})
     * when it is non-empty, followed by the node name.
     *
     * @param info the node info
     * @return the location path, never {@code null}
     */
    public static String locationPath(JIPipeNodeInfo info) {
        JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
        if (primary != null) {
            List<String> names = functionChainNames(primary);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    sb.append(" > ");
                }
                sb.append(names.get(i));
            }
            sb.append(" > ").append(info.getName());
            return sb.toString();
        }

        // Legacy shape.
        StringBuilder sb = new StringBuilder();
        JIPipeNodeTypeCategory category = info.getCategory();
        sb.append(category != null ? category.getName() : "Internal");
        String menuPath = StringUtils.getCleanedMenuPath(info.getMenuPath());
        if (!menuPath.isEmpty()) {
            for (String entry : menuPath.split("\n")) {
                sb.append(" > ").append(entry.trim());
            }
        }
        sb.append(" > ").append(info.getName());
        return sb.toString();
    }

    /**
     * The four behavior flags of the node, read from the primary function
     * when present, else from the legacy category, else the safe defaults.
     * <p>
     * The safe defaults for a null category mirror the
     * {@link JIPipeNodeTypeCategory} interface defaults: {@code canExtract()}
     * and {@code isRunnable()} are both {@code true} in the interface; the two
     * visibility flags are abstract in the interface (no default) and default
     * here to the permissive {@code true}.
     *
     * @param info the node info
     * @return the resolved behavior flags, never {@code null}
     */
    public static BehaviorFlags behaviorFlags(JIPipeNodeInfo info) {
        JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
        if (primary != null) {
            return new BehaviorFlags(
                    primary.isVisibleInPipeline(),
                    primary.isVisibleInCompartments(),
                    primary.canExtract(),
                    primary.isRunnable());
        }
        JIPipeNodeTypeCategory category = info.getCategory();
        if (category != null) {
            return new BehaviorFlags(
                    category.isVisibleInPipeline(),
                    category.isVisibleInCompartments(),
                    category.canExtract(),
                    category.isRunnable());
        }
        // Null category: safe defaults mirroring the interface defaults
        // (canExtract/isRunnable true) + permissive visibility (true).
        return new BehaviorFlags(true, true, true, true);
    }

    /**
     * The search ids for the node's function/category, in the exact format
     * the node database uses.
     * <p>
     * Converted: the primary function's id plus every secondary function's id.
     * Unconverted: the legacy category id, in exactly the form the node
     * database builder ({@code org.hkijena.jipipe.api.nodes.database.
     * JIPipeNodeDatabaseBuilderRun}) puts into an entry's
     * {@code categoryIds} ({@code nodeInfo.getCategory().getId()}).
     *
     * @param info the node info
     * @return the search ids, never {@code null}
     */
    public static Set<String> searchIds(JIPipeNodeInfo info) {
        Set<String> ids = new LinkedHashSet<>();
        JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
        if (primary != null) {
            ids.add(primary.getId());
            Set<JIPipeNodeTaxonomyFunction> secondary = info.getSecondaryFunctions();
            if (secondary != null) {
                for (JIPipeNodeTaxonomyFunction fn : secondary) {
                    ids.add(fn.getId());
                }
            }
            return ids;
        }

        // Legacy shape: exactly the category id the database builder uses.
        JIPipeNodeTypeCategory category = info.getCategory();
        if (category != null) {
            ids.add(category.getId());
        }
        return ids;
    }

    /**
     * Collects the function chain names from the root down to the given
     * leaf function, in display order. Guards against a cyclic parent chain
     * with a visited set so it always terminates.
     *
     * @param leaf the leaf (primary) function
     * @return root-first list of names; never empty
     */
    private static List<String> functionChainNames(JIPipeNodeTaxonomyFunction leaf) {
        List<String> leafFirst = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        JIPipeNodeTaxonomyFunction current = leaf;
        while (current != null) {
            if (!visited.add(current.getId())) {
                // Cyclic chain: stop to avoid an infinite loop.
                break;
            }
            leafFirst.add(current.getName());
            current = current.getParent();
        }
        // Reverse to root-first display order.
        List<String> result = new ArrayList<>(leafFirst.size());
        for (int i = leafFirst.size() - 1; i >= 0; i--) {
            result.add(leafFirst.get(i));
        }
        return result;
    }
}
