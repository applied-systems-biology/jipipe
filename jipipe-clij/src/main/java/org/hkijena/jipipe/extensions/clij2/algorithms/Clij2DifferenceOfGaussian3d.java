package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.DifferenceOfGaussian3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.DifferenceOfGaussian3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Difference Of Gaussian 3D", description = "Applies Gaussian blur to the input image twice with different sigma values resulting in two images which are then subtracted from each other." + "It is recommended to apply this operation to images of type float (32 bit) as results might be negative. Works for following image dimensions: 3D.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2DifferenceOfGaussian3d extends JIPipeSimpleIteratingAlgorithm {
    float sigma1x;
    float sigma1y;
    float sigma1z;
    float sigma2x;
    float sigma2y;
    float sigma2z;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2DifferenceOfGaussian3d(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2DifferenceOfGaussian3d(Clij2DifferenceOfGaussian3d other) {
        super(other);
        this.sigma1x = other.sigma1x;
        this.sigma1y = other.sigma1y;
        this.sigma1z = other.sigma1z;
        this.sigma2x = other.sigma2x;
        this.sigma2y = other.sigma2y;
        this.sigma2z = other.sigma2z;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class, progressInfo).getImage();
        ClearCLBuffer output = clij2.create(input);
        DifferenceOfGaussian3D.differenceOfGaussian(clij2, input, output, sigma1x, sigma1y, sigma1z, sigma2x, sigma2y, sigma2z);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output), progressInfo);
    }

    @JIPipeParameter("sigma1x")
    public float getSigma1x() {
        return sigma1x;
    }

    @JIPipeParameter("sigma1x")
    public void setSigma1x(float value) {
        this.sigma1x = value;
    }

    @JIPipeParameter("sigma1y")
    public float getSigma1y() {
        return sigma1y;
    }

    @JIPipeParameter("sigma1y")
    public void setSigma1y(float value) {
        this.sigma1y = value;
    }

    @JIPipeParameter("sigma1z")
    public float getSigma1z() {
        return sigma1z;
    }

    @JIPipeParameter("sigma1z")
    public void setSigma1z(float value) {
        this.sigma1z = value;
    }

    @JIPipeParameter("sigma2x")
    public float getSigma2x() {
        return sigma2x;
    }

    @JIPipeParameter("sigma2x")
    public void setSigma2x(float value) {
        this.sigma2x = value;
    }

    @JIPipeParameter("sigma2y")
    public float getSigma2y() {
        return sigma2y;
    }

    @JIPipeParameter("sigma2y")
    public void setSigma2y(float value) {
        this.sigma2y = value;
    }

    @JIPipeParameter("sigma2z")
    public float getSigma2z() {
        return sigma2z;
    }

    @JIPipeParameter("sigma2z")
    public void setSigma2z(float value) {
        this.sigma2z = value;
    }

}