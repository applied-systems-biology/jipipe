package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;

public class Image3DRendererSettings extends AbstractJIPipeParameterCollection {
    private Image3DRenderType renderType = Image3DRenderType.Volume;
    private OptionalIntegerParameter overrideResolutionFactor = new OptionalIntegerParameter(false, 2);

    private int maximumMemory = 128;
    public Image3DRendererSettings() {
    }

    public Image3DRendererSettings(Image3DRendererSettings other) {
        copyFrom(other);
    }

    public void copyFrom(Image3DRendererSettings other) {
        this.renderType = other.renderType;
        this.maximumMemory = other.maximumMemory;
        this.overrideResolutionFactor = new OptionalIntegerParameter(other.overrideResolutionFactor);
    }

    @JIPipeDocumentation(name = "Render type", description = "The way how images are rendered in 3D")
    @JIPipeParameter("render-type")
    public Image3DRenderType getRenderType() {
        return renderType;
    }

    @JIPipeParameter("render-type")
    public void setRenderType(Image3DRenderType renderType) {
        this.renderType = renderType;
    }

    @JIPipeDocumentation(name = "Override resolution factor", description = "If enabled, set the resolution factor manually. Otherwise it is determined by the maximum allocated memory.")
    @JIPipeParameter("override-resolution-factor")
    public OptionalIntegerParameter getOverrideResolutionFactor() {
        return overrideResolutionFactor;
    }

    @JIPipeParameter("override-resolution-factor")
    public void setOverrideResolutionFactor(OptionalIntegerParameter overrideResolutionFactor) {
        this.overrideResolutionFactor = overrideResolutionFactor;
    }

    @JIPipeDocumentation(name = "Maximum allocated memory (MB)", description = "The maximum memory allocated to the image display. Determines the resolution factor if not manually overridden. The lowest value is 64.")
    @JIPipeParameter("maximum-memory")
    public int getMaximumMemory() {
        return maximumMemory;
    }

    @JIPipeParameter("maximum-memory")
    public void setMaximumMemory(int maximumMemory) {
        this.maximumMemory = maximumMemory;
    }

    public double getExpectedMemoryAllocationMegabytes(ImagePlus image) {
//        if(renderType == Image3DRenderType.Volume) {
//            return 4.0 * image.getWidth() * image.getHeight() * image.getNSlices() * image.getNFrames() / 1024 / 1024;
//        }
//        else if(renderType == Image3DRenderType.OrthoSlice || renderType == Image3DRenderType.MultiOrthoSlices) {
//            return 4.0 * image.getWidth() * image.getHeight() * 3.0 * image.getNFrames() / 1024 / 1024;
//        }
//        else {
//            return 4.0 * image.getWidth() * image.getHeight() * image.getNFrames() / 1024 / 1024;
//        }
        return 4.0 * image.getWidth() * image.getHeight() * image.getNSlices() * image.getNFrames() / 1024 / 1024;
    }

    public int getResolutionFactor(ImagePlus image) {
        if(overrideResolutionFactor.isEnabled()) {
            return Math.max(1, overrideResolutionFactor.getContent());
        }
        else {
            return (int) Math.ceil(getExpectedMemoryAllocationMegabytes(image) / Math.max(64, maximumMemory));
        }
    }
}
