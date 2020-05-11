package org.hkijena.acaq5.extensions.parameters.filters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A filter for path filenames that can handle multiple filter modes
 */
public class PathFilter implements Predicate<Path>, ACAQValidatable {

    private Mode mode = Mode.Contains;
    private String filterString;
    private PathMatcher globPathMatcher;

    /**
     * Initializes a new filter. Defaults to no filter string and Mode.Contains
     */
    public PathFilter() {

    }

    /**
     * Copies the filter
     *
     * @param other the original
     */
    public PathFilter(PathFilter other) {
        this.mode = other.mode;
        this.filterString = other.filterString;
        this.globPathMatcher = other.globPathMatcher;
    }

    @JsonGetter
    public Mode getMode() {
        return mode;
    }

    @JsonSetter
    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.Glob)
            setFilterString(filterString);
    }

    @JsonGetter
    public String getFilterString() {
        return filterString;
    }

    @JsonSetter
    public void setFilterString(String filterString) {
        this.filterString = filterString;
        if (mode == Mode.Glob && filterString != null && !filterString.isEmpty()) {
            try {
                globPathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filterString);
            } catch (Exception e) {
                globPathMatcher = null;
            }
        } else {
            globPathMatcher = null;
        }
    }

    @Override
    public boolean test(Path path) {
        switch (mode) {
            case Contains:
                return path.getFileName().toString().contains(filterString);
            case Glob:
                return globPathMatcher.matches(path.getFileName());
            case Regex:
                return path.getFileName().toString().matches(filterString);
            default:
                throw new RuntimeException("Unknown mode!");
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (mode == Mode.Glob && globPathMatcher == null) {
            report.reportIsInvalid("Invalid filter string!",
                    "The glob file filter '" + filterString + "' is invalid!",
                    "Please change it to a valid filter string.",
                    this);
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
        PathFilter that = (PathFilter) o;
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
         * Checks via String.contains
         */
        Contains,
        /**
         * Checks via a Glob matcher
         */
        Glob,
        /**
         * Checks via a regular expression
         */
        Regex
    }

}
