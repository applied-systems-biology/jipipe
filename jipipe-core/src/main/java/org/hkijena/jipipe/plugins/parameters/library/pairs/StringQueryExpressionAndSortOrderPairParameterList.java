package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link StringQueryExpressionAndSortOrderPairParameter}
 */
public class StringQueryExpressionAndSortOrderPairParameterList extends JIPipeListParameter<StringQueryExpressionAndSortOrderPairParameter> {
    /**
     * Creates a new instance
     */
    public StringQueryExpressionAndSortOrderPairParameterList() {
        super(StringQueryExpressionAndSortOrderPairParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringQueryExpressionAndSortOrderPairParameterList(StringQueryExpressionAndSortOrderPairParameterList other) {
        super(StringQueryExpressionAndSortOrderPairParameter.class);
        for (StringQueryExpressionAndSortOrderPairParameter filter : other) {
            add(new StringQueryExpressionAndSortOrderPairParameter(filter));
        }
    }
}
