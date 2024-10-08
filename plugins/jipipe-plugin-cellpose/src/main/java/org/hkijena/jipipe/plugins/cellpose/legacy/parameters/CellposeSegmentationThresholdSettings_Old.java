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

package org.hkijena.jipipe.plugins.cellpose.legacy.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

@Deprecated
public class CellposeSegmentationThresholdSettings_Old extends AbstractJIPipeParameterCollection {
    private double flowThreshold = 0.4;
    private double cellProbabilityThreshold = 0;
    private int minSize = 15;
    private double stitchThreshold = 0;

    public CellposeSegmentationThresholdSettings_Old() {
    }

    public CellposeSegmentationThresholdSettings_Old(CellposeSegmentationThresholdSettings_Old other) {
        this.flowThreshold = other.flowThreshold;
        this.cellProbabilityThreshold = other.cellProbabilityThreshold;
        this.minSize = other.minSize;
        this.stitchThreshold = other.stitchThreshold;
    }

    @SetJIPipeDocumentation(name = "Flow threshold (2D)", description = "Flow error threshold (all cells with errors below threshold are kept) (not used for 3D)")
    @JIPipeParameter("flow-threshold")
    public double getFlowThreshold() {
        return flowThreshold;
    }

    @JIPipeParameter("flow-threshold")
    public void setFlowThreshold(double flowThreshold) {
        this.flowThreshold = flowThreshold;
    }

    @SetJIPipeDocumentation(name = "Cell probability threshold", description = "Cell probability threshold (all pixels with prob above threshold kept for masks)")
    @JIPipeParameter("cell-probability-threshold")
    public double getCellProbabilityThreshold() {
        return cellProbabilityThreshold;
    }

    @JIPipeParameter("cell-probability-threshold")
    public void setCellProbabilityThreshold(double cellProbabilityThreshold) {
        this.cellProbabilityThreshold = cellProbabilityThreshold;
    }

    @SetJIPipeDocumentation(name = "Minimum size", description = "Minimum number of pixels per mask, can turn off with -1")
    @JIPipeParameter("min-size")
    public int getMinSize() {
        return minSize;
    }

    @JIPipeParameter("min-size")
    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    @SetJIPipeDocumentation(name = "Stitch threshold", description = "If stitch_threshold>0.0 and not do_3D and equal image sizes, " +
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
