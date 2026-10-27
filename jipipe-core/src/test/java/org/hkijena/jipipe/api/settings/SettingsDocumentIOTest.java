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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsDocumentIOTest {
    private static final String FIXTURE = """
            {
              "$schema": 2,
              "$meta": { "written-by-version": "9.9.9", "written-at": "2026-01-01T00:00:00Z", "writer-id": "deadbeef" },
              "sheet:org.hkijena.jipipe:general-ui": { "theme": "Modern", "a-newer-knob": 42 },
              "registry:ui-graph-editor": { "any/dock/show-labels": true },
              "future-thing:abc": { "x": [1, 2, 3] },
              "state:recent-projects": ["/a/b.jip"]
            }
            """;

    @Test void preservesUnknownBlocksVerbatim() throws Exception {
        JsonNode in = JsonUtils.getObjectMapper().readTree(FIXTURE);
        SettingsDocument d = SettingsDocumentIO.fromJson(SettingsScope.APPLICATION, in, p -> true);
        assertTrue(d.isPassThrough("future-thing:abc"));
        JsonNode out = SettingsDocumentIO.toJson(d);
        assertEquals(in.get("future-thing:abc"), out.get("future-thing:abc"));
    }

    @Test void preservesUnknownSubPathsInsideKnownBlocks() throws Exception {
        JsonNode in = JsonUtils.getObjectMapper().readTree(FIXTURE);
        SettingsDocument d = SettingsDocumentIO.fromJson(SettingsScope.APPLICATION, in, p -> true);
        JsonNode out = SettingsDocumentIO.toJson(d);
        assertEquals(42, out.get("sheet:org.hkijena.jipipe:general-ui").get("a-newer-knob").asInt());
    }

    @Test void fullRoundTripIsStable() throws Exception {
        JsonNode in = JsonUtils.getObjectMapper().readTree(FIXTURE);
        JsonNode once = SettingsDocumentIO.toJson(SettingsDocumentIO.fromJson(SettingsScope.APPLICATION, in, p -> true));
        JsonNode twice = SettingsDocumentIO.toJson(SettingsDocumentIO.fromJson(SettingsScope.APPLICATION, once, p -> true));
        assertEquals(once, twice);
    }
}
