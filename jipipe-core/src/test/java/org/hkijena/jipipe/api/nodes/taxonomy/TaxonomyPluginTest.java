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

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that the {@code TaxonomyPlugin} (discovered via the SciJava
 * {@code @Plugin} annotation during {@link JIPipe#ensureInstance()}) actually
 * registers the curated taxonomy into the live registries:
 * <ul>
 * <li>the 84 functions (13 top-level branches + 71 seed leaves),</li>
 * <li>the 14 frameworks,</li>
 * <li>a valid taxonomy ({@link JIPipeNodesServiceComponent#validateTaxonomy()}
 * empty).</li>
 * </ul>
 * <p>
 * Families are NOT asserted: after this task no node yet carries a
 * {@link JIPipeNodeTaxonomyFamilyStepAnnotation}, so {@code getFamilies()} is
 * expected to be empty (families land in Task 13).
 */
class TaxonomyPluginTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @Test
    void allTaxonomyRegistered() {
        // 13 top-level branches + 71 seed leaves (incl. the dual-visible
        // GraphAnnotationsFunction, the RoiPropertiesFunction audit leaf, the
        // 10 full-registry-audit Images leaves, the 7
        // curated-reorganization leaves, and the 4 Scripting language
        // sub-leaves)
        assertEquals(84, JIPipe.getNodes().getFunctions().all().size(),
            "Expected 84 taxonomy functions registered by TaxonomyPlugin");
        assertEquals(14, JIPipe.getNodes().getFrameworks().all().size(),
            "Expected 14 taxonomy frameworks registered by TaxonomyPlugin");

        // No node carries a primary function yet, so no membership errors;
        // the function tree and framework set must be internally valid.
        assertTrue(JIPipe.getNodes().validateTaxonomy().isEmpty(),
            "Expected a valid taxonomy but got: " + JIPipe.getNodes().validateTaxonomy());

        // Families are only assembled from node step annotations (Task 13);
        // after this task there are none.
        assertTrue(JIPipe.getNodes().getFamilies().isEmpty(),
            "Expected no families yet (Task 13), got: " + JIPipe.getNodes().getFamilies().keySet());
    }
}
