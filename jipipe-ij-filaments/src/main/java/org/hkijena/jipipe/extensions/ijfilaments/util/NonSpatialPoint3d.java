package org.hkijena.jipipe.extensions.ijfilaments.util;

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
