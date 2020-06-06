package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.roi;

/**
 * Available ways to address the shape of a {@link ij.gui.Roi}
 */
public enum RoiOutline {
    Polygon,
    ClosedPolygon,
    ConvexHull,
    BoundingRectangle
}
