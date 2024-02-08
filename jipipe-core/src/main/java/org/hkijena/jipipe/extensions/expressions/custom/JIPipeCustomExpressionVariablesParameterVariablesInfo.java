package org.hkijena.jipipe.extensions.expressions.custom;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class JIPipeCustomExpressionVariablesParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();

        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("custom", "Custom variables", "A map containing custom expression variables (keys are the parameter keys)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("custom.<Custom variable key>", "", "Custom variable parameters are added with a prefix 'custom.'"));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
