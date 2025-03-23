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

package org.hkijena.jipipe.plugins.imagejdatatypes.util;

/**
 * Available ways to address the shape of a {@link ij.gui.Roi}
 */
public enum RoiOutline {
    Polygon("Polygon"),
    ClosedPolygon("Closed polygon"),
    ConvexHull("Convex hull"),
    BoundingRectangle("Bounding rectangle"),
    MinimumBoundingRectangle("Minimum bounding rectangle"),
    OrientedLine("Oriented line"),
    FitCircle("Fit circle"),
    FitEllipse("Fit ellipse"),
    FitSpline("Fit spline"),
    DeleteFitSpline("Remove spline fit"),
    FitSplineStraighten("Fit spline (straightening)"),
    AreaToLine("Area to line"),
    LineToArea("Line to area"),;

    private final String label;

    RoiOutline(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
