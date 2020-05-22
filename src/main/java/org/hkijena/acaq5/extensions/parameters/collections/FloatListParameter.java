package org.hkijena.acaq5.extensions.parameters.collections;

/**
 * A list of {@link Float}
 */
public class FloatListParameter extends ListParameter<Float> {
    /**
     * Creates a new empty list
     */
    public FloatListParameter() {
        super(Float.class);
    }

    @Override
    public Float addNewInstance() {
        add(0.0f);
        return 0.0f;
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public FloatListParameter(FloatListParameter other) {
        super(Float.class);
        addAll(other);
    }
}
