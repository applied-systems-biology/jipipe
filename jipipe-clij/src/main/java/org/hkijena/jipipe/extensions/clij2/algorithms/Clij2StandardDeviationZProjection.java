package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.StandardDeviationZProjection;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.StandardDeviationZProjection}
 */
@JIPipeDocumentation(name = "CLIJ2 Standard Deviation Z Projection", description = "Determines the standard deviation intensity projection of an image stack along Z. Works for following image dimensions: 3D -> 2D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2StandardDeviationZProjection extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2StandardDeviationZProjection(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2StandardDeviationZProjection(Clij2StandardDeviationZProjection other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataInterface.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        StandardDeviationZProjection.standardDeviationZProjection(clij2, input, output);

        dataInterface.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

}