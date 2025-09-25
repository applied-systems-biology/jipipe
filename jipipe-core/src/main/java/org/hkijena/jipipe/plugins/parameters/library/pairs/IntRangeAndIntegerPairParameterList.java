package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link IntRangeAndIntegerPairParameter}
 */
public class IntRangeAndIntegerPairParameterList extends JIPipeListParameter<IntRangeAndIntegerPairParameter> {
    /**
     * Creates a new instance
     */
    public IntRangeAndIntegerPairParameterList() {
        super(IntRangeAndIntegerPairParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntRangeAndIntegerPairParameterList(IntRangeAndIntegerPairParameterList other) {
        super(IntRangeAndIntegerPairParameter.class);
        for (IntRangeAndIntegerPairParameter filter : other) {
            add(new IntRangeAndIntegerPairParameter(filter));
        }
    }
}
