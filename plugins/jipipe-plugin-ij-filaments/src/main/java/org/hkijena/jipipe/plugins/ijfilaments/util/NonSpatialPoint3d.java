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

package org.hkijena.jipipe.plugins.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class NonSpatialPoint3d {
    private int channel;

    private int frame;

    public NonSpatialPoint3d() {
    }

    public NonSpatialPoint3d(int channel, int frame) {
        this.channel = channel;
        this.frame = frame;
    }

    public NonSpatialPoint3d(NonSpatialPoint3d other) {
        this.channel = other.channel;
        this.frame = other.frame;
    }

    @JsonGetter("channel")
    public int getChannel() {
        return channel;
    }

    @JsonSetter("channel")
    public void setChannel(int channel) {
        this.channel = channel;
    }

    @JsonGetter("frame")
    public int getFrame() {
        return frame;
    }

    @JsonSetter("frame")
    public void setFrame(int frame) {
        this.frame = frame;
    }
}
