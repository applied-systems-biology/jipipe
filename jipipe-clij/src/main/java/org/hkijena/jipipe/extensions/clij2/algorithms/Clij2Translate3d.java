package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Translate3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.JIPipeInputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeOutputSlot;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeInfo;
import org.hkijena.jipipe.api.algorithm.JIPipeDataBatch;
import org.hkijena.jipipe.api.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Translate3D}
 */
@JIPipeDocumentation(name = "CLIJ2 Translate 3D", description = "Translate an image stack in X, Y and Z. Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Transform")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "input", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "output", autoCreate = true)

public class Clij2Translate3d extends JIPipeSimpleIteratingAlgorithm {
    Float translateX;
    Float translateY;
    Float translateZ;


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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer input = dataBatch.getInputData(getInputSlot("input"), CLIJImageData.class).getImage();
        ClearCLBuffer output = clij2.create(input);
        Translate3D.translate3D(clij2, input, output, translateX, translateY, translateZ);

        dataBatch.addOutputData(getOutputSlot("output"), new CLIJImageData(output));
    }

    @JIPipeParameter("translate-x")
    public Float getTranslateX() {
        return translateX;
    }

    @JIPipeParameter("translate-x")
    public void setTranslateX(Float value) {
        this.translateX = value;
    }

    @JIPipeParameter("translate-y")
    public Float getTranslateY() {
        return translateY;
    }

    @JIPipeParameter("translate-y")
    public void setTranslateY(Float value) {
        this.translateY = value;
    }

    @JIPipeParameter("translate-z")
    public Float getTranslateZ() {
        return translateZ;
    }

    @JIPipeParameter("translate-z")
    public void setTranslateZ(Float value) {
        this.translateZ = value;
    }

}