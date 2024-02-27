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

package org.hkijena.jipipe.extensions.expressions.functions.string;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@SetJIPipeDocumentation(name = "Truncate string", description = "Truncates a string to the specified length")
public class StringTruncateFunction extends ExpressionFunction {

    public StringTruncateFunction() {
        super("STRING_TRUNCATE", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text", String.class);
            case 1:
                return new ParameterInfo("max_length", "The maximum length", Integer.class);
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
        String text = StringUtils.nullToEmpty(parameters.get(0));
        int maxLength = ((Number) parameters.get(1)).intValue();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
