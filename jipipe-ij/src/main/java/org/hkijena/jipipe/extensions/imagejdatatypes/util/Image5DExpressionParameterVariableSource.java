package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that contains basic information about an image
 */
public class Image5DExpressionParameterVariableSource implements ExpressionParameterVariableSource {

    private final static Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
        VARIABLES.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
        VARIABLES.add(new ExpressionParameterVariable("Number of channels", "Number of channel planes", "num_c"));
        VARIABLES.add(new ExpressionParameterVariable("Number of Z slices", "Number of Z planes", "num_z"));
        VARIABLES.add(new ExpressionParameterVariable("Number of frames", "Number of T planes", "num_t"));
        VARIABLES.add(new ExpressionParameterVariable("Number of dimensions", "Number of dimensions", "num_d"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}

