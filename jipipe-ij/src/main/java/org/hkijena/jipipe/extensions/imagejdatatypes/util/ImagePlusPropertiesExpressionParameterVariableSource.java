package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import ij.ImagePlus;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that contains image statistics
 */
public class ImagePlusPropertiesExpressionParameterVariableSource implements ExpressionParameterVariableSource {

    private final static Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
        VARIABLES.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
        VARIABLES.add(new ExpressionParameterVariable("Number of channels", "Number of channel planes", "num_c"));
        VARIABLES.add(new ExpressionParameterVariable("Number of Z slices", "Number of Z planes", "num_z"));
        VARIABLES.add(new ExpressionParameterVariable("Number of frames", "Number of T planes", "num_t"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }

    public static void extractValues(ExpressionVariables variables, ImagePlus imagePlus) {
        variables.set("width", imagePlus.getWidth());
        variables.set("height", imagePlus.getHeight());
        variables.set("num_c", imagePlus.getNChannels());
        variables.set("num_z", imagePlus.getNSlices());
        variables.set("num_t", imagePlus.getNFrames());
    }
}

