package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;

import java.util.function.Predicate;

/**
 * A collection of multiple {@link StringFilter}
 * The filters are connected via "OR"
 */
public class StringFilterListParameter extends ListParameter<StringFilter> implements Predicate<String> {
    /**
     * Creates a new instance
     */
    public StringFilterListParameter() {
        super(StringFilter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterListParameter(StringFilterListParameter other) {
        super(StringFilter.class);
        for (StringFilter filter : other) {
            add(new StringFilter(filter));
        }
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
}
