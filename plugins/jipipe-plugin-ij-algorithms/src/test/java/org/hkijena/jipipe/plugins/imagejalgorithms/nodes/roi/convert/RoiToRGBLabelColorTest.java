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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.convert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * #1339 (Review Focus #1 + #3): the legacy split label-font parameters
 * (label-font-family/label-size/label-foreground) of "Convert 2D ROI to RGB"
 * migrate into the color-less {@code label-font} parameter plus an <em>enabled</em>
 * {@code label-color} optional parameter (old label colors preserved). Fresh
 * nodes keep the color disabled, so at runtime each label follows the line
 * color of its own ROI.
 */
class RoiToRGBLabelColorTest {

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

    private static JsonNode serialize(RoiToRGBAlgorithm node) {
        try {
            return JsonUtils.getObjectMapper().readTree(JsonUtils.toJsonString(node));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static RoiToRGBAlgorithm loadFrom(JsonNode node) {
        RoiToRGBAlgorithm fresh = (RoiToRGBAlgorithm) JIPipe.createNode(RoiToRGBAlgorithm.class);
        fresh.fromJson(node, JIPipeValidationReportContext.UNSPECIFIED,
                new JIPipeValidationReport(), new JIPipeNotificationInbox());
        return fresh;
    }

    /**
     * Builds the JSON of a node that carries a non-default font, and turns it
     * into a PRE-MIGRATION project: drop the "label-font" key, add the legacy
     * split keys (family string / size number / style enum name / plain hex
     * color — the shape {@code JIPipeFontFamilySizeStyleParameter.legacyColor}
     * accepts).
     */
    private static ObjectNode legacyJson() {
        RoiToRGBAlgorithm node = (RoiToRGBAlgorithm) JIPipe.createNode(RoiToRGBAlgorithm.class);
        node.setLabelFont(compose(14, Font.BOLD, "DejaVu Sans"));
        ObjectNode json = (ObjectNode) serialize(node);

        JsonNode fontNode = json.get("label-font");
        json.remove("label-font");
        json.put("label-font-family", fontNode.get("family").textValue());
        json.put("label-size", fontNode.get("size").asInt());
        json.put("label-foreground", "#FF0000");
        // Note: no style key — this node's label font never had a style
        // parameter before the migration, so a legacy project does not
        // contain one either.
        return json;
    }

    @Test
    void legacyLabelForegroundMigratesIntoEnabledLabelColor() {
        RoiToRGBAlgorithm loaded = loadFrom(legacyJson());

        // The legacy foreground becomes an ENABLED label color (preserved).
        OptionalColorParameter labelColor = loaded.getLabelColor();
        assertTrue(labelColor.isEnabled(), "The legacy label-foreground must migrate into an enabled label color");
        assertEquals(new Color(0xFF0000), labelColor.getContent());

        // The font is the color-less family-size-style type, composed from the
        // legacy split keys. There was no legacy style key, so the style is
        // plain.
        JIPipeFontFamilySizeStyleParameter font = loaded.getLabelFont();
        assertEquals("DejaVu Sans", font.getFamily());
        assertEquals(14, font.getSize());
        assertEquals(Font.PLAIN, font.getStyle());
    }

    @Test
    void freshNodeHasDisabledLabelColorAndSizeStyleFont() {
        RoiToRGBAlgorithm fresh = (RoiToRGBAlgorithm) JIPipe.createNode(RoiToRGBAlgorithm.class);

        assertFalse(fresh.getLabelColor().isEnabled(),
                "A fresh node must not fix the label color — labels follow the ROI line color");
        // Default label font: legacy size 9, plain, default family.
        assertEquals(JIPipeFontFamilySizeStyleParameter.defaultFamily(), fresh.getLabelFont().getFamily());
        assertEquals(9, fresh.getLabelFont().getSize());
        assertEquals(Font.PLAIN, fresh.getLabelFont().getStyle());
    }

    @Test
    void migrationIsIdempotent() {
        RoiToRGBAlgorithm loaded = loadFrom(legacyJson());

        // The legacy keys must be gone after the migration (new format only).
        ObjectNode reserialized = (ObjectNode) serialize(loaded);
        assertFalse(reserialized.has("label-font-family"), "label-font-family must not survive the migration: " + reserialized);
        assertFalse(reserialized.has("label-size"), "label-size must not survive the migration: " + reserialized);
        assertFalse(reserialized.has("label-foreground"), "label-foreground must not survive the migration: " + reserialized);

        // Loading the migrated node again must not change anything.
        RoiToRGBAlgorithm reloaded = loadFrom(reserialized);
        assertTrue(reloaded.getLabelColor().isEnabled());
        assertEquals(new Color(0xFF0000), reloaded.getLabelColor().getContent());
        assertEquals(loaded.getLabelFont(), reloaded.getLabelFont());
    }
}
