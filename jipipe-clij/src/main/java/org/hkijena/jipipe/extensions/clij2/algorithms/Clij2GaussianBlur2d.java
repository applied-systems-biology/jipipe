package org.hkijena.jipipe.extensions.clij2.algorithms;

import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.haesleinhuepf.clij2.plugins.GaussianBlur2D;

@JIPipeDocumentation(name = "CLIJ2 Gaussian Blur 2D", description="")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "CLIJ2")
@AlgorithmInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@AlgorithmOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2GaussianBlur2d extends JIPipeSimpleIteratingAlgorithm {
    float blurSigmaX;
    float blurSigmaY;


    public Clij2GaussianBlur2d(JIPipeAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public Clij2GaussianBlur2d(Clij2GaussianBlur2d other) {
        super(other);
        this.blurSigmaX = other.blurSigmaX;
        this.blurSigmaY = other.blurSigmaY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        ClearCLBuffer src = dataInterface.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        GaussianBlur2D.gaussianBlur(clij2, src, dst, blurSigmaX, blurSigmaY);

        dataInterface.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
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