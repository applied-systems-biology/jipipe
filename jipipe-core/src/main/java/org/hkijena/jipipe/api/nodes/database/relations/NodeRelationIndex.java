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

package org.hkijena.jipipe.api.nodes.database.relations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The pure, JIPipe-free relation index behind the Similar / Previous / Next
 * model (spec §10.1).
 * <p>
 * The index holds the directed workflow edges contributed by the pluggable
 * {@link NodeRelationSource}s and answers the two directional queries:
 * <ul>
 * <li>{@link #next(String)} — the ids that run <em>after</em> the given node
 * (its workflow successors);</li>
 * <li>{@link #previous(String)} — the ids that run <em>before</em> the given
 * node (its workflow predecessors).</li>
 * </ul>
 * Within each list the ids are ranked by <b>support</b> (the number of times
 * the edge was contributed — i.e. the number of distinct sources/templates
 * carrying it) descending, then by node id ascending, so the ordering is
 * deterministic even for a tie in support.
 * <p>
 * The "similar" relation is deliberately <em>not</em> part of this index: it
 * is a taxonomy-tier query over the database's entries (function → framework
 * → tags), computed by {@code JIPipeNodeDatabase#relations} with this index's
 * previous/next results excluded. Keeping it out of the index keeps the index
 * a pure edge structure that is trivially unit-testable without booting the
 * node registry (see {@code NodeRelationIndexTest}).
 * <p>
 * <b>Stale edges.</b> An edge may reference a node id that no longer exists
 * (e.g. a template whose node was removed from the registry). Such edges must
 * be dropped, never fatal: callers that mine raw ids use
 * {@link #addEdgesIgnoringUnresolved(Set, List)} (which drops any edge whose
 * endpoint is not in the supplied known-id set and leaves the warning to the
 * caller), while sources that can resolve against the registry skip +
 * {@code logger.warn} before contributing at all (see
 * {@link NodeRelationSource}).
 * <p>
 * Not thread-safe; build it fully on a background thread, then publish it as
 * an immutable snapshot (the database does this by swapping a volatile
 * reference, mirroring its {@code relatedCache} contract).
 */
public class NodeRelationIndex {

    /** successor direction: from -> (to -> support) */
    private final Map<String, Map<String, Integer>> nextSupport = new HashMap<>();
    /** predecessor direction: to -> (from -> support) */
    private final Map<String, Map<String, Integer>> previousSupport = new HashMap<>();

    /**
     * Adds a single directed {@link Edge} ({@code to} runs after {@code from}).
     * Re-adding the same edge increases its support.
     *
     * @param edge the directed workflow edge
     */
    public void addEdge(Edge edge) {
        addEdge(edge.from(), edge.to());
    }

    /**
     * Adds a single directed edge {@code from -> to} ({@code to} runs after
     * {@code from}). Re-adding the same edge increases its support.
     *
     * @param from the predecessor node id
     * @param to   the successor node id
     */
    public void addEdge(String from, String to) {
        if (from == null || to == null) {
            return;
        }
        increment(nextSupport, from, to);
        increment(previousSupport, to, from);
    }

    /**
     * Adds a batch of edges, dropping (silently) any edge whose {@code from}
     * or {@code to} is not contained in {@code knownIds}. The caller is
     * responsible for logging a warning about the dropped edges (see the
     * class Javadoc): the index is pure and does not log.
     *
     * @param knownIds the set of node ids that currently exist
     * @param edges    the candidate edges, some of which may be stale
     */
    public void addEdgesIgnoringUnresolved(Set<String> knownIds, List<Edge> edges) {
        for (Edge edge : edges) {
            if (edge.from() == null || edge.to() == null) {
                continue;
            }
            if (!knownIds.contains(edge.from()) || !knownIds.contains(edge.to())) {
                continue;
            }
            addEdge(edge.from(), edge.to());
        }
    }

    /**
     * The direct workflow successors of {@code nodeId}, ranked by support
     * descending then node id ascending.
     *
     * @param nodeId the node id
     * @return the successor ids, possibly empty (never {@code null})
     */
    public List<String> next(String nodeId) {
        return rankedIds(nextSupport.get(nodeId));
    }

    /**
     * The direct workflow predecessors of {@code nodeId}, ranked by support
     * descending then node id ascending.
     *
     * @param nodeId the node id
     * @return the predecessor ids, possibly empty (never {@code null})
     */
    public List<String> previous(String nodeId) {
        return rankedIds(previousSupport.get(nodeId));
    }

    private static void increment(Map<String, Map<String, Integer>> direction,
                                  String source, String target) {
        direction.computeIfAbsent(source, id -> new HashMap<>())
            .merge(target, 1, Integer::sum);
    }

    private static List<String> rankedIds(Map<String, Integer> supportByTarget) {
        if (supportByTarget == null || supportByTarget.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(supportByTarget.entrySet());
        entries.sort(
            Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        List<String> result = new ArrayList<>(entries.size());
        for (Map.Entry<String, Integer> entry : entries) {
            result.add(entry.getKey());
        }
        return result;
    }
}
