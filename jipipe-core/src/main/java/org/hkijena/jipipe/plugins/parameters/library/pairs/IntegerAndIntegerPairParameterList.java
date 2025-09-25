package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link IntegerAndIntegerPairParameter}
 */
public class IntegerAndIntegerPairParameterList extends JIPipeListParameter<IntegerAndIntegerPairParameter> {
    /**
     * Creates a new instance
     */
    public IntegerAndIntegerPairParameterList() {
        super(IntegerAndIntegerPairParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerAndIntegerPairParameterList(IntegerAndIntegerPairParameterList other) {
        super(IntegerAndIntegerPairParameter.class);
        for (IntegerAndIntegerPairParameter filter : other) {
            add(new IntegerAndIntegerPairParameter(filter));
        }
    }
}
