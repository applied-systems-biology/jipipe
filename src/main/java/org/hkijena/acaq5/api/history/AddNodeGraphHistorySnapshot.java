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

package org.hkijena.acaq5.api.history;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;

import java.util.Map;
import java.util.Set;

public class AddNodeGraphHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQGraph graph;
    private final BiMap<String, ACAQGraphNode> nodes = HashBiMap.create();

    public AddNodeGraphHistorySnapshot(ACAQGraph graph, Set<ACAQGraphNode> nodes) {
        this.graph = graph;
        for (ACAQGraphNode node : nodes) {
            this.nodes.put(node.getIdInGraph(), node);
        }
    }

    @Override
    public String getName() {
        return "Add nodes";
    }

    @Override
    public void undo() {
        for (ACAQGraphNode node : nodes.values()) {
            graph.removeNode(node, true);
        }
    }

    @Override
    public void redo() {
        for (Map.Entry<String, ACAQGraphNode> entry : nodes.entrySet()) {
            graph.insertNode(entry.getKey(), entry.getValue(), entry.getValue().getCompartment());
        }
    }

    public BiMap<String, ACAQGraphNode> getNodes() {
        return nodes;
    }

    public ACAQGraph getGraph() {
        return graph;
    }
}
