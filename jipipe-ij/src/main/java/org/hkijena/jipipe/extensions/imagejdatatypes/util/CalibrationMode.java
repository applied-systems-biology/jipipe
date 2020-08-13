/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

public enum CalibrationMode {
    AutomaticImageJ(0, 0),
    MinMax(0, 0),
    Custom(0, 0),
    Depth8Bit(0, 255),
    Depth10Bit(0, 1023),
    Depth12Bit(0, 4095),
    Depth14Bit(0, 16383),
    Depth15Bit(0, 32767),
    Depth16Bit(0, 65535),
    Unit(0, 1),
    UnitAroundZero(-1, 1);

    private final double min;
    private final double max;

    CalibrationMode(double min, double max) {
        this.min = min;
        this.max = max;
    }


    @Override
    public String toString() {
        String name = null;
        switch (this) {
            case Depth8Bit:
                name = "8-bit";
                break;
            case Depth10Bit:
                name = "10-bit";
                break;
            case Depth12Bit:
                name = "12-bit";
                break;
            case Depth14Bit:
                name = "14-bit";
                break;
            case Depth15Bit:
                name = "15-bit";
                break;
            case Depth16Bit:
                name = "16-bit";
                break;
            case Unit:
                name = "Unit";
                break;
            case UnitAroundZero:
                name = "Unit around zero";
                break;
        }
        if (name == null) {
            return this.name();
        } else {
            return name + " (" + min + " - " + max + ")";
        }
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
