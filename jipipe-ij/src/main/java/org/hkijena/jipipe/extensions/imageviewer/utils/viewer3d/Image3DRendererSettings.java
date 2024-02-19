package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;

public class Image3DRendererSettings extends AbstractJIPipeParameterCollection {
    private Image3DRenderType renderType = Image3DRenderType.Volume;
    private OptionalIntegerParameter overrideResamplingFactor = new OptionalIntegerParameter(false, 2);

    private int maximumMemory = 128;

    public Image3DRendererSettings() {
    }

    public Image3DRendererSettings(Image3DRendererSettings other) {
        copyFrom(other);
    }

    public void copyFrom(Image3DRendererSettings other) {
        this.renderType = other.renderType;
        this.maximumMemory = other.maximumMemory;
        this.overrideResamplingFactor = new OptionalIntegerParameter(other.overrideResamplingFactor);
    }

    @SetJIPipeDocumentation(name = "Render type", description = "The way how images are rendered in 3D")
    @JIPipeParameter("render-type")
    public Image3DRenderType getRenderType() {
        return renderType;
    }

    @JIPipeParameter("render-type")
    public void setRenderType(Image3DRenderType renderType) {
        this.renderType = renderType;
    }

    @SetJIPipeDocumentation(name = "Force resampling", description = "If enabled, set the resampling factor manually. Otherwise it is determined by the maximum allocated memory.")
    @JIPipeParameter("override-resampling-factor")
    public OptionalIntegerParameter getOverrideResamplingFactor() {
        return overrideResamplingFactor;
    }

    @JIPipeParameter("override-resampling-factor")
    public void setOverrideResamplingFactor(OptionalIntegerParameter overrideResamplingFactor) {
        this.overrideResamplingFactor = overrideResamplingFactor;
    }

    @SetJIPipeDocumentation(name = "Maximum GPU memory (MB)", description = "The maximum memory allocated to the image display. Determines the resolution factor if not manually overridden. The lowest value is 64.")
    @JIPipeParameter("maximum-memory")
    public int getMaximumMemory() {
        return maximumMemory;
    }

    @JIPipeParameter("maximum-memory")
    public void setMaximumMemory(int maximumMemory) {
        this.maximumMemory = maximumMemory;
    }

    public double getExpectedMemoryAllocationMegabytes(ImagePlus image) {
        return 4.0 * image.getNChannels() * image.getWidth() * image.getHeight() * image.getNSlices() * image.getNFrames() / 1024 / 1024;
    }

    public int getResamplingFactor(ImagePlus image) {
        if (overrideResamplingFactor.isEnabled()) {
            return Math.max(1, overrideResamplingFactor.getContent());
        } else {
            return (int) Math.ceil(getExpectedMemoryAllocationMegabytes(image) / Math.max(64, maximumMemory));
        }
    }
}
