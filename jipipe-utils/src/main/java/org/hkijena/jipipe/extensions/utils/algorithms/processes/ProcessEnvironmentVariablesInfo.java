package org.hkijena.jipipe.extensions.utils.algorithms.processes;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class ProcessEnvironmentVariablesInfo implements ExpressionParameterVariablesInfo {
    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
        result.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
        result.add(new JIPipeExpressionParameterVariableInfo("executable", "Executable", "The executable"));
        result.add(new JIPipeExpressionParameterVariableInfo("input_folder", "Input data folder", "Points to the directory where input data is stored. Contains subdirectories named according to the input slots that follow the JIPipe data format."));
        result.add(new JIPipeExpressionParameterVariableInfo("output_folder", "Output data folder", "Points to the directory where input data is stored. Contains subdirectories named according to the input slots that follow the JIPipe data format."));
        return result;
    }
}
