package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.OptionalParameter;

/**
 * Optional {@link Float}
 */
public class OptionalFloatParameter extends OptionalParameter<Float> {

    /**
     * Creates a new instance
     */
    public OptionalFloatParameter() {
        super(Float.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalFloatParameter(OptionalFloatParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Float setNewInstance() {
        setContent(0f);
        return 0f;
    }
}
