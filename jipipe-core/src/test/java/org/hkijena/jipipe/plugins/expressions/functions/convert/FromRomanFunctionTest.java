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

package org.hkijena.jipipe.plugins.expressions.functions.convert;

import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FromRomanFunctionTest {

    private final FromRomanFunction function = new FromRomanFunction();

    private long eval(Object... args) {
        return (Long) function.evaluate(Arrays.asList(args), new JIPipeExpressionVariablesMap());
    }

    @Test
    void mappings() {
        assertEquals(1L, eval("I"));
        assertEquals(4L, eval("IV"));
        assertEquals(9L, eval("IX"));
        assertEquals(14L, eval("XIV"));
        assertEquals(40L, eval("XL"));
        assertEquals(90L, eval("XC"));
        assertEquals(400L, eval("CD"));
        assertEquals(900L, eval("CM"));
        assertEquals(1990L, eval("MCMXC"));
        assertEquals(1994L, eval("MCMXCIV"));
        assertEquals(2024L, eval("MMXXIV"));
        assertEquals(3999L, eval("MMMCMXCIX"));
    }

    @Test
    void lowercaseAccepted() {
        assertEquals(1994L, eval("mcmxciv"));
    }

    @Test
    void nonCanonicalThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval("IIII"));
        assertThrows(IllegalArgumentException.class, () -> eval("VV"));
        assertThrows(IllegalArgumentException.class, () -> eval("XM"));
        assertThrows(IllegalArgumentException.class, () -> eval("IC"));
    }

    @Test
    void emptyThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval(""));
    }

    @Test
    void invalidCharThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval("ABC"));
    }

    @Test
    void numberInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval(5L));
    }

    /**
     * Extended/overflow-M notation (MMMM = 4000) is outside the documented 1–3999
     * domain and would break the TO_ROMAN/FROM_ROMAN round-trip invariant.
     */
    @Test
    void outOfDomainThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval("MMMM"));
    }

    @Test
    void roundTrip() {
        ToRomanFunction toRoman = new ToRomanFunction();
        for (long n : new long[]{1, 4, 9, 1994, 3999}) {
            String roman = (String) toRoman.evaluate(Arrays.asList(n), new JIPipeExpressionVariablesMap());
            assertEquals(n, eval(roman));
        }
    }
}
