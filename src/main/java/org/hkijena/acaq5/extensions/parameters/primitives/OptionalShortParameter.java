package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.optional.OptionalParameter;

/**
 * Optional {@link Short}
 */
public class OptionalShortParameter extends OptionalParameter<Short> {

    /**
     * Creates a new instance
     */
    public OptionalShortParameter() {
        super(Short.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalShortParameter(OptionalShortParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Short setNewInstance() {
        setContent((short) 0);
        return (short) 0;
    }
}
