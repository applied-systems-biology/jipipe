package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

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
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        return ((Collection)parameters.get(0)).stream().sorted(Comparator.naturalOrder().reversed()).collect(Collectors.toList());
    }
}
