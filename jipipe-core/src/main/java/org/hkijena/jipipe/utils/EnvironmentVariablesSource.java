package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnvironmentVariablesSource implements ExpressionParameterVariablesInfo {
    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<ExpressionParameterVariable> result = new HashSet<>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            result.add(new ExpressionParameterVariable(entry.getKey(), entry.getValue(), entry.getKey()));
        }
        return result;
    }
}
