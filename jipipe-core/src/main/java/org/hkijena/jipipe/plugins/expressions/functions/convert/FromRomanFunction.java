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

@SetJIPipeDocumentation(name = "Roman numeral to number", description = "Parses a Roman numeral (standard notation) to its number, e.g. MCMXCIV = 1994. Uppercase and lowercase are accepted; non-canonical numerals are rejected.")
public class FromRomanFunction extends ExpressionFunction {

    public FromRomanFunction() {
        super("FROM_ROMAN", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("text", "The Roman numeral to parse.", Object.class);
        }
        return null;
    }

    @Override
    public String getSignature() {
        return getName() + "(text)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object value = parameters.get(0);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("A Roman numeral must be a string, got: " + value);
        }
        String s = ((String) value).trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Roman numeral must not be empty.");
        }
        int total = 0;
        for (int i = 0; i < s.length(); i++) {
            int v = valueOf(s.charAt(i));
            int next = (i + 1 < s.length()) ? valueOf(s.charAt(i + 1)) : 0;
            total += v < next ? -v : v;
        }
        // Validate the canonical form: a valid input re-encodes to itself
        if (!ToRomanFunction.toRomanCanonical(total).equals(s)) {
            throw new IllegalArgumentException("Not a valid Roman numeral: \"" + value + "\"");
        }
        return (long) total;
    }

    private int valueOf(char c) {
        switch (c) {
            case 'I': return 1;
            case 'V': return 5;
            case 'X': return 10;
            case 'L': return 50;
            case 'C': return 100;
            case 'D': return 500;
            case 'M': return 1000;
            default:
                throw new IllegalArgumentException("Not a Roman numeral character: \"" + c + "\"");
        }
    }
}
