package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

/**
 * A list of {@link Float}
 */
public class FloatList extends ListParameter<Float> {
    /**
     * Creates a new empty list
     */
    public FloatList() {
        super(Float.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FloatList(FloatList other) {
        super(Float.class);
        addAll(other);
    }

    @Override
    public Float addNewInstance() {
        add(0.0f);
        return 0.0f;
    }
}
