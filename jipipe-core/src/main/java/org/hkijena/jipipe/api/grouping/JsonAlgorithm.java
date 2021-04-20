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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceAccessGroupList;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.parameters.*;

import java.util.*;

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

    /**
     * Replaces the targeted {@link JsonAlgorithm} by a {@link NodeGroup}
     *
     * @param algorithm the algorithm
     */
    public static void unpackToNodeGroup(JsonAlgorithm algorithm) {
        JIPipeGraph graph = algorithm.getGraph();
        NodeGroup group = JIPipe.createNode("node-group", NodeGroup.class);
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
        graph.insertNode(group, algorithm.getCompartmentUUIDInGraph());

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

    public GraphNodeParameters getExportedParameters() {
        return exportedParameters;
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
//        JIPipeParameterTree standardParameters = new JIPipeParameterTree(this,
//                JIPipeParameterTree.IGNORE_CUSTOM | JIPipeParameterTree.FORCE_REFLECTION);
//        HashMap<String, JIPipeParameterAccess> map = new HashMap<>(standardParameters.getParameters());
//        map.values().removeIf(access -> access.getSource() != this);
//        return map;
        return Collections.emptyMap();
    }

    @Override
    public boolean getIncludeReflectionParameters() {
        return true;
    }

    @Override
    public Map<String, JIPipeParameterCollection> getChildParameterCollections() {
        this.exportedParameters.setGraph(getWrappedGraph());
        Map<String, JIPipeParameterCollection> result = new HashMap<>();
//        result.put("jipipe:data-batch-generation", getBatchGenerationSettings());
        result.put("exported", new GraphNodeParameterReferenceAccessGroupList(exportedParameters, getWrappedGraph().getParameterTree(false), true));
        return result;
    }

    @JIPipeDocumentation(name = "Data batch generation", description = "Only used if the graph iteration mode is not set to 'Pass data through'. " +
            "This algorithm can have multiple inputs. This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", visibility = JIPipeParameterVisibility.Visible, collapsed = true)
    public JIPipeMergingAlgorithm.DataBatchGenerationSettings getBatchGenerationSettings() {
        return super.getBatchGenerationSettings();
    }

    @JIPipeDocumentation(name = "Graph iteration mode", description = "Determines how the wrapped graph is iterated:" +
            "<ul>" +
            "<li>The data can be passed through. This means that the wrapped graph receives all data as-is and will be executed once.</li>" +
            "<li>The wrapped graph can be executed per data batch. Here you can choose between an iterative data batch (one item per slot) " +
            "or a merging data batch (multiple items per slot).</li>" +
            "</ul>")
    @JIPipeParameter("iteration-mode")
    public IterationMode getIterationMode() {
        return super.getIterationMode();
    }

    @JIPipeParameter("iteration-mode")
    public void setIterationMode(IterationMode iterationMode) {
        super.setIterationMode(iterationMode);
    }
}
