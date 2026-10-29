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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Number to Roman numeral", description = "Converts an integer (1–3999) to a Roman numeral, e.g. 1994 = MCMXCIV. Fractional input is truncated to an integer.")
public class ToRomanFunction extends ExpressionFunction {

    private static final long[][] VALUES = {{1000}, {900}, {500}, {400}, {100}, {90}, {50}, {40}, {10}, {9}, {5}, {4}, {1}};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    public ToRomanFunction() {
        super("TO_ROMAN", 1);
    }

    /**
     * Encodes the given non-negative number as a Roman numeral. Values up to 3999 use
     * standard notation; larger values use overflow-M notation (e.g. 4000 = MMMM).
     * Package-private for the canonical-form validation in {@link FromRomanFunction}.
     */
    static String toRomanCanonical(long n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (n >= VALUES[i][0]) {
                sb.append(SYMBOLS[i]);
                n -= VALUES[i][0];
            }
        }
        return sb.toString();
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("number", "The integer to convert (1 to 3999).", Object.class);
        }
        return null;
    }

    @Override
    public String getSignature() {
        return getName() + "(number)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        long n = toLong(parameters.get(0));
        if (n < 1 || n > 3999) {
            throw new IllegalArgumentException("Roman numerals are defined for 1 to 3999, got " + n + ".");
        }
        return toRomanCanonical(n);
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                // fall through to long
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert to a number: \"" + value + "\"", e);
            }
        }
        throw new IllegalArgumentException("Cannot convert to a number: " + value);
    }
}
