package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that contains basic information about an image
 */
public class Image5DExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {

    private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_c", "Number of channels", "Number of channel planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_z", "Number of Z slices", "Number of Z planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_t", "Number of frames", "Number of T planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_d", "Number of dimensions", "Number of dimensions"));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}

