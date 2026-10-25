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

package org.hkijena.jipipe.plugins.parameters.library.primitives;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;

import static org.junit.jupiter.api.Assertions.*;

/**
 * #1339: the shared legacy font-migration logic that every migrated node's
 * {@code onDeserialized} relies on — composing the old split
 * family/size/style/color parameters into one {@link JIPipeFontParameter},
 * with defaults for absent keys (Review Focus #5: old projects load intact).
 */
class FontParameterMigrationTest {

    private static JsonNode json(String s) throws Exception {
        return JsonUtils.getObjectMapper().readTree(s);
    }

    @Test
    void composesAllLegacyKeys() throws Exception {
        JsonNode n = json("{\"font-family\":\"DejaVu Sans\",\"font-size\":24,\"font-style\":\"Bold\",\"font-color\":\"#3B9DFF\"}");
        JIPipeFontParameter p = JIPipeFontParameter.composeFromLegacy(n, "font-family", "font-size", "font-style", "font-color");
        assertEquals("DejaVu Sans", p.getFamily());
        assertEquals(24, p.getSize());
        assertEquals(Font.BOLD, p.getStyle());
        assertEquals(new Color(0x3b, 0x9d, 0xff).getRGB(), p.getColor().getRGB());
    }

    @Test
    void missingKeysFallBackToDefaultsNoNpe() throws Exception {
        // Only the family present — size/style/color default
        JsonNode n = json("{\"font-family\":\"Serif\"}");
        JIPipeFontParameter p = JIPipeFontParameter.composeFromLegacy(n, "font-family", "font-size", "font-style", "font-color");
        assertEquals("Serif", p.getFamily());
        assertEquals(12, p.getSize());
        assertEquals(Font.PLAIN, p.getStyle());
        assertEquals(Color.WHITE.getRGB(), p.getColor().getRGB());
    }

    @Test
    void emptyObjectYieldsPureDefaults() throws Exception {
        JIPipeFontParameter p = JIPipeFontParameter.composeFromLegacy(json("{}"), "a", "b", "c", "d");
        assertEquals(JIPipeFontParameter.defaultFamily(), p.getFamily());
        assertEquals(12, p.getSize());
        assertEquals(Font.PLAIN, p.getStyle());
        assertEquals(Color.WHITE.getRGB(), p.getColor().getRGB());
    }

    @Test
    void styleNamesMapToFlags() {
        assertEquals(Font.PLAIN, JIPipeFontParameter.styleNameToFlags("Plain"));
        assertEquals(Font.BOLD, JIPipeFontParameter.styleNameToFlags("Bold"));
        assertEquals(Font.ITALIC, JIPipeFontParameter.styleNameToFlags("Italic"));
        assertEquals(Font.BOLD + Font.ITALIC, JIPipeFontParameter.styleNameToFlags("BoldItalic"));
        assertEquals(Font.PLAIN, JIPipeFontParameter.styleNameToFlags(null));
        assertEquals(Font.PLAIN, JIPipeFontParameter.styleNameToFlags("???"));
    }

    @Test
    void optionalColorLegacyFormIsRead() throws Exception {
        // A legacy OptionalColorParameter serializes as {content, enabled}
        JsonNode n = json("{\"font-family\":\"Dialog\",\"font-size\":10,\"text-color\":{\"content\":\"#00CC00\",\"enabled\":true}}");
        JIPipeFontParameter p = JIPipeFontParameter.composeFromLegacy(n, "font-family", "font-size", null, "text-color");
        assertEquals(10, p.getSize());
        assertEquals(new Color(0, 0xcc, 0).getRGB(), p.getColor().getRGB());
    }

    @Test
    void plainHexColorLegacyFormIsRead() throws Exception {
        JsonNode n = json("{\"label-font-family\":\"Dialog\",\"label-size\":9,\"label-foreground\":\"#FF8800\"}");
        JIPipeFontParameter p = JIPipeFontParameter.composeFromLegacy(n, "label-font-family", "label-size", null, "label-foreground");
        assertEquals(new Color(0xff, 0x88, 0).getRGB(), p.getColor().getRGB());
        assertEquals("Dialog", p.getFamily());
        assertEquals(9, p.getSize());
    }
}
