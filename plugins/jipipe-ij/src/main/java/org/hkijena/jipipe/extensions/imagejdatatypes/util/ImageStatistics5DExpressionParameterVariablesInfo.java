/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that contains image statistics
 */
public class ImageStatistics5DExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {

    private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("z", "Z coordinates", "The Z coordinate within the image (first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("c", "Channel coordinates", "The channel (C) coordinate within the image (first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("t", "Frame coordinates", "The frame (T) coordinate within the image (first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_c", "Number of channels", "Number of channel planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_z", "Number of Z slices", "Number of Z planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_t", "Number of frames", "Number of T planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("pixels", "Pixels", "Array of all pixel values that are part of the statistics"));

        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_area", "Area", "Area of selection in square pixels."));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_stdev", "Pixel value standard deviation", "Measures the standard deviation of greyscale pixel values"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_min", "Pixel value min", "Measures the minimum of greyscale pixel values"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_max", "Pixel value max", "Measures the maximum of greyscale pixel values"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_mean", "Pixel value mean", "Measures the mean of greyscale pixel values"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_mode", "Pixel value mode", "Most frequently occurring gray value within the selection"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_median", "Pixel value median", "The median value of the pixels in the image or selection"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_kurtosis", "Pixel value kurtosis", "The fourth order moment about the greyscale pixel value mean"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_int_den", "Pixel value integrated density", "The product of Area and Mean Gray Value"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_raw_int_den", "Pixel value raw integrated density", "The sum of the values of the pixels in the image or selection"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_skewness", "Pixel value skewness", "The sum of the values of the pixels in the image or selection"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_area_fraction", "Area fraction", "The percentage of non-zero pixels"));

        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("stat_histogram", "Histogram",
                "An array the represents the histogram (index is the pixel value, value is the number of pixels with this value) of the currently analyzed area"
        ));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}

