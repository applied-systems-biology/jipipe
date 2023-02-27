package org.hkijena.jipipe.extensions.ij3d.utils;

public enum Measurement3D {
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

    Measurement3D(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    public static boolean includes(int nativeValue, Measurement3D target) {
        return (nativeValue & target.nativeValue) == target.nativeValue;
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
