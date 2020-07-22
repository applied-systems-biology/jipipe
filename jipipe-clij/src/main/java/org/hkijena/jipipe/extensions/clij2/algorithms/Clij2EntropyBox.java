package org.hkijena.jipipe.extensions.clij2.algorithms;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.EntropyBox;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CLIJ2 algorithm ported from {@link net.haesleinhuepf.clij2.plugins.EntropyBox}
 */
@JIPipeDocumentation(name = "CLIJ2 Entropy Box", description = "Determines the local entropy in a box with a given radius around every pixel. Works for following image dimensions: 2D, 3D. Developed by Pit Kludig and Robert Haase.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Math\nCalculate\nEntropy")
@JIPipeInputSlot(value = CLIJImageData.class, slotName = "src", autoCreate = true)
@JIPipeOutputSlot(value = CLIJImageData.class, slotName = "dst", autoCreate = true)

public class Clij2EntropyBox extends JIPipeSimpleIteratingAlgorithm {
    Integer radiusX;
    Integer radiusY;
    Integer radiusZ;
    Float minIntensity;
    Float maxIntensity;


    /**
     * Creates a new instance
     *
     * @param info The algorithm info
     */
    public Clij2EntropyBox(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public Clij2EntropyBox(Clij2EntropyBox other) {
        super(other);
        this.radiusX = other.radiusX;
        this.radiusY = other.radiusY;
        this.radiusZ = other.radiusZ;
        this.minIntensity = other.minIntensity;
        this.maxIntensity = other.maxIntensity;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        CLIJ2 clij2 = CLIJ2.getInstance();
        CLIJ clij = clij2.getCLIJ();
        ClearCLBuffer src = dataBatch.getInputData(getInputSlot("src"), CLIJImageData.class).getImage();
        ClearCLBuffer dst = clij2.create(src);
        EntropyBox.entropyBox(clij2, src, dst, radiusX, radiusY, radiusZ, minIntensity, maxIntensity);

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

    @JIPipeParameter("min-intensity")
    public Float getMinIntensity() {
        return minIntensity;
    }

    @JIPipeParameter("min-intensity")
    public void setMinIntensity(Float value) {
        this.minIntensity = value;
    }

    @JIPipeParameter("max-intensity")
    public Float getMaxIntensity() {
        return maxIntensity;
    }

    @JIPipeParameter("max-intensity")
    public void setMaxIntensity(Float value) {
        this.maxIntensity = value;
    }

}