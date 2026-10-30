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

package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test for {@link JIPipeProjectSettingsDocument}: it reads both the
 * legacy project {@code "settings"} shape (top-level keys are unprefixed
 * sheet ids) and the new shape ({@code $schema}/{@code $meta},
 * {@code sheet:}-prefixed blocks), upgrades the legacy shape, and preserves
 * unknown sheet blocks verbatim (pass-through).
 */
class JIPipeProjectSettingsDocumentTest {

    @Test
    void legacyProjectSettingsBlockIsUpgradedWithPassThrough() throws Exception {
        JsonNode legacy = JsonUtils.getObjectMapper().readTree(
                "{\"org.hkijena.jipipe:project-io\":{\"format\":\"jipx\"},\"unknown-sheet\":{\"z\":1}}");
        JIPipeProjectSettingsDocument d = JIPipeProjectSettingsDocument.fromJson(legacy, p -> true);
        JsonNode out = d.toJson();
        assertEquals("jipx", out.get("sheet:org.hkijena.jipipe:project-io").get("format").asText());
        assertEquals(1, out.get("sheet:unknown-sheet").get("z").asInt());
        assertEquals(2, out.get("$schema").asInt());
    }

    @Test
    void newShapeIsReadAsIs() throws Exception {
        JsonNode in = JsonUtils.getObjectMapper().readTree("{\"$schema\":2,\"sheet:a\":{\"b\":1}}");
        assertEquals(1, JIPipeProjectSettingsDocument.fromJson(in, p -> true).toJson().get("sheet:a").get("b").asInt());
    }

    @Test
    void unknownSheetBlocksSurviveEvenWhenNotRegistered() throws Exception {
        JsonNode legacy = JsonUtils.getObjectMapper().readTree(
                "{\"org.hkijena.jipipe:project-io\":{\"format\":\"jipx\"},\"unknown-sheet\":{\"z\":1,\"nested\":{\"a\":[1,2]}}}");
        JIPipeProjectSettingsDocument d = JIPipeProjectSettingsDocument.fromJson(legacy,
                "sheet:org.hkijena.jipipe:project-io"::equals);
        assertTrue(d.isPassThrough("sheet:unknown-sheet"));
        assertEquals("{\"z\":1,\"nested\":{\"a\":[1,2]}}", d.toJson().get("sheet:unknown-sheet").toString());
        assertEquals("{\"format\":\"jipx\"}", d.toJson().get("sheet:org.hkijena.jipipe:project-io").toString());
    }

    @Test
    void newDocumentIsProjectScopedAndSchema2() {
        JIPipeProjectSettingsDocument d = new JIPipeProjectSettingsDocument(p -> true);
        assertEquals(SettingsScope.PROJECT, d.document().scope());
        assertEquals(2, d.toJson().get("$schema").asInt());
    }
}