package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

/**
 * A list of {@link Double}
 */
public class DoubleList extends ListParameter<Double> {
    /**
     * Creates a new empty list
     */
    public DoubleList() {
        super(Double.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DoubleList(DoubleList other) {
        super(Double.class);
        addAll(other);
    }

    @Override
    public Double addNewInstance() {
        add(0.0);
        return 0.0;
    }
}
