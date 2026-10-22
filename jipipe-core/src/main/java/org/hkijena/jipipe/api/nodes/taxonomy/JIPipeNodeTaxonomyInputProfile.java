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
 * The derived input profile of a node, describing what kind of data it consumes.
 * Profiles are derived from the data types of a node's input slots, falling back
 * to {@link #NA} when the profile cannot be determined.
 */
public enum JIPipeNodeTaxonomyInputProfile {
    /**
     * Consumes 2D spatial data (e.g., images)
     */
    PLANE_2D,

    /**
     * Consumes 3D spatial data (e.g., volumes)
     */
    VOLUME_3D,

    /**
     * Consumes any input profile
     */
    ANY,

    /**
     * Not applicable - no derivable input profile (total-derivation fallback)
     */
    NA
}
