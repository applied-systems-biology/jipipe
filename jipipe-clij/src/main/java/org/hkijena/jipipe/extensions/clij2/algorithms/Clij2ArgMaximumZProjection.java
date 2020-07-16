package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ArgMaximumZProjection;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ArgMaximumZProjection}
 */
@JIPipeDocumentation(name = "CLIJ2 Arg Maximum Z Projection", description = "Determines the maximum projection of an image stack along Z." + "Furthermore, another 2D image is generated with pixels containing the z-index where the maximum was found (zero based). Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Dimensions")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_max", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_arg", autoCreate = true)

public class Clij2ArgMaximumZProjection extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2ArgMaximumZProjection(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ArgMaximumZProjection(Clij2ArgMaximumZProjection other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_max = clij2.create(src);
        ClearCLBuffer dst_arg = clij2.create(src);
        ArgMaximumZProjection.argMaximumZProjection(clij2, src, dst_max, dst_arg);

        dataBatch.addOutputData(getOutputSlot("dst_max"), new CLIJImageData(dst_max));
        dataBatch.addOutputData(getOutputSlot("dst_arg"), new CLIJImageData(dst_arg));
    }

}