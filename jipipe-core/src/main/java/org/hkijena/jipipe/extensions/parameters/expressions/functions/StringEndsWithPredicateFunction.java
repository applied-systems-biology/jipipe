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

package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "String ends with", description = "Tests if a string starts with a pattern.")
public class StringEndsWithPredicateFunction extends ExpressionFunction {

    public StringEndsWithPredicateFunction() {
        super("STRING_ENDS_WITH", 2);
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
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        String text = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        return text.endsWith(pattern);
    }

    @Override
    public String getSignature() {
        return String.format("%s(%s, %s)", getName(), "text", "pattern");
    }
}
