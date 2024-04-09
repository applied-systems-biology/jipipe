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

package org.hkijena.jipipe.plugins.ij3d.utils;

public enum ROI3DMeasurement {
    Index(1),
    Name(2),
    Comment(4),
    Area(8),
    Volume(16),
    Center(32),
    ShapeMeasurements(64),
    DistCenterStats(128),
    PixelValueStats(256),
    ContourPixelValueStats(512),
    BoundingBox(1024),
    Calibration(2048),
    MassCenter(4096),
    Location(8192),
    Color(16384),
    CustomMetadata(32768);

    private final int nativeValue;

    ROI3DMeasurement(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public static boolean includes(int nativeValue, ROI3DMeasurement target) {
        return (nativeValue & target.nativeValue) == target.nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    @Override
    public String toString() {
        switch (this) {
            case ShapeMeasurements:
                return "Shape measurements";
            case DistCenterStats:
                return "Distance to center (+ statistics)";
            case PixelValueStats:
                return "Pixel value statistics";
            case ContourPixelValueStats:
                return "Contour pixel value statistics";
            case BoundingBox:
                return "Bounding box";
            case MassCenter:
                return "Mass center";
            case CustomMetadata:
                return "Custom metadata";
            default:
                return name();
        }
    }
}
