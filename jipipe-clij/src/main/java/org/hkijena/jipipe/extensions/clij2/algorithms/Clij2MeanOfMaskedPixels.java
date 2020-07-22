package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MeanOfMaskedPixels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MeanOfMaskedPixels}
 */
@JIPipeDocumentation(name = "CLIJ2 Mean Of Masked Pixels", description = "Determines the mean intensity in a masked image. " + "Only in pixels which have non-zero values in another binary mask image. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ProcessorNodeTypeCategory.class, menuPath = "Mask")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "clImage", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "mask", autoCreate = true)

public class Clij2MeanOfMaskedPixels extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2MeanOfMaskedPixels(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MeanOfMaskedPixels(Clij2MeanOfMaskedPixels other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer clImage = dataBatch.getInputData(getInputSlot("clImage"), CLIJImageData.class).getImage();
        ClearCLBuffer mask = clij2.create(clImage);
        MeanOfMaskedPixels.meanOfMaskedPixels(clij2, clImage, mask);

        dataBatch.addOutputData(getOutputSlot("mask"), new CLIJImageData(mask));
    }

}