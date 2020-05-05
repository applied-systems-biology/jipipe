package org.hkijena.acaq5.api.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A filter for path filenames that can handle multiple filter modes
 */
public class StringFilter implements Predicate<String>, ACAQValidatable {

    private Mode mode = Mode.Equals;
    private String filterString;

    /**
     * Initializes a new filter. Defaults to no filter string and Mode.Contains
     */
    public StringFilter() {

    }

    /**
     * Initializes a new filter
     *
     * @param mode         filter mode
     * @param filterString filter string
     */
    public StringFilter(Mode mode, String filterString) {
        this.mode = mode;
        this.filterString = filterString;
    }

    /**
     * Copies the filter
     *
     * @param other the original
     */
    public StringFilter(StringFilter other) {
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
    }

    @Override
    public String toString() {
        return mode + "(\"" + Objects.toString(filterString, "").replace("\\", "\\\\").replace("\"", "\\\"") + "\")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringFilter that = (StringFilter) o;
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

}
