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

package org.hkijena.jipipe.plugins.multiparameters.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;

import java.util.Map;

/**
 * Generates {@link ParametersData} objects
 */
@SetJIPipeDocumentation(name = "Project-wide parameter", description = "Extracts a parameter set from global (project-wide) parameters.")
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Annotations", create = true, optional = true)
@AddJIPipeOutputSlot(value = ParametersData.class, name = "Parameters", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
public class DefineParametersFromGlobalParametersAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private JIPipeExpressionParameter filter = new JIPipeExpressionParameter("");


    public DefineParametersFromGlobalParametersAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DefineParametersFromGlobalParametersAlgorithm(DefineParametersFromGlobalParametersAlgorithm other) {
        super(other);
        this.filter = new JIPipeExpressionParameter(other.filter);
    }

    @SetJIPipeDocumentation(name = "Keep parameter if ...", description = "Allows to filter out parameters")
    @JIPipeParameter("filter")
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(key = "key", name = "Parameter key", description = "The parameter key")
    @AddJIPipeExpressionParameterVariable(key = "value", name = "Parameter value", description = "The parameter value")
    public JIPipeExpressionParameter getFilter() {
        return filter;
    }

    @JIPipeParameter("filter")
    public void setFilter(JIPipeExpressionParameter filter) {
        this.filter = filter;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeProject project = getProject();
        if(project == null) {
            progressInfo.log("WARNING: Project is null! Cannot extract parameters.");
            return;
        }

        ParametersData parametersData = new ParametersData();
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap(iterationStep);
        for (Map.Entry<String, JIPipeParameterAccess> entry : project.getMetadata().getGlobalParameters().getParameters().entrySet()) {
            variablesMap.put(entry.getKey(), entry.getValue().get(Object.class));
            if(filter.test(variablesMap)) {
                parametersData.getParameterData().put(entry.getKey(), entry.getValue().get(Object.class));
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), parametersData, progressInfo);
    }

    @Override
    protected boolean isAllowEmptyIterationStep() {
        return true;
    }
}
