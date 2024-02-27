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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

public enum ROI2DRelationMeasurement {
    Colocalization(1),
    PercentageColocalization(2),
    OverlapsBox(4),
    Includes(8),
    IncludesBox(16),
    DistanceCenter(256),
    PolygonDistanceStats(512),
    IntersectionStats(32768),
    CurrentStats(65536),
    OtherStats(131072);

    private final int nativeValue;

    ROI2DRelationMeasurement(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public static boolean includes(int nativeValue, ROI2DRelationMeasurement target) {
        return (nativeValue & target.nativeValue) == target.nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    @Override
    public String toString() {
        switch (this) {
            case PercentageColocalization:
                return "Colocalization (%)";
            case OverlapsBox:
                return "Bounding boxes overlaps (0/1)";
            case Includes:
                return "Includes (0/1)";
            case IncludesBox:
                return "Bounding box includes (0/1)";
            case DistanceCenter:
                return "Distance center (2D)";
            case PolygonDistanceStats:
                return "Polygon distance (2D) min/max/avg";
            case IntersectionStats:
                return "Intersection object statistics";
            case CurrentStats:
                return "Current ROI statistics";
            case OtherStats:
                return "Other ROI statistics";
            default:
                return name();
        }
    }
}
