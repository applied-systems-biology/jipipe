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

class NumToLettersFunctionTest {

    private final NumToLettersFunction function = new NumToLettersFunction();

    private String eval(Object... args) {
        return (String) function.evaluate(Arrays.asList(args), new JIPipeExpressionVariablesMap());
    }

    /**
     * The classic (default, start = 1) bijective base-26 mapping, including the
     * 26/27, double-letter and triple-letter boundaries.
     */
    @Test
    void classicStartOne() {
        assertEquals("A", eval(1L));
        assertEquals("Z", eval(26L));
        assertEquals("AA", eval(27L));
        assertEquals("AB", eval(28L));
        assertEquals("AZ", eval(52L));
        assertEquals("BA", eval(53L));
        assertEquals("ZZ", eval(702L));
        assertEquals("AAA", eval(703L));
    }

    @Test
    void doubleInputAutoConverts() {
        assertEquals("AA", eval(27.0d));
    }

    @Test
    void stringInputParses() {
        assertEquals("E", eval("5"));
    }

    /**
     * With start = 0 the mapping becomes 0-indexed.
     */
    @Test
    void startZeroIsZeroIndexed() {
        assertEquals("A", eval(0L, 0L));
        assertEquals("Z", eval(25L, 0L));
        assertEquals("AA", eval(26L, 0L));
    }

    @Test
    void belowStartThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval(0L));
    }

    @Test
    void nonNumericThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval("not a number"));
    }
}
