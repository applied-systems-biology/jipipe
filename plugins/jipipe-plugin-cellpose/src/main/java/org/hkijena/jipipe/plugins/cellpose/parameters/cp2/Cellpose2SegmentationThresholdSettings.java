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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp2;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;

public class Cellpose2SegmentationThresholdSettings extends AbstractJIPipeParameterCollection {
    private double flowThreshold = 0.4;
    private double cellProbabilityThreshold = 0;
    private OptionalDoubleParameter stitchThreshold = new OptionalDoubleParameter(0, false);
    private boolean excludeOnEdges = false;

    public Cellpose2SegmentationThresholdSettings() {
    }

    public Cellpose2SegmentationThresholdSettings(Cellpose2SegmentationThresholdSettings other) {
        this.flowThreshold = other.flowThreshold;
        this.cellProbabilityThreshold = other.cellProbabilityThreshold;
        this.stitchThreshold = other.stitchThreshold;
        this.excludeOnEdges = other.excludeOnEdges;
    }

    @SetJIPipeDocumentation(name = "Exclude masks on edges", description = "Discard masks which touch edges of image")
    @JIPipeParameter("exclude-on-edges")
    public boolean isExcludeOnEdges() {
        return excludeOnEdges;
    }

    @JIPipeParameter("exclude-on-edges")
    public void setExcludeOnEdges(boolean excludeOnEdges) {
        this.excludeOnEdges = excludeOnEdges;
    }

    @SetJIPipeDocumentation(name = "Flow threshold (2D)", description = "Flow error threshold, 0 turns off this optional QC step. Default: 0.4")
    @JIPipeParameter("flow-threshold")
    public double getFlowThreshold() {
        return flowThreshold;
    }

    @JIPipeParameter("flow-threshold")
    public void setFlowThreshold(double flowThreshold) {
        this.flowThreshold = flowThreshold;
    }

    @SetJIPipeDocumentation(name = "Cell probability threshold", description = "Cellprob threshold, default is 0, decrease to find more and larger masks")
    @JIPipeParameter("cell-probability-threshold")
    public double getCellProbabilityThreshold() {
        return cellProbabilityThreshold;
    }

    @JIPipeParameter("cell-probability-threshold")
    public void setCellProbabilityThreshold(double cellProbabilityThreshold) {
        this.cellProbabilityThreshold = cellProbabilityThreshold;
    }

    @SetJIPipeDocumentation(name = "Stitch threshold", description = "Compute masks in 2D then stitch together masks with IoU>0.9 across planes")
    @JIPipeParameter("stitch-threshold")
    public OptionalDoubleParameter getStitchThreshold() {
        return stitchThreshold;
    }

    @JIPipeParameter("stitch-threshold")
    public void setStitchThreshold(OptionalDoubleParameter stitchThreshold) {
        this.stitchThreshold = stitchThreshold;
    }
}
