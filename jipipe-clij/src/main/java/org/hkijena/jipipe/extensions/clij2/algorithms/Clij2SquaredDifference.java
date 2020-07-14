package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.SquaredDifference;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.SquaredDifference}
 */
@JIPipeDocumentation(name = "CLIJ2 Squared Difference", description = "Determines the squared difference pixel by pixel between two images. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Math\nCalculate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "buffer1", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "buffer2", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "result", autoCreate = true)

public class Clij2SquaredDifference extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2SquaredDifference(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2SquaredDifference(Clij2SquaredDifference other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer buffer1 = dataInterface.getInputData(getInputSlot("buffer1"), CLIJImageData.class).getImage();
        ClearCLBuffer buffer2 = dataInterface.getInputData(getInputSlot("buffer2"), CLIJImageData.class).getImage();
        ClearCLBuffer result = clij2.create(buffer1);
        SquaredDifference.squaredDifference(clij2, buffer1, buffer2, result);

        dataInterface.addOutputData(getOutputSlot("result"), new CLIJImageData(result));
    }

}