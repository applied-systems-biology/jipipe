package org.hkijena.acaq5.api.grouping;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An algorithm that was imported from a Json extension.
 */
public class JsonAlgorithm extends GraphWrapperAlgorithm {

    /**
     * Creates a new instance
     *
     * @param declaration the declaration
     */
    public JsonAlgorithm(JsonAlgorithmDeclaration declaration) {
        super(declaration, new ACAQAlgorithmGraph(declaration.getGraph()));
    }

    /**
     * Makes a copy
     *
     * @param other the original
     */
    public JsonAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
    }

    /**
     * Replaces the targeted {@link JsonAlgorithm} by a {@link NodeGroup}
     *
     * @param algorithm the algorithm
     */
    public static void unpackToNodeGroup(JsonAlgorithm algorithm) {
        ACAQAlgorithmGraph graph = algorithm.getGraph();
        NodeGroup group = ACAQAlgorithm.newInstance("node-group");
        group.setCustomName(algorithm.getName());
        group.setCustomDescription(algorithm.getCustomDescription());
        group.setEnabled(algorithm.isEnabled());
        group.setPassThrough(algorithm.isPassThrough());
        group.setWrappedGraph(new ACAQAlgorithmGraph(algorithm.getWrappedGraph()));
        group.setLocations(algorithm.getLocations());

        List<Map.Entry<ACAQDataSlot, ACAQDataSlot>> edges = new ArrayList<>();
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : graph.getSlotEdges()) {
            if (edge.getKey().getAlgorithm() == algorithm || edge.getValue().getAlgorithm() == algorithm) {
                edges.add(edge);
            }
        }

        graph.removeNode(algorithm);
        graph.insertNode(group, algorithm.getCompartment());

        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : edges) {
            if (edge.getKey().getAlgorithm() == algorithm) {
                // Output node
                ACAQDataSlot source = group.getOutputSlot(edge.getKey().getName());
                graph.connect(source, edge.getValue());
            } else if (edge.getValue().getAlgorithm() == algorithm) {
                // Input node
                ACAQDataSlot target = group.getInputSlot(edge.getKey().getName());
                graph.connect(edge.getKey(), target);
            }
        }

    }
}
