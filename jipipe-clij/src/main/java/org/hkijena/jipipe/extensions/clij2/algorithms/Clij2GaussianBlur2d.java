package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GaussianBlur2D;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GaussianBlur2D}
 */
@JIPipeDocumentation(name = "CLIJ2 Gaussian Blur 2D", description = "Computes the Gaussian blurred image of an image given two sigma values in X and Y. " + "Thus, the filterkernel can have non-isotropic shape." + "The implementation is done separable. In case a sigma equals zero, the direction is not blurred. Works for following image dimensions: 2D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Blur")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2GaussianBlur2d extends JIPipeSimpleIteratingAlgorithm {
    float blurSigmaX;
    float blurSigmaY;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2GaussianBlur2d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GaussianBlur2d(Clij2GaussianBlur2d other) {
        super(other);
        this.blurSigmaX = other.blurSigmaX;
        this.blurSigmaY = other.blurSigmaY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer dst = clij2.create(src);
        GaussianBlur2D.gaussianBlur(clij2, src, dst, blurSigmaX, blurSigmaY);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst), progressInfo);
    }

    @JIPipeParameter("blur-sigma-x")
    public float getBlurSigmaX() {
        return blurSigmaX;
    }

    @JIPipeParameter("blur-sigma-x")
    public void setBlurSigmaX(float value) {
        this.blurSigmaX = value;
    }

    @JIPipeParameter("blur-sigma-y")
    public float getBlurSigmaY() {
        return blurSigmaY;
    }

    @JIPipeParameter("blur-sigma-y")
    public void setBlurSigmaY(float value) {
        this.blurSigmaY = value;
    }

}