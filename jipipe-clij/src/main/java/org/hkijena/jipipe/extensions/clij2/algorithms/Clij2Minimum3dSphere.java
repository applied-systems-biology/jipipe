package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Minimum3DSphere;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.Minimum3DSphere}
 */
@JIPipeDocumentation(name = "CLIJ2 Minimum 3D Sphere", description = "Computes the local minimum of a pixels spherical neighborhood. " + "The spheres size is specified by " + "its half-width, half-height and half-depth (radius). Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Math\nLocal")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2Minimum3dSphere extends JIPipeSimpleIteratingAlgorithm {
    Integer radiusX;
    Integer radiusY;
    Integer radiusZ;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2Minimum3dSphere(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2Minimum3dSphere(Clij2Minimum3dSphere other) {
        super(other);
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
        this.radiusZ = other.radiusZ;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        Minimum3DSphere.minimum3DSphere(clij2, src, dst, radiusX, radiusY, radiusZ);

        dataBatch.addOutputData(getOutputSlot("dst"), new CLIJImageData(dst));
    }

    @JIPipeParameter("radius-x")
    public Integer getRadiusX() {
        return radiusX;
    }

    @JIPipeParameter("radius-x")
    public void setRadiusX(Integer value) {
        this.radiusX = value;
    }

    @JIPipeParameter("radius-y")
    public Integer getRadiusY() {
        return radiusY;
    }

    @JIPipeParameter("radius-y")
    public void setRadiusY(Integer value) {
        this.radiusY = value;
    }

    @JIPipeParameter("radius-z")
    public Integer getRadiusZ() {
        return radiusZ;
    }

    @JIPipeParameter("radius-z")
    public void setRadiusZ(Integer value) {
        this.radiusZ = value;
    }

}