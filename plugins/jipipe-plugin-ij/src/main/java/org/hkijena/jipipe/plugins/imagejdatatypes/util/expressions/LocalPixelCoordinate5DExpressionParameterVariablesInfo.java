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

package org.hkijena.jipipe.plugins.imagejdatatypes.util.expressions;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that allows to address x, y
 */
public class LocalPixelCoordinate5DExpressionParameterVariablesInfo implements JIPipeExpressionVariablesInfo {

    private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("x", "X coordinate", "The X coordinate within the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("y", "Y coordinate", "The Y coordinate within the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("lw", "Local area width", "The width of the local area window"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("lh", "Local area height", "The height of the local area window"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("z", "Z coordinate", "The Z coordinate within the image (first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("c", "Channel coordinate", "The channel (C) coordinate within the image (first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("t", "Frame coordinate", "The frame (T) coordinate within the image (first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_c", "Number of channels", "Number of channel planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_z", "Number of Z slices", "Number of Z planes"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_t", "Number of frames", "Number of T planes"));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}

