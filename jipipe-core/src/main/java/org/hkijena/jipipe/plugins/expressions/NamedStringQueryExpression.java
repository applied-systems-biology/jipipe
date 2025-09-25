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

package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.plugins.parameters.api.pairs.JIPipePairParameter;

@AddJIPipeDocumentationDescription(description = "Returns the named strings only if the expression returns TRUE for key value pair. Within the expression, there are two variables available: 'key' and 'value'. Example: <pre>(key CONTAINS \"sample\") AND (value CONTAINS \"aspergillus\")</pre>")
public class NamedStringQueryExpression extends JIPipePairParameter<String, StringQueryExpression> {

    public NamedStringQueryExpression() {
        super(String.class, StringQueryExpression.class);
        setKey("");
        setValue(new StringQueryExpression("TRUE"));
    }

    public NamedStringQueryExpression(JIPipePairParameter<String, StringQueryExpression> other) {
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
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        variableSet.set("key", key);
        variableSet.set("value", value);
        return getValue().test(variableSet);
    }

}
