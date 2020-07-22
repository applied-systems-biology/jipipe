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

package org.hkijena.jipipe.api.grouping;

import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An algorithm that was imported from a Json extension.
 */
public class JsonAlgorithm extends GraphWrapperAlgorithm implements JIPipeCustomParameterCollection {

    private GraphNodeParameters exportedParameters = new GraphNodeParameters();

    /**
     * Creates a new instance
     *
     * @param info the info
     */
    public JsonAlgorithm(JsonNodeInfo info) {
        super(info, new JIPipeGraph(info.getGraph()));
        exportedParameters = new GraphNodeParameters(info.getExportedParameters());
    }

    /**
     * Makes a copy
     *
     * @param other the original
     */
    public JsonAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
    }

    public GraphNodeParameters getExportedParameters() {
        return exportedParameters;
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        JIPipeParameterTree standardParameters = new JIPipeParameterTree(this,
                JIPipeParameterTree.IGNORE_CUSTOM | JIPipeParameterTree.FORCE_REFLECTION);
        return standardParameters.getParameters();
    }

    @Override
    public Map<String, JIPipeParameterCollection> getChildParameterCollections() {
        this.exportedParameters.setGraph(getWrappedGraph());
        Map<String, JIPipeParameterCollection> result = new HashMap<>();
        result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, getWrappedGraph().getParameterTree(), true));
        return result;
    }

    /**
     * Replaces the targeted {@link JsonAlgorithm} by a {@link NodeGroup}
     *
     * @param algorithm the algorithm
     */
    public static void unpackToNodeGroup(JsonAlgorithm algorithm) {
        JIPipeGraph graph = algorithm.getGraph();
        NodeGroup group = JIPipeAlgorithm.newInstance("node-group");
        group.setCustomName(algorithm.getName());
        group.setCustomDescription(algorithm.getCustomDescription());
        group.setEnabled(algorithm.isEnabled());
        group.setPassThrough(algorithm.isPassThrough());
        group.setWrappedGraph(new JIPipeGraph(algorithm.getWrappedGraph()));
        group.setLocations(algorithm.getLocations());

        List<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> edges = new ArrayList<>();
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
            if (edge.getKey().getNode() == algorithm || edge.getValue().getNode() == algorithm) {
                edges.add(edge);
            }
        }

        graph.removeNode(algorithm, false);
        graph.insertNode(group, algorithm.getCompartment());

        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : edges) {
            if (edge.getKey().getNode() == algorithm) {
                // Output node
                JIPipeDataSlot source = group.getOutputSlot(edge.getKey().getName());
                graph.connect(source, edge.getValue());
            } else if (edge.getValue().getNode() == algorithm) {
                // Input node
                JIPipeDataSlot target = group.getInputSlot(edge.getValue().getName());
                graph.connect(edge.getKey(), target);
            }
        }

    }
}
