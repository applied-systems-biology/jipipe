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

import org.hkijena.jipipe.desktop.app.grapheditor.relationspanel.RelationLayerCalculator.Layer;
import org.hkijena.jipipe.desktop.app.grapheditor.relationspanel.RelationLayerCalculator.NodePos;
import org.hkijena.jipipe.desktop.app.grapheditor.relationspanel.RelationLayerCalculator.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RelationLayerCalculator}, the pure BFS layering behind the
 * Relations panel (MR 3 of the navigator overhaul, #1358).
 * <p>
 * All coordinates are chosen so that node centers land on exact integers and
 * physical distances are unambiguous.
 */
class RelationLayerCalculatorTest {

    private static Map<String, NodePos> nodes(NodePos... positions) {
        Map<String, NodePos> result = new LinkedHashMap<>();
        for (NodePos pos : positions) {
            result.put(pos.id(), pos);
        }
        return result;
    }

    private static List<String> ids(List<Layer> layers) {
        List<String> result = new ArrayList<>();
        for (Layer layer : layers) {
            result.add(layer.id());
        }
        return result;
    }

    private static List<List<String>> idsPerLevel(List<List<Layer>> levels) {
        List<List<String>> result = new ArrayList<>();
        for (List<Layer> level : levels) {
            result.add(ids(level));
        }
        return result;
    }

    @Test
    void singleCenterWithOneUpstreamAndOneDownstreamLevel() {
        Result result = RelationLayerCalculator.compute(
                List.of("c"),
                nodes(new NodePos("u", -100, 0), new NodePos("c", 0, 0), new NodePos("d", 100, 0)),
                Map.of("c", List.of("d")),
                Map.of("c", List.of("u")));
        assertEquals(Set.of("c"), result.centers());
        assertEquals(List.of(List.of("u")), idsPerLevel(result.upstream()));
        assertEquals(List.of(List.of("d")), idsPerLevel(result.downstream()));
        assertEquals(1.0, result.upstream().get(0).get(0).distanceToCenter(), 1e-9);
        assertEquals(1.0, result.downstream().get(0).get(0).distanceToCenter(), 1e-9);
    }

    /**
     * Edges c1 - m - s - c2 (c1 feeds m, m feeds s, s feeds c2): the shared
     * node s is two steps downstream of c1 but one step upstream of c2, so it
     * lands upstream at distance 1, the minimum over the centers.
     */
    @Test
    void sharedNodeBetweenCentersUsesTheMinimumDistance() {
        Result result = RelationLayerCalculator.compute(
                List.of("c1", "c2"),
                nodes(new NodePos("c1", 0, 0), new NodePos("m", 100, 0),
                        new NodePos("s", 200, 0), new NodePos("c2", 300, 0)),
                Map.of("c1", List.of("m"), "m", List.of("s"), "s", List.of("c2")),
                Map.of("m", List.of("c1"), "s", List.of("m"), "c2", List.of("s")));
        assertEquals(Set.of("c1", "c2"), result.centers());
        assertEquals(List.of(List.of("m")), idsPerLevel(result.downstream()));
        assertEquals(List.of(List.of("s")), idsPerLevel(result.upstream()));
        assertEquals(1.0, result.upstream().get(0).get(0).distanceToCenter(), 1e-9);
    }

    /**
     * Edges a - m - y - b with centers a and b: y is two steps downstream of
     * a but one step upstream of b, so it goes upstream (the nearer side),
     * while m is one step downstream of a but two steps upstream of b, so it
     * goes downstream.
     */
    @Test
    void nodeReachableInBothDirectionsGoesToTheNearerSide() {
        Result result = RelationLayerCalculator.compute(
                List.of("a", "b"),
                nodes(new NodePos("a", 0, 0), new NodePos("m", 100, 0),
                        new NodePos("y", 200, 0), new NodePos("b", 300, 0)),
                Map.of("a", List.of("m"), "m", List.of("y"), "y", List.of("b")),
                Map.of("m", List.of("a"), "y", List.of("m"), "b", List.of("y")));
        assertEquals(List.of(List.of("m")), idsPerLevel(result.downstream()));
        assertEquals(List.of(List.of("y")), idsPerLevel(result.upstream()));
        assertEquals(1.0, result.downstream().get(0).get(0).distanceToCenter(), 1e-9);
        assertEquals(1.0, result.upstream().get(0).get(0).distanceToCenter(), 1e-9);
    }

    /**
     * The cycle a - x - a reaches x at distance 1 in both directions, so the
     * tie assigns it to the upstream side.
     */
    @Test
    void distanceTieGoesUpstream() {
        Result result = RelationLayerCalculator.compute(
                List.of("a"),
                nodes(new NodePos("a", 0, 0), new NodePos("x", 50, 0)),
                Map.of("a", List.of("x"), "x", List.of("a")),
                Map.of("a", List.of("x"), "x", List.of("a")));
        assertTrue(result.downstream().isEmpty());
        assertEquals(List.of(List.of("x")), idsPerLevel(result.upstream()));
    }

    /**
     * The center feeds d1 before d2 (adjacency order), but d2 is physically
     * much nearer: the layer must order by ascending physical distance, not
     * by BFS order.
     */
    @Test
    void withinLayerOrderIsAscendingPhysicalDistanceNotBfsOrder() {
        Result result = RelationLayerCalculator.compute(
                List.of("c"),
                nodes(new NodePos("c", 0, 0), new NodePos("d1", 900, 0), new NodePos("d2", 10, 0)),
                Map.of("c", List.of("d1", "d2")),
                Map.of());
        assertEquals(List.of("d2", "d1"), ids(result.downstream().get(0)));
    }

    /**
     * n1 and n2 are one step downstream of both centers; n1 sits next to c1
     * and n2 sits next to c2, so the physical distance must be measured
     * against each node's nearest center (and must beat the BFS order, which
     * visits n2 first).
     */
    @Test
    void physicalDistanceIsMeasuredToTheNearestCenter() {
        Result result = RelationLayerCalculator.compute(
                List.of("c1", "c2"),
                nodes(new NodePos("c1", 0, 0), new NodePos("c2", 1000, 0),
                        new NodePos("n1", 5, 0), new NodePos("n2", 1200, 0)),
                Map.of("c1", List.of("n2", "n1"), "c2", List.of("n1", "n2")),
                Map.of());
        assertEquals(List.of("n1", "n2"), ids(result.downstream().get(0)));
    }

    /**
     * Chains u2 - u1 - c - d1 - d2 around the single center c: the upstream
     * levels are ordered furthest first, the downstream levels nearest first.
     */
    @Test
    void upstreamLayersAreFurthestFirstAndDownstreamLayersNearestFirst() {
        Result result = RelationLayerCalculator.compute(
                List.of("c"),
                nodes(new NodePos("u2", -200, 0), new NodePos("u1", -100, 0),
                        new NodePos("c", 0, 0), new NodePos("d1", 100, 0), new NodePos("d2", 200, 0)),
                Map.of("c", List.of("d1"), "d1", List.of("d2")),
                Map.of("c", List.of("u1"), "u1", List.of("u2")));
        assertEquals(List.of(List.of("u2"), List.of("u1")), idsPerLevel(result.upstream()));
        assertEquals(List.of(List.of("d1"), List.of("d2")), idsPerLevel(result.downstream()));
        assertEquals(2.0, result.upstream().get(0).get(0).distanceToCenter(), 1e-9);
        assertEquals(1.0, result.upstream().get(1).get(0).distanceToCenter(), 1e-9);
        assertEquals(1.0, result.downstream().get(0).get(0).distanceToCenter(), 1e-9);
        assertEquals(2.0, result.downstream().get(1).get(0).distanceToCenter(), 1e-9);
    }

    /**
     * c1 feeds c2 (both centers) and the cycle a - x - a: neither center may
     * appear in a layer, even when another center reaches it, while the tie
     * puts x upstream.
     */
    @Test
    void centersAreExcludedFromAllLayers() {
        Result result = RelationLayerCalculator.compute(
                List.of("c1", "c2", "a"),
                nodes(new NodePos("c1", 0, 0), new NodePos("c2", 100, 0),
                        new NodePos("a", 500, 0), new NodePos("x", 550, 0)),
                Map.of("c1", List.of("c2"), "a", List.of("x"), "x", List.of("a")),
                Map.of("c2", List.of("c1"), "a", List.of("x"), "x", List.of("a")));
        assertTrue(result.downstream().isEmpty());
        assertEquals(List.of(List.of("x")), idsPerLevel(result.upstream()));
    }

    /**
     * The center feeds "ghost", which has no position and which feeds "d",
     * which has one: the BFS traverses THROUGH the position-less id, so "d"
     * appears downstream at BFS distance 2, while "ghost" itself gets no
     * layer entry and no crash happens.
     */
    @Test
    void unknownIdsInAdjacencyAreIgnored() {
        Result result = RelationLayerCalculator.compute(
                List.of("c"),
                nodes(new NodePos("u", -100, 0), new NodePos("c", 0, 0), new NodePos("d", 200, 0)),
                Map.of("c", List.of("ghost"), "ghost", List.of("d")),
                Map.of("c", List.of("u")));
        assertEquals(List.of(List.of(new Layer("d", 2.0))), result.downstream());
        assertEquals(List.of(List.of("u")), idsPerLevel(result.upstream()));
        assertFalse(result.centers().contains("ghost"));
    }
}
