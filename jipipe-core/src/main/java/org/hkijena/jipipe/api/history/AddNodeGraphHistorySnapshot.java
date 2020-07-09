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

import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;

import java.util.Set;

public class AddNodeGraphHistorySnapshot implements JIPipeAlgorithmGraphHistorySnapshot {

    private final JIPipeGraph graph;
    private final Set<JIPipeGraphNode> nodes;

    public AddNodeGraphHistorySnapshot(JIPipeGraph graph, Set<JIPipeGraphNode> nodes) {
        this.graph = graph;
        this.nodes = nodes;
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
        for (JIPipeGraphNode node : nodes) {
            graph.insertNode(node, node.getCompartment());
        }

    }

    public Set<JIPipeGraphNode> getNodes() {
        return nodes;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }
}
