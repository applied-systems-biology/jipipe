package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GetMeanSquaredError;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ProcessorNodeTypeCategory;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GetMeanSquaredError}
 */
@JIPipeDocumentation(name = "CLIJ2 Get Mean Squared Error", description = "Determines the mean squared error (MSE) between two images. " + "The MSE will be stored in the variable mean_squared_error. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ProcessorNodeTypeCategory.class, menuPath = "Math")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "buffer1", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "buffer2", autoCreate = true)

public class Clij2GetMeanSquaredError extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2GetMeanSquaredError(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GetMeanSquaredError(Clij2GetMeanSquaredError other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer buffer1 = dataBatch.getInputData(getInputSlot("buffer1"), CLIJImageData.class).getImage();
        ClearCLBuffer buffer2 = clij2.create(buffer1);
        GetMeanSquaredError.getMeanSquaredError(clij2, buffer1, buffer2);

        dataBatch.addOutputData(getOutputSlot("buffer2"), new CLIJImageData(buffer2));
    }

}