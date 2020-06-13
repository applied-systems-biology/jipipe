package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.optional.OptionalParameter;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Optional {@link java.nio.file.Path}
 */
public class OptionalPathParameter extends OptionalParameter<Path> {

    /**
     * Creates a new instance
     */
    public OptionalPathParameter() {
        super(Path.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public OptionalPathParameter(OptionalPathParameter other) {
        super(other);
        this.setContent(other.getContent());
    }

    @Override
    public Path setNewInstance() {
        setContent(Paths.get(""));
        return getContent();
    }
}
