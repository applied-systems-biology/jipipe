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
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;

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
