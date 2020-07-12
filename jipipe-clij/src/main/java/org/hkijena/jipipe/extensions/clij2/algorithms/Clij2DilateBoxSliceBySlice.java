package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.DilateBoxSliceBySlice;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.DilateBoxSliceBySlice}
 */
@JIPipeDocumentation(name = "CLIJ2 Dilate Box Slice By Slice", description = "Computes a binary image with pixel values 0 and 1 containing the binary dilation of a given input image." + "The dilation takes the Moore-neighborhood (8 pixels in 2D and 26 pixels in 3d) into account." + "The pixels in the input image with pixel value not equal to 0 will be interpreted as 1." + "This method is comparable to the 'Dilate' menu in ImageJ in case it is applied to a 2D image. The only" + "difference is that the output image contains values 0 and 1 instead of 0 and 255." + "This filter is applied slice by slice in 2D. Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Morphology\nDilate")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2DilateBoxSliceBySlice extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2DilateBoxSliceBySlice(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2DilateBoxSliceBySlice(Clij2DilateBoxSliceBySlice other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        DilateBoxSliceBySlice.dilateBoxSliceBySlice(clij2, src, dst);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

}