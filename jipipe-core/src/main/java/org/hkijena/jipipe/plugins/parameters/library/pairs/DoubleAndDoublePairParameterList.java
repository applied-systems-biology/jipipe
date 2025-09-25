package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link DoubleAndDoublePairParameter}
 */
public class DoubleAndDoublePairParameterList extends JIPipeListParameter<DoubleAndDoublePairParameter> {
    /**
     * Creates a new instance
     */
    public DoubleAndDoublePairParameterList() {
        super(DoubleAndDoublePairParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DoubleAndDoublePairParameterList(DoubleAndDoublePairParameterList other) {
        super(DoubleAndDoublePairParameter.class);
        for (DoubleAndDoublePairParameter filter : other) {
            add(new DoubleAndDoublePairParameter(filter));
        }
    }
}
