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

import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.api.pairs.JIPipePairParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;

/**
 * A parameter that renames a matching string into another string
 */
@PairParameterSettings(singleRow = false)
public class StringQueryExpressionAndStringQueryPairParameter extends JIPipePairParameter<StringQueryExpression, StringQueryExpression> {

    /**
     * Creates a new instance
     */
    public StringQueryExpressionAndStringQueryPairParameter() {
        super(StringQueryExpression.class, StringQueryExpression.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringQueryExpressionAndStringQueryPairParameter(StringQueryExpressionAndStringQueryPairParameter other) {
        super(other);
    }

}
