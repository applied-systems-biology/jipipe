package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Sort array (ascending)", description = "Sorts an array in ascending order")
public class SortAscendingArrayFunction extends ExpressionFunction {

    public SortAscendingArrayFunction() {
        super("SORT_ASCENDING", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("array", "The array", Collection.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return ((Collection) parameters.get(0)).stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }
}
