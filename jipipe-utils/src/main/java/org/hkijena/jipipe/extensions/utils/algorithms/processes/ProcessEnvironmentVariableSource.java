package org.hkijena.jipipe.extensions.utils.algorithms.processes;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

public class ProcessEnvironmentVariableSource implements ExpressionParameterVariableSource {
    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        Set<ExpressionParameterVariable> result = new HashSet<>();
        result.add(new ExpressionParameterVariable("<Annotations>", "Annotations of the current data batch", ""));
        result.add(new ExpressionParameterVariable("Executable", "The executable", "executable"));
        return result;
    }
}
