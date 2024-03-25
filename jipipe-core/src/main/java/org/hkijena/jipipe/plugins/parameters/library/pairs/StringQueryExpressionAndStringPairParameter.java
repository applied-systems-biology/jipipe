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

package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;

/**
 * A parameter that renames a matching string into another string
 */
@PairParameterSettings(singleRow = false)
@AddJIPipeDocumentationDescription(description = "The string selection parameter has two modes: " +
        "(1) Selecting an existing string, and (2) Matching an existing strings by boolean operators<br/>" +
        "<ol><li>Type in the string in double quotes. Example: <pre>\"hello world\"</pre></li>" +
        "<li>The function iterates through all strings. It should return TRUE for one of them. You will have a variable 'value' available within the expression. Example: <pre>value CONTAINS \"hello\"</pre></li></ol>")
public class StringQueryExpressionAndStringPairParameter extends PairParameter<StringQueryExpression, String> {

    /**
     * Creates a new instance
     */
    public StringQueryExpressionAndStringPairParameter() {
        super(StringQueryExpression.class, String.class);
    }

    public StringQueryExpressionAndStringPairParameter(String expression, String key) {
        super(StringQueryExpression.class, String.class);
        setKey(new StringQueryExpression(expression));
        setValue(key);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringQueryExpressionAndStringPairParameter(StringQueryExpressionAndStringPairParameter other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringQueryExpressionAndStringPairParameter}
     */
    public static class List extends ListParameter<StringQueryExpressionAndStringPairParameter> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringQueryExpressionAndStringPairParameter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringQueryExpressionAndStringPairParameter.class);
            for (StringQueryExpressionAndStringPairParameter filter : other) {
                add(new StringQueryExpressionAndStringPairParameter(filter));
            }
        }
    }
}
