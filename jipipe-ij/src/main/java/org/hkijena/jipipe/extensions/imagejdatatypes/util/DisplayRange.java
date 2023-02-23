package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Stores information about the calibration of an image
 */
public class DisplayRange {

    private double displayedMin;

    private double displayedMax;

    public DisplayRange() {
    }

    public DisplayRange(double displayedMin, double displayedMax) {
        this.displayedMin = displayedMin;
        this.displayedMax = displayedMax;
    }

    @JsonGetter("min")
    public double getDisplayedMin() {
        return displayedMin;
    }

    @JsonSetter("min")
    public void setDisplayedMin(double displayedMin) {
        this.displayedMin = displayedMin;
    }

    @JsonGetter("max")
    public double getDisplayedMax() {
        return displayedMax;
    }

    @JsonSetter("max")
    public void setDisplayedMax(double displayedMax) {
        this.displayedMax = displayedMax;
    }
}
