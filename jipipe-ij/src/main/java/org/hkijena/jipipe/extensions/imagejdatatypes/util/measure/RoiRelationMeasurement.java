package org.hkijena.jipipe.extensions.imagejdatatypes.util.measure;

public enum RoiRelationMeasurement {
    Overlap(1),
    PercentageOverlap(2),
    OverlapsBox(4),
    Includes(8),
    IncludesBox(16),
    MinDistanceBorder(32),
    IntersectionStatistics(64),
    CurrentStatistics(128),
    OtherStatistics(256);

    private final int nativeValue;

    RoiRelationMeasurement(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    public static boolean includes(int nativeValue, RoiRelationMeasurement target) {
        return (nativeValue & target.nativeValue) == target.nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    @Override
    public String toString() {
        switch (this) {
            case IncludesBox:
                return "Bounding box includes (0/1)";
            case OverlapsBox:
                return "Bounding boxes overlaps (0/1)";
            case PercentageOverlap:
                return "Overlap (%)";
            case MinDistanceBorder:
                return "Minimum distance (borders)";
            case IntersectionStatistics:
                return "Intersection ROI statistics";
            case CurrentStatistics:
                return "Current ROI statistics";
            case OtherStatistics:
                return "Other ROI statistics";
            default:
                return name();
        }
    }
}
