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

package org.hkijena.pipelinej.api.history;

import org.hkijena.pipelinej.api.algorithm.ACAQGraph;
import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;

import java.util.Set;

public class AddNodeGraphHistorySnapshot implements ACAQAlgorithmGraphHistorySnapshot {

    private final ACAQGraph graph;
    private final Set<ACAQGraphNode> nodes;

    public AddNodeGraphHistorySnapshot(ACAQGraph graph, Set<ACAQGraphNode> nodes) {
        this.graph = graph;
        this.nodes = nodes;
    }

    @Override
    public String getName() {
        return "Add nodes";
    }

    @Override
    public void undo() {
        for (ACAQGraphNode node : nodes) {
            graph.removeNode(node, true);
        }
    }

    @Override
    public void redo() {
        for (ACAQGraphNode node : nodes) {
            graph.insertNode(node, node.getCompartment());
        }

    }

    public Set<ACAQGraphNode> getNodes() {
        return nodes;
    }

    public ACAQGraph getGraph() {
        return graph;
    }
}
