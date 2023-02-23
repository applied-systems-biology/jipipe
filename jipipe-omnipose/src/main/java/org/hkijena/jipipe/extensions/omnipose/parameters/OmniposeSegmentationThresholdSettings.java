package org.hkijena.jipipe.extensions.omnipose.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;

public class OmniposeSegmentationThresholdSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private double flowThreshold = 0.4;
    private double maskThreshold = 0;

    private double diamThreshold = 12;
    private OptionalDoubleParameter stitchThreshold = new OptionalDoubleParameter(0, false);
    private boolean excludeOnEdges = false;

    public OmniposeSegmentationThresholdSettings() {
    }

    public OmniposeSegmentationThresholdSettings(OmniposeSegmentationThresholdSettings other) {
        this.flowThreshold = other.flowThreshold;
        this.maskThreshold = other.maskThreshold;
        this.stitchThreshold = other.stitchThreshold;
        this.excludeOnEdges = other.excludeOnEdges;
        this.diamThreshold = other.diamThreshold;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Exclude masks on edges", description = "Discard masks which touch edges of image")
    @JIPipeParameter("exclude-on-edges")
    public boolean isExcludeOnEdges() {
        return excludeOnEdges;
    }

    @JIPipeParameter("exclude-on-edges")
    public void setExcludeOnEdges(boolean excludeOnEdges) {
        this.excludeOnEdges = excludeOnEdges;
    }

    @JIPipeDocumentation(name = "Flow threshold (2D)", description = "Flow error threshold, 0 turns off this optional QC step. Default: 0.4")
    @JIPipeParameter("flow-threshold")
    public double getFlowThreshold() {
        return flowThreshold;
    }

    @JIPipeParameter("flow-threshold")
    public void setFlowThreshold(double flowThreshold) {
        this.flowThreshold = flowThreshold;
    }

    @JIPipeDocumentation(name = "Mask threshold", description = "Mask threshold, default is 0, decrease to find more and larger masks")
    @JIPipeParameter("mask-threshold")
    public double getMaskThreshold() {
        return maskThreshold;
    }

    @JIPipeParameter("mask-threshold")
    public void setMaskThreshold(double maskThreshold) {
        this.maskThreshold = maskThreshold;
    }

    @JIPipeDocumentation(name = "Cell diameter threshold", description = "Cell diameter threshold for upscaling before mask reconstruction, default 12.")
    @JIPipeParameter("diam-threshold")
    public double getDiamThreshold() {
        return diamThreshold;
    }

    @JIPipeParameter("diam-threshold")
    public void setDiamThreshold(double diamThreshold) {
        this.diamThreshold = diamThreshold;
    }

    @JIPipeDocumentation(name = "Stitch threshold", description = "Compute masks in 2D then stitch together masks with IoU>0.9 across planes")
    @JIPipeParameter("stitch-threshold")
    public OptionalDoubleParameter getStitchThreshold() {
        return stitchThreshold;
    }

    @JIPipeParameter("stitch-threshold")
    public void setStitchThreshold(OptionalDoubleParameter stitchThreshold) {
        this.stitchThreshold = stitchThreshold;
    }
}
