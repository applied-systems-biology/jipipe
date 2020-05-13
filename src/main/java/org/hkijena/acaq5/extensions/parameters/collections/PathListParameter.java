package org.hkijena.acaq5.extensions.parameters.collections;

import java.nio.file.Path;

/**
 * Collection of paths. Used as parameter type.
 */
public class PathListParameter extends ListParameter<Path> {
    /**
     * Creates a new instance
     */
    public PathListParameter() {
        super(Path.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PathListParameter(PathListParameter other) {
        super(Path.class);
        addAll(other);
    }
}
