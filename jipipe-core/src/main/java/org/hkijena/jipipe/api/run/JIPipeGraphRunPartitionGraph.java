package org.hkijena.jipipe.api.run;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JIPipeGraphRunPartitionGraph extends DirectedAcyclicGraph<Set<JIPipeGraphNode>, DefaultEdge> {
    public JIPipeGraphRunPartitionGraph() {
        super(DefaultEdge.class);
    }

    public JIPipeGraphRunPartitionGraph(JIPipeGraph graph) {
        super(DefaultEdge.class);
        addGraph(graph);
    }

    public void addGraph(JIPipeGraph graph) {
        JIPipeGraph copy = new JIPipeGraph(graph);

        // Delete all inter-partition edges
        for (JIPipeGraphEdge edge : ImmutableList.copyOf(copy.getGraph().edgeSet())) {
            JIPipeGraphNode source = copy.getGraph().getEdgeSource(edge).getNode();
            JIPipeGraphNode target = copy.getGraph().getEdgeTarget(edge).getNode();
            if (source instanceof JIPipeAlgorithm && target instanceof JIPipeAlgorithm) {
                if (!Objects.equals(((JIPipeAlgorithm) source).getRuntimePartition(), ((JIPipeAlgorithm) target).getRuntimePartition())) {
                    copy.getGraph().removeEdge(edge);
                }
            }
        }

        // Find components
        ConnectivityInspector<JIPipeDataSlot, JIPipeGraphEdge> inspector = new ConnectivityInspector<>(copy.getGraph());

        // Create vertices (contains nodes from the original graph)
        Map<JIPipeGraphNode, Set<JIPipeGraphNode>> components = new HashMap<>();
        for (Set<JIPipeDataSlot> slots : inspector.connectedSets()) {
            Set<JIPipeGraphNode> nodes = slots.stream().map(JIPipeDataSlot::getNode).map(graph::getEquivalentNode).collect(Collectors.toSet());
            addVertex(nodes);
            for (JIPipeGraphNode node : nodes) {
                components.put(graph.getEquivalentNode(node), nodes);
            }
        }

        // Create edges
        for (Set<JIPipeGraphNode> nodes : vertexSet()) {
            for (JIPipeGraphNode node : nodes) {
                if (node instanceof JIPipeAlgorithm) {
                    for (JIPipeInputDataSlot inputSlot : node.getInputSlots()) {
                        for (JIPipeGraphEdge edge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                            JIPipeDataSlot source = graph.getGraph().getEdgeSource(edge);
                            if (source.getNode() instanceof JIPipeAlgorithm) {
                                if (!Objects.equals(((JIPipeAlgorithm) source.getNode()).getRuntimePartition(),
                                        ((JIPipeAlgorithm) node).getRuntimePartition())) {
                                    addEdge(components.get(source.getNode()), nodes);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "[Partitions] " + vertexSet().size() + " vertices, " + edgeSet().size() + " edges";
    }
}
