package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.DifferenceOfGaussian2D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.AlgorithmInputSlot;
import org.hkijena.jipipe.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.DifferenceOfGaussian2D}
 */
@JIPipeDocumentation(name = "CLIJ2 Difference Of Gaussian 2D", description = "Applies Gaussian blur to the input image twice with different sigma values resulting in two images which are then subtracted from each other." + "It is recommended to apply this operation to images of type Float (32 bit) as results might be negative. Works for following image dimensions: 2D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Features")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2DifferenceOfGaussian2d extends JIPipeSimpleIteratingAlgorithm {
    Float sigma1x;
    Float sigma1y;
    Float sigma2x;
    Float sigma2y;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2DifferenceOfGaussian2d(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2DifferenceOfGaussian2d(Clij2DifferenceOfGaussian2d other) {
        super(other);
        this.sigma1x = other.sigma1x;
        this.sigma1y = other.sigma1y;
        this.sigma2x = other.sigma2x;
        this.sigma2y = other.sigma2y;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataInterface.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        DifferenceOfGaussian2D.differenceOfGaussian(clij2, input, output, sigma1x, sigma1y, sigma2x, sigma2y);

        dataInterface.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("sigma1x")
    public Float getSigma1x() {
        return sigma1x;
    }

    @JIPipeParameter("sigma1x")
    public void setSigma1x(Float value) {
        this.sigma1x = value;
    }

    @JIPipeParameter("sigma1y")
    public Float getSigma1y() {
        return sigma1y;
    }

    @JIPipeParameter("sigma1y")
    public void setSigma1y(Float value) {
        this.sigma1y = value;
    }

    @JIPipeParameter("sigma2x")
    public Float getSigma2x() {
        return sigma2x;
    }

    @JIPipeParameter("sigma2x")
    public void setSigma2x(Float value) {
        this.sigma2x = value;
    }

    @JIPipeParameter("sigma2y")
    public Float getSigma2y() {
        return sigma2y;
    }

    @JIPipeParameter("sigma2y")
    public void setSigma2y(Float value) {
        this.sigma2y = value;
    }

}