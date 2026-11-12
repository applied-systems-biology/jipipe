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

package org.hkijena.jipipe.desktop.app.grapheditor.relationspanel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pure BFS layering of the graph around the selected centers for the
 * Relations panel (MR 3 of the navigator overhaul, #1358).
 * <p>
 * Works on plain ids and canvas positions only: the adjacency maps are
 * prepared by the panel ({@code downstreamAdj} maps a node id to the ids it
 * feeds, {@code upstreamAdj} is the reverse), so this class does not depend
 * on any graph type and is testable without a display.
 * <p>
 * Side assignment: a node reachable from the centers in both directions
 * belongs to the side with the smaller BFS distance; a tie goes upstream.
 * The centers themselves are excluded from all layers. Ids mentioned by the
 * adjacency maps but absent from the node positions are ignored (no crash,
 * no layer entry).
 */
public class RelationLayerCalculator {

    private RelationLayerCalculator() {
    }

    /**
     * A node's canvas position (the center of the node).
     */
    public record NodePos(String id, double x, double y) {
    }

    /**
     * One node in a layer: its id and its BFS distance to the nearest center
     * on the side it was assigned to.
     */
    public record Layer(String id, double distanceToCenter) {
    }

    /**
     * The layering around the centers.
     * <p>
     * {@code upstream} is ordered furthest first (the top of the panel),
     * {@code downstream} is ordered nearest first (just below the center).
     * Within a level, layers are ordered by ascending physical distance to
     * the nearest center (ties by id, so recomputes are deterministic).
     * Empty levels are omitted.
     */
    public record Result(Set<String> centers, List<List<Layer>> upstream, List<List<Layer>> downstream) {
    }

    /**
     * Computes the upstream and downstream BFS layers around the centers.
     *
     * @param centerIds     the selected node ids (the centers)
     * @param nodes         canvas positions, keyed by node id
     * @param downstreamAdj forward adjacency: node id to the ids it feeds
     * @param upstreamAdj   reverse adjacency: node id to the ids that feed it
     * @return the layering; both layer lists are unmodifiable
     */
    public static Result compute(Collection<String> centerIds,
                                 Map<String, NodePos> nodes,
                                 Map<String, List<String>> downstreamAdj,
                                 Map<String, List<String>> upstreamAdj) {
        Set<String> centers = new LinkedHashSet<>(centerIds);
        Map<String, Integer> downstreamDistances = breadthFirstDistances(centers, downstreamAdj);
        Map<String, Integer> upstreamDistances = breadthFirstDistances(centers, upstreamAdj);

        // Nearest-first for downstream, furthest-first for upstream: the
        // TreeMap ordering IS the panel's vertical ordering per side.
        TreeMap<Integer, List<Layer>> upstreamLevels = new TreeMap<>(Comparator.reverseOrder());
        TreeMap<Integer, List<Layer>> downstreamLevels = new TreeMap<>();

        Set<String> candidates = new HashSet<>(downstreamDistances.keySet());
        candidates.addAll(upstreamDistances.keySet());
        candidates.removeAll(centers);

        for (String id : candidates) {
            NodePos pos = nodes.get(id);
            if (pos == null) {
                // The adjacency may mention ids that have no position (e.g.
                // annotation nodes filtered by the panel): they get no layer.
                continue;
            }
            Integer downstreamDistance = downstreamDistances.get(id);
            Integer upstreamDistance = upstreamDistances.get(id);
            boolean upstreamSide;
            int distance;
            if (upstreamDistance == null) {
                upstreamSide = false;
                distance = downstreamDistance;
            } else if (downstreamDistance == null) {
                upstreamSide = true;
                distance = upstreamDistance;
            } else if (upstreamDistance <= downstreamDistance) {
                // Reachable in both directions: the nearer side wins, ties
                // go upstream.
                upstreamSide = true;
                distance = upstreamDistance;
            } else {
                upstreamSide = false;
                distance = downstreamDistance;
            }
            (upstreamSide ? upstreamLevels : downstreamLevels).computeIfAbsent(distance, k -> new ArrayList<>()).add(new Layer(id, distance));
        }

        Map<String, NodePos> centerPositions = new HashMap<>();
        for (String center : centers) {
            NodePos pos = nodes.get(center);
            if (pos != null) {
                centerPositions.put(center, pos);
            }
        }
        return new Result(Set.copyOf(centers),
                orderedLevels(upstreamLevels, centerPositions, nodes),
                orderedLevels(downstreamLevels, centerPositions, nodes));
    }

    /**
     * BFS distances from the union of the centers over the given adjacency
     * (the centers themselves have distance 0).
     */
    private static Map<String, Integer> breadthFirstDistances(Set<String> centers, Map<String, List<String>> adjacency) {
        Map<String, Integer> distances = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String center : centers) {
            distances.put(center, 0);
            queue.add(center);
        }
        while (!queue.isEmpty()) {
            String id = queue.poll();
            int nextDistance = distances.get(id) + 1;
            for (String target : adjacency.getOrDefault(id, List.of())) {
                if (distances.containsKey(target)) {
                    continue;
                }
                distances.put(target, nextDistance);
                queue.add(target);
            }
        }
        return distances;
    }

    /**
     * Orders the levels of one side (the map's key order) and each level's
     * layers by ascending physical distance to the nearest center (ties by
     * id, so recomputes are deterministic).
     */
    private static List<List<Layer>> orderedLevels(TreeMap<Integer, List<Layer>> levels,
                                                   Map<String, NodePos> centerPositions,
                                                   Map<String, NodePos> nodes) {
        List<List<Layer>> result = new ArrayList<>();
        for (List<Layer> level : levels.values()) {
            List<Layer> ordered = new ArrayList<>(level);
            ordered.sort((a, b) -> {
                int byDistance = Double.compare(nearestCenterDistance(a.id(), centerPositions, nodes),
                        nearestCenterDistance(b.id(), centerPositions, nodes));
                if (byDistance != 0) {
                    return byDistance;
                }
                return a.id().compareTo(b.id());
            });
            result.add(List.copyOf(ordered));
        }
        return result;
    }

    /**
     * The Euclidean distance from a node to its nearest center, or
     * {@link Double#POSITIVE_INFINITY} when no center has a position.
     */
    private static double nearestCenterDistance(String id, Map<String, NodePos> centerPositions, Map<String, NodePos> nodes) {
        NodePos pos = nodes.get(id);
        double nearest = Double.POSITIVE_INFINITY;
        for (NodePos center : centerPositions.values()) {
            double dx = pos.x() - center.x();
            double dy = pos.y() - center.y();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < nearest) {
                nearest = distance;
            }
        }
        return nearest;
    }
}
