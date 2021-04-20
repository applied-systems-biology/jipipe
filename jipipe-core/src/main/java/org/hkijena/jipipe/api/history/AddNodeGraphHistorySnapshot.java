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

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AddNodeGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph graph;
    private final List<JIPipeGraphNode> nodes = new ArrayList<>();
    private final List<UUID> compartments = new ArrayList<>();

    public AddNodeGraphHistorySnapshot(JIPipeGraph graph, Set<JIPipeGraphNode> nodes) {
        this.graph = graph;
        for (JIPipeGraphNode node : nodes) {
           this.nodes.add(node);
           compartments.add(node.getCompartmentUUIDInGraph());
        }
    }

    @Override
    public String getName() {
        return "Add nodes";
    }

    @Override
    public void undo() {
        for (JIPipeGraphNode node : nodes) {
            graph.removeNode(node, true);
        }
    }

    @Override
    public void redo() {
        for (int i = 0; i < nodes.size(); i++) {
            graph.insertNode(nodes.get(i), compartments.get(i));
        }
    }

    public List<JIPipeGraphNode> getNodes() {
        return nodes;
    }

    public List<UUID> getCompartments() {
        return compartments;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }
}
