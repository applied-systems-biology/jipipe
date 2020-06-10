package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.filters.StringOrDoubleFilter;

import java.util.function.Predicate;

/**
 * A collection of multiple {@link org.hkijena.acaq5.extensions.parameters.filters.StringOrDoubleFilter}
 * The filters are connected via "OR"
 */
public class StringOrDoubleFilterListParameter extends ListParameter<StringOrDoubleFilter> implements Predicate<Object> {
    /**
     * Creates a new instance
     */
    public StringOrDoubleFilterListParameter() {
        super(StringOrDoubleFilter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringOrDoubleFilterListParameter(StringOrDoubleFilterListParameter other) {
        super(StringOrDoubleFilter.class);
        for (StringOrDoubleFilter filter : other) {
            add(new StringOrDoubleFilter(filter));
        }
    }

    /**
     * Returns true if one or more filters report that the string matches
     *
     * @param s the string
     * @return if a filter matches
     */
    @Override
    public boolean test(Object s) {
        for (StringOrDoubleFilter filter : this) {
            if (filter.test(s))
                return true;
        }
        return false;
    }
}
