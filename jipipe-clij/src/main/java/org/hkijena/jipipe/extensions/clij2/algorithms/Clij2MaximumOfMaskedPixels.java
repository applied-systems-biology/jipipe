package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MaximumOfMaskedPixels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MaximumOfMaskedPixels}
 */
@JIPipeDocumentation(name = "CLIJ2 Maximum Of Masked Pixels", description = "Determines the maximum intensity in an image, but only in pixels which have non-zero values in another mask image. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "clImage", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "mask", autoCreate = true)

public class Clij2MaximumOfMaskedPixels extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MaximumOfMaskedPixels(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MaximumOfMaskedPixels(Clij2MaximumOfMaskedPixels other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer clImage = dataBatch.getInputData(getInputSlot("clImage"), CLIJImageData.class).getImage();
        ClearCLBuffer mask = clij2.create(clImage);
        MaximumOfMaskedPixels.maximumOfMaskedPixels(clij2, clImage, mask);

        dataBatch.addOutputData(getOutputSlot("mask"), new CLIJImageData(mask));
    }

}