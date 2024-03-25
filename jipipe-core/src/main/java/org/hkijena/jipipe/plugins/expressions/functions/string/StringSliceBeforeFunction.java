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

package org.hkijena.jipipe.plugins.expressions.functions.string;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Slice string", description = "Gets a substring. " +
        "This function will return the sub-string between two indices (first index is exclusive, second is inclusive). " +
        "If only one index is provided, all characters before the start index are included.")
public class StringSliceBeforeFunction extends ExpressionFunction {

    public StringSliceBeforeFunction() {
        super("SUBSTRING_BEFORE", 2, 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text to split", String.class);
            case 1:
                return new ParameterInfo("end", "The end index (exclusive)", Integer.class);
            case 2:
                return new ParameterInfo("start", "The start index (inclusive)", Integer.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(text, pattern)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String text = "" + parameters.get(0);

        int end = ((Number) parameters.get(1)).intValue();

        // Case: Limit string
        if (parameters.size() == 2 && end >= text.length()) {
            return text;
        }

        int start = parameters.size() >= 3 ? ((Number) parameters.get(2)).intValue() : 0;

        return text.substring(start, end);
    }
}
