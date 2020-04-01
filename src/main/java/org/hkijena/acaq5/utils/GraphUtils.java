package org.hkijena.acaq5.utils;

import org.jgrapht.Graph;
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

}
