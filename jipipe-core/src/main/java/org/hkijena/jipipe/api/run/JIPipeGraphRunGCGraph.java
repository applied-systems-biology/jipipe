package org.hkijena.jipipe.api.run;

import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Set;

/**
 * A graph that allows to track which data is currently being used
 * Contains {@link org.hkijena.jipipe.api.data.JIPipeOutputDataSlot} and {@link org.hkijena.jipipe.api.nodes.JIPipeGraphNode}
 */
public class JIPipeGraphRunGCGraph extends DefaultDirectedGraph<Object, DefaultEdge> {
    public JIPipeGraphRunGCGraph() {
        super(DefaultEdge.class);
    }

    public JIPipeGraphRunGCGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter) {
        super(DefaultEdge.class);
        addGraph(graph, nodeFilter);
    }

    public void addGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter) {
        for (JIPipeGraphNode graphNode : nodeFilter) {
            addVertex(graphNode);
            for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                addVertex(outputSlot);
                addEdge(graphNode, outputSlot);
            }
        }
        for (JIPipeGraphNode graphNode :  nodeFilter) {
            for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
                for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                    addEdge(graph.getGraph().getEdgeSource(graphEdge), inputSlot);
                }
            }
        }
    }
}
