package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.OptionalParameter;

/**
 * Optional {@link Double}
 */
public class OptionalDoubleParameter extends OptionalParameter<Double> {

    /**
     * Creates a new instance
     */
    public OptionalDoubleParameter() {
        super(Double.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalDoubleParameter(OptionalDoubleParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Double setNewInstance() {
        setContent(0.0);
        return 0.0;
    }
}
