package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.OptionalParameter;

/**
 * Optional {@link Boolean}
 */
public class OptionalBooleanParameter extends OptionalParameter<Boolean> {

    /**
     * Creates a new instance
     */
    public OptionalBooleanParameter() {
        super(Boolean.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalBooleanParameter(OptionalBooleanParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Boolean setNewInstance() {
        setContent(false);
        return false;
    }
}
