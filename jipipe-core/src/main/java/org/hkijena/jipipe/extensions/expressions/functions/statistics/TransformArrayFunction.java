package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Transform array", description = "Applies an expression for each item in the array and returns the result. Equivalent to FOREACH.")
public class TransformArrayFunction extends ExpressionFunction {
    public TransformArrayFunction() {
        super("TRANSFORM_ARRAY", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        JIPipeExpressionVariablesMap localVariables = new JIPipeExpressionVariablesMap(variables);
        List<?> items = ImmutableList.copyOf((Collection<?>) parameters.get(0));
        localVariables.set("values", items);
        String function = JIPipeExpressionEvaluator.unescapeString("\"" + parameters.get(1) + "\"");
        List<Object> results = new ArrayList<>();
        for (Object item : items) {
            localVariables.set("value", item);
            results.add(JIPipeExpressionParameter.getEvaluatorInstance().evaluate(function, localVariables));
        }
        return results;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Sequence", "The array.", Collection.class);
        } else {
            return new ParameterInfo("Expression", "An expression that is applied to each item in the array. The value is written into a variable <code>value</code>, while all values can be access via <code>values</code>. Tip: use ${ [your expression] } to provide the expression.", String.class);
        }
    }
}
