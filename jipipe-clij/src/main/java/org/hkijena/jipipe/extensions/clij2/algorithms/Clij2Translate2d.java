package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Translate2D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Translate2D}
 */
@JIPipeDocumentation(name = "CLIJ2 Translate 2D", description = "Translate an image stack in X and Y. Works for following image dimensions: 2D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2Translate2d extends JIPipeSimpleIteratingAlgorithm {
    float translateX;
    float translateY;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Translate2d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Translate2d(Clij2Translate2d other) {
        super(other);
        this.translateX = other.translateX;
        this.translateY = other.translateY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer output = clij2.create(input);
        Translate2D.translate2D(clij2, input, output, translateX, translateY);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output), progressInfo);
    }

    @JIPipeParameter("translate-x")
    public float getTranslateX() {
        return translateX;
    }

    @JIPipeParameter("translate-x")
    public void setTranslateX(float value) {
        this.translateX = value;
    }

    @JIPipeParameter("translate-y")
    public float getTranslateY() {
        return translateY;
    }

    @JIPipeParameter("translate-y")
    public void setTranslateY(float value) {
        this.translateY = value;
    }

}