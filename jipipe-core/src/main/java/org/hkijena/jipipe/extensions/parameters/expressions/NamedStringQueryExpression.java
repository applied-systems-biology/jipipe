/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.expressions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameter;

import java.util.HashMap;
import java.util.Map;

public class NamedStringQueryExpression extends PairParameter<String, StringQueryExpression> {

    public static final String DOCUMENTATION_DESCRIPTION = "Returns the named strings only if the expression returns TRUE for key value pair. Within the expression, there are two variables available: 'key' and 'value'. Example: <pre>(key CONTAINS \"sample\") AND (value CONTAINS \"aspergillus\")</pre>";

    public NamedStringQueryExpression() {
        super(String.class, StringQueryExpression.class);
        setKey("");
        setValue(new StringQueryExpression("TRUE"));
    }

    public NamedStringQueryExpression(PairParameter<String, StringQueryExpression> other) {
        super(other);
    }

    /**
     * Tests if the key value pair is queried
     *
     * @param key   the key
     * @param value the value
     * @return if queried
     */
    public boolean test(String key, String value) {
        ExpressionParameters variableSet = new ExpressionParameters();
        variableSet.set("key", key);
        variableSet.set("value", value);
        return getValue().test(variableSet);
    }

    public static class List extends ListParameter<NamedStringQueryExpression> {

        public List() {
            super(NamedStringQueryExpression.class);
        }

        public List(List other) {
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
            ExpressionParameters variableSet = new ExpressionParameters();
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
}
