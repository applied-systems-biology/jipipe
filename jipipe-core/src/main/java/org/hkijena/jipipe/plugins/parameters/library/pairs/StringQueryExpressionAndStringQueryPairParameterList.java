package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link StringQueryExpressionAndStringQueryPairParameter}
 */
public class StringQueryExpressionAndStringQueryPairParameterList extends JIPipeListParameter<StringQueryExpressionAndStringQueryPairParameter> {
    /**
     * Creates a new instance
     */
    public StringQueryExpressionAndStringQueryPairParameterList() {
        super(StringQueryExpressionAndStringQueryPairParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringQueryExpressionAndStringQueryPairParameterList(StringQueryExpressionAndStringQueryPairParameterList other) {
        super(StringQueryExpressionAndStringQueryPairParameter.class);
        for (StringQueryExpressionAndStringQueryPairParameter filter : other) {
            add(new StringQueryExpressionAndStringQueryPairParameter(filter));
        }
    }
}
