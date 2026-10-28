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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.draw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Font;

import static org.junit.jupiter.api.Assertions.*;

/**
 * #1339 (Review Focus #5): the reference font migration — a pre-migration
 * project (split font-family/font-size/font-style keys) loads with its font
 * settings intact (composed into the single {@code font} parameter), the new
 * format is idempotent, and a legacy JSON missing keys falls back to defaults
 * without an NPE.
 */
class DrawTextRoiFontMigrationTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static JIPipeFontFamilySizeStyleParameter compose(int size, int style, String family) {
        JIPipeFontFamilySizeStyleParameter f = new JIPipeFontFamilySizeStyleParameter();
        f.migrateFrom(family, size, style);
        return f;
    }

    private static String legacyStyleName(int style) {
        if ((style & Font.BOLD) != 0 && (style & Font.ITALIC) != 0) return "BoldItalic";
        if ((style & Font.BOLD) != 0) return "Bold";
        if ((style & Font.ITALIC) != 0) return "Italic";
        return "Plain";
    }

    private static JsonNode serialize(DrawTextRoiAlgorithm node) {
        try {
            return JsonUtils.getObjectMapper().readTree(JsonUtils.toJsonString(node));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static DrawTextRoiAlgorithm loadFrom(JsonNode node) {
        DrawTextRoiAlgorithm fresh = (DrawTextRoiAlgorithm) JIPipe.createNode(DrawTextRoiAlgorithm.class);
        fresh.fromJson(node, JIPipeValidationReportContext.UNSPECIFIED,
                new JIPipeValidationReport(), new JIPipeNotificationInbox());
        return fresh;
    }

    @Test
    void legacySplitKeysComposeIntoFontParameter() {
        // A node with a non-default font, saved in the NEW format.
        DrawTextRoiAlgorithm node = (DrawTextRoiAlgorithm) JIPipe.createNode(DrawTextRoiAlgorithm.class);
        node.setFont(compose(24, Font.BOLD, "DejaVu Sans"));
        ObjectNode json = (ObjectNode) serialize(node);

        // Turn it into a PRE-MIGRATION project: drop the "font" key, add the
        // legacy split keys (family string / size number / style enum name).
        JsonNode fontNode = json.get("font");
        json.remove("font");
        json.put("font-family", fontNode.get("family").textValue());
        json.put("font-size", fontNode.get("size").asInt());
        json.put("font-style", legacyStyleName(fontNode.get("style").asInt()));

        DrawTextRoiAlgorithm loaded = loadFrom(json);
        assertEquals("DejaVu Sans", loaded.getFont().getFamily());
        assertEquals(24, loaded.getFont().getSize());
        assertEquals(Font.BOLD, loaded.getFont().getStyle());
    }

    @Test
    void newFormatIsIdempotent() {
        DrawTextRoiAlgorithm node = (DrawTextRoiAlgorithm) JIPipe.createNode(DrawTextRoiAlgorithm.class);
        node.setFont(compose(18, Font.ITALIC, "SansSerif"));
        JsonNode json = serialize(node);

        DrawTextRoiAlgorithm loaded = loadFrom(json);
        // The "font" key deserializes directly; the legacy branch is not taken.
        assertEquals(18, loaded.getFont().getSize());
        assertEquals(Font.ITALIC, loaded.getFont().getStyle());
        assertEquals("SansSerif", loaded.getFont().getFamily());
        // No legacy keys remain in the serialized (loaded) node.
        ObjectNode reserialized = (ObjectNode) serialize(loaded);
        assertFalse(reserialized.has("font-family"));
        assertFalse(reserialized.has("font-style"));
        assertTrue(reserialized.has("font"));
    }

    @Test
    void legacyJsonMissingKeysFallsBackToDefaultsNoNpe() {
        DrawTextRoiAlgorithm node = (DrawTextRoiAlgorithm) JIPipe.createNode(DrawTextRoiAlgorithm.class);
        ObjectNode json = (ObjectNode) serialize(node);
        json.remove("font");
        // Only a style key is present — family/size must default.
        json.put("font-style", "Bold");

        DrawTextRoiAlgorithm loaded = loadFrom(json);
        assertEquals(JIPipeFontFamilySizeStyleParameter.defaultFamily(), loaded.getFont().getFamily());
        assertEquals(12, loaded.getFont().getSize());
        assertEquals(Font.BOLD, loaded.getFont().getStyle());
    }

    @Test
    void migratedFontHasNoColorField() {
        // These nodes take the text color from the ROI line color, so the
        // font parameter must not carry (and must not serialize) a color
        // field (Review Focus #5 / the spec's core contract).
        DrawTextRoiAlgorithm node = (DrawTextRoiAlgorithm) JIPipe.createNode(DrawTextRoiAlgorithm.class);
        node.setFont(compose(24, Font.BOLD, "DejaVu Sans"));
        ObjectNode json = (ObjectNode) serialize(node);

        // Simulate a pre-migration project (split keys), then load it.
        JsonNode fontNode = json.get("font");
        json.remove("font");
        json.put("font-family", fontNode.get("family").textValue());
        json.put("font-size", fontNode.get("size").asInt());
        json.put("font-style", legacyStyleName(fontNode.get("style").asInt()));

        DrawTextRoiAlgorithm loaded = loadFrom(json);
        JsonNode fontJson = JsonUtils.getObjectMapper().readTree(JsonUtils.toJsonString(loaded.getFont()));
        assertFalse(fontJson.has("color"), "The migrated font must not contain a color field: " + fontJson);
        assertTrue(fontJson.has("family") && fontJson.has("size") && fontJson.has("style"),
                "The migrated font must contain family/size/style: " + fontJson);
    }
}
