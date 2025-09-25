package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

import java.util.HashMap;
import java.util.Map;

public class NamedStringQueryExpressionList extends JIPipeListParameter<NamedStringQueryExpression> {

    public NamedStringQueryExpressionList() {
        super(NamedStringQueryExpression.class);
    }

    public NamedStringQueryExpressionList(NamedStringQueryExpressionList other) {
        super(NamedStringQueryExpression.class);
        for (NamedStringQueryExpression expression : other) {
            add(new NamedStringQueryExpression(expression));
        }
    }

    /**
     * Filters only the named strings where the query applies.
     *
     * @param input the input map
     * @return map that contains only the key value pairs queried to be true
     */
    public Map<String, String> query(Map<String, String> input) {
        Map<String, String> result = new HashMap<>();
        Map<String, StringQueryExpression> expressionMap = new HashMap<>();
        for (NamedStringQueryExpression expression : this) {
            expressionMap.put(expression.getKey(), expression.getValue());
        }
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            variableSet.set("key", entry.getKey());
            variableSet.set("value", entry.getValue());
            StringQueryExpression expression = expressionMap.getOrDefault(entry.getKey(), null);
            if (expression != null) {
                if (expression.test(variableSet)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }
}
