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

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * #1339, Task 2: the full font parameter model ({@link
 * JIPipeFontFamilySizeStyleColorParameter}) — serialization round-trip,
 * lenient deserialization of missing keys (defaults), and {@code toFont()}.
 */
class JIPipeFontFamilySizeStyleColorParameterTest {

    @Test
    void roundTrip() throws Exception {
        JIPipeFontFamilySizeStyleColorParameter p = new JIPipeFontFamilySizeStyleColorParameter();
        p.migrateFrom("DejaVu Sans", 24, Font.BOLD + Font.ITALIC, new Color(0x3b, 0x9d, 0xff));

        String json = JsonUtils.getObjectMapper().writeValueAsString(p);
        JIPipeFontFamilySizeStyleColorParameter back =
                JsonUtils.getObjectMapper().readerFor(JIPipeFontFamilySizeStyleColorParameter.class).readValue(json);

        assertEquals(p, back);
        assertEquals("DejaVu Sans", back.getFamily());
        assertEquals(24, back.getSize());
        assertEquals(Font.BOLD + Font.ITALIC, back.getStyle());
        assertEquals(new Color(0x3b, 0x9d, 0xff).getRGB(), back.getColor().getRGB());
    }

    @Test
    void missingKeysFallBackToDefaults() throws Exception {
        // Only color present — family/size/style must default, no NPE
        JIPipeFontFamilySizeStyleColorParameter p =
                JsonUtils.getObjectMapper().readerFor(JIPipeFontFamilySizeStyleColorParameter.class)
                        .readValue("{\"color\":\"#FF0000\"}");
        assertEquals(JIPipeFontFamilySizeStyleParameter.defaultFamily(), p.getFamily());
        assertEquals(12, p.getSize());
        assertEquals(Font.PLAIN, p.getStyle());
        assertEquals(new Color(0xff, 0, 0).getRGB(), p.getColor().getRGB());
    }

    @Test
    void toFontReflectsFields() {
        JIPipeFontFamilySizeStyleColorParameter p = new JIPipeFontFamilySizeStyleColorParameter();
        p.migrateFrom("Dialog", 30, Font.ITALIC, Color.WHITE);
        Font f = p.toFont();
        assertEquals(30, f.getSize());
        assertEquals(Font.ITALIC, f.getStyle());
        assertEquals("Dialog", f.getFamily());
    }

    @Test
    void copyIsIndependent() {
        JIPipeFontFamilySizeStyleColorParameter p = new JIPipeFontFamilySizeStyleColorParameter();
        p.migrateFrom("Dialog", 20, Font.BOLD, Color.RED);
        JIPipeFontFamilySizeStyleColorParameter c = (JIPipeFontFamilySizeStyleColorParameter) p.copy();
        assertEquals(p, c);
        c.migrateFrom("Dialog", 99, Font.PLAIN, Color.BLUE);
        assertEquals(20, p.getSize());
        assertNotEquals(p, c);
    }
}
