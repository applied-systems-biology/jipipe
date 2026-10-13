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

package org.hkijena.jipipe.api.nodes.taxonomy.families;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamily;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the four seed {@link org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFamily}
 * classes: their stable string-literal ids must be unique and non-empty,
 * their names and descriptions non-empty, and their templates and examples
 * must be {@code null} (template/example wiring is planned for Phase 3).
 * <p>
 * The family <em>step</em> graph of {@link TrackMateFamily} is validated
 * in the trackmate plugin module ({@code TrackMateFamilyStepsTest})
 * instead of here, because the annotated node classes live in that plugin
 * and jipipe-core must not depend on its plugins.
 */
class SeedFamiliesTest {

    private static final JIPipeNodeTaxonomyFamily[] FAMILIES = {
        new TrackMateFamily(), new Cellpose4Family(), new MachineLearningFamily(), new PlottingFamily()
    };

    /**
     * All four families use the exact stable id literals, which are unique
     * and non-empty.
     */
    @Test
    void familyIdsAreUniqueAndNonEmpty() {
        Set<String> ids = new HashSet<>();
        for (JIPipeNodeTaxonomyFamily family : FAMILIES) {
            String id = family.getId();
            assertNotNull(id);
            assertTrue(!id.isBlank(), "family id must be non-blank: " + family.getClass().getSimpleName());
            assertTrue(ids.add(id), "duplicate family id: " + id);
        }
        assertEquals("jipipe:family:trackmate", new TrackMateFamily().getId());
        assertEquals("jipipe:family:cellpose4", new Cellpose4Family().getId());
        assertEquals("jipipe:family:machine-learning", new MachineLearningFamily().getId());
        assertEquals("jipipe:family:plotting", new PlottingFamily().getId());
        assertEquals(4, ids.size(), "expected exactly four unique family ids, got: " + ids);
    }

    /**
     * Every family has a non-empty display name and description.
     */
    @Test
    void namesAndDescriptionsAreNonEmpty() {
        for (JIPipeNodeTaxonomyFamily family : FAMILIES) {
            String name = family.getName();
            String description = family.getDescription();
            assertTrue(name != null && !name.isBlank(),
                "family name must be non-blank: " + family.getClass().getSimpleName());
            assertTrue(description != null && !description.isBlank(),
                "family description must be non-blank: " + family.getClass().getSimpleName());
        }
    }

    /**
     * Templates and examples are not wired yet (Phase 3); the seed
     * families must both return {@code null}.
     */
    @Test
    void templatesAndExamplesAreNull() {
        for (JIPipeNodeTaxonomyFamily family : FAMILIES) {
            assertEquals(null, family.getTemplate(),
                "template must be null in Phase 1a: " + family.getClass().getSimpleName());
            assertEquals(null, family.getExample(),
                "example must be null in Phase 1a: " + family.getClass().getSimpleName());
        }
    }

    /**
     * The seed families carry no tags yet.
     */
    @Test
    void tagsAreEmpty() {
        for (JIPipeNodeTaxonomyFamily family : FAMILIES) {
            assertTrue(family.getTags().isEmpty(),
                "tags must be empty in Phase 1a: " + family.getClass().getSimpleName());
        }
    }
}
