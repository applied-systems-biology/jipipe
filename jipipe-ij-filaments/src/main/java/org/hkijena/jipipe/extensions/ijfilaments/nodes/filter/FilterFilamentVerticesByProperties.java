package org.hkijena.jipipe.extensions.ijfilaments.nodes.filter;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertexVariablesInfo;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.HashSet;
import java.util.Set;

@JIPipeDocumentation(name = "Filter filament vertices", description = "Filters filament vertices by various properties")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Filter")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", autoCreate = true)
public class FilterFilamentVerticesByProperties extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customExpressionVariables;
    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("");

    public FilterFilamentVerticesByProperties(JIPipeNodeInfo info) {
        super(info);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(this);
    }

    public FilterFilamentVerticesByProperties(FilterFilamentVerticesByProperties other) {
        super(other);
        this.customExpressionVariables = new CustomExpressionVariablesParameter(other.customExpressionVariables, this);
        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        ExpressionVariables variables = new ExpressionVariables();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        customExpressionVariables.writeToVariables(variables, true, "custom", true, "custom");

        Set<FilamentVertex> toDelete = new HashSet<>();
        for (FilamentVertex vertex : outputData.vertexSet()) {
            // Write variables
            FilamentVertexVariablesInfo.writeToVariables(outputData, vertex, variables, "");
            if (!filter.test(variables)) {
                toDelete.add(vertex);
            }
        }
        outputData.removeAllVertices(toDelete);

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Only keep vertex if", description = "If the filter is left empty or returns TRUE, the vertex is kept. Otherwise the vertex is deleted.")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterVariable(fromClass = FilamentVertexVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = TextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @JIPipeExpressionParameterVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    @JIPipeExpressionParameterSettings(hint = "per vertex")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(custom, \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomExpressionVariables() {
        return customExpressionVariables;
    }
}
