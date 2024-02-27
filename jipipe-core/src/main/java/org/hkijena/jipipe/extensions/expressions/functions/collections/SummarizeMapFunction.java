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

package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Summarize map", description = "Generates a string that displays all entries of a map")
public class SummarizeMapFunction extends ExpressionFunction {

    public SummarizeMapFunction() {
        super("SUMMARIZE_MAP", 1, 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("Map", "The map to be summarized", Map.class);
            case 1:
                return new ParameterInfo("Delimiter", "Delimiter string between entries (default is ' ')", String.class);
            case 2:
                return new ParameterInfo("Equals", "Equals string between keys and values (default is '=')", String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Map<?, ?> map = (Map<?, ?>) parameters.get(0);
        String delimiter = " ";
        String equals = "=";
        if (parameters.size() >= 2)
            delimiter = StringUtils.nullToEmpty(parameters.get(1));
        if (parameters.size() >= 3)
            equals = StringUtils.nullToEmpty(parameters.get(2));
        String finalEquals = equals;
        return map.keySet().stream().sorted().map(key -> key + finalEquals + map.get(key)).collect(Collectors.joining(delimiter));
    }
}
