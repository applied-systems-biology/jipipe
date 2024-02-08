package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.threshold;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class ThresholdsExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
        result.add(new JIPipeExpressionParameterVariableInfo("thresholds", "Thresholds", "An array of numeric thresholds"));
        return result;
    }
}
