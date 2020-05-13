package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.filters.PathFilter;

/**
 * A collection of multiple {@link PathFilter}
 * The filters are connected via "OR"
 */
public class PathFilterListParameter extends ListParameter<PathFilter> {
    /**
     * Creates a new instance
     */
    public PathFilterListParameter() {
        super(PathFilter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public PathFilterListParameter(PathFilterListParameter other) {
        super(PathFilter.class);
        for (PathFilter pathFilter : other) {
            add(new PathFilter(pathFilter));
        }
    }
}
