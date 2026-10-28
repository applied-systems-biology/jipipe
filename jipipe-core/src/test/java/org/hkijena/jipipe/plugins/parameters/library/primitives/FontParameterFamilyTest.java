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
 * #1339: the lighter members of the font parameter family —
 * family+size / family+size+style / family+size+color — serialization
 * round-trips (with the absent fields really absent from the JSON), plain
 * {@code toFont()}, and the shared {@code fromLegacyKeys} migration helpers.
 */
class FontParameterFamilyTest {

    private static JsonNode json(String s) throws Exception {
        return JsonUtils.getObjectMapper().readTree(s);
    }

    @Test
    void sizeStyleRoundTrip() throws Exception {
        JIPipeFontFamilySizeStyleParameter p = new JIPipeFontFamilySizeStyleParameter();
        p.setFamily("DejaVu Sans");
        p.setSize(24);
        p.setStyle(Font.BOLD + Font.ITALIC);

        String json = JsonUtils.getObjectMapper().writeValueAsString(p);
        JIPipeFontFamilySizeStyleParameter back =
                JsonUtils.getObjectMapper().readerFor(JIPipeFontFamilySizeStyleParameter.class).readValue(json);

        assertEquals(p, back);
        JsonNode n = JsonUtils.getObjectMapper().readTree(json);
        assertTrue(n.has("family"));
        assertTrue(n.has("size"));
        assertTrue(n.has("style"));
        assertFalse(n.has("color"));
    }

    @Test
    void familySizeToFontIsPlain() {
        JIPipeFontFamilySizeParameter p = new JIPipeFontFamilySizeParameter();
        p.setFamily("Dialog");
        p.setSize(30);
        Font f = p.toFont();
        assertEquals(30, f.getSize());
        assertEquals(Font.PLAIN, f.getStyle());
    }

    @Test
    void familySizeColorRoundTrip() throws Exception {
        JIPipeFontFamilySizeColorParameter p = new JIPipeFontFamilySizeColorParameter();
        p.setFamily("DejaVu Sans");
        p.setSize(24);
        p.setColor(new Color(0x3b, 0x9d, 0xff));

        String json = JsonUtils.getObjectMapper().writeValueAsString(p);
        JIPipeFontFamilySizeColorParameter back =
                JsonUtils.getObjectMapper().readerFor(JIPipeFontFamilySizeColorParameter.class).readValue(json);

        assertEquals(p, back);
        JsonNode n = JsonUtils.getObjectMapper().readTree(json);
        assertTrue(n.has("color"));
        assertFalse(n.has("style"));
    }

    @Test
    void missingKeysFallBackToDefaults() throws Exception {
        JsonNode empty = json("{}");

        JIPipeFontFamilySizeParameter fs =
                JIPipeFontFamilySizeParameter.fromLegacyKeys(empty, "a", "b", null, null, 12);
        assertEquals(JIPipeFontFamilySizeStyleParameter.defaultFamily(), fs.getFamily());
        assertEquals(12, fs.getSize());

        JIPipeFontFamilySizeStyleParameter fsStyle =
                JIPipeFontFamilySizeStyleParameter.fromLegacyKeys(empty, "a", "b", "c", "d", 12);
        assertEquals(JIPipeFontFamilySizeStyleParameter.defaultFamily(), fsStyle.getFamily());
        assertEquals(12, fsStyle.getSize());
        assertEquals(Font.PLAIN, fsStyle.getStyle());

        JIPipeFontFamilySizeColorParameter fsColor =
                JIPipeFontFamilySizeColorParameter.fromLegacyKeys(empty, "a", "b", "c", "d", 12);
        assertEquals(JIPipeFontFamilySizeStyleParameter.defaultFamily(), fsColor.getFamily());
        assertEquals(12, fsColor.getSize());
        assertEquals(Color.WHITE.getRGB(), fsColor.getColor().getRGB());

        // A non-default defaultSize is respected when the size key is absent
        JIPipeFontFamilySizeParameter fs18 =
                JIPipeFontFamilySizeParameter.fromLegacyKeys(empty, "a", "b", null, null, 18);
        assertEquals(18, fs18.getSize());
    }

    @Test
    void fromLegacyKeysComposesStyle() throws Exception {
        // Same legacy values as FontParameterMigrationTest.composesAllLegacyKeys;
        // the color key is ignored by the style class
        JsonNode n = json("{\"font-family\":\"DejaVu Sans\",\"font-size\":24,\"font-style\":\"Bold\",\"font-color\":\"#3B9DFF\"}");
        JIPipeFontFamilySizeStyleParameter p =
                JIPipeFontFamilySizeStyleParameter.fromLegacyKeys(n, "font-family", "font-size", "font-style", "font-color", 12);
        assertEquals("DejaVu Sans", p.getFamily());
        assertEquals(24, p.getSize());
        assertEquals(JIPipeFontFamilySizeStyleParameter.styleNameToFlags("Bold"), p.getStyle());
        assertEquals(Font.BOLD, p.getStyle());
    }

    @Test
    void fromLegacyKeysComposesColor() throws Exception {
        // Same legacy values as FontParameterMigrationTest.composesAllLegacyKeys;
        // the style key is ignored by the color class
        JsonNode n = json("{\"font-family\":\"DejaVu Sans\",\"font-size\":24,\"font-style\":\"Bold\",\"font-color\":\"#3B9DFF\"}");
        JIPipeFontFamilySizeColorParameter p =
                JIPipeFontFamilySizeColorParameter.fromLegacyKeys(n, "font-family", "font-size", "font-style", "font-color", 12);
        assertEquals("DejaVu Sans", p.getFamily());
        assertEquals(24, p.getSize());
        assertEquals(new Color(0x3b, 0x9d, 0xff).getRGB(), p.getColor().getRGB());
    }

    @Test
    void canvasNoteStyleMigratesPrefixedKeys() throws Exception {
        // #1339 pin: the exact key set + prefix DefaultTextBoxGraphCanvasNote.onDeserialized
        // uses for the title style (the nested collection is loaded via flat slash-keys),
        // plus the title's default size (18). If the node's keys ever drift, this input
        // records what they were. All keys belong to the title prefix — the node composes
        // each style from its own prefix.
        JsonNode n = json("{\"title-style/font-family\":\"Dialog\",\"title-style/font-size\":18,\"title-style/color\":\"#000000\"}");
        JIPipeFontFamilySizeStyleColorParameter p =
                JIPipeFontFamilySizeStyleColorParameter.fromLegacyKeys(n,
                        "title-style/font-family", "title-style/font-size",
                        "title-style/font-style", "title-style/color", 18);
        assertEquals("Dialog", p.getFamily());
        assertEquals(18, p.getSize());
        assertEquals(Color.BLACK.getRGB(), p.getColor().getRGB());
        assertEquals(Font.PLAIN, p.getStyle());
    }
}
