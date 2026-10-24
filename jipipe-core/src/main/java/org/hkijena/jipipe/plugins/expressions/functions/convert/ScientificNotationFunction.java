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
import java.util.Locale;

@SetJIPipeDocumentation(name = "Number to scientific notation", description = "Formats a number as a scientific-notation string (e.g. 0.00012 = 1.2E-4, 1500 = 1.5E3). The exponent is not zero-padded and carries no plus sign. The optional decimals parameter sets the mantissa precision (default 1).")
public class ScientificNotationFunction extends ExpressionFunction {

    public ScientificNotationFunction() {
        super("SCIENTIFIC_NOTATION", 1, 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("number", "The number to format.", Object.class);
            case 1:
                return new ParameterInfo("decimals", "Mantissa precision (whole number ≥ 0). Default 1.", Object.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(number, decimals)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        double value = toDouble(parameters.get(0));
        int decimals = 1;
        if (parameters.size() > 1) {
            double d = toDouble(parameters.get(1));
            if (d < 0 || d != Math.floor(d)) {
                throw new IllegalArgumentException("decimals must be a whole number ≥ 0, got: " + d);
            }
            decimals = (int) d;
        }
        String raw = String.format(Locale.ROOT, "%." + decimals + "E", value);   // e.g. "1.5E+03", "1.2E-04", "0.0E+00" (Locale.ROOT: locale-neutral digits; precision is a validated integer, safe to embed literally)
        int e = raw.indexOf('E');
        String mantissa = raw.substring(0, e);
        String exp = raw.substring(e + 1);                                   // e.g. "+03", "-04", "+00"
        boolean neg = exp.startsWith("-");
        String digits = exp.replaceFirst("^[+-]", "");
        digits = digits.replaceFirst("^0+(?=.)", "");                        // strip leading zeros, keep ≥ 1 digit
        return mantissa + "E" + (neg ? "-" : "") + digits;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert to a number: \"" + value + "\"", e);
            }
        }
        throw new IllegalArgumentException("Cannot convert to a number: " + value);
    }
}
