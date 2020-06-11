package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

import java.nio.file.Path;

/**
 * Collection of paths. Used as parameter type.
 */
public class PathList extends ListParameter<Path> {
    /**
     * Creates a new instance
     */
    public PathList() {
        super(Path.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PathList(PathList other) {
        super(Path.class);
        addAll(other);
    }
}
