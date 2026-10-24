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

class BaseConvertFunctionTest {

    private final BaseConvertFunction function = new BaseConvertFunction();

    private Object eval(Object... args) {
        return function.evaluate(Arrays.asList(args), new JIPipeExpressionVariablesMap());
    }

    @Test
    void defaultsToHex() {
        assertEquals("ff", eval(255L));
        assertEquals("0", eval(0L));
        assertEquals("a", eval(10L));
    }

    @Test
    void targetBase() {
        assertEquals("1000", eval(8L, 2L));
        assertEquals("10", eval(8L, 8L));
        assertEquals("ff", eval(255L, 16L));
    }

    @Test
    void stringSourceWithFromBase() {
        assertEquals(255L, eval("ff", 10L, 16L));
        assertEquals("a", eval("1010", 16L, 2L));
        assertEquals(35L, eval("z", 10L, 36L));
    }

    @Test
    void negative() {
        assertEquals("-ff", eval(-255L));
        assertEquals(-255L, eval("-ff", 10L, 16L));
    }

    /**
     * A non-decimal string without an explicit fromBase must fail closed,
     * not silently round-trip in the default base.
     */
    @Test
    void nonDecimalStringFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> eval("ff"));
    }

    @Test
    void invalidBaseThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval(255L, 37L));
        assertThrows(IllegalArgumentException.class, () -> eval(255L, 1L));
        assertThrows(IllegalArgumentException.class, () -> eval(255L, 10L, 1L));
    }

    @Test
    void invalidDigitThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval("zz", 10L, 16L));
    }

    @Test
    void nonNumericThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval("not a number"));
    }
}
