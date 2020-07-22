package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Watershed;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Watershed}
 */
@JIPipeDocumentation(name = "CLIJ2 Watershed", description = "Apply a binary watershed to a binary image and introduces black pixels between objects. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Binary")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "thresholded", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2Watershed extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Watershed(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Watershed(Clij2Watershed other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer thresholded = dataBatch.getInputData(getInputSlot("thresholded"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(thresholded);
        Watershed.watershed(clij2, thresholded, output);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

}