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

import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameter;

/**
 * A parameter that renames an integer into another integer
 */
public class ExpressionAndExpressionPairParameter extends PairParameter<JIPipeExpressionParameter, JIPipeExpressionParameter> {

    /**
     * Creates a new instance
     */
    public ExpressionAndExpressionPairParameter() {
        super(JIPipeExpressionParameter.class, JIPipeExpressionParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ExpressionAndExpressionPairParameter(ExpressionAndExpressionPairParameter other) {
        super(other);
        if (other.getKey() != null)
            this.setKey(new JIPipeExpressionParameter(other.getKey()));
        if (other.getValue() != null)
            this.setValue(new JIPipeExpressionParameter(other.getValue()));
    }

    /**
     * A collection of multiple {@link ExpressionAndExpressionPairParameter}
     */
    public static class List extends ListParameter<ExpressionAndExpressionPairParameter> {
        /**
         * Creates a new instance
         */
        public List() {
            super(ExpressionAndExpressionPairParameter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(ExpressionAndExpressionPairParameter.class);
            for (ExpressionAndExpressionPairParameter filter : other) {
                add(new ExpressionAndExpressionPairParameter(filter));
            }
        }
    }
}
