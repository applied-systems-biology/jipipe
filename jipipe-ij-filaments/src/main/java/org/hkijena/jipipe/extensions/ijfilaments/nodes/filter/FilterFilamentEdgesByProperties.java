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

package org.hkijena.jipipe.extensions.ijfilaments.nodes.filter;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdgeVariablesInfo;

import java.util.HashSet;
import java.util.Set;

@SetJIPipeDocumentation(name = "Filter filament edges", description = "Filters filament edges by various properties")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Filter")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = Filaments3DData.class, slotName = "Output", create = true)
public class FilterFilamentEdgesByProperties extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("");

    public FilterFilamentEdgesByProperties(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterFilamentEdgesByProperties(FilterFilamentEdgesByProperties other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = iterationStep.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        Filaments3DData outputData = new Filaments3DData(inputData);

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variables);

        Set<FilamentEdge> toDelete = new HashSet<>();
        for (FilamentEdge edge : outputData.edgeSet()) {
            // Write variables
            FilamentEdgeVariablesInfo.writeToVariables(outputData, edge, variables, "");
            if (!filter.test(variables)) {
                toDelete.add(edge);
            }
        }
        outputData.removeAllEdges(toDelete);

        iterationStep.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Only keep edge if", description = "If the filter is left empty or returns TRUE, the vertex is kept. Otherwise the vertex is deleted.")
    @JIPipeParameter("filter")
    @JIPipeExpressionParameterVariable(fromClass = FilamentEdgeVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    @JIPipeExpressionParameterSettings(hint = "per edge")
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
