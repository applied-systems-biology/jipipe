package org.hkijena.jipipe.plugins.opencv.nodes.generators;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.opencv.datatypes.OpenCvImageData;
import org.hkijena.jipipe.plugins.opencv.utils.OpenCvDepth;

@SetJIPipeDocumentation(name = "Generate Gaussian Kernel (OpenCV)", description = "Returns Gaussian filter coefficients. ")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = OpenCvImageData.class, name = "Output", create = true)
public class GaussianKernelGenerator extends JIPipeSimpleIteratingAlgorithm {
    private int size = 31;
    private double sigma = 5;

    public GaussianKernelGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    public GaussianKernelGenerator(GaussianKernelGenerator other) {
        super(other);
        this.size = other.size;
        this.sigma = other.sigma;
    }

    @SetJIPipeDocumentation(name = "Size", description = "Size of the filter returned.")
    @JIPipeParameter(value = "size", uiOrder = -100)
    public int getSize() {
        return size;
    }

    @JIPipeParameter("size")
    public void setSize(int size) {
        this.size = size;
    }

    @SetJIPipeDocumentation(name = "Sigma", description = "Gaussian standard deviation. If it is non-positive, it is computed from the size as sigma = 0.3*((ksize-1)*0.5 - 1) + 0.8. ")
    @JIPipeParameter("sigma")
    public double getSigma() {
        return sigma;
    }

    @JIPipeParameter("sigma")
    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        opencv_core.Mat gaussianKernel = opencv_imgproc.getGaussianKernel(size, sigma, OpenCvDepth.CV_32F.getNativeValue());
        iterationStep.addOutputData(getFirstOutputSlot(), new OpenCvImageData(gaussianKernel), progressInfo);
    }
}
