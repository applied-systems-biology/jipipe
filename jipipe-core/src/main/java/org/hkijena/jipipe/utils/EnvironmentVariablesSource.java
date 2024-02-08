package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnvironmentVariablesSource implements ExpressionParameterVariablesInfo {
    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            result.add(new JIPipeExpressionParameterVariableInfo(entry.getKey(), entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
