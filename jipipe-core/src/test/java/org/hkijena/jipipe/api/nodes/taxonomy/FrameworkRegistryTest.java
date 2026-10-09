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

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link FrameworkRegistry} registration, lookup, the id-derived
 * palette color, and startup validation.
 */
class FrameworkRegistryTest {

    /**
     * Minimal test implementation of {@link JIPipeNodeTaxonomyFramework}.
     */
    private static class TestFramework implements JIPipeNodeTaxonomyFramework {
        private final String id;
        private final String name;
        private final String abbreviation;
        private final String patternStyleId;

        private TestFramework(String id, String name, String abbreviation, String patternStyleId) {
            this.id = id;
            this.name = name;
            this.abbreviation = abbreviation;
            this.patternStyleId = patternStyleId;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAbbreviation() {
            return abbreviation;
        }

        @Override
        public String getPatternStyleId() {
            return patternStyleId;
        }
    }

    @Test
    void duplicateIdsRejected() {
        FrameworkRegistry registry = new FrameworkRegistry();
        registry.register(new TestFramework("cellpose", "Cellpose", "CP", "initials"));
        registry.register(new TestFramework("cellpose", "Cellpose (duplicate)", "CP2", "initials"));

        Set<String> errors = registry.validate();
        assertTrue(errors.stream().anyMatch(e -> e.contains("cellpose")),
            "duplicate ids must be reported with the offending id, got: " + errors);
    }

    @Test
    void abbreviationOverFourCharsRejected() {
        FrameworkRegistry registry = new FrameworkRegistry();
        registry.register(new TestFramework("abcde-framework", "ABCDE Framework", "ABCDE", "initials"));

        Set<String> errors = registry.validate();
        assertTrue(errors.stream().anyMatch(e -> e.contains("abcde-framework")),
            "overlong abbreviations must be reported with the offending id, got: " + errors);
    }

    @Test
    void abbreviationEmptyRejected() {
        FrameworkRegistry registry = new FrameworkRegistry();
        registry.register(new TestFramework("empty-abbr", "Empty Abbr", "", "initials"));

        Set<String> errors = registry.validate();
        assertTrue(errors.stream().anyMatch(e -> e.contains("empty-abbr")),
            "empty abbreviations must be reported with the offending id, got: " + errors);
    }

    @Test
    void colorStablePerId() {
        FrameworkRegistry registryA = new FrameworkRegistry();
        TestFramework same = new TestFramework("cellpose", "Cellpose", "CP", "initials");
        registryA.register(same);
        FrameworkRegistry registryB = new FrameworkRegistry();
        TestFramework sameInB = new TestFramework("cellpose", "Cellpose", "CP", "initials");
        registryB.register(sameInB);

        Color colorA = registryA.byId("cellpose").getColor();
        Color colorB = registryB.byId("cellpose").getColor();
        assertNotNull(colorA);
        assertEquals(colorA, colorB); // same id ⇒ same color across two registry instances
        assertEquals(same.getColor(), colorA); // direct and via lookup agree

        // Two different ids can map to different palette entries.
        TestFramework other = new TestFramework("trackmate", "TrackMate", "TM", "initials");
        assertNotEquals(same.getColor(), other.getColor());
    }
}
