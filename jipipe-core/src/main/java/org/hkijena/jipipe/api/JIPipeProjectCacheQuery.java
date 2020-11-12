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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A query that allows to access a {@link JIPipeProjectCache}. This query caches the node cache states, so they are not always recalculated (which is expensive!)
 */
public class JIPipeProjectCacheQuery {
    private final JIPipeProject project;
    private BiMap<String, JIPipeGraphNode> nodes = HashBiMap.create();
    private List<JIPipeGraphNode> traversedNodes;
    private BiMap<JIPipeGraphNode, String> cachedRawStates = HashBiMap.create();
    private BiMap<JIPipeGraphNode, String> cachedIterationStates = HashBiMap.create();

    public JIPipeProjectCacheQuery(JIPipeProject project) {
        this.project = project;
        rebuild();
    }

    /**
     * Checks if the query is invalid.
     * @return if the cache is invalid
     */
    public boolean isInvalid() {
        if(!Objects.equals(new ArrayList<>(project.getGraph().traverseAlgorithms()), traversedNodes))
            return true;
        for (Map.Entry<JIPipeGraphNode, String> entry : cachedRawStates.entrySet()) {
            JIPipeGraphNode node = entry.getKey();
            if(node == null)
                return true;
            if(node instanceof JIPipeAlgorithm) {
                String currentState = ((JIPipeAlgorithm) node).getStateId();
                if(!Objects.equals(currentState, entry.getValue()))
                    return true;
            }
        }
        return true;
    }

    /**
     * Rebuilds the stored states
     */
    public void rebuild() {
        // Fetch traversed nodes again. Make a copy
        this.traversedNodes = new ArrayList<>(project.getGraph().traverseAlgorithms());

        // Fetch nodes and make a copy. Required because users might invalidate (delete) the nodes
        this.nodes.clear();
        for (Map.Entry<String, JIPipeGraphNode> entry : project.getGraph().getNodes().entrySet()) {
            this.nodes.put(entry.getKey(), entry.getValue());
        }

        // Cached raw states
        cachedRawStates.clear();
        for (JIPipeGraphNode node : traversedNodes) {
            String state = "";
            if(node instanceof JIPipeAlgorithm) {
                state = ((JIPipeAlgorithm) node).getStateId();
            }
            cachedRawStates.put(node, state);
        }

        // Cached iteration states
        StringBuilder currentIterationState = new StringBuilder();
        for (JIPipeGraphNode node : traversedNodes) {
            String rawState = cachedRawStates.get(node);
            currentIterationState.append(rawState).append("\n");
            cachedIterationStates.put(node, currentIterationState.toString());
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

    public List<JIPipeGraphNode> getTraversedNodes() {
        return traversedNodes;
    }

    public BiMap<JIPipeGraphNode, String> getCachedRawStates() {
        return cachedRawStates;
    }

    public BiMap<JIPipeGraphNode, String> getCachedIterationStates() {
        return cachedIterationStates;
    }

    /**
     * Returns the cache of the current algorithm state.
     * @param node the node
     * @return map of slot name to cache slot
     */
    public Map<String, JIPipeDataSlot> getCachedCache(JIPipeGraphNode node) {
        return project.getCache().extract((JIPipeAlgorithm)node, getCachedId(node));
    }

    /**
     * Returns the current cache state of an algorithm node.
     * Please note that this will not necessarily be the most current id, only the one that was extracted by the last rebuild()
     * @param node the node
     * @return the current cache state with the current local date & time
     */
    public JIPipeProjectCache.State getCachedId(JIPipeGraphNode node) {
        return new JIPipeProjectCache.State(LocalDateTime.now(), cachedIterationStates.get(node));
    }
}
