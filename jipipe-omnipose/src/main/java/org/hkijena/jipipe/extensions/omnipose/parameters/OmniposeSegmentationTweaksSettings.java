package org.hkijena.jipipe.extensions.omnipose.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalDoubleParameter;

public class OmniposeSegmentationTweaksSettings implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    private boolean netAverage = true;
    private boolean interpolate = true;
    private OptionalDoubleParameter anisotropy = new OptionalDoubleParameter(1.0, false);
    private boolean disableResample = false;

    private boolean fastMode = false;
    private boolean cluster = false;


    public OmniposeSegmentationTweaksSettings() {
    }

    public OmniposeSegmentationTweaksSettings(OmniposeSegmentationTweaksSettings other) {
        this.netAverage = other.netAverage;
        this.interpolate = other.interpolate;
        this.anisotropy = new OptionalDoubleParameter(other.anisotropy);
        this.disableResample = other.disableResample;
        this.cluster = other.cluster;
        this.fastMode = other.fastMode;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Fast mode", description = "disable dynamics on full image (makes algorithm faster for images with large diameters)")
    @JIPipeParameter("fast-mode")
    public boolean isFastMode() {
        return fastMode;
    }

    @JIPipeParameter("fast-mode")
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
    }

    @JIPipeDocumentation(name = "Cluster", description = "DBSCAN clustering. Reduces oversegmentation of thin features")
    @JIPipeParameter("cluster")
    public boolean isCluster() {
        return cluster;
    }

    @JIPipeParameter("cluster")
    public void setCluster(boolean cluster) {
        this.cluster = cluster;
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

    @JIPipeDocumentation(name = "Interpolate (2D)", description = "Interpolate during 2D dynamics (not available in 3D)")
    @JIPipeParameter("interpolate")
    public boolean isInterpolate() {
        return interpolate;
    }

    @JIPipeParameter("interpolate")
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    @JIPipeDocumentation(name = "Disable resample", description = "Disable dynamics on full image (makes algorithm faster for images with large diameters)")
    @JIPipeParameter("no-resample")
    public boolean isDisableResample() {
        return disableResample;
    }

    @JIPipeParameter("no-resample")
    public void setDisableResample(boolean disableResample) {
        this.disableResample = disableResample;
    }
}