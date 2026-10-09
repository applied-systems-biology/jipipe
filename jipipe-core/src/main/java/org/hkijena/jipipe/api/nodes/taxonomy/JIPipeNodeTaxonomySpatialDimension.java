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

package org.hkijena.jipipe.api.nodes.taxonomy;

/**
 * The declared spatial dimension of a node's operating space.
 * This is a closed set: only 2D and 3D spatial data exist in JIPipe.
 */
public enum JIPipeNodeTaxonomySpatialDimension {
    /**
     * Two-dimensional spatial data (e.g., images, tables with x/y columns)
     */
    SPATIAL_2D,

    /**
     * Three-dimensional spatial data (e.g., volumes, 3D point clouds)
     */
    SPATIAL_3D
}
