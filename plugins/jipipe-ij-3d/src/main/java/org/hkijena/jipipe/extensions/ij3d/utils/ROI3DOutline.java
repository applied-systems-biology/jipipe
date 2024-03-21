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
