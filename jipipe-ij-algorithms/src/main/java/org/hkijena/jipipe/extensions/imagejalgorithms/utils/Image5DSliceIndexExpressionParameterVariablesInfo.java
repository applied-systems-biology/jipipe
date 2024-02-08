package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import ij.ImagePlus;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashSet;
import java.util.Set;

public class Image5DSliceIndexExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        Set<ExpressionParameterVariable> result = new HashSet<>();
        result.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
        result.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
        result.add(new ExpressionParameterVariable("Image Z slices", "The number of Z slices in the image", "size_z"));
        result.add(new ExpressionParameterVariable("Image channels", "The number of channel (C) slices in the image", "size_c"));
        result.add(new ExpressionParameterVariable("Image frames", "The number of frames (T) in the image", "size_t"));
        result.add(new ExpressionParameterVariable("Image number of dimensions", "The number dimensions of the image", "size_d"));
        result.add(new ExpressionParameterVariable("Z coordinate", "The Z coordinate within the image (first index is zero)", "z"));
        result.add(new ExpressionParameterVariable("Channel coordinate", "The channel (C) coordinate within the image (first index is zero)", "c"));
        result.add(new ExpressionParameterVariable("Frame coordinate", "The frame (T) coordinate within the image (first index is zero)", "t"));
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
