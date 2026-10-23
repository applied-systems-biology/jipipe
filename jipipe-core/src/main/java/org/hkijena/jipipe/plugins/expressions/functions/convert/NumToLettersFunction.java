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

@SetJIPipeDocumentation(
        name = "Number to letters",
        description = "Converts a number to a string of letters, like spreadsheet column labels " +
                "(1 = A, 2 = B, …, 26 = Z, 27 = AA, 702 = ZZ, 703 = AAA, …) — bijective base-26. " +
                "The optional start parameter is the number that maps to the first letter (A); default 1.")
public class NumToLettersFunction extends ExpressionFunction {
    public NumToLettersFunction() {
        super("NUM_TO_LETTERS", 1, 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("number", "The number to convert to a column label.", Object.class);
            case 1:
                return new ParameterInfo("start", "The number that maps to the first letter (A). Default 1.", Object.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(number, start)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        long n = toLong(parameters.get(0));
        long start = parameters.size() > 1 ? toLong(parameters.get(1)) : 1;
        long n0 = n - start;                 // 0-based: `start` maps to 0 (letter A)
        if (n0 < 0) {
            throw new IllegalArgumentException(
                    "Number " + n + " is below the start value " + start +
                            " (the smallest valid input is " + start + ").");
        }
        long b = n0 + 1;                     // bijective base-26 value (A = 1)
        StringBuilder sb = new StringBuilder();
        while (b > 0) {
            b -= 1;
            sb.append((char) ('A' + (int) (b % 26)));
            b /= 26;
        }
        return sb.reverse().toString();
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
