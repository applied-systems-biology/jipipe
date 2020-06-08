package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.OptionalParameter;

/**
 * Optional {@link String}
 */
public class OptionalStringParameter extends OptionalParameter<String> {

    /**
     * Creates a new instance
     */
    public OptionalStringParameter() {
        super(String.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalStringParameter(OptionalStringParameter other) {
        super(String.class);
        this.setContent(other.getContent());
    }

    @Override
    public String setNewInstance() {
        setContent("");
        return "";
    }
}
