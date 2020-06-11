package org.hkijena.acaq5.extensions.parameters.predicates;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A filter for path filenames that can handle multiple filter modes
 */
public class StringPredicate implements Predicate<String>, ACAQValidatable {

    private Mode mode = Mode.Equals;
    private String filterString;

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
     */
    public StringPredicate(Mode mode, String filterString) {
        this.mode = mode;
        this.filterString = filterString;
    }

    /**
     * Copies the filter
     *
     * @param other the original
     */
    public StringPredicate(StringPredicate other) {
        this.mode = other.mode;
        this.filterString = other.filterString;
    }

    @JsonGetter
    public Mode getMode() {
        return mode;
    }

    @JsonSetter
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @JsonGetter
    public String getFilterString() {
        return filterString;
    }

    @JsonSetter
    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }

    @Override
    public boolean test(String path) {
        switch (mode) {
            case Equals:
                return ("" + path).equals(filterString);
            case Contains:
                return ("" + path).contains(filterString);
            case Regex:
                return ("" + path).matches(filterString);
            default:
                throw new RuntimeException("Unknown mode!");
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if(mode == Mode.Regex) {
            try {
                Pattern.compile(filterString);
            }
            catch (PatternSyntaxException e) {
                report.forCategory("RegEx").reportIsInvalid("RegEx syntax is wrong!",
                        "The regular expression string is wrong.",
                        "Please check the syntax. If you are not familiar with it, you can find plenty of resources online.",
                        this);
            }
        }
    }

    @Override
    public String toString() {
        return mode + "(\"" + Objects.toString(filterString, "").replace("\\", "\\\\").replace("\"", "\\\"") + "\")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringPredicate that = (StringPredicate) o;
        return mode == that.mode &&
                Objects.equals(filterString, that.filterString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, filterString);
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
