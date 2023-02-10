package org.hkijena.jipipe.extensions.ijfilaments.nodes.filter;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdgeVariableSource;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertexVariableSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JIPipeDocumentation(name = "Filter filament edges", description = "Filters filament edges by various properties")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = FilamentsData.class, slotName = "Output", autoCreate = true)
public class FilterFilamentEdgesByProperties extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customExpressionVariables;
    private DefaultExpressionParameter filter = new DefaultExpressionParameter("");

    public FilterFilamentEdgesByProperties(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
    }

    public FilterFilamentEdgesByProperties(FilterFilamentEdgesByProperties other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.filter = new DefaultExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData inputData = dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo);
        FilamentsData outputData = new FilamentsData(inputData);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(dataBatch.getMergedTextAnnotations());
        customExpressionVariables.writeToVariables(variables, true, "custom", true, "custom");

        Set<FilamentEdge> toDelete = new HashSet<>();
        for (FilamentEdge edge : outputData.edgeSet()) {
            // Write variables
            for (Map.Entry<String, String> entry : edge.getMetadata().entrySet()) {
                variables.set("metadata." + entry.getKey(), entry.getValue());
            }
            FilamentEdgeVariableSource.writeToVariables(outputData, edge, variables, "");
            if(!filter.test(variables)) {
                toDelete.add(edge);
            }
        }
        outputData.removeAllEdges(toDelete);

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Only keep edge if", description = "If the filter is left empty or returns TRUE, the vertex is kept. Otherwise the vertex is deleted.")
    @JIPipeParameter("filter")
    @ExpressionParameterSettingsVariable(fromClass = FilamentEdgeVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @ExpressionParameterSettingsVariable(key = "metadata", name = "Vertex metadata", description = "A map containing the vertex metadata/properties (string keys, string values)")
    @ExpressionParameterSettingsVariable(name = "metadata.<Metadata key>", description = "Vertex metadata/properties accessible via their string keys")
    @ExpressionParameterSettings(hint = "per edge")
    public DefaultExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(DefaultExpressionParameter filter) {
        this.filter = filter;
    }
}
