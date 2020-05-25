package org.hkijena.acaq5.extensions.parameters.collections;

/**
 * A list of {@link Integer}
 */
public class IntListParameter extends ListParameter<Integer> {
    /**
     * Creates a new empty list
     */
    public IntListParameter() {
        super(Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntListParameter(IntListParameter other) {
        super(Integer.class);
        addAll(other);
    }

    @Override
    public Integer addNewInstance() {
        add(0);
        return 0;
    }
}
