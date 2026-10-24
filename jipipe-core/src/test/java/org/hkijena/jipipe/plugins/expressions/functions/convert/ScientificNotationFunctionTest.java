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

class ScientificNotationFunctionTest {

    private final ScientificNotationFunction function = new ScientificNotationFunction();

    private String eval(Object... args) {
        return (String) function.evaluate(Arrays.asList(args), new JIPipeExpressionVariablesMap());
    }

    @Test
    void defaultDecimals() {
        assertEquals("1.2E-4", eval(0.00012));
        assertEquals("1.5E3", eval(1500.0));
        assertEquals("1.0E3", eval(1000.0));
    }

    @Test
    void customDecimals() {
        assertEquals("1.235E3", eval(1234.5678, 3L));
        assertEquals("2E3", eval(1500.0, 0L));
    }

    @Test
    void zero() {
        assertEquals("0.0E0", eval(0.0));
    }

    @Test
    void negative() {
        assertEquals("-1.5E3", eval(-1500.0));
    }

    @Test
    void largeExponent() {
        assertEquals("1.0E12", eval(1.0e12));
    }

    @Test
    void negativeExponent() {
        assertEquals("3.1E-7", eval(3.14e-7));
    }

    @Test
    void nonNumericThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval("abc"));
    }

    @Test
    void fractionalDecimalsThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval(1500.0, 2.5));
    }

    @Test
    void negativeDecimalsThrows() {
        assertThrows(IllegalArgumentException.class, () -> eval(1500.0, -1L));
    }
}
