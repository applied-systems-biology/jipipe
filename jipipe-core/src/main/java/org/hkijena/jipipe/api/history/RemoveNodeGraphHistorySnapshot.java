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
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;

import java.util.*;

public class RemoveNodeGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph graph;
    private final BiMap<String, JIPipeGraphNode> nodes = HashBiMap.create();
    private final List<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> connections = new ArrayList<>();

    public RemoveNodeGraphHistorySnapshot(JIPipeGraph graph, Set<JIPipeGraphNode> nodes) {
        this.graph = graph;
        for (JIPipeGraphNode node : nodes) {
            this.nodes.put(node.getIdInGraph(), node);
            for (JIPipeDataSlot target : node.getInputSlots()) {
                JIPipeDataSlot source = graph.getSourceSlot(target);
                if (source != null) {
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
        for (Map.Entry<String, JIPipeGraphNode> entry : nodes.entrySet()) {
            graph.insertNode(entry.getKey(), entry.getValue(), entry.getValue().getCompartment());
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> connection : connections) {
            graph.connect(connection.getKey(), connection.getValue());
        }
    }

    public BiMap<String, JIPipeGraphNode> getNodes() {
        return nodes;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }
}
