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
