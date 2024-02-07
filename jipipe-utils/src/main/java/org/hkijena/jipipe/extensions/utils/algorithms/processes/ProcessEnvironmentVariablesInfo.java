package org.hkijena.jipipe.extensions.utils.algorithms.processes;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class ProcessEnvironmentVariablesInfo implements ExpressionParameterVariablesInfo {
    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<ExpressionParameterVariable> result = new HashSet<>();
        result.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
        result.add(new ExpressionParameterVariable("Executable", "The executable", "executable"));
        result.add(new ExpressionParameterVariable("Input data folder", "Points to the directory where input data is stored. Contains subdirectories named according to the input slots that follow the JIPipe data format.", "input_folder"));
        result.add(new ExpressionParameterVariable("Output data folder", "Points to the directory where input data is stored. Contains subdirectories named according to the input slots that follow the JIPipe data format.", "output_folder"));
        return result;
    }
}
