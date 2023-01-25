package org.hkijena.jipipe.ui.grapheditor.general.nodeui;

import org.hkijena.jipipe.api.data.JIPipeSlotType;

import java.awt.*;

public class JIPipeNodeUIAddSlotButtonActiveArea extends JIPipeNodeUIActiveArea {
    private final JIPipeSlotType slotType;

    private Point nativeLocation;

    private double nativeWidth;

    public JIPipeNodeUIAddSlotButtonActiveArea(JIPipeSlotType slotType) {
        super(50);
        this.slotType = slotType;
    }

    public JIPipeSlotType getSlotType() {
        return slotType;
    }

    public Point getNativeLocation() {
        return nativeLocation;
    }

    public void setNativeLocation(Point nativeLocation) {
        this.nativeLocation = nativeLocation;
    }

    public double getNativeWidth() {
        return nativeWidth;
    }

    public void setNativeWidth(double nativeWidth) {
        this.nativeWidth = nativeWidth;
    }
}
