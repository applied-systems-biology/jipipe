package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.optional.OptionalParameter;

/**
 * Optional {@link Long}
 */
public class OptionalLongParameter extends OptionalParameter<Long> {

    /**
     * Creates a new instance
     */
    public OptionalLongParameter() {
        super(Long.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalLongParameter(OptionalLongParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Long setNewInstance() {
        setContent(0L);
        return 0L;
    }
}
