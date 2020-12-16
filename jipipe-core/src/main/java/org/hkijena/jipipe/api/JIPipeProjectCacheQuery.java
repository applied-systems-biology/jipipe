/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * A query that allows to access a {@link JIPipeProjectCache}. This query caches the node cache states, so they are not always recalculated (which is expensive!)
 */
public class JIPipeProjectCacheQuery {
    private final JIPipeProject project;
    private BiMap<String, JIPipeGraphNode> nodes = HashBiMap.create();
    private BiMap<JIPipeGraphNode, JIPipeProjectCache.State> cachedStates = HashBiMap.create();
    private DefaultDirectedGraph<JIPipeProjectCache.State, DefaultEdge> stateGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    public JIPipeProjectCacheQuery(JIPipeProject project) {
        this.project = project;
        rebuild();
    }

    /**
     * Rebuilds the stored states
     */
    public void rebuild() {
        // Fetch nodes and make a copy. Required because users might invalidate (delete) the nodes
        this.nodes.clear();
        for (Map.Entry<String, JIPipeGraphNode> entry : project.getGraph().getNodes().entrySet()) {
            this.nodes.put(entry.getKey(), entry.getValue());
        }

        // Create the state graph
        stateGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (JIPipeGraphNode node : project.getGraph().getNodes().values()) {
            JIPipeProjectCache.State state = new JIPipeProjectCache.State(node, new HashSet<>(), LocalDateTime.now());
            stateGraph.addVertex(state);
            cachedStates.put(node, state);
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : project.getGraph().getSlotEdges()) {
            stateGraph.addEdge(cachedStates.get(edge.getKey().getNode()), cachedStates.get(edge.getValue().getNode()));
        }

        // Resolve connections
        for (JIPipeProjectCache.State state : stateGraph.vertexSet()) {
            for (DefaultEdge edge : stateGraph.incomingEdgesOf(state)) {
                JIPipeProjectCache.State source = stateGraph.getEdgeSource(edge);
                state.getPredecessorStates().add(source);
            }
        }
    }

    public JIPipeGraphNode getNode(String id) {
        return nodes.get(id);
    }

    public JIPipeProject getProject() {
        return project;
    }

    public BiMap<String, JIPipeGraphNode> getNodes() {
        return nodes;
    }

    /**
     * Returns the cache of the current algorithm state.
     *
     * @param node the node
     * @return map of slot name to cache slot
     */
    public Map<String, JIPipeDataSlot> getCachedCache(JIPipeGraphNode node) {
        return project.getCache().extract(node, getCachedId(node));
    }

    /**
     * Returns the current cache state of an algorithm node.
     * Please note that this will not necessarily be the most current id, only the one that was extracted by the last rebuild()
     *
     * @param node the node
     * @return the current cache state with the current local date & time
     */
    public JIPipeProjectCache.State getCachedId(JIPipeGraphNode node) {
        return cachedStates.get(node);
    }
}
