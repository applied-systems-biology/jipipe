package org.hkijena.jipipe.extensions.ij3d.utils;

public enum ROI3DRelationMeasurement {
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

    ROI3DRelationMeasurement(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public static boolean includes(int nativeValue, ROI3DRelationMeasurement target) {
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
            case RadiusCenter:
                return "Radius center";
            case RadiusCenterOpposite:
                return "Radius center (opposite)";
            case DistanceCenter2D:
                return "Distance center (2D)";
            case DistanceCenter:
                return "Distance center (3D)";
            case DistanceHausdorff:
                return "Distance (Hausdorff)";
            case DistanceBorder:
                return "Minimum distance (borders)";
            case EdgeContactColocalization:
                return "Edge contact (colocalization)";
            case EdgeContactSide:
                return "Edge contact (include side pixels)";
            case EdgeContactDiagonal:
                return "Edge contact (include side and diagonal pixels)";
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
