package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Sort array (ascending)", description = "Sorts an array in ascending order")
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
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        return ((Collection) parameters.get(0)).stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }
}
