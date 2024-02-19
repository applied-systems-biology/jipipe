package org.hkijena.jipipe.extensions.cellpose.parameters;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;

public class CellposeChannelSettings extends AbstractJIPipeParameterCollection {
    private OptionalIntegerParameter segmentedChannel = new OptionalIntegerParameter(false, 0);
    private OptionalIntegerParameter nuclearChannel = new OptionalIntegerParameter(false, 0);

    private boolean allChannels = false;

    private boolean invert = false;

    public CellposeChannelSettings() {

    }

    public CellposeChannelSettings(CellposeChannelSettings other) {
        this.segmentedChannel = new OptionalIntegerParameter(other.segmentedChannel);
        this.nuclearChannel = new OptionalIntegerParameter(other.nuclearChannel);
        this.allChannels = other.allChannels;
        this.invert = other.invert;
    }

    @SetJIPipeDocumentation(name = "Segmented channel", description = "Channel to segment; 0: GRAY, 1: RED, 2: GREEN, 3: BLUE. Default: 0")
    @JIPipeParameter(value = "segmented-channel", uiOrder = -100)
    public OptionalIntegerParameter getSegmentedChannel() {
        return segmentedChannel;
    }

    @JIPipeParameter("segmented-channel")
    public void setSegmentedChannel(OptionalIntegerParameter segmentedChannel) {
        this.segmentedChannel = segmentedChannel;
    }

    @SetJIPipeDocumentation(name = "Nuclear channel", description = "Nuclear channel (only used by certain models); 0: NONE, 1: RED, 2: GREEN, 3: BLUE. Default: 0")
    @JIPipeParameter(value = "nuclear-channel", uiOrder = -90)
    public OptionalIntegerParameter getNuclearChannel() {
        return nuclearChannel;
    }

    @JIPipeParameter("nuclear-channel")
    public void setNuclearChannel(OptionalIntegerParameter nuclearChannel) {
        this.nuclearChannel = nuclearChannel;
    }

    @SetJIPipeDocumentation(name = "Use all channels", description = "Use all channels in image if using own model and images with special channels")
    @JIPipeParameter("all-channels")
    public boolean isAllChannels() {
        return allChannels;
    }

    @JIPipeParameter("all-channels")
    public void setAllChannels(boolean allChannels) {
        this.allChannels = allChannels;
    }

    @SetJIPipeDocumentation(name = "Invert", description = "Invert grayscale channel")
    @JIPipeParameter("invert")
    public boolean isInvert() {
        return invert;
    }

    @JIPipeParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
    }
}
