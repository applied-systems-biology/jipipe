package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that contains image statistics
 */
public class ImagePlusPropertiesExpressionParameterVariableSource implements ExpressionParameterVariableSource {

    private final static Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("<Annotations>", "Available annotations are available as variables", ""));
        VARIABLES.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
        VARIABLES.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
        VARIABLES.add(new ExpressionParameterVariable("Number of channels", "Number of channel planes", "num_c"));
        VARIABLES.add(new ExpressionParameterVariable("Number of Z slices", "Number of Z planes", "num_z"));
        VARIABLES.add(new ExpressionParameterVariable("Number of frames", "Number of T planes", "num_t"));
        VARIABLES.add(new ExpressionParameterVariable("Physical dimension (X)", "Physical dimension in format '[value] [unit]'", "physical_dimension_x"));
        VARIABLES.add(new ExpressionParameterVariable("Physical dimension (Y)", "Physical dimension in format '[value] [unit]'", "physical_dimension_y"));
        VARIABLES.add(new ExpressionParameterVariable("Physical dimension (Z)", "Physical dimension in format '[value] [unit]'", "physical_dimension_z"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }

    public static void extractValues(ExpressionVariables variables, ImagePlus imagePlus, Collection<JIPipeTextAnnotation> annotations) {
        for (JIPipeTextAnnotation annotation : annotations) {
            variables.set(annotation.getName(), annotation.getValue());
        }
        variables.set("width", imagePlus.getWidth());
        variables.set("height", imagePlus.getHeight());
        variables.set("num_c", imagePlus.getNChannels());
        variables.set("num_z", imagePlus.getNSlices());
        variables.set("num_t", imagePlus.getNFrames());
        Calibration calibration = imagePlus.getCalibration();
        variables.set("physical_dimension_x", (calibration.getX(1) + " " + calibration.getXUnit()).trim());
        variables.set("physical_dimension_y", (calibration.getY(1) + " " + calibration.getYUnit()).trim());
        variables.set("physical_dimension_z", (calibration.getZ(1) + " " + calibration.getZUnit()).trim());

    }
}

