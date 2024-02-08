package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import ij.ImagePlus;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashSet;
import java.util.Set;

public class Image5DSliceIndexExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
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

    public static void apply(JIPipeExpressionVariablesMap target, ImagePlus img, ImageSliceIndex index) {
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
}
