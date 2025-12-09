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

public enum Roi3dRelationMeasurement {
    Colocalization(1),
    PercentageColocalization(2),
    OverlapsBox(4),
    Includes(8),
    IncludesBox(16),
    RadiusCenter(32),
    RadiusCenterOpposite(64),
    DistanceCenter2D(128),
    DistanceCenter(256),
    DistanceHausdorff(512),

    DistanceBorder(1024),

    DistanceCenterBorder(2048),
    EdgeContactColocalization(4096),
    EdgeContactSide(8192),
    EdgeContactDiagonal(16384),
    IntersectionStats(32768),
    CurrentStats(65536),
    OtherStats(131072);

    private final int nativeValue;

    Roi3dRelationMeasurement(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public static boolean includes(int nativeValue, Roi3dRelationMeasurement target) {
        return (nativeValue & target.nativeValue) == target.nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    @Override
    public String toString() {
        return switch (this) {
            case PercentageColocalization -> "Colocalization (%)";
            case OverlapsBox -> "Bounding boxes overlaps (0/1)";
            case Includes -> "Includes (0/1)";
            case IncludesBox -> "Bounding box includes (0/1)";
            case RadiusCenter -> "Radius center";
            case RadiusCenterOpposite -> "Radius center (opposite)";
            case DistanceCenter2D -> "Distance center (2D)";
            case DistanceCenter -> "Distance center (3D)";
            case DistanceHausdorff -> "Distance (Hausdorff)";
            case DistanceBorder -> "Minimum distance (borders, slow!)";
            case EdgeContactColocalization -> "Edge contact (colocalization, slow!)";
            case EdgeContactSide -> "Edge contact (include side pixels, slow!)";
            case EdgeContactDiagonal -> "Edge contact (include side and diagonal pixels, slow!)";
            case IntersectionStats -> "Intersection object statistics";
            case CurrentStats -> "Current ROI statistics";
            case OtherStats -> "Other ROI statistics";
            default -> name();
        };
    }
}
