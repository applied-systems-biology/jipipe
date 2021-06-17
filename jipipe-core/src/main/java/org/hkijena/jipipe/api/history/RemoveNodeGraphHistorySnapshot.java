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

package org.hkijena.jipipe.api.history;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RemoveNodeGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph graph;
    private final BiMap<UUID, JIPipeGraphNode> nodes = HashBiMap.create();
    private final Map<UUID, UUID> nodeCompartments = new HashMap<>();
    private final List<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> connections = new ArrayList<>();

    public RemoveNodeGraphHistorySnapshot(JIPipeGraph graph, Set<JIPipeGraphNode> nodes) {
        this.graph = graph;
        for (JIPipeGraphNode node : nodes) {
            this.nodes.put(node.getUUIDInGraph(), node);
            this.nodeCompartments.put(node.getUUIDInGraph(), node.getUUIDInGraph());
            for (JIPipeDataSlot target : node.getInputSlots()) {
                Set<JIPipeDataSlot> sources = graph.getSourceSlots(target);
                for (JIPipeDataSlot source : sources) {
                    connections.add(new AbstractMap.SimpleEntry<>(source, target));
                }
            }
            for (JIPipeDataSlot source : node.getOutputSlots()) {
                for (JIPipeDataSlot target : graph.getTargetSlots(source)) {
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
        for (JIPipeGraphNode node : nodes.values()) {
            graph.removeNode(node, true);
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<UUID, JIPipeGraphNode> entry : nodes.entrySet()) {
            graph.insertNode(entry.getKey(), entry.getValue(), nodeCompartments.get(entry.getKey()));
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> connection : connections) {
            graph.connect(connection.getKey(), connection.getValue());
        }
    }

    public BiMap<UUID, JIPipeGraphNode> getNodes() {
        return nodes;
    }

    public Map<UUID, UUID> getNodeCompartments() {
        return nodeCompartments;
    }

    public List<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> getConnections() {
        return connections;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }
}
