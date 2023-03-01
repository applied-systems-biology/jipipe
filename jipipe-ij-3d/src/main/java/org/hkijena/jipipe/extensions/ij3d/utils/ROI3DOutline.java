package org.hkijena.jipipe.extensions.ij3d.utils;

public enum ROI3DOutline {
    BoundingBox,
    BoundingBoxOriented,
    ConvexHull,
    Surface,
    ConvexSurface;

    @Override
    public String toString() {
        switch (this) {
            case BoundingBox:
                return "Bounding box";
            case BoundingBoxOriented:
                return "Bounding box (oriented)";
            case ConvexHull:
                return "Convex hull";
            case ConvexSurface:
                return "Convex surface";
            default:
                return name();
        }
    }
}
