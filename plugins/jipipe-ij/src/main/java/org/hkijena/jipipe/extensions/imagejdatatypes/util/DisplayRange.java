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
