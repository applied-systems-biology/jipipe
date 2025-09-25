package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link StringQueryExpressionAndStringPairParameter}
 */
public class StringQueryExpressionAndStringPairParameterList extends JIPipeListParameter<StringQueryExpressionAndStringPairParameter> {
    /**
     * Creates a new instance
     */
    public StringQueryExpressionAndStringPairParameterList() {
        super(StringQueryExpressionAndStringPairParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringQueryExpressionAndStringPairParameterList(StringQueryExpressionAndStringPairParameterList other) {
        super(StringQueryExpressionAndStringPairParameter.class);
        for (StringQueryExpressionAndStringPairParameter filter : other) {
            add(new StringQueryExpressionAndStringPairParameter(filter));
        }
    }
}
