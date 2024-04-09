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

import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "Split string", description = "Splits a string by the right parameter into an array. For example you can split a string 'a_b_c' by '_' into 'a', 'b', and 'c'")
public class StringSplitFunction extends ExpressionFunction {

    public StringSplitFunction() {
        super("SPLIT_STRING", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text to split", String.class);
            case 1:
                return new ParameterInfo("pattern", "The pattern where the string is split", String.class);
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
        String pattern = "" + parameters.get(1);
        return Arrays.asList(text.split(pattern));
    }
}
