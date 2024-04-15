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

@SetJIPipeDocumentation(name = "Generate Gabor Kernel (OpenCV)", description = "Returns Gabor filter coefficients.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = OpenCvImageData.class, slotName = "Output", create = true)
public class GaborKernelGenerator extends JIPipeSimpleIteratingAlgorithm {
    private int width = 31;
    private int height = 31;
    private double sigma = 5;
    private double theta = 0;
    private double lambd = 10;
    private double gamma = 0.5;
    private double psi = Math.PI * 0.5;

    public GaborKernelGenerator(JIPipeNodeInfo info) {
        super(info);
    }

    public GaborKernelGenerator(GaborKernelGenerator other) {
        super(other);
        this.width = other.width;
        this.height = other.height;
        this.sigma = other.sigma;
        this.theta = other.theta;
        this.lambd = other.lambd;
        this.gamma = other.gamma;
        this.psi = other.psi;
    }

    @SetJIPipeDocumentation(name = "Width", description = "Size of the filter returned.")
    @JIPipeParameter(value = "width", uiOrder = -100)
    public int getWidth() {
        return width;
    }

    @JIPipeParameter("width")
    public void setWidth(int width) {
        this.width = width;
    }

    @SetJIPipeDocumentation(name = "Height", description = "Size of the filter returned.")
    @JIPipeParameter(value = "height", uiOrder = -99)
    public int getHeight() {
        return height;
    }

    @JIPipeParameter("height")
    public void setHeight(int height) {
        this.height = height;
    }

    @SetJIPipeDocumentation(name = "Sigma", description = "Standard deviation of the gaussian envelope.")
    @JIPipeParameter("sigma")
    public double getSigma() {
        return sigma;
    }

    @JIPipeParameter("sigma")
    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    @SetJIPipeDocumentation(name = "Theta", description = "Orientation of the normal to the parallel stripes of a Gabor function.")
    @JIPipeParameter("theta")
    public double getTheta() {
        return theta;
    }

    @JIPipeParameter("theta")
    public void setTheta(double theta) {
        this.theta = theta;
    }

    @SetJIPipeDocumentation(name = "Lambd", description = "Wavelength of the sinusoidal factor.")
    @JIPipeParameter("lambd")
    public double getLambd() {
        return lambd;
    }

    @JIPipeParameter("lambd")
    public void setLambd(double lambd) {
        this.lambd = lambd;
    }

    @SetJIPipeDocumentation(name = "Gamma", description = "Spatial aspect ratio.")
    @JIPipeParameter("gamma")
    public double getGamma() {
        return gamma;
    }

    @JIPipeParameter("gamma")
    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    @SetJIPipeDocumentation(name = "Psi", description = "Phase offset.")
    @JIPipeParameter("psi")
    public double getPsi() {
        return psi;
    }

    @JIPipeParameter("psi")
    public void setPsi(double psi) {
        this.psi = psi;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        opencv_core.Mat gaborKernel = opencv_imgproc.getGaborKernel(new opencv_core.Size(width, height), sigma, theta, lambd, gamma, psi, OpenCvDepth.CV_32F.getNativeValue());
        iterationStep.addOutputData(getFirstOutputSlot(), new OpenCvImageData(gaborKernel), progressInfo);
    }
}
