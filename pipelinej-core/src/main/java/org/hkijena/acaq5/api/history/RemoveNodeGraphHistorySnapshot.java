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
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.*;

public class RemoveNodeGraphHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQGraph graph;
    private final BiMap<String, ACAQGraphNode> nodes = HashBiMap.create();
    private final List<Map.Entry<ACAQDataSlot, ACAQDataSlot>> connections = new ArrayList<>();

    public RemoveNodeGraphHistorySnapshot(ACAQGraph graph, Set<ACAQGraphNode> nodes) {
        this.graph = graph;
        for (ACAQGraphNode node : nodes) {
            this.nodes.put(node.getIdInGraph(), node);
            for (ACAQDataSlot target : node.getInputSlots()) {
                ACAQDataSlot source = graph.getSourceSlot(target);
                if (source != null) {
                    connections.add(new AbstractMap.SimpleEntry<>(source, target));
                }
            }
            for (ACAQDataSlot source : node.getOutputSlots()) {
                for (ACAQDataSlot target : graph.getTargetSlots(source)) {
                    connections.add(new AbstractMap.SimpleEntry<>(source, target));
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Remove nodes";
    }

    @Override
    public void redo() {
        for (ACAQGraphNode node : nodes.values()) {
            graph.removeNode(node, true);
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<String, ACAQGraphNode> entry : nodes.entrySet()) {
            graph.insertNode(entry.getKey(), entry.getValue(), entry.getValue().getCompartment());
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> connection : connections) {
            graph.connect(connection.getKey(), connection.getValue());
        }
    }

    public BiMap<String, ACAQGraphNode> getNodes() {
        return nodes;
    }

    public ACAQGraph getGraph() {
        return graph;
    }
}
