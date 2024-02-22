package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Summarize annotations map", description = "Generates a string that displays all entries of a map that contains text annotation keys and values")
public class SummarizeAnnotationsMapFunction extends ExpressionFunction {

    public SummarizeAnnotationsMapFunction() {
        super("SUMMARIZE_ANNOTATIONS_MAP", 1, 5);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("Map", "The map to be summarized", Map.class);
            case 1:
                return new ParameterInfo("Filter prefix", "Only consider annotation keys with the given prefix (default is '\"\"')", String.class);
            case 2:
                return new ParameterInfo("Only values", "If TRUE, only values are written (default is 'FALSE'", Boolean.class);
            case 3:
                return new ParameterInfo("Delimiter", "Delimiter string between entries (default is '\" \"')", String.class);
            case 4:
                return new ParameterInfo("Equals", "Equals string between keys and values (default is '\"=\"')", String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Map<?, ?> map = (Map<?, ?>) parameters.get(0);
        String delimiter = " ";
        String equals = "=";
        String prefix = "";
        boolean onlyValues = false;
        if (parameters.size() >= 2)
            prefix = StringUtils.nullToEmpty(parameters.get(1));
        if (parameters.size() >= 3)
            onlyValues = (boolean) parameters.get(2);
        if (parameters.size() >= 4)
            delimiter = StringUtils.nullToEmpty(parameters.get(3));
        if (parameters.size() >= 5)
            equals = StringUtils.nullToEmpty(parameters.get(4));
        String finalEquals = equals;
        String finalPrefix = prefix;
        if (onlyValues)
            return map.keySet().stream().filter(key -> key.toString().startsWith(finalPrefix)).sorted().map(key -> "" + map.get(key)).collect(Collectors.joining(delimiter));
        else
            return map.keySet().stream().filter(key -> key.toString().startsWith(finalPrefix)).sorted().map(key -> key + finalEquals + map.get(key)).collect(Collectors.joining(delimiter));
    }
}
