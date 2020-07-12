package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.UndefinedToZero;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.UndefinedToZero}
 */
@JIPipeDocumentation(name = "CLIJ2 Undefined To Zero", description = "Copies all pixels instead those which are not a number (NaN) or infinity (inf), which are replaced by 0. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Math\nReplace")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2UndefinedToZero extends JIPipeSimpleIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2UndefinedToZero(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2UndefinedToZero(Clij2UndefinedToZero other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        UndefinedToZero.undefinedToZero(clij2, src, dst);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

}