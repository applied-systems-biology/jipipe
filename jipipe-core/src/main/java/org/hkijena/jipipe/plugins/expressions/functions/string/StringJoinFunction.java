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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Join string array", description = "Given an array of strings, the array is joined into one string with a delimiter. " +
        "For example you can join 'a', 'b' and 'c' into 'a,b,c'")
public class StringJoinFunction extends ExpressionFunction {

    public StringJoinFunction() {
        super("JOIN_STRING", 1, 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "The array of strings", Collection.class);
            case 1:
                return new ParameterInfo("delimiter", "The delimiter that is put in between the strings", String.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(array, delimiter)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        List<String> strings = new ArrayList<>();
        for (Object item : (Collection<?>) parameters.get(0)) {
            strings.add("" + item);
        }
        String delimiter = parameters.size() > 1 ? ("" + parameters.get(1)) : "";
        return String.join(delimiter, strings);
    }
}
