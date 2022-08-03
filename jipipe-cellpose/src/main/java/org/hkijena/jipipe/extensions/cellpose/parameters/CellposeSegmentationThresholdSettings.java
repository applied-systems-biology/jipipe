package org.hkijena.jipipe.extensions.cellpose.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;

public class CellposeSegmentationThresholdSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private double flowThreshold = 0.4;
    private double cellProbabilityThreshold = 0;
    private OptionalDoubleParameter stitchThreshold = new OptionalDoubleParameter(0, false);
    private boolean excludeOnEdges = false;

    public CellposeSegmentationThresholdSettings() {
    }

    public CellposeSegmentationThresholdSettings(CellposeSegmentationThresholdSettings other) {
        this.flowThreshold = other.flowThreshold;
        this.cellProbabilityThreshold = other.cellProbabilityThreshold;
        this.stitchThreshold = other.stitchThreshold;
        this.excludeOnEdges = other.excludeOnEdges;
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

    @JIPipeDocumentation(name = "Cell probability threshold", description = "Cellprob threshold, default is 0, decrease to find more and larger masks")
    @JIPipeParameter("cell-probability-threshold")
    public double getCellProbabilityThreshold() {
        return cellProbabilityThreshold;
    }

    @JIPipeParameter("cell-probability-threshold")
    public void setCellProbabilityThreshold(double cellProbabilityThreshold) {
        this.cellProbabilityThreshold = cellProbabilityThreshold;
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
