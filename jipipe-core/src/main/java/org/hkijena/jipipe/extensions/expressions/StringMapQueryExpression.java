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

package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;

import java.util.Collection;
import java.util.Map;

@JIPipeDocumentationDescription(description = "The key value pair values are available as variables. If there are spaces or special characters inside the variable names, " +
        "use 'GET_VARIABLE' to access the values. To check if a key is available, use the 'EXISTS' operator. Example: <pre>\"Aspergillus\" EXISTS AND \"Raw\" EXISTS</pre>")
public class StringMapQueryExpression extends DefaultExpressionParameter {

    public StringMapQueryExpression() {
    }

    public StringMapQueryExpression(String expression) {
        super(expression);
    }

    public StringMapQueryExpression(AbstractExpressionParameter other) {
        super(other);
    }

    /**
     * Tests if the query matches the key value pairs
     *
     * @param map the key value pairs
     * @return if matches
     */
    public boolean test(Map<String, String> map) {
        ExpressionVariables variableSet = new ExpressionVariables();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            variableSet.set(entry.getKey(), entry.getValue());
        }
        return test(variableSet);
    }

    /**
     * Tests if the query matches the keys=values. A map where key=value is true is generated.
     *
     * @param values the key=values
     * @return if matches
     */
    public boolean test(Collection<String> values) {
        ExpressionVariables variableSet = new ExpressionVariables();
        for (String value : values) {
            variableSet.set(value, value);
        }
        return test(variableSet);
    }
}
