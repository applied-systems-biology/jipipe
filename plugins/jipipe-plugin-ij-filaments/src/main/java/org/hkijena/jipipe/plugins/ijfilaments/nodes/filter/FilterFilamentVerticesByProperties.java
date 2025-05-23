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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.filter;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.util.AllFilamentVertexVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertexVariablesInfo;

import java.util.*;

@SetJIPipeDocumentation(name = "Filter filament vertices", description = "Filters filament vertices by various properties")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class FilterFilamentVerticesByProperties extends JIPipeSimpleIteratingAlgorithm {
    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("");

    public FilterFilamentVerticesByProperties(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterFilamentVerticesByProperties(FilterFilamentVerticesByProperties other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);

        // Collect all measurements
        Set<String> measurementKeys = new HashSet<>();
        Map<FilamentVertex, JIPipeExpressionVariablesMap> allMeasurements = new HashMap<>();
        for (FilamentVertex vertex : outputData.vertexSet()) {
            JIPipeExpressionVariablesMap forEdge = new JIPipeExpressionVariablesMap(iterationStep);
            FilamentVertexVariablesInfo.writeToVariables(outputData, vertex, forEdge, "");
            allMeasurements.put(vertex, forEdge);
            measurementKeys.addAll(forEdge.keySet());
        }

        // Create the all. measurements
        JIPipeExpressionVariablesMap allMeasurementsCalculated = new JIPipeExpressionVariablesMap(iterationStep);
        for (String key : measurementKeys) {
            List<Object> allValues = new ArrayList<>();
            for (FilamentVertex vertex : outputData.vertexSet()) {
                JIPipeExpressionVariablesMap forEdge = allMeasurements.get(vertex);
                Object value = forEdge.get(key);
                if (value != null) {
                    allValues.add(value);
                }
            }
            allMeasurementsCalculated.put("all." + key, allValues);
        }

        // Merge all. measurements
        for (JIPipeExpressionVariablesMap map : allMeasurements.values()) {
            map.putAll(allMeasurementsCalculated);
        }

        Set<FilamentVertex> toDelete = new HashSet<>();
        for (FilamentVertex vertex : outputData.vertexSet()) {
            if (!filter.test(allMeasurements.get(vertex))) {
                toDelete.add(vertex);
            }
        }
        outputData.removeAllVertices(toDelete);

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Only keep vertex if", description = "If the filter is left empty or returns TRUE, the vertex is kept. Otherwise the vertex is deleted.")
    @JIPipeParameter("filter")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = AllFilamentVertexVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }
}
