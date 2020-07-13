package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.GaussianBlur3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.GaussianBlur3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Gaussian Blur 3D", description = "Computes the Gaussian blurred image of an image given two sigma values in X, Y and Z. " + "Thus, the filterkernel can have non-isotropic shape." + "The implementation is done separable. In case a sigma equals zero, the direction is not blurred. Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Blur")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2GaussianBlur3d extends JIPipeSimpleIteratingAlgorithm {
    Float blurSigmaX;
    Float blurSigmaY;
    Float blurSigmaZ;


    /**
     * Creates a new instance
     *
     * @param declaration The algorithm declaration
     */
    public Clij2GaussianBlur3d(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2GaussianBlur3d(Clij2GaussianBlur3d other) {
        super(other);
        this.blurSigmaX = other.blurSigmaX;
        this.blurSigmaY = other.blurSigmaY;
        this.blurSigmaZ = other.blurSigmaZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        GaussianBlur3D.gaussianBlur(clij2, src, dst, blurSigmaX, blurSigmaY, blurSigmaZ);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("blur-sigma-x")
    public Float getBlurSigmaX() {
        return blurSigmaX;
    }

    @JIPipeParameter("blur-sigma-x")
    public void setBlurSigmaX(Float value) {
        this.blurSigmaX = value;
    }

    @JIPipeParameter("blur-sigma-y")
    public Float getBlurSigmaY() {
        return blurSigmaY;
    }

    @JIPipeParameter("blur-sigma-y")
    public void setBlurSigmaY(Float value) {
        this.blurSigmaY = value;
    }

    @JIPipeParameter("blur-sigma-z")
    public Float getBlurSigmaZ() {
        return blurSigmaZ;
    }

    @JIPipeParameter("blur-sigma-z")
    public void setBlurSigmaZ(Float value) {
        this.blurSigmaZ = value;
    }

}