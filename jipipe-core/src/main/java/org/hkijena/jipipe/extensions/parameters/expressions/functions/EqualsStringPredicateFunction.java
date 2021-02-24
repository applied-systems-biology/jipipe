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

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.List;
import java.util.Objects;

@JIPipeDocumentation(name = "String equals", description = "Tests if the left operand is the same as the right operand.")
public class EqualsStringPredicateFunction extends ExpressionFunction {

    public EqualsStringPredicateFunction() {
        super("STRING_EQUALS", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        String text = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        return Objects.equals(text, pattern);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text1", "The first text", String.class);
            case 1:
                return new ParameterInfo("text2", "The second text", String.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return String.format("%s(%s, %s)", getName(), "text1", "text2");
    }
}
