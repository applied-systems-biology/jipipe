/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.expressions.functions.string;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "last index of substring", description = "Finds the last index of a substring in the string. " +
        "Returns -1 if the substring is not contained in the string.")
public class StringLastIndexOfFunction extends ExpressionFunction {

    public StringLastIndexOfFunction() {
        super("STRING_LAST_INDEX_OF", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text to search in", String.class);
            case 1:
                return new ParameterInfo("pattern", "The pattern to search in the text", String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        String text = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        return text.lastIndexOf(pattern);
    }

    @Override
    public String getSignature() {
        return String.format("%s(%s, %s)", getName(), "text", "pattern");
    }
}
