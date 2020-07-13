package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ApplyVectorField3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ApplyVectorField3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Apply Vector Field 3D", description = "Deforms an image stack according to distances provided in the given vector image stacks." + "It is recommended to use 32-bit image stacks for input, output and vector image stacks.  Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Deform")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "vectorX", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "vectorY", autoCreate = true)
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "vectorZ", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2ApplyVectorField3d extends JIPipeIteratingAlgorithm {


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2ApplyVectorField3d(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ApplyVectorField3d(Clij2ApplyVectorField3d other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer vectorX = dataInterface.getInputData(getInputSlot("vectorX"), CLIJImageData.class).getImage();
        ClearCLBuffer vectorY = dataInterface.getInputData(getInputSlot("vectorY"), CLIJImageData.class).getImage();
        ClearCLBuffer vectorZ = dataInterface.getInputData(getInputSlot("vectorZ"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        ApplyVectorField3D.applyVectorField(clij2, src, vectorX, vectorY, vectorZ, dst);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

}