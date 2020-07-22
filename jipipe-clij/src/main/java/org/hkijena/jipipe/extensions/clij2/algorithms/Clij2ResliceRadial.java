package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ResliceRadial;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ResliceRadial}
 */
@JIPipeDocumentation(name = "CLIJ2 Reslice Radial", description = "Computes a radial projection of an image stack. " + "Starting point for the line is the given point in any " + "X/Y-plane of a given input image stack. Furthermore, radius of the resulting projection must be given and scaling factors in X and Y in case pixels are not isotropic.This operation is similar to ImageJs 'Radial Reslice' method but offers less flexibility. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Dimensions\nReslice")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2ResliceRadial extends JIPipeSimpleIteratingAlgorithm {
    Float deltaAngle;
    Float startAngleDegrees;
    Float centerX;
    Float centerY;
    Float scaleFactorX;
    Float scaleFactorY;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2ResliceRadial(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2ResliceRadial(Clij2ResliceRadial other) {
        super(other);
        this.deltaAngle = other.deltaAngle;
        this.startAngleDegrees = other.startAngleDegrees;
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.scaleFactorX = other.scaleFactorX;
        this.scaleFactorY = other.scaleFactorY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        ResliceRadial.resliceRadial(clij2, src, dst, deltaAngle, startAngleDegrees, centerX, centerY, scaleFactorX, scaleFactorY);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("delta-angle")
    public Float getDeltaAngle() {
        return deltaAngle;
    }

    @JIPipeParameter("delta-angle")
    public void setDeltaAngle(Float value) {
        this.deltaAngle = value;
    }

    @JIPipeParameter("start-angle-degrees")
    public Float getStartAngleDegrees() {
        return startAngleDegrees;
    }

    @JIPipeParameter("start-angle-degrees")
    public void setStartAngleDegrees(Float value) {
        this.startAngleDegrees = value;
    }

    @JIPipeParameter("center-x")
    public Float getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(Float value) {
        this.centerX = value;
    }

    @JIPipeParameter("center-y")
    public Float getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(Float value) {
        this.centerY = value;
    }

    @JIPipeParameter("scale-factor-x")
    public Float getScaleFactorX() {
        return scaleFactorX;
    }

    @JIPipeParameter("scale-factor-x")
    public void setScaleFactorX(Float value) {
        this.scaleFactorX = value;
    }

    @JIPipeParameter("scale-factor-y")
    public Float getScaleFactorY() {
        return scaleFactorY;
    }

    @JIPipeParameter("scale-factor-y")
    public void setScaleFactorY(Float value) {
        this.scaleFactorY = value;
    }

}