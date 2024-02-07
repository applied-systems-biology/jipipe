package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that contains image statistics
 */
public class ImageStatistics5DExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {

    private final static Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
        VARIABLES.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
        VARIABLES.add(new ExpressionParameterVariable("Z coordinates", "The Z coordinate within the image (first index is zero)", "z"));
        VARIABLES.add(new ExpressionParameterVariable("Channel coordinates", "The channel (C) coordinate within the image (first index is zero)", "c"));
        VARIABLES.add(new ExpressionParameterVariable("Frame coordinates", "The frame (T) coordinate within the image (first index is zero)", "t"));
        VARIABLES.add(new ExpressionParameterVariable("Number of channels", "Number of channel planes", "num_c"));
        VARIABLES.add(new ExpressionParameterVariable("Number of Z slices", "Number of Z planes", "num_z"));
        VARIABLES.add(new ExpressionParameterVariable("Number of frames", "Number of T planes", "num_t"));
        VARIABLES.add(new ExpressionParameterVariable("Pixels", "Array of all pixel values that are part of the statistics", "pixels"));

        VARIABLES.add(new ExpressionParameterVariable("Area", "Area of selection in square pixels.", "stat_area"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value standard deviation", "Measures the standard deviation of greyscale pixel values", "stat_stdev"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value min", "Measures the minimum of greyscale pixel values", "stat_min"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value max", "Measures the maximum of greyscale pixel values", "stat_max"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value mean", "Measures the mean of greyscale pixel values", "stat_mean"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value mode", "Most frequently occurring gray value within the selection", "stat_mode"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value median", "The median value of the pixels in the image or selection", "stat_median"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value kurtosis", "The fourth order moment about the greyscale pixel value mean", "stat_kurtosis"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value integrated density", "The product of Area and Mean Gray Value", "stat_int_den"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value raw integrated density", "The sum of the values of the pixels in the image or selection", "stat_raw_int_den"));
        VARIABLES.add(new ExpressionParameterVariable("Pixel value skewness", "The sum of the values of the pixels in the image or selection", "stat_skewness"));
        VARIABLES.add(new ExpressionParameterVariable("Area fraction", "The percentage of non-zero pixels", "stat_area_fraction"));

        VARIABLES.add(new ExpressionParameterVariable("Histogram",
                "An array the represents the histogram (index is the pixel value, value is the number of pixels with this value) of the currently analyzed area",
                "stat_histogram"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}

