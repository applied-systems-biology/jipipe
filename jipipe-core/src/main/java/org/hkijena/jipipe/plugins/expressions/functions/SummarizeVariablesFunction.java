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

package org.hkijena.jipipe.plugins.expressions.functions;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Summarize variables", description = "Generates a string that displays all variables")
public class SummarizeVariablesFunction extends ExpressionFunction {

    public SummarizeVariablesFunction() {
        super("SUMMARIZE_VARIABLES", 0, 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("Delimiter", "Delimiter string between entries (default is ' ')", String.class);
            case 1:
                return new ParameterInfo("Equals", "Equals string between keys and values (default is '=')", String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String delimiter = " ";
        String equals = "=";
        if (parameters.size() >= 1)
            delimiter = StringUtils.nullToEmpty(parameters.get(0));
        if (parameters.size() >= 2)
            equals = StringUtils.nullToEmpty(parameters.get(1));
        String finalEquals = equals;
        return variables.keySet().stream().sorted().map(key -> key + finalEquals + variables.get(key)).collect(Collectors.joining(delimiter));
    }
}
