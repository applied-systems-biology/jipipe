package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.ResliceRadial;
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
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.ResliceRadial}
 */
@JIPipeDocumentation(name = "CLIJ2 Reslice Radial", description = "Computes a radial projection of an image stack. " + "Starting point for the line is the given point in any " + "X/Y-plane of a given input image stack. Furthermore, radius of the resulting projection must be given and scaling factors in X and Y in case pixels are not isotropic.This operation is similar to ImageJs 'Radial Reslice' method but offers less flexibility. Works for following image dimensions: 2D, 3D.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions\nReslice")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2ResliceRadial extends JIPipeSimpleIteratingAlgorithm {
    float deltaAngle;
    float startAngleDegrees;
    float centerX;
    float centerY;
    float scaleFactorX;
    float scaleFactorY;


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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progress) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        ResliceRadial.resliceRadial(clij2, src, dst, deltaAngle, startAngleDegrees, centerX, centerY, scaleFactorX, scaleFactorY);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("delta-angle")
    public float getDeltaAngle() {
        return deltaAngle;
    }

    @JIPipeParameter("delta-angle")
    public void setDeltaAngle(float value) {
        this.deltaAngle = value;
    }

    @JIPipeParameter("start-angle-degrees")
    public float getStartAngleDegrees() {
        return startAngleDegrees;
    }

    @JIPipeParameter("start-angle-degrees")
    public void setStartAngleDegrees(float value) {
        this.startAngleDegrees = value;
    }

    @JIPipeParameter("center-x")
    public float getCenterX() {
        return centerX;
    }

    @JIPipeParameter("center-x")
    public void setCenterX(float value) {
        this.centerX = value;
    }

    @JIPipeParameter("center-y")
    public float getCenterY() {
        return centerY;
    }

    @JIPipeParameter("center-y")
    public void setCenterY(float value) {
        this.centerY = value;
    }

    @JIPipeParameter("scale-factor-x")
    public float getScaleFactorX() {
        return scaleFactorX;
    }

    @JIPipeParameter("scale-factor-x")
    public void setScaleFactorX(float value) {
        this.scaleFactorX = value;
    }

    @JIPipeParameter("scale-factor-y")
    public float getScaleFactorY() {
        return scaleFactorY;
    }

    @JIPipeParameter("scale-factor-y")
    public void setScaleFactorY(float value) {
        this.scaleFactorY = value;
    }

}