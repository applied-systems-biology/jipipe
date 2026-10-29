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
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DataSourcesFunction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure view over a node's taxonomy, built for the
 * node-taxonomy conversion (Phase 1b).
 * <p>
 * Every accessor prefers the node's
 * {@link JIPipeNodeInfo#getPrimaryFunction() primary taxonomy function} when
 * present (every registered node, after the 1b conversions). It is a set of
 * static, stateless methods over the {@link JIPipeNodeInfo} - no {@code
 * JIPipe} instance or registry is required, because the function objects
 * carry their own parent chain and behavior flags.
 * <p>
 * All methods are null-safe: an fn-null node (the defensive state, only
 * reachable via {@code JIPipeEmptyNodeInfo}, never a registered node) or an
 * empty secondary-function set never throws.
 */
public final class TaxonomyView {

    private TaxonomyView() {
    }

    /**
     * The display name for the node's function.
     * <p>
     * A primary function present: its name. Otherwise (the fn-null
     * defensive state): the constant {@code "Internal"}.
     *
     * @param info the node info
     * @return the display name, never {@code null}
     */
    public static String functionDisplayName(JIPipeNodeInfo info) {
        JIPipeNodeTaxonomyFunction primary = info.getPrimaryFunction();
        if (primary != null) {
            return primary.getName();
        }
        // fn-null defensive state (only reachable via JIPipeEmptyNodeInfo,
        // never a registered node): no legacy category to read, so the
        // display name is the constant "Internal".
        return "Internal";
    }

    /**
     * The human-readable location path of the node.
     * <p>
     * A primary function present: the function chain from the root down to
     * the primary function, joined by {@code " > "}, followed by the node
     * name - e.g. {@code "Images > Filtering > Gaussian Blur"}. Otherwise
     * (the fn-null defensive state): just the node name, with no branch
     * prefix.
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

        // fn-null defensive state (only reachable via JIPipeEmptyNodeInfo,
        // never a registered node): no category/menu path to read, so the
        // location path is just the node name (no branch prefix).
        return info.getName();
    }

    /**
     * The four behavior flags of the node, read from the primary function
     * when present, else the safe defaults.
     * <p>
     * The safe defaults for an fn-null node (the defensive state, only
     * reachable via {@code JIPipeEmptyNodeInfo}, never a registered node) are
     * all {@code true}: {@code canExtract()} and {@code isRunnable()} mirror
     * the taxonomy defaults; the two visibility flags are permissive.
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
        // fn-null defensive state (only reachable via JIPipeEmptyNodeInfo,
        // never a registered node): no category to read, so the safe
        // defaults (all true) - canExtract/isRunnable mirror the taxonomy
        // defaults; the two visibility flags are permissive true.
        return new BehaviorFlags(true, true, true, true);
    }

    /**
     * Whether the node's primary function (or one of its ancestors) IS the
     * given top-level function branch - the taxonomy replacement for the
     * legacy "category is an instance of &lt;Category&gt;" checks.
     * <p>
     * Walks the primary function's {@link JIPipeNodeTaxonomyFunction#getParent()
     * parent} chain up to the root, so a branch counts when it is the leaf
     * itself or any ancestor. A node with no primary function (unconverted,
     * or a defensive null) is never under any branch.
     *
     * @param info   the node info
     * @param branch the top-level function branch class to test for
     * @return {@code true} if the primary function's chain reaches a function
     * of the given branch class
     */
    public static boolean isUnderFunction(JIPipeNodeInfo info, Class<? extends JIPipeNodeTaxonomyFunction> branch) {
        JIPipeNodeTaxonomyFunction fn = info.getPrimaryFunction();
        while (fn != null) {
            if (branch.isInstance(fn)) {
                return true;
            }
            fn = fn.getParent();
        }
        return false;
    }

    /**
     * The data-source signal post-taxonomy: a node under the
     * {@link DataSourcesFunction} branch.
     * <p>
     * Replaces the legacy "category is an instance of
     * DataSourceNodeTypeCategory" check, which broke for converted nodes
     * (their legacy category is the generic {@code Internal} category, so
     * the {@code instanceof} is false). The
     * {@code @ConfigureJIPipeNode.dataSourceMenuLocation()} data type is a
     * separate, per-type grouping signal and is unaffected.
     *
     * @param info the node info
     * @return {@code true} if the node's primary function chain reaches the
     * {@link DataSourcesFunction} branch
     */
    public static boolean isDataSource(JIPipeNodeInfo info) {
        return isUnderFunction(info, DataSourcesFunction.class);
    }

    /**
     * The search ids for the node's taxonomy functions, in the exact format
     * the node database uses.
     * <p>
     * A primary function present: the primary function's id plus every
     * secondary function's id. No primary function (the fn-null defensive
     * state, only reachable via {@code JIPipeEmptyNodeInfo}, never a
     * registered node): empty.
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

        // fn-null defensive state (only reachable via JIPipeEmptyNodeInfo,
        // never a registered node): no category id to read, so the search
        // ids are empty.
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

    /**
     * The four behavior flags that gate where and how a node appears and
     * can be manipulated, read from the primary taxonomy function.
     *
     * @param isVisibleInPipeline     whether the node shows in the pipeline editor
     * @param isVisibleInCompartments whether the node shows in the compartment editor
     * @param canExtract              whether the node can be extracted (copied) from a graph
     * @param isRunnable              whether the node can be run
     */
    public record BehaviorFlags(boolean isVisibleInPipeline,
                                boolean isVisibleInCompartments,
                                boolean canExtract,
                                boolean isRunnable) {
    }
}
