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

import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.junit.jupiter.api.Test;

import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D;
import static org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the spatial dimension and input profile taxonomy types.
 */
class InputProfileTest {

    @Test
    void profileOfUnregisteredDataTypeIsNA() {
        InputProfileProvider empty = c -> JIPipeNodeTaxonomyInputProfile.NA;
        assertEquals(JIPipeNodeTaxonomyInputProfile.NA, empty.getProfile(JIPipeDataTable.class)); // total: never throws
    }

    @Test
    void spatialDimensionIsClosed() {
        assertArrayEquals(new JIPipeNodeTaxonomySpatialDimension[]{SPATIAL_2D, SPATIAL_3D},
                JIPipeNodeTaxonomySpatialDimension.values()); // no 4D/5D spatial exists
    }
}
