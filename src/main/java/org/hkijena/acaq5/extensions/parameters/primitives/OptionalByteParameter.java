package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.OptionalParameter;

/**
 * Optional {@link Byte}
 */
public class OptionalByteParameter extends OptionalParameter<Byte> {

    /**
     * Creates a new instance
     */
    public OptionalByteParameter() {
        super(Byte.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalByteParameter(OptionalByteParameter other) {
        super(Byte.class);
        this.setContent(other.getContent());
    }

    @Override
    public Byte setNewInstance() {
        setContent((byte) 0);
        return (byte) 0;
    }
}
