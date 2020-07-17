/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.predicates;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A filter for path filenames that can handle multiple filter modes
 */
public class StringPredicate implements Predicate<String>, JIPipeValidatable {

    private Mode mode = Mode.Equals;
    private String filterString;
    private boolean invert = false;

    /**
     * Initializes a new filter. Defaults to no filter string and Mode.Contains
     */
    public StringPredicate() {

    }

    /**
     * Initializes a new filter
     *
     * @param mode         filter mode
     * @param filterString filter string
     * @param invert       if the predicate should be inverted
     */
    public StringPredicate(Mode mode, String filterString, boolean invert) {
        this.mode = mode;
        this.filterString = filterString;
        this.invert = invert;
    }

    /**
     * Copies the filter
     *
     * @param other the original
     */
    public StringPredicate(StringPredicate other) {
        this.mode = other.mode;
        this.filterString = other.filterString;
        this.invert = other.invert;
    }

    @JsonGetter("mode")
    public Mode getMode() {
        return mode;
    }

    @JsonSetter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @JsonGetter("filter-string")
    public String getFilterString() {
        return filterString;
    }

    @JsonSetter("filter-string")
    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }

    @JsonGetter("invert")
    public boolean isInvert() {
        return invert;
    }

    @JsonSetter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @Override
    public boolean test(String path) {
        boolean result;
        switch (mode) {
            case Equals:
                result = StringUtils.orElse(path, "").equals(filterString);
                break;
            case Contains:
                result = StringUtils.orElse(path, "").contains(filterString);
                break;
            case Regex:
                result = StringUtils.orElse(path, "").matches(filterString);
                break;
            default:
                throw new RuntimeException("Unknown mode!");
        }
        if (!invert)
            return result;
        else
            return !result;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (mode == Mode.Regex) {
            try {
                Pattern.compile(filterString);
            } catch (PatternSyntaxException e) {
                report.forCategory("RegEx").reportIsInvalid("RegEx syntax is wrong!",
                        "The regular expression string is wrong.",
                        "Please check the syntax. If you are not familiar with it, you can find plenty of resources online.",
                        this);
            }
        }
    }

    @Override
    public String toString() {
        return (invert ? "!" : "") + mode + "(\"" + Objects.toString(filterString, "").replace("\\", "\\\\").replace("\"", "\\\"") + "\")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringPredicate that = (StringPredicate) o;
        return mode == that.mode && invert == that.invert &&
                Objects.equals(filterString, that.filterString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, filterString, invert);
    }

    /**
     * Available filter modes
     */
    public enum Mode {
        /**
         * Checks if the string is the same
         */
        Equals,
        /**
         * Checks via String.contains
         */
        Contains,
        /**
         * Checks via a regular expression
         */
        Regex
    }

    /**
     * A collection of multiple {@link StringPredicate}
     * The filters are connected via "OR"
     */
    public static class List extends ListParameter<StringPredicate> implements Predicate<String> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringPredicate.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringPredicate.class);
            for (StringPredicate filter : other) {
                add(new StringPredicate(filter));
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
            for (StringPredicate stringPredicate : this) {
                if (stringPredicate.test(s))
                    return true;
            }
            return false;
        }
    }
}
