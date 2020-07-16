package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.DepthColorProjection;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.DepthColorProjection}
 */
@JIPipeDocumentation(name = "CLIJ2 Depth Color Projection", description = "Determines a maximum projection of an image stack and does a color coding of the determined arg Z (position of the found maximum). " + "Second parameter is a Lookup-Table in the form of an 8-bit image stack 255 pixels wide, 1 pixel high with 3 planes representing red, green and blue intensities." + "Resulting image is a 3D image with three Z-planes representing red, green and blue channels. Works for following image dimensions: 3D.")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Dimensions")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "lut", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst_depth", autoCreate = true)

public class Clij2DepthColorProjection extends JIPipeIteratingAlgorithm {
    Float min_display_intensity;
    Float max_display_intensity;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2DepthColorProjection(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2DepthColorProjection(Clij2DepthColorProjection other) {
        super(other);
        this.min_display_intensity = other.min_display_intensity;
        this.max_display_intensity = other.max_display_intensity;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer lut = dataBatch.getInputData(getInputSlot("lut"), CLIJImageData.class).getImage();
        ClearCLBuffer dst_depth = clij2.create(src);
        DepthColorProjection.depthColorProjection(clij2, src, lut, dst_depth, min_display_intensity, max_display_intensity);

        dataBatch.addOutputData(getOutputSlot("dst_depth"), new CLIJImageData(dst_depth));
    }

    @JIPipeParameter("min-display-intensity")
    public Float getMin_display_intensity() {
        return min_display_intensity;
    }

    @JIPipeParameter("min-display-intensity")
    public void setMin_display_intensity(Float value) {
        this.min_display_intensity = value;
    }

    @JIPipeParameter("max-display-intensity")
    public Float getMax_display_intensity() {
        return max_display_intensity;
    }

    @JIPipeParameter("max-display-intensity")
    public void setMax_display_intensity(Float value) {
        this.max_display_intensity = value;
    }

}