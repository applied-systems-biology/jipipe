package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Sort array (descending)", description = "Sorts an array in descending order")
public class SortDescendingArrayFunction extends ExpressionFunction {

    public SortDescendingArrayFunction() {
        super("SORT_DESCENDING", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("array", "The array", Collection.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return ((Collection) parameters.get(0)).stream().sorted(Comparator.naturalOrder().reversed()).collect(Collectors.toList());
    }
}
