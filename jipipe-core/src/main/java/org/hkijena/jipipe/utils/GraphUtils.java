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

package org.hkijena.jipipe.utils;

import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for graph
 */
public class GraphUtils {

    private GraphUtils() {

    }

    /**
     * Gets all successors of the vertex
     *
     * @param graph  the graph
     * @param vertex the vertex
     * @param <V>    vertex type
     * @param <E>    edge type
     * @return list of successors
     */
    public static <V, E> List<V> getAllSuccessors(Graph<V, E> graph, V vertex) {
        BreadthFirstIterator<V, E> breadthFirstIterator = new BreadthFirstIterator<>(graph, vertex);
        List<V> result = new ArrayList<>();
        while (breadthFirstIterator.hasNext()) {
            result.add(breadthFirstIterator.next());
        }

        return result;
    }

    /**
     * Gets all parent nodes of the vertex
     *
     * @param graph  the graph
     * @param vertex the vertex
     * @param <V>    vertex type
     * @param <E>    edge type
     * @return list of parent nodes
     */
    public static <V, E> List<V> getAllPredecessors(Graph<V, E> graph, V vertex) {
        return getAllSuccessors(new EdgeReversedGraph<>(graph), vertex);
    }

    public static <V, E> boolean hasCycle(DefaultDirectedGraph<V, E> graph) {
        CycleDetector<V, E> cycleDetector = new CycleDetector<>(graph);
        return cycleDetector.detectCycles();
    }

    public static <V, E> V getFirstPredecessor(Graph<V, E> graph, V vertex) {
        for (E edge : graph.incomingEdgesOf(vertex)) {
            return graph.getEdgeSource(edge);
        }
        return null;
    }
}
