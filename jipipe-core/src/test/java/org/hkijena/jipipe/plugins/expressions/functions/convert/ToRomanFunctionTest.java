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

class ToRomanFunctionTest {

    private final ToRomanFunction function = new ToRomanFunction();

    private String eval(Object... args) {
        return (String) function.evaluate(Arrays.asList(args), new JIPipeExpressionVariablesMap());
    }

    @Test
    void mappings() {
        assertEquals("I", eval(1L));
        assertEquals("IV", eval(4L));
        assertEquals("IX", eval(9L));
        assertEquals("XIV", eval(14L));
        assertEquals("XL", eval(40L));
        assertEquals("XC", eval(90L));
        assertEquals("CD", eval(400L));
        assertEquals("CM", eval(900L));
        assertEquals("MCMXCIV", eval(1994L));
        assertEquals("MMXXIV", eval(2024L));
        assertEquals("MMMCMXCIX", eval(3999L));
    }

    @Test
    void doubleInputAutoConverts() {
        assertEquals("MMXXIV", eval(2024.0));
    }

    @Test
    void stringInputParses() {
        assertEquals("MMXXIV", eval("2024"));
    }

    @Test
    void outOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval(0L));
        assertThrows(IllegalArgumentException.class, () -> eval(-1L));
        assertThrows(IllegalArgumentException.class, () -> eval(4000L));
    }
}
