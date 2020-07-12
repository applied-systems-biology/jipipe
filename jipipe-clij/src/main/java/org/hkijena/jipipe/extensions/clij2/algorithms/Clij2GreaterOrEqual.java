package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GreaterOrEqual;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GreaterOrEqual}
 */
@JIPipeDocumentation(name = "CLIJ2 Greater Or Equal", description = "Determines if two images A and B greater or equal pixel wise. " + "f(a, b) = 1 if a >= b; 0 otherwise.  Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src1", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src2", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2GreaterOrEqual extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2GreaterOrEqual(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GreaterOrEqual(Clij2GreaterOrEqual other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src1 = dataInterface.getInputData(getInputSlot("src1"), CLIJImageData.class).getImage();
        ClearCLBuffer src2 = dataInterface.getInputData(getInputSlot("src2"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src1);
        GreaterOrEqual.greaterOrEqual(clij2, src1, src2, dst);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

}