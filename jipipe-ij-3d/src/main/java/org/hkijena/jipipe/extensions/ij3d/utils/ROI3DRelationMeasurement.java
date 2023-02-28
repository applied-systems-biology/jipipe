package org.hkijena.jipipe.extensions.ij3d.utils;

public enum ROI3DRelationMeasurement {
    Colocalization(1),
    PercentageColocalization(2),
    Angle(4),
    OverlapsBox(8),
    Includes(16),
    IncludesBox(32),
    RadiusCenter(64),
    RadiusCenterOpposite(128),
    DistanceCenter2D(256),
    DistanceCenter(512),
    DistanceHausdorff(1024),
    EdgeContactColocalization(2048),
    EdgeContactSide(4096),
    EdgeContactDiagonal(8192),
    IntersectionStats(16384);

    private final int nativeValue;

    ROI3DRelationMeasurement(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
