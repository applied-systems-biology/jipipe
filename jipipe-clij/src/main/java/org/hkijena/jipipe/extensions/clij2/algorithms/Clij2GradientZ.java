package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GradientZ;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GradientZ}
 */
@JIPipeDocumentation(name = "CLIJ2 Gradient Z", description = "Computes the gradient of gray values along Z. " + "Assuming a, b and c are three adjacent" + " pixels in Z direction. In the target image will be saved as: <pre>b' = c - a;</pre> Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Gradient")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2GradientZ extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2GradientZ(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GradientZ(Clij2GradientZ other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        GradientZ.gradientZ(clij2, src, dst);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

}