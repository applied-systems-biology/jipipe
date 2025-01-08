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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashSet;
import java.util.Set;

public class AlignedImage5DSliceIndexExpressionParameterVariablesInfo implements JIPipeExpressionVariablesInfo {
    public static void apply(JIPipeExpressionVariablesMap target, ImagePlus reference, ImagePlus img, ImageSliceIndex index) {
        target.set("reference.width", img.getWidth());
        target.set("reference.height", img.getHeight());
        target.set("reference.size_d", img.getNDimensions());
        target.set("reference.size_c", img.getNChannels());
        target.set("reference.size_z", img.getNSlices());
        target.set("reference.size_t", img.getNFrames());
        target.set("width", img.getWidth());
        target.set("height", img.getHeight());
        target.set("size_d", img.getNDimensions());
        target.set("size_c", img.getNChannels());
        target.set("size_z", img.getNSlices());
        target.set("size_t", img.getNFrames());
        target.set("c", index.getC());
        target.set("z", index.getZ());
        target.set("t", index.getT());
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
        result.add(new JIPipeExpressionParameterVariableInfo("reference.width", "Reference image width", "The width of the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("reference.height", "Reference image height", "The height of the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("reference.size_z", "Reference image Z slices", "The number of Z slices in the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("reference.size_c", "Reference image channels", "The number of channel (C) slices in the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("reference.size_t", "Reference image frames", "The number of frames (T) in the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("reference.size_d", "Reference image number of dimensions", "The number dimensions of the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("size_z", "Image Z slices", "The number of Z slices in the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("size_c", "Image channels", "The number of channel (C) slices in the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("size_t", "Image frames", "The number of frames (T) in the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("size_d", "Image number of dimensions", "The number dimensions of the image"));
        result.add(new JIPipeExpressionParameterVariableInfo("z", "Z coordinate", "The Z coordinate within the image (first index is zero)"));
        result.add(new JIPipeExpressionParameterVariableInfo("c", "Channel coordinate", "The channel (C) coordinate within the image (first index is zero)"));
        result.add(new JIPipeExpressionParameterVariableInfo("t", "Frame coordinate", "The frame (T) coordinate within the image (first index is zero)"));
        return result;
    }
}
