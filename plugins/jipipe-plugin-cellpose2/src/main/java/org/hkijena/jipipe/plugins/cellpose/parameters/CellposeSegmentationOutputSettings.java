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

package org.hkijena.jipipe.plugins.cellpose.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

public class CellposeSegmentationOutputSettings extends AbstractJIPipeParameterCollection {

    private boolean outputLabels = true;
    private boolean outputFlowsXY = false;

    private boolean outputFlowsZ = false;

    private boolean outputFlowsD = false;
    private boolean outputProbabilities = false;
    private boolean outputROI = true;

    public CellposeSegmentationOutputSettings() {
    }

    public CellposeSegmentationOutputSettings(CellposeSegmentationOutputSettings other) {
        this.outputLabels = other.outputLabels;
        this.outputFlowsXY = other.outputFlowsXY;
        this.outputFlowsZ = other.outputFlowsZ;
        this.outputFlowsD = other.outputFlowsD;
        this.outputProbabilities = other.outputProbabilities;
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

    @SetJIPipeDocumentation(name = "Output flows XY", description = "An RGB image that indicates the x and y flow of each pixel")
    @JIPipeParameter("output-flows-xy")
    public boolean isOutputFlowsXY() {
        return outputFlowsXY;
    }

    @JIPipeParameter("output-flows-xy")
    public void setOutputFlowsXY(boolean outputFlowsXY) {
        this.outputFlowsXY = outputFlowsXY;
    }

    @SetJIPipeDocumentation(name = "Output probabilities", description = "An image indicating the cell probabilities for each pixel")
    @JIPipeParameter("output-probabilities")
    public boolean isOutputProbabilities() {
        return outputProbabilities;
    }

    @JIPipeParameter("output-probabilities")
    public void setOutputProbabilities(boolean outputProbabilities) {
        this.outputProbabilities = outputProbabilities;
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

    @SetJIPipeDocumentation(name = "Output flows Z", description = "Flows in Z direction (black for non-3D images)")
    @JIPipeParameter("output-flows-z")
    public boolean isOutputFlowsZ() {
        return outputFlowsZ;
    }

    @JIPipeParameter("output-flows-z")
    public void setOutputFlowsZ(boolean outputFlowsZ) {
        this.outputFlowsZ = outputFlowsZ;
    }

    @SetJIPipeDocumentation(name = "Output flows d", description = "Multi-channel image that contains the flows [dZ, dY, dX, cell probability] (3D images) / [dY, dX, cell probability] (2D images)")
    @JIPipeParameter("output-flows-d")
    public boolean isOutputFlowsD() {
        return outputFlowsD;
    }

    @JIPipeParameter("output-flows-d")
    public void setOutputFlowsD(boolean outputFlowsD) {
        this.outputFlowsD = outputFlowsD;
    }

}
