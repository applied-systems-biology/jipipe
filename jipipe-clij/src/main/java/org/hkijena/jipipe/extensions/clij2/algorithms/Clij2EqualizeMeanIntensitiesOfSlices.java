package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.EqualizeMeanIntensitiesOfSlices;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnableInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.EqualizeMeanIntensitiesOfSlices}
 */
@JIPipeDocumentation(name = "CLIJ2 Equalize Mean Intensities Of Slices", description = "Determines correction factors for each z-slice so that the average intensity in all slices can be made the same and multiplies these factors with the slices. " + "This functionality is similar to the 'Simple Ratio Bleaching Correction' in Fiji. Works for following image dimensions: 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2EqualizeMeanIntensitiesOfSlices extends JIPipeSimpleIteratingAlgorithm {
    int referenceSlice;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2EqualizeMeanIntensitiesOfSlices(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        EqualizeMeanIntensitiesOfSlices.equalizeMeanIntensitiesOfSlices(clij2, input, output, referenceSlice);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("reference-slice")
    public int getReferenceSlice() {
        return referenceSlice;
    }

    @JIPipeParameter("reference-slice")
    public void setReferenceSlice(int value) {
        this.referenceSlice = value;
    }

}