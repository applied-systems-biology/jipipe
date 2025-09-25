package org.hkijena.jipipe.plugins.parameters.library.primitives;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A collection of multiple {@link StringOrDouble}
 * The filters are connected via "OR"
 */
public class StringOrDoubleList extends JIPipeListParameter<StringOrDouble> {
    /**
     * Creates a new instance
     */
    public StringOrDoubleList() {
        super(StringOrDouble.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringOrDoubleList(StringOrDoubleList other) {
        super(StringOrDouble.class);
        for (StringOrDouble filter : other) {
            add(new StringOrDouble(filter));
        }
    }
}
