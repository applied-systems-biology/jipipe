package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Translate3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Translate3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Translate 3D", description = "Translate an image stack in X, Y and Z. Works for following image dimensions: 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2Translate3d extends JIPipeSimpleIteratingAlgorithm {
    float translateX;
    float translateY;
    float translateZ;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Translate3d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Translate3d(Clij2Translate3d other) {
        super(other);
        this.translateX = other.translateX;
        this.translateY = other.translateY;
        this.translateZ = other.translateZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        Translate3D.translate3D(clij2, input, output, translateX, translateY, translateZ);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
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

    @JIPipeParameter("translate-z")
    public float getTranslateZ() {
        return translateZ;
    }

    @JIPipeParameter("translate-z")
    public void setTranslateZ(float value) {
        this.translateZ = value;
    }

}