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

package org.hkijena.jipipe.plugins.parameters;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleColorParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.JIPipeFontFamilySizeStyleParameter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * #1339, Task 2: the font parameter family is registered under the four
 * stable string-literal IDs (Global Constraints), each bound to its value
 * class, and the dev {@code jipipe:font} ID is gone (Review Focus #2).
 * <p>
 * The registry is a LIVE service component, so this test asserts <em>presence</em>
 * of the four curated core IDs (and their field classes) rather than pinning a
 * total count — plugins may legitimately register additional parameter types.
 */
class FontRegistrationIdsTest {

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
    }

    @Test
    void fontFamilySizeIsRegistered() {
        assertRegistered("jipipe:font-family-size", JIPipeFontFamilySizeParameter.class);
    }

    @Test
    void fontFamilySizeStyleIsRegistered() {
        assertRegistered("jipipe:font-family-size-style", JIPipeFontFamilySizeStyleParameter.class);
    }

    @Test
    void fontFamilySizeColorIsRegistered() {
        assertRegistered("jipipe:font-family-size-color", JIPipeFontFamilySizeColorParameter.class);
    }

    @Test
    void fontFamilySizeStyleColorIsRegistered() {
        assertRegistered("jipipe:font-family-size-style-color", JIPipeFontFamilySizeStyleColorParameter.class);
    }

    @Test
    void devFontIdIsGone() {
        assertNull(JIPipe.getParameterTypes().getInfoById("jipipe:font"),
                "the dev 'jipipe:font' ID must be deleted (Review Focus #2)");
    }

    private static void assertRegistered(String id, Class<?> expectedFieldClass) {
        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoById(id);
        assertNotNull(info, "parameter type '" + id + "' is not registered");
        assertEquals(expectedFieldClass, info.getFieldClass(),
                "'" + id + "' is bound to the wrong field class: " + info.getFieldClass());
    }
}
