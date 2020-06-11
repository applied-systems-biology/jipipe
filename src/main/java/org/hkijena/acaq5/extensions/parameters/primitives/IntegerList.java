package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

/**
 * A list of {@link Integer}
 */
public class IntegerList extends ListParameter<Integer> {
    /**
     * Creates a new empty list
     */
    public IntegerList() {
        super(Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerList(IntegerList other) {
        super(Integer.class);
        addAll(other);
    }

    @Override
    public Integer addNewInstance() {
        add(0);
        return 0;
    }
}
