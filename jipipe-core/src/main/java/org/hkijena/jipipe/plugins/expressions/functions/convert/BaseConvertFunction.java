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

@SetJIPipeDocumentation(name = "Convert number base", description = "Converts a number between bases 2–36. A number input is the value itself; a string input is parsed in the fromBase (default 10). The result is a string in the toBase (default 16, lowercase), or a number when toBase is 10. Digits a–z are accepted for bases above 10.")
public class BaseConvertFunction extends ExpressionFunction {

    public BaseConvertFunction() {
        super("BASE_CONVERT", 1, 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("value", "The number to convert. A number is the value itself; a string is parsed in the fromBase.", Object.class);
            case 1:
                return new ParameterInfo("toBase", "Base to convert to (2–36). Default 16.", Object.class);
            case 2:
                return new ParameterInfo("fromBase", "Base the value is given in (2–36). Default 10; only applies to string inputs.", Object.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(value, toBase, fromBase)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object value = parameters.get(0);
        int toBase = parameters.size() > 1 ? (int) toLong(parameters.get(1)) : 16;
        int fromBase = parameters.size() > 2 ? (int) toLong(parameters.get(2)) : 10;
        checkBase(fromBase, "fromBase");
        checkBase(toBase, "toBase");
        long n;
        if (value instanceof Number) {
            n = ((Number) value).longValue();
        } else if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Value must not be empty.");
            }
            boolean neg = s.startsWith("-");
            if (neg || s.startsWith("+")) {
                s = s.substring(1);
            }
            try {
                n = Long.parseLong(s, fromBase);
                if (neg) {
                    n = -n;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot parse \"" + value + "\" in base " + fromBase + ".", e);
            }
        } else {
            throw new IllegalArgumentException("Cannot convert to a number: " + value);
        }
        if (toBase == 10) {
            return n;
        }
        return Long.toString(n, toBase);
    }

    private void checkBase(int base, String name) {
        if (base < 2 || base > 36) {
            throw new IllegalArgumentException(name + " must be between 2 and 36, got: " + base);
        }
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
