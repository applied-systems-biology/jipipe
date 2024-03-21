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

package org.hkijena.jipipe.extensions.omnipose.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;

public class OmniposeSegmentationThresholdSettings extends AbstractJIPipeParameterCollection {
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

    @SetJIPipeDocumentation(name = "Mask threshold", description = "Mask threshold, default is 0, decrease to find more and larger masks")
    @JIPipeParameter("mask-threshold")
    public double getMaskThreshold() {
        return maskThreshold;
    }

    @JIPipeParameter("mask-threshold")
    public void setMaskThreshold(double maskThreshold) {
        this.maskThreshold = maskThreshold;
    }

    @SetJIPipeDocumentation(name = "Cell diameter threshold", description = "Cell diameter threshold for upscaling before mask reconstruction, default 12.")
    @JIPipeParameter("diam-threshold")
    public double getDiamThreshold() {
        return diamThreshold;
    }

    @JIPipeParameter("diam-threshold")
    public void setDiamThreshold(double diamThreshold) {
        this.diamThreshold = diamThreshold;
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
