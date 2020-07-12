package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ImageToStack;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ImageToStack}
 */
@JIPipeDocumentation(name = "CLIJ2 Image To Stack", description = "Copies a single slice into a stack a given number of times. Works for following image dimensions: 2D -> 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2ImageToStack extends JIPipeSimpleIteratingAlgorithm {
    Integer numSlices;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2ImageToStack(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ImageToStack(Clij2ImageToStack other) {
        super(other);
        this.numSlices = other.numSlices;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        ImageToStack.imageToStack(clij2, src, dst, numSlices);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("num-slices")
    public Integer getNumSlices() {
        return numSlices;
    }

    @JIPipeParameter("num-slices")
    public void setNumSlices(Integer value) {
        this.numSlices = value;
    }

}