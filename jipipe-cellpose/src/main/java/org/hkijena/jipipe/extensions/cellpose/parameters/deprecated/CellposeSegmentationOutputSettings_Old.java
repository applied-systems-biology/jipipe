package org.hkijena.jipipe.extensions.cellpose.parameters.deprecated;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.cellpose.algorithms.CellposeAlgorithm_Old;

/**
 * @deprecated Used by the old {@link CellposeAlgorithm_Old}
 */
public class CellposeSegmentationOutputSettings_Old implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private boolean outputLabels = false;
    private boolean outputFlows = false;
    private boolean outputProbabilities = false;
    private boolean outputStyles = false;
    private boolean outputROI = true;

    public CellposeSegmentationOutputSettings_Old() {
    }

    public CellposeSegmentationOutputSettings_Old(CellposeSegmentationOutputSettings_Old other) {
        this.outputLabels = other.outputLabels;
        this.outputFlows = other.outputFlows;
        this.outputProbabilities = other.outputProbabilities;
        this.outputStyles = other.outputStyles;
        this.outputROI = other.outputROI;
    }

    @JIPipeDocumentation(name = "Output labels", description = "Output an image that contains a unique greyscale value for each detected object.")
    @JIPipeParameter("output-labels")
    public boolean isOutputLabels() {
        return outputLabels;
    }

    @JIPipeParameter("output-labels")
    public void setOutputLabels(boolean outputLabels) {
        this.outputLabels = outputLabels;
    }

    @JIPipeDocumentation(name = "Output flows", description = "Output an HSB image that indicates the flow of each pixel.")
    @JIPipeParameter("output-flows")
    public boolean isOutputFlows() {
        return outputFlows;
    }

    @JIPipeParameter("output-flows")
    public void setOutputFlows(boolean outputFlows) {
        this.outputFlows = outputFlows;
    }

    @JIPipeDocumentation(name = "Output probabilities", description = "Output a greyscale image indicating the probability of each pixel being an object.")
    @JIPipeParameter("output-probabilities")
    public boolean isOutputProbabilities() {
        return outputProbabilities;
    }

    @JIPipeParameter("output-probabilities")
    public void setOutputProbabilities(boolean outputProbabilities) {
        this.outputProbabilities = outputProbabilities;
    }

    @JIPipeDocumentation(name = "Output styles", description = "Output a 1D vector that summarizes the image.")
    @JIPipeParameter("output-styles")
    public boolean isOutputStyles() {
        return outputStyles;
    }

    @JIPipeParameter("output-styles")
    public void setOutputStyles(boolean outputStyles) {
        this.outputStyles = outputStyles;
    }

    @JIPipeDocumentation(name = "Output ROI", description = "Output a ROI list containing all detected objects")
    @JIPipeParameter("output-roi")
    public boolean isOutputROI() {
        return outputROI;
    }

    @JIPipeParameter("output-roi")
    public void setOutputROI(boolean outputROI) {
        this.outputROI = outputROI;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
