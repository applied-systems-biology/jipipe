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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /**
     * Project-scope {@code $meta} must be <b>fully deterministic</b>: a project
     * file is shared and version-controlled, so no per-instance id and no wall
     * clock may enter it. Otherwise the first save of every existing legacy
     * project would differ from the original for no reason (a spurious
     * one-line diff per project), even though later saves are stable.
     * <p>
     * The chain is: legacy (unprefixed, no {@code $meta}) → read → write, run
     * twice through <em>independent</em> document instances, which is exactly
     * what two consecutive load→save cycles do. Both outputs must be identical
     * and {@code $meta} must hold {@code written-by-version} only.
     */
    @Test
    void projectMetaIsFullyDeterministicAndHoldsOnlyWrittenByVersion() throws Exception {
        JsonNode legacy = JsonUtils.getObjectMapper().readTree(
                "{\"org.hkijena.jipipe:project-io\":{\"format\":\"jipx\"},\"unknown-sheet\":{\"z\":1}}");

        JIPipeProjectSettingsDocument first = JIPipeProjectSettingsDocument.fromJson(legacy, p -> true);
        first.stampMeta("6.0.0");
        JsonNode firstOut = first.toJson();

        JIPipeProjectSettingsDocument second = JIPipeProjectSettingsDocument.fromJson(legacy, p -> true);
        second.stampMeta("6.0.0");
        JsonNode secondOut = second.toJson();

        assertEquals(firstOut, secondOut, "Two independent read/write cycles of the same input must produce identical JSON");
        JsonNode meta = firstOut.get("$meta");
        assertTrue(meta.isObject(), "$meta must be written");
        assertEquals(1, meta.size(), "Project $meta must hold written-by-version only, but holds: " + meta);
        assertEquals("6.0.0", meta.get("written-by-version").asText());
        assertFalse(meta.has("writer-id"), "A random writer-id would make the first save of every legacy project a diff");
        assertFalse(meta.has("written-at"), "A timestamp would make every save a diff");
    }

    /**
     * The new-shape discriminator must not be a bare {@code has("$schema")}:
     * a hand-edited or third-party legacy object may contain a literal
     * {@code "$schema"} key. Without the corroborating markers ({@code $meta}
     * or a {@code sheet:} block) such an object is still LEGACY — its sheet
     * keys get prefixed and the stray {@code "$schema"} key is preserved
     * verbatim as a pass-through block.
     */
    @Test
    void legacyObjectWithStraySchemaKeyIsStillTreatedAsLegacy() throws Exception {
        JsonNode legacy = JsonUtils.getObjectMapper().readTree(
                "{\"$schema\":\"https://example.org/my-schema.json\",\"unknown-sheet\":{\"z\":1}}");
        JIPipeProjectSettingsDocument d = JIPipeProjectSettingsDocument.fromJson(legacy, p -> false);
        JsonNode out = d.toJson();
        assertEquals(1, out.get("sheet:unknown-sheet").get("z").asInt(), "Legacy keys must be prefixed");
        assertTrue(d.isPassThrough("sheet:$schema"), "The stray key must survive as a block");
        assertEquals("https://example.org/my-schema.json", out.get("sheet:$schema").asText(),
                "The stray $schema value must be preserved verbatim");
        assertEquals(2, out.get("$schema").asInt(), "The document's own $schema must be the numeric schema version");
    }
}
