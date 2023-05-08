package org.hkijena.jipipe.extensions.cellpose.parameters.deprecated;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;

@Deprecated
public class CellposeSegmentationEnhancementSettings_Old extends AbstractJIPipeParameterCollection {
    private boolean normalize = true;
    private boolean netAverage = true;
    private boolean augment = false;
    private boolean interpolate = true;
    private OptionalDoubleParameter anisotropy = new OptionalDoubleParameter(1.0, false);

    public CellposeSegmentationEnhancementSettings_Old() {
    }

    public CellposeSegmentationEnhancementSettings_Old(CellposeSegmentationEnhancementSettings_Old other) {
        this.normalize = other.normalize;
        this.netAverage = other.netAverage;
        this.augment = other.augment;
        this.interpolate = other.interpolate;
        this.anisotropy = new OptionalDoubleParameter(other.anisotropy);
    }

    @JIPipeDocumentation(name = "Normalize", description = "Normalize data so 0.0=1st percentile and 1.0=99th percentile of image intensities in each channel")
    @JIPipeParameter("normalize")
    public boolean isNormalize() {
        return normalize;
    }

    @JIPipeParameter("normalize")
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }

    @JIPipeDocumentation(name = "Anisotropy (3D)", description = "For 3D segmentation, optional rescaling factor " +
            "(e.g. set to 2.0 if Z is sampled half as dense as X or Y)")
    @JIPipeParameter("anisotropy")
    public OptionalDoubleParameter getAnisotropy() {
        return anisotropy;
    }

    @JIPipeParameter("anisotropy")
    public void setAnisotropy(OptionalDoubleParameter anisotropy) {
        this.anisotropy = anisotropy;
    }

    @JIPipeDocumentation(name = "Average all networks", description = "Runs the 4 built-in networks and averages them if True, runs one network if disabled")
    @JIPipeParameter("net-average")
    public boolean isNetAverage() {
        return netAverage;
    }

    @JIPipeParameter("net-average")
    public void setNetAverage(boolean netAverage) {
        this.netAverage = netAverage;
    }

    @JIPipeDocumentation(name = "Augment", description = "Tiles image with overlapping tiles and flips overlapped regions to augment")
    @JIPipeParameter("augment")
    public boolean isAugment() {
        return augment;
    }

    @JIPipeParameter("augment")
    public void setAugment(boolean augment) {
        this.augment = augment;
    }

    @JIPipeDocumentation(name = "Interpolate (2D)", description = "Interpolate during 2D dynamics (not available in 3D)")
    @JIPipeParameter("interpolate")
    public boolean isInterpolate() {
        return interpolate;
    }

    @JIPipeParameter("interpolate")
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }
}
