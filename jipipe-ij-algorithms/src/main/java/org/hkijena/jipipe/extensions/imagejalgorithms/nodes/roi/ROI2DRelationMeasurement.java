package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

public enum ROI2DRelationMeasurement {
    Colocalization(1),
    PercentageColocalization(2),
    OverlapsBox(4),
    Includes(8),
    IncludesBox(16),
    DistanceCenter(256),
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
