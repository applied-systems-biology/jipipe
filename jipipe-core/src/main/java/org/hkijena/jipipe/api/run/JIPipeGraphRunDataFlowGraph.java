package org.hkijena.jipipe.api.run;

import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.Set;

/**
 * Graph that contains input slots, nodes, and output slots with weights
 * Contains {@link JIPipeInputDataSlot}, {@link JIPipeOutputDataSlot}, and {@link JIPipeGraphNode}
 * Used for finding the optimal path through the graph
 */
public class JIPipeGraphRunDataFlowGraph extends DefaultDirectedWeightedGraph<Object, DefaultWeightedEdge> {

    public static final int DATA_FLOW_WEIGHT_INTERNAL = 0;
    public static final int DATA_FLOW_WEIGHT_LIGHT = 4;
    public static final int DATA_FLOW_WEIGHT_HEAVY = 1; // Algorithm should go through heavy data asap

    public JIPipeGraphRunDataFlowGraph() {
        super(DefaultWeightedEdge.class);
    }
    
    public JIPipeGraphRunDataFlowGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter) {
        super(DefaultWeightedEdge.class);
        addGraph(graph, nodeFilter);
    }

    public void addGraph(JIPipeGraph graph, Set<JIPipeGraphNode> nodeFilter) {
        for (JIPipeGraphNode graphNode : nodeFilter) {
            addVertex(graphNode);
            for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
                addVertex(inputSlot);
            }
            for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                addVertex(outputSlot);
            }
        }
        for (JIPipeGraphNode graphNode : nodeFilter) {
            for (JIPipeInputDataSlot inputSlot : graphNode.getInputSlots()) {
                // Edge to the node (weight 0)
                setEdgeWeight(addEdge(inputSlot, graphNode), DATA_FLOW_WEIGHT_INTERNAL);

                // Inputs
                for (JIPipeGraphEdge graphEdge : graph.getGraph().incomingEdgesOf(inputSlot)) {
                    int weight;
                    if(JIPipeDataInfo.getInstance(inputSlot.getAcceptedDataType()).isHeavy()) {
                        weight = DATA_FLOW_WEIGHT_HEAVY;
                    }
                    else {
                        weight = DATA_FLOW_WEIGHT_LIGHT;
                    }
                    JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(graphEdge);
                    if(containsVertex(edgeSource)) {
                        setEdgeWeight(addEdge(edgeSource, inputSlot), weight);
                    }
                }
            }
            for (JIPipeOutputDataSlot outputSlot : graphNode.getOutputSlots()) {
                // Edge from the node (weight by data)
                int weight;
                if(JIPipeDataInfo.getInstance(outputSlot.getAcceptedDataType()).isHeavy()) {
                    weight = DATA_FLOW_WEIGHT_HEAVY;
                }
                else {
                    weight = DATA_FLOW_WEIGHT_LIGHT;
                }
                setEdgeWeight(addEdge(graphNode, outputSlot), weight);
            }
        }
    }
}
