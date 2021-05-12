/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.parameters;

import ij.ImagePlus;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;

import java.util.HashSet;
import java.util.Set;

public class ImageQueryExpressionVariableSource implements ExpressionParameterVariableSource {
    private final static Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
        VARIABLES.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
        VARIABLES.add(new ExpressionParameterVariable("Image type", "The type of the image. Valid values are 'GRAY8', 'GRAY16', 'GRAY32', 'COLOR_256', and 'COLOR_RGB'", "type"));
        VARIABLES.add(new ExpressionParameterVariable("Image depth", "The depth (number of slices) of the image", "depth"));
        VARIABLES.add(new ExpressionParameterVariable("Image channels", "The channels (number of channel slices) of the image", "num_channels"));
        VARIABLES.add(new ExpressionParameterVariable("Image frames", "The frames of the image", "num_frames"));
        VARIABLES.add(new ExpressionParameterVariable("Image title", "The title of the image", "title"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }

    /**
     * Builds the proper variable set for an image
     *
     * @param imagePlus   the image
     * @param variableSet the target
     */
    public static void buildVariablesSet(ImagePlus imagePlus, ExpressionParameters variableSet) {
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
}
