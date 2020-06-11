package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.optional.OptionalParameter;

/**
 * Optional {@link Integer}
 */
public class OptionalIntegerParameter extends OptionalParameter<Integer> {

    /**
     * Creates a new instance
     */
    public OptionalIntegerParameter() {
        super(Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalIntegerParameter(OptionalIntegerParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Integer setNewInstance() {
        setContent(0);
        return 0;
    }
}
