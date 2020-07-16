package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.AddImagesWeighted;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.AddImagesWeighted}
 */
@JIPipeDocumentation(name = "CLIJ2 Add Images Weighted", description = "Calculates the sum of pairs of pixels x and y from images X and Y weighted with factors a and b." + "<pre>f(x, y, a, b) = x * a + y * b</pre> Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Math\nCalculate")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src1", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2AddImagesWeighted extends JIPipeIteratingAlgorithm {
    Float factor;
    Float factor1;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2AddImagesWeighted(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2AddImagesWeighted(Clij2AddImagesWeighted other) {
        super(other);
        this.factor = other.factor;
        this.factor1 = other.factor1;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer src1 = dataBatch.getInputData(getInputSlot("src1"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        AddImagesWeighted.addImagesWeighted(clij2, src, src1, dst, factor, factor1);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("factor")
    public Float getFactor() {
        return factor;
    }

    @JIPipeParameter("factor")
    public void setFactor(Float value) {
        this.factor = value;
    }

    @JIPipeParameter("factor1")
    public Float getFactor1() {
        return factor1;
    }

    @JIPipeParameter("factor1")
    public void setFactor1(Float value) {
        this.factor1 = value;
    }

}