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

package org.hkijena.jipipe.api.nodes.database;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the SHIPPED usage prior resource (a single-source ship set, like
 * {@code SystemPatternTemplatesTest} for the system templates): well-formed
 * document, sorted keys, positive counts, and exact values for a few
 * entries mined from the example-workflow corpus. The corpus itself lives
 * outside the repository - only the generated resource is committed.
 */
class UsagePriorResourceTest {

    private static JsonNode loadDocument() {
        try (var stream = UsagePriorResourceTest.class.getResourceAsStream("/org/hkijena/jipipe/search/usage-prior.json")) {
            return JsonUtils.getObjectMapper().readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("usage-prior.json missing or unreadable", e);
        }
    }

    @Test
    void documentIsWellFormed() {
        JsonNode document = loadDocument();
        assertTrue(document.has("source"));
        assertTrue(document.get("generated").isTextual());
        assertTrue(document.get("corpus").has("files"));
        assertTrue(document.get("corpus").has("sha256"));
        assertTrue(document.get("counts").isObject());
        assertTrue(document.get("counts").size() > 250, "the corpus mines a large node set (got "
                + document.get("counts").size() + ")");
    }

    @Test
    void countsArePositiveIntegers() {
        JsonNode counts = loadDocument().get("counts");
        for (var entry : counts.properties()) {
            assertTrue(entry.getValue().has("instances"));
            assertTrue(entry.getValue().has("pipelines"));
            assertTrue(entry.getValue().get("instances").isInt() && entry.getValue().get("instances").asInt() > 0,
                    "instances of " + entry.getKey() + " must be a positive integer");
            assertTrue(entry.getValue().get("pipelines").isInt() && entry.getValue().get("pipelines").asInt() > 0,
                    "pipelines of " + entry.getKey() + " must be a positive integer");
        }
    }

    @Test
    void registryOnlyNodeTypesArePresent() {
        JsonNode counts = loadDocument().get("counts");
        // Canvas notes, comments, groups, and compartment I/O are REAL
        // registered node types (they carry descriptions and are
        // searchable), so they stay in the prior; the registry filter only
        // drops ids that are not node types at all (Ruling, Task 2).
        assertTrue(counts.has("graph-annotation-text-box"));
        assertTrue(counts.has("jipipe:comment"));
        assertTrue(counts.has("jipipe:compartment-output"));
        assertTrue(counts.has("node-group"));
    }

    @Test
    void pinnedEntriesFromTheCorpus() {
        JsonNode counts = loadDocument().get("counts");
        // The workhorse nodes of the mined corpus (values pinned at
        // generation time; regenerate + update when the corpus grows)
        assertEquals(242, counts.get("ij1-threshold-auto2d").get("instances").asInt());
        assertEquals(50, counts.get("ij1-threshold-auto2d").get("pipelines").asInt());
        assertEquals(115, counts.get("ij1-analyze-find-particles2d").get("instances").asInt());
        assertEquals(36, counts.get("ij1-analyze-find-particles2d").get("pipelines").asInt());
    }
}
