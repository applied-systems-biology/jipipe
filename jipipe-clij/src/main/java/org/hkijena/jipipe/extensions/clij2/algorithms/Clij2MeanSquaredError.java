package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.MeanSquaredError;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.MeanSquaredError}
 */
@JIPipeDocumentation(name = "CLIJ2 Mean Squared Error", description = "Determines the mean squared error (MSE) between two images. " + "The MSE will be stored in a new row of ImageJs" + "Results table in the column 'MSE'. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Math\nCalculate")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "buffer1", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "buffer2", autoCreate = true)

public class Clij2MeanSquaredError extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2MeanSquaredError(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2MeanSquaredError(Clij2MeanSquaredError other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer buffer1 = dataInterface.getInputData(getInputSlot("buffer1"), CLIJImageData.class).getImage();
        ClearCLBuffer buffer2 = clij2.create(buffer1);
        MeanSquaredError.meanSquaredError(clij2, buffer1, buffer2);

        dataInterface.addOutputData(getOutputSlot("buffer2"), new CLIJImageData(buffer2));
    }

}