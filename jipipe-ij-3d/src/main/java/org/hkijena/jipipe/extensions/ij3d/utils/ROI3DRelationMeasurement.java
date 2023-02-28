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
    Roi1Stats(65536),
    Roi2Stats(131072);

    private final int nativeValue;

    ROI3DRelationMeasurement(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    public static boolean includes(int nativeValue, ROI3DRelationMeasurement target) {
        return (nativeValue & target.nativeValue) == target.nativeValue;
    }
}
