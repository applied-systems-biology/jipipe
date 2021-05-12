package org.hkijena.jipipe.extensions.cellpose.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

public class ThresholdParameters implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private double flowThreshold = 0.4;
    private double cellProbabilityThreshold = 0;
    private int minSize = 15;
    private double stitchThreshold = 0;

    public ThresholdParameters() {
    }

    public ThresholdParameters(ThresholdParameters other) {
        this.flowThreshold = other.flowThreshold;
        this.cellProbabilityThreshold = other.cellProbabilityThreshold;
        this.minSize = other.minSize;
        this.stitchThreshold = other.stitchThreshold;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Flow threshold (2D)", description = "Flow error threshold (all cells with errors below threshold are kept) (not used for 3D)")
    @JIPipeParameter("flow-threshold")
    public double getFlowThreshold() {
        return flowThreshold;
    }

    @JIPipeParameter("flow-threshold")
    public void setFlowThreshold(double flowThreshold) {
        this.flowThreshold = flowThreshold;
    }

    @JIPipeDocumentation(name = "Cell probability threshold", description = "Cell probability threshold (all pixels with prob above threshold kept for masks)")
    @JIPipeParameter("cell-probability-threshold")
    public double getCellProbabilityThreshold() {
        return cellProbabilityThreshold;
    }

    @JIPipeParameter("cell-probability-threshold")
    public void setCellProbabilityThreshold(double cellProbabilityThreshold) {
        this.cellProbabilityThreshold = cellProbabilityThreshold;
    }

    @JIPipeDocumentation(name = "Minimum size", description = "Minimum number of pixels per mask, can turn off with -1")
    @JIPipeParameter("min-size")
    public int getMinSize() {
        return minSize;
    }

    @JIPipeParameter("min-size")
    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    @JIPipeDocumentation(name = "Stitch threshold", description = "If stitch_threshold>0.0 and not do_3D and equal image sizes, " +
            "masks are stitched in 3D to return volume segmentation")
    @JIPipeParameter("stitch-threshold")
    public double getStitchThreshold() {
        return stitchThreshold;
    }

    @JIPipeParameter("stitch-threshold")
    public void setStitchThreshold(double stitchThreshold) {
        this.stitchThreshold = stitchThreshold;
    }
}
