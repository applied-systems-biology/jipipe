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

package org.hkijena.jipipe.plugins.cellpose.parameters.cp3;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDoubleParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.OptionalVector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.VectorParameterSettings;

public class Cellpose3SegmentationTweaksSettings extends AbstractJIPipeParameterCollection {
    private OptionalVector2iParameter normalizePercentile = new OptionalVector2iParameter(new Vector2iParameter(1, 99), false);
    private boolean normalize = true;
    private boolean interpolate = true;
    private boolean resample = true;
    private int niter = 0;
    private OptionalDoubleParameter anisotropy = new OptionalDoubleParameter(1.0, false);
    private double flow3DSmoothing = 0;

    public Cellpose3SegmentationTweaksSettings() {
    }

    public Cellpose3SegmentationTweaksSettings(Cellpose3SegmentationTweaksSettings other) {
        this.normalize = other.normalize;
        this.interpolate = other.interpolate;
        this.anisotropy = new OptionalDoubleParameter(other.anisotropy);
        this.normalizePercentile = new OptionalVector2iParameter(other.normalizePercentile);
        this.niter = other.niter;
        this.flow3DSmoothing = other.flow3DSmoothing;
    }

    @SetJIPipeDocumentation(name = "Niter dynamics (elongated objects)", description = "Number of iterations for dynamics for mask creation, default of 0 means it is proportional to diameter, set to a larger number like 2000 for very long ROIs")
    @JIPipeParameter(value = "niter", important = true)
    public int getNiter() {
        return niter;
    }

    @JIPipeParameter("niter")
    public void setNiter(int niter) {
        this.niter = niter;
    }

    @SetJIPipeDocumentation(name = "Flow smoothing (3D)", description = "Stddev of gaussian for smoothing of dP for dynamics in 3D, default of 0 means no smoothing")
    @JIPipeParameter("flow-3d-smoothing")
    public double getFlow3DSmoothing() {
        return flow3DSmoothing;
    }

    @JIPipeParameter("flow-3d-smoothing")
    public void setFlow3DSmoothing(double flow3DSmoothing) {
        this.flow3DSmoothing = flow3DSmoothing;
    }

    @SetJIPipeDocumentation(name = "Normalize", description = "Normalize data so 0.0=1st percentile and 1.0=99th percentile of image intensities in each channel")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    @SetJIPipeDocumentation(name = "Anisotropy (3D)", description = "For 3D segmentation, optional rescaling factor " +
            "(e.g. set to 2.0 if Z is sampled half as dense as X or Y)")
    @JIPipeParameter("anisotropy")
    public OptionalDoubleParameter getAnisotropy() {
        return anisotropy;
    }

    @JIPipeParameter("anisotropy")
    public void setAnisotropy(OptionalDoubleParameter anisotropy) {
        this.anisotropy = anisotropy;
    }


    @SetJIPipeDocumentation(name = "Interpolate (2D)", description = "Interpolate during 2D dynamics (not available in 3D)")
    @JIPipeParameter("interpolate")
    public boolean isInterpolate() {
        return interpolate;
    }

    @JIPipeParameter("interpolate")
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    @JIPipeParameter("normalize-percentile")
    @SetJIPipeDocumentation(name = "Normalize percentile", description = "The lower and upper percentile values for normalization")
    @VectorParameterSettings(xLabel = "Lower", yLabel = "Upper")
    public OptionalVector2iParameter getNormalizePercentile() {
        return normalizePercentile;
    }

    @JIPipeParameter("normalize-percentile")
    public void setNormalizePercentile(OptionalVector2iParameter normalizePercentile) {
        this.normalizePercentile = normalizePercentile;
    }

    @SetJIPipeDocumentation(name = "Resample", description = "Enable dynamics on the full image, making the algorithm slower for images with large object diameters")
    @JIPipeParameter("resample")
    public boolean isResample() {
        return resample;
    }

    @JIPipeParameter("resample")
    public void setResample(boolean resample) {
        this.resample = resample;
    }
}
