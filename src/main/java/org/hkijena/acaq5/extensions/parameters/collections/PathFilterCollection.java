package org.hkijena.acaq5.extensions.parameters.collections;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.acaq5.extensions.parameters.filters.PathFilter;

/**
 * A collection of multiple {@link PathFilter}
 * The filters are connected via "OR"
 */
@JsonDeserialize(using = PathFilterCollection.Deserializer.class)
public class PathFilterCollection extends CollectionParameter<PathFilter> {
    /**
     * Creates a new instance
     */
    public PathFilterCollection() {
        super(PathFilter.class);
    }

    /**
     * Deserializes a {@link PathFilterCollection}
     */
    public static class Deserializer extends CollectionParameter.Deserializer<PathFilter> {
        @Override
        public Class<PathFilter> getContentClass() {
            return PathFilter.class;
        }

        @Override
        public CollectionParameter<PathFilter> newInstance() {
            return new PathFilterCollection();
        }
    }
}
