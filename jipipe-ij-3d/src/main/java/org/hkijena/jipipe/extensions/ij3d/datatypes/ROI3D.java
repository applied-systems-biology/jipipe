package org.hkijena.jipipe.extensions.ij3d.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import mcib3d.geom.Object3D;
import org.hkijena.jipipe.extensions.ij3d.IJ3DUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around an {@link mcib3d.geom.Object3D} that also provides additional information and methods found within {@link ij.gui.Roi}
 */
public class ROI3D {
    private Object3D object3D;

    private Map<String, String> metadata = new HashMap<>();

    private int channel;

    private int frame;

    private Color fillColor = Color.RED;

    public ROI3D() {
    }

    public ROI3D(ROI3D other) {
        this.object3D = IJ3DUtils.duplicateObject3D(other.object3D);
        this.metadata = other.metadata;
        this.channel = other.channel;
        this.frame = other.frame;
        this.fillColor = other.fillColor;
    }

    public void copyMetadata(ROI3D other) {
        this.metadata = other.metadata;
        this.channel = other.channel;
        this.frame = other.frame;
        this.fillColor = other.fillColor;
    }

    public ROI3D(Object3D object3D) {
        this.object3D = object3D;
    }

    public Object3D getObject3D() {
        return object3D;
    }

    public void setObject3D(Object3D object3D) {
        this.object3D = object3D;
    }

    @JsonGetter("metadata")
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
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

    @JsonGetter("fill-color")
    public Color getFillColor() {
        return fillColor;
    }

    @JsonSetter("fill-color")
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public boolean sameChannel(int channel) {
        return this.channel <= 0 || channel <= 0 || channel == this.channel;
    }

    public boolean sameFrame(int frame) {
        return this.frame <= 0 || frame <= 0 || frame == this.frame;
    }


}
