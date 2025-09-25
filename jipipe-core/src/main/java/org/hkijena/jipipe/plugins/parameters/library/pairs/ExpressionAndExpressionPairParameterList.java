package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link ExpressionAndExpressionPairParameter}
 */
public class ExpressionAndExpressionPairParameterList extends JIPipeListParameter<ExpressionAndExpressionPairParameter> {
    /**
     * Creates a new instance
     */
    public ExpressionAndExpressionPairParameterList() {
        super(ExpressionAndExpressionPairParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ExpressionAndExpressionPairParameterList(ExpressionAndExpressionPairParameterList other) {
        super(ExpressionAndExpressionPairParameter.class);
        for (ExpressionAndExpressionPairParameter filter : other) {
            add(new ExpressionAndExpressionPairParameter(filter));
        }
    }
}
