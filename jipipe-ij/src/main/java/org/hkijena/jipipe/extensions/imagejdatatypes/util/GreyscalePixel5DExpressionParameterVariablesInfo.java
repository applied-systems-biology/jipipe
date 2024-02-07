package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that allows to address x, y, and value
 */
public class GreyscalePixel5DExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {

    private final static Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
        VARIABLES.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
        VARIABLES.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
        VARIABLES.add(new ExpressionParameterVariable("X coordinate", "The X coordinate within the image", "x"));
        VARIABLES.add(new ExpressionParameterVariable("Y coordinate", "The Y coordinate within the image", "y"));
        VARIABLES.add(new ExpressionParameterVariable("Z coordinate", "The Z coordinate within the image (first index is zero)", "z"));
        VARIABLES.add(new ExpressionParameterVariable("Channel coordinate", "The channel (C) coordinate within the image (first index is zero)", "c"));
        VARIABLES.add(new ExpressionParameterVariable("Frame coordinate", "The frame (T) coordinate within the image (first index is zero)", "t"));
        VARIABLES.add(new ExpressionParameterVariable("Number of channels", "Number of channel planes", "num_c"));
        VARIABLES.add(new ExpressionParameterVariable("Number of Z slices", "Number of Z planes", "num_z"));
        VARIABLES.add(new ExpressionParameterVariable("Number of frames", "Number of T planes", "num_t"));
        VARIABLES.add(new ExpressionParameterVariable("Value", "The greyscale value at the (x,y) position", "value"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
