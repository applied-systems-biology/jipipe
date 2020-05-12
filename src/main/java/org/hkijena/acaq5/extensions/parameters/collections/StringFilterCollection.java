package org.hkijena.acaq5.extensions.parameters.collections;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;

import java.util.function.Predicate;

/**
 * A collection of multiple {@link StringFilter}
 * The filters are connected via "OR"
 */
@JsonDeserialize(using = StringFilterCollection.Deserializer.class)
public class StringFilterCollection extends CollectionParameter<StringFilter> implements Predicate<String> {
    /**
     * Creates a new instance
     */
    public StringFilterCollection() {
        super(StringFilter.class);
    }

    /**
     * Returns true if one or more filters report that the string matches
     *
     * @param s the string
     * @return if a filter matches
     */
    @Override
    public boolean test(String s) {
        for (StringFilter stringFilter : this) {
            if (stringFilter.test(s))
                return true;
        }
        return false;
    }

    /**
     * Deserializes a {@link StringFilterCollection}
     */
    public static class Deserializer extends CollectionParameter.Deserializer<StringFilter> {
        @Override
        public Class<StringFilter> getContentClass() {
            return StringFilter.class;
        }

        @Override
        public CollectionParameter<StringFilter> newInstance() {
            return new StringFilterCollection();
        }
    }
}
