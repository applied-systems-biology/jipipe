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

package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that contains basic information about an image
 */
public class Image5DExpressionParameterVariablesInfo implements JIPipeExpressionVariablesInfo {

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

    public static void writeToVariables(ImagePlus img, JIPipeExpressionVariablesMap variables) {
        variables.put("width", img.getWidth());
        variables.put("height", img.getHeight());
        variables.put("num_c", img.getNChannels());
        variables.put("num_z", img.getNSlices());
        variables.put("num_t", img.getNFrames());
        variables.put("num_d", img.getNDimensions());
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}

