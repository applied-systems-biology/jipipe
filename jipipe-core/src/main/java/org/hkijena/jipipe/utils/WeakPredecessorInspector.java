package org.hkijena.jipipe.utils;

import org.jgrapht.Graph;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;

import java.util.*;

/**
 * Calculates the predecessor set of a vertex
 *
 * @param <V>
 * @param <E>
 */
public class WeakPredecessorInspector<V, E> implements GraphListener<V, E> {

    private final Graph<V, E> graph;
    private final Map<V, Set<V>> predecessors = new HashMap<>();

    public WeakPredecessorInspector(Graph<V, E> graph) {
        this.graph = graph;
    }

    public Set<V> getAllPredecessors(V vertex) {
        Set<V> result = predecessors.getOrDefault(vertex, null);
        if (result == null) {
            result = calculatePredecessors(vertex);
            predecessors.put(vertex, result);
        }

        return result;
    }

    private Set<V> calculatePredecessors(V vertex) {
        Set<V> result = new HashSet<>();
        Stack<V> stack = new Stack<>();
        stack.push(vertex);

        while (!stack.isEmpty()) {
            V top = stack.pop();
            if (top != vertex) {
                result.add(top);
            }
            for (E edge : graph.incomingEdgesOf(top)) {
                stack.push(graph.getEdgeSource(edge));
            }
        }

        return result;
    }

    @Override
    public void edgeAdded(GraphEdgeChangeEvent<V, E> graphEdgeChangeEvent) {
        predecessors.clear();
    }

    @Override
    public void edgeRemoved(GraphEdgeChangeEvent<V, E> graphEdgeChangeEvent) {
        predecessors.clear();
    }

    @Override
    public void vertexAdded(GraphVertexChangeEvent<V> graphVertexChangeEvent) {

    }

    @Override
    public void vertexRemoved(GraphVertexChangeEvent<V> graphVertexChangeEvent) {
        predecessors.clear();
    }
}
