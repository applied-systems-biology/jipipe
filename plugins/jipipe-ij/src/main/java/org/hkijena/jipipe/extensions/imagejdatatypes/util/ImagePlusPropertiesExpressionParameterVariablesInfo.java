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

import ij.ImagePlus;
import ij.measure.Calibration;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that contains image statistics
 */
public class ImagePlusPropertiesExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {

    private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_c", "Number of channels", "Number of channel planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_z", "Number of Z slices", "Number of Z planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_t", "Number of frames", "Number of T planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("physical_dimension_x", "Physical dimension (X)", "Physical dimension in format '[value] [unit]'"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("physical_dimension_y", "Physical dimension (Y)", "Physical dimension in format '[value] [unit]'"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("physical_dimension_z", "Physical dimension (Z)", "Physical dimension in format '[value] [unit]'"));
    }

    public static void extractValues(JIPipeExpressionVariablesMap variables, ImagePlus imagePlus, Collection<JIPipeTextAnnotation> annotations) {
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

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}

