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

package org.hkijena.jipipe.api.looping;

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates a set of nodes that are in a loop
 */
public class LoopGroup {
    private final JIPipeGraph graph;
    private LoopStartNode loopStartNode;
    private Set<JIPipeGraphNode> nodes = new HashSet<>();
    private Set<JIPipeGraphNode> loopEndNodes = new HashSet<>();

    public LoopGroup(JIPipeGraph graph) {
        this.graph = graph;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public LoopStartNode getLoopStartNode() {
        return loopStartNode;
    }

    public void setLoopStartNode(LoopStartNode loopStartNode) {
        this.loopStartNode = loopStartNode;
    }

    public Set<JIPipeGraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(Set<JIPipeGraphNode> nodes) {
        this.nodes = nodes;
    }

    public Set<JIPipeGraphNode> getLoopEndNodes() {
        return loopEndNodes;
    }

    public void setLoopEndNodes(Set<JIPipeGraphNode> loopEndNodes) {
        this.loopEndNodes = loopEndNodes;
    }
}
