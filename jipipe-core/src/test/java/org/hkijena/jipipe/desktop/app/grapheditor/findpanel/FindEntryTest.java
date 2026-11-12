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

package org.hkijena.jipipe.desktop.app.grapheditor.findpanel;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeEmptyData;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link FindEntry#matches(String)} (the query matching) and
 * {@link FindEntry#matchesFacets(String, Set, Set, Set)} (the Add-nodes-style
 * facet matching) of the Find panel's search model (MR 2 of the navigator
 * overhaul, #803).
 */
class FindEntryTest {

    private static final String FRAMEWORK_ID = "test-framework";
    private static final Set<String> TAGS = Set.of("images", "tracking");
    private static final Set<Class<? extends JIPipeData>> INPUT_TYPES = Set.of(JIPipeData.class);
    private static final Set<Class<? extends JIPipeData>> OUTPUT_TYPES = Set.of(JIPipeEmptyData.class);

    private static FindEntry entry(String name, String description, String uuidString) {
        return new FindEntry(UUID.randomUUID(), name, description, uuidString, UUID.randomUUID(), "Compartment", false,
                null, Set.of(), Set.of(), Set.of());
    }

    private static FindEntry entry() {
        return new FindEntry(UUID.randomUUID(), "Node", "Description", "0123abcd", UUID.randomUUID(), "Compartment", false,
                FRAMEWORK_ID, TAGS, INPUT_TYPES, OUTPUT_TYPES);
    }

    @Test
    void matchesName() {
        assertTrue(entry("Cellpose Segmentation", "Runs the model", "0123abcd").matches("segmentation"));
    }

    @Test
    void matchesDescription() {
        assertTrue(entry("Cellpose Segmentation", "Runs the Cellpose 42 model", "0123abcd").matches("cellpose 42"));
    }

    @Test
    void matchesUuidString() {
        assertTrue(entry("Cellpose Segmentation", "Runs the model", "0123abcd-4321").matches("0123abcd"));
    }

    @Test
    void matchingIsCaseInsensitive() {
        FindEntry entry = entry("Cellpose Segmentation", "Runs the model", "0123abcd");
        assertTrue(entry.matches("SEGMENTATION"));
        assertTrue(entry.matches("RUNS THE"));
        assertTrue(entry.matches("0123ABCD"));
    }

    @Test
    void nullAndEmptyQueriesMatchEverything() {
        FindEntry entry = entry("Anything", "Else", "deadbeef");
        assertTrue(entry.matches(null));
        assertTrue(entry.matches(""));
    }

    @Test
    void nonMatchingQueryReturnsFalse() {
        assertFalse(entry("Cellpose Segmentation", "Runs the model", "0123abcd").matches("ilastik"));
    }

    @Test
    void frameworkFilterPassesWhenEmptySelection() {
        assertTrue(entry().matchesFacets(null, Set.of(), Set.of(), Set.of()));
    }

    @Test
    void frameworkFilterPassesWhenFrameworkMatches() {
        assertTrue(entry().matchesFacets(FRAMEWORK_ID, Set.of(), Set.of(), Set.of()));
    }

    @Test
    void frameworkFilterBlocksWhenFrameworkDiffers() {
        assertFalse(entry().matchesFacets("other-framework", Set.of(), Set.of(), Set.of()));
    }

    @Test
    void tagFilterPassesWhenSelectionIntersectsEntryTags() {
        assertTrue(entry().matchesFacets(null, Set.of("tracking"), Set.of(), Set.of()));
    }

    @Test
    void tagFilterBlocksWhenSelectionIsDisjoint() {
        assertFalse(entry().matchesFacets(null, Set.of("other-tag"), Set.of(), Set.of()));
    }

    @Test
    void inputFilterPassesWhenOneInputDataTypeIsSelected() {
        assertTrue(entry().matchesFacets(null, Set.of(), Set.of(JIPipeData.class), Set.of()));
    }

    @Test
    void inputFilterBlocksWhenNoInputDataTypeIsSelected() {
        assertFalse(entry().matchesFacets(null, Set.of(), Set.of(JIPipeEmptyData.class), Set.of()));
    }

    @Test
    void outputFilterPassesWhenOneOutputDataTypeIsSelected() {
        assertTrue(entry().matchesFacets(null, Set.of(), Set.of(), Set.of(JIPipeEmptyData.class)));
    }

    @Test
    void outputFilterBlocksWhenNoOutputDataTypeIsSelected() {
        assertFalse(entry().matchesFacets(null, Set.of(), Set.of(), Set.of(JIPipeData.class)));
    }

    @Test
    void emptyFacetSelectionsPassEverything() {
        assertTrue(entry().matchesFacets(null, Set.of(), Set.of(), Set.of()));
    }

    @Test
    void allFiltersMustPassTogether() {
        assertTrue(entry().matchesFacets(FRAMEWORK_ID, TAGS, INPUT_TYPES, OUTPUT_TYPES));
        assertFalse(entry().matchesFacets(FRAMEWORK_ID, TAGS, Set.of(JIPipeEmptyData.class), OUTPUT_TYPES));
    }
}
