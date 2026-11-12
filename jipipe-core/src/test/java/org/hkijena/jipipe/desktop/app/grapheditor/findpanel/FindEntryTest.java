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

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link FindEntry#matches(String)}, the query matching of the
 * Find panel's search model (MR 2 of the navigator overhaul, #803).
 */
class FindEntryTest {

    private static FindEntry entry(String name, String description, String uuidString) {
        return new FindEntry(UUID.randomUUID(), name, description, uuidString, UUID.randomUUID(), "Compartment", false);
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
}
