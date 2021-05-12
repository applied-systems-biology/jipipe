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

package org.hkijena.jipipe.extensions.parameters.pairs;

import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;

/**
 * A parameter that renames a matching string into another string
 */
@PairParameterSettings(singleRow = false)
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
