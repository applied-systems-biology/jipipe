package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Scale3D;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Scale3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Scale 3D", description = "Scales an image with a given factor. Works for following image dimensions: 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2Scale3d extends JIPipeSimpleIteratingAlgorithm {
    float factorX;
    float factorY;
    float factorZ;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Scale3d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Scale3d(Clij2Scale3d other) {
        super(other);
        this.factorX = other.factorX;
        this.factorY = other.factorY;
        this.factorZ = other.factorZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnableInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        Scale3D.scale3D(clij2, input, output, factorX, factorY, factorZ);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("factor-x")
    public float getFactorX() {
        return factorX;
    }

    @JIPipeParameter("factor-x")
    public void setFactorX(float value) {
        this.factorX = value;
    }

    @JIPipeParameter("factor-y")
    public float getFactorY() {
        return factorY;
    }

    @JIPipeParameter("factor-y")
    public void setFactorY(float value) {
        this.factorY = value;
    }

    @JIPipeParameter("factor-z")
    public float getFactorZ() {
        return factorZ;
    }

    @JIPipeParameter("factor-z")
    public void setFactorZ(float value) {
        this.factorZ = value;
    }

}