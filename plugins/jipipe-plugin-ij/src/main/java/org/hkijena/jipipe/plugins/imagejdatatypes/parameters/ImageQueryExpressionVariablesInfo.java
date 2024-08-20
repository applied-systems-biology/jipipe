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

package org.hkijena.jipipe.plugins.imagejdatatypes.parameters;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;

import java.util.HashSet;
import java.util.Set;

public class ImageQueryExpressionVariablesInfo implements JIPipeExpressionVariablesInfo {
    private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("type", "Image type", "The type of the image. Valid values are 'GRAY8', 'GRAY16', 'GRAY32', 'COLOR_256', and 'COLOR_RGB'"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("depth", "Image depth", "The depth (number of slices) of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_channels", "Image channels", "The channels (number of channel slices) of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_frames", "Image frames", "The frames of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("title", "Image title", "The title of the image"));
    }

    /**
     * Builds the proper variable set for an image
     *
     * @param imagePlus   the image
     * @param variableSet the target
     */
    public static void buildVariablesSet(ImagePlus imagePlus, JIPipeExpressionVariablesMap variableSet) {
        variableSet.set("width", imagePlus.getWidth());
        variableSet.set("height", imagePlus.getHeight());
        switch (imagePlus.getType()) {
            case ImagePlus.GRAY8:
                variableSet.set("type", "GRAY8");
                break;
            case ImagePlus.GRAY16:
                variableSet.set("type", "GRAY16");
                break;
            case ImagePlus.GRAY32:
                variableSet.set("type", "GRAY32");
                break;
            case ImagePlus.COLOR_256:
                variableSet.set("type", "COLOR_256");
                break;
            case ImagePlus.COLOR_RGB:
                variableSet.set("type", "COLOR_RGB");
                break;
        }
        variableSet.set("depth", imagePlus.getNSlices());
        variableSet.set("num_channels", imagePlus.getNChannels());
        variableSet.set("num_frames", imagePlus.getNFrames());
        variableSet.set("title", imagePlus.getTitle());
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
