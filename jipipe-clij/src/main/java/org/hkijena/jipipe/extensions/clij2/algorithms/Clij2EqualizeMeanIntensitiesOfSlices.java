package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.EqualizeMeanIntensitiesOfSlices;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.EqualizeMeanIntensitiesOfSlices}
 */
@JIPipeDocumentation(name = "CLIJ2 Equalize Mean Intensities Of Slices", description = "Determines correction factors for each z-slice so that the average intensity in all slices can be made the same and multiplies these factors with the slices. " + "This functionality is similar to the 'Simple Ratio Bleaching Correction' in Fiji. Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2EqualizeMeanIntensitiesOfSlices extends JIPipeSimpleIteratingAlgorithm {
    Integer referenceSlice;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2EqualizeMeanIntensitiesOfSlices(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2EqualizeMeanIntensitiesOfSlices(Clij2EqualizeMeanIntensitiesOfSlices other) {
        super(other);
        this.referenceSlice = other.referenceSlice;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataInterface.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        EqualizeMeanIntensitiesOfSlices.equalizeMeanIntensitiesOfSlices(clij2, input, output, referenceSlice);

        dataInterface.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("reference-slice")
    public Integer getReferenceSlice() {
        return referenceSlice;
    }

    @JIPipeParameter("reference-slice")
    public void setReferenceSlice(Integer value) {
        this.referenceSlice = value;
    }

}