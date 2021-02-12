package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

public class ThresholdsExpressionParameterVariableSource implements ExpressionParameterVariableSource {
    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        Set<ExpressionParameterVariable> result = new HashSet<>();
        result.add(new ExpressionParameterVariable("Thresholds", "An array of numeric thresholds", "thresholds"));
        return result;
    }
}
