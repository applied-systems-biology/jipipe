package org.hkijena.jipipe.extensions.expressions.custom;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class JIPipeCustomExpressionVariablesParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    public static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();

        VARIABLES.add(new ExpressionParameterVariable("Custom variables", "A map containing custom expression variables (keys are the parameter keys)", "custom"));
        VARIABLES.add(new ExpressionParameterVariable("", "Custom variable parameters are added with a prefix 'custom.'", "custom.<Custom variable key>"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
