package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.threshold;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

public class ThresholdsExpressionParameterVariableSource implements ExpressionParameterVariableSource {
    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<ExpressionParameterVariable> result = new HashSet<>();
        result.add(new ExpressionParameterVariable("Thresholds", "An array of numeric thresholds", "thresholds"));
        return result;
    }
}
