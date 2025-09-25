package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link StringAndStringPairParameter}
 */
public class StringAndStringPairParameterList extends JIPipeListParameter<StringAndStringPairParameter> {
    /**
     * Creates a new instance
     */
    public StringAndStringPairParameterList() {
        super(StringAndStringPairParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringAndStringPairParameterList(StringAndStringPairParameterList other) {
        super(StringAndStringPairParameter.class);
        for (StringAndStringPairParameter filter : other) {
            add(new StringAndStringPairParameter(filter));
        }
    }
}
