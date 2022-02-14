package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Scale2D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Scale2D}
 */
@JIPipeDocumentation(name = "CLIJ2 Scale 2D", description = "Scales an image with a given factor. Works for following image dimensions: 2D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2Scale2d extends JIPipeSimpleIteratingAlgorithm {
    float factorX;
    float factorY;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Scale2d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Scale2d(Clij2Scale2d other) {
        super(other);
        this.factorX = other.factorX;
        this.factorY = other.factorY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer output = clij2.create(input);
        Scale2D.scale2D(clij2, input, output, factorX, factorY);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output), progressInfo);
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

}