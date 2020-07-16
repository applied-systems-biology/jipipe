package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.StandardDeviationOfMaskedPixels;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.StandardDeviationOfMaskedPixels}
 */
@JIPipeDocumentation(name = "CLIJ2 Standard Deviation Of Masked Pixels", description = "Determines the standard deviation of all pixels in an image which have non-zero value in a corresponding mask image. " + "The value will be stored in a new row of ImageJs" + "Results table in the column 'Masked_standard_deviation'. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Math\nCalculate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "buffer1", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "mask", autoCreate = true)

public class Clij2StandardDeviationOfMaskedPixels extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2StandardDeviationOfMaskedPixels(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2StandardDeviationOfMaskedPixels(Clij2StandardDeviationOfMaskedPixels other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer buffer1 = dataBatch.getInputData(getInputSlot("buffer1"), CLIJImageData.class).getImage();
        ClearCLBuffer mask = clij2.create(buffer1);
        StandardDeviationOfMaskedPixels.standardDeviationOfMaskedPixels(clij2, buffer1, mask);

        dataBatch.addOutputData(getOutputSlot("mask"), new CLIJImageData(mask));
    }

}