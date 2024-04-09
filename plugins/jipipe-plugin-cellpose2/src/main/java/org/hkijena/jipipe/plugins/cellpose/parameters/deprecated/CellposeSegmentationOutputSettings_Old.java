/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.cellpose.parameters.deprecated;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.cellpose.algorithms.deprecated.CellposeAlgorithm_Old;

/**
 * @deprecated Used by the old {@link CellposeAlgorithm_Old}
 */
@Deprecated
public class CellposeSegmentationOutputSettings_Old extends AbstractJIPipeParameterCollection {
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

    @SetJIPipeDocumentation(name = "Output labels", description = "Output an image that contains a unique greyscale value for each detected object.")
    @JIPipeParameter("output-labels")
    public boolean isOutputLabels() {
        return outputLabels;
    }

    @JIPipeParameter("output-labels")
    public void setOutputLabels(boolean outputLabels) {
        this.outputLabels = outputLabels;
    }

    @SetJIPipeDocumentation(name = "Output flows", description = "Output an HSB image that indicates the flow of each pixel.")
    @JIPipeParameter("output-flows")
    public boolean isOutputFlows() {
        return outputFlows;
    }

    @JIPipeParameter("output-flows")
    public void setOutputFlows(boolean outputFlows) {
        this.outputFlows = outputFlows;
    }

    @SetJIPipeDocumentation(name = "Output probabilities", description = "Output a greyscale image indicating the probability of each pixel being an object.")
    @JIPipeParameter("output-probabilities")
    public boolean isOutputProbabilities() {
        return outputProbabilities;
    }

    @JIPipeParameter("output-probabilities")
    public void setOutputProbabilities(boolean outputProbabilities) {
        this.outputProbabilities = outputProbabilities;
    }

    @SetJIPipeDocumentation(name = "Output styles", description = "Output a 1D vector that summarizes the image.")
    @JIPipeParameter("output-styles")
    public boolean isOutputStyles() {
        return outputStyles;
    }

    @JIPipeParameter("output-styles")
    public void setOutputStyles(boolean outputStyles) {
        this.outputStyles = outputStyles;
    }

    @SetJIPipeDocumentation(name = "Output ROI", description = "Output a ROI list containing all detected objects")
    @JIPipeParameter("output-roi")
    public boolean isOutputROI() {
        return outputROI;
    }

    @JIPipeParameter("output-roi")
    public void setOutputROI(boolean outputROI) {
        this.outputROI = outputROI;
    }
}
