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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.util.AllFilamentComponentVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentComponentVariablesInfo;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.jgrapht.alg.connectivity.ConnectivityInspector;

import java.util.*;

@SetJIPipeDocumentation(name = "Filter filament components", description = "Filters filament connected components by various properties")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class FilterFilamentsByProperties extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("");

    public FilterFilamentsByProperties(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterFilamentsByProperties(FilterFilamentsByProperties other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DGraphData.class, progressInfo);
        Filaments3DGraphData outputData = new Filaments3DGraphData(inputData);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        ConnectivityInspector<FilamentVertex, FilamentEdge> connectivityInspector = outputData.getConnectivityInspector();
        List<Set<FilamentVertex>> connectedSets = connectivityInspector.connectedSets();
        String consensusPhysicalSizeUnit = outputData.getConsensusPhysicalSizeUnit();
        Set<FilamentVertex> toDelete = new HashSet<>();
        List<ResultsTableData> measurementList = new ArrayList<>();
        ResultsTableData mergedMeasurements = new ResultsTableData();

        // Collect all measurements
        for (int i = 0; i < connectedSets.size(); i++) {
            ResultsTableData measurements = new ResultsTableData();
            Set<FilamentVertex> vertices = connectedSets.get(i);
            outputData.measureComponent(measurements, consensusPhysicalSizeUnit, vertices);

            measurementList.add(measurements);
            mergedMeasurements.addRows(measurements);
        }

        // Create all. variables
        for (String columnName : mergedMeasurements.getColumnNames()) {
            variables.set("all." + columnName, mergedMeasurements.getColumnReference(columnName).getDataAsObjectList());
        }

        // Filter
        for (int i = 0; i < connectedSets.size(); i++) {
            ResultsTableData measurements = measurementList.get(i);
            Set<FilamentVertex> vertices = connectedSets.get(i);
            for (int col = 0; col < measurements.getColumnCount(); col++) {
                variables.set(measurements.getColumnName(col), measurements.getValueAt(0, col));
            }

            if (!filter.test(variables)) {
                toDelete.addAll(vertices);
            }
        }


        outputData.removeAllVertices(toDelete);
        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Only keep filament if", description = "If the filter is left empty or returns TRUE, the filament is kept. Otherwise the vertex is deleted.")
    @JIPipeParameter("filter")
    @AddJIPipeExpressionParameterVariable(fromClass = FilamentComponentVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = AllFilamentComponentVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per filament")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }
}
