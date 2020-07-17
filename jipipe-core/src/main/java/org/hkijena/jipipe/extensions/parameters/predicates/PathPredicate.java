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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A filter for path filenames that can handle multiple filter modes
 */
public class PathPredicate implements Predicate<Path>, JIPipeValidatable {

    private Mode mode = Mode.Contains;
    private String filterString;
    private PathMatcher globPathMatcher;
    private boolean invert = false;

    /**
     * Initializes a new filter. Defaults to no filter string and Mode.Contains
     */
    public PathPredicate() {

    }

    /**
     * Copies the filter
     *
     * @param other the original
     */
    public PathPredicate(PathPredicate other) {
        this.mode = other.mode;
        this.filterString = other.filterString;
        this.globPathMatcher = other.globPathMatcher;
        this.invert = other.invert;
    }

    @JsonGetter("mode")
    public Mode getMode() {
        return mode;
    }

    @JsonSetter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.Glob)
            setFilterString(filterString);
    }

    @JsonGetter("filter-string")
    public String getFilterString() {
        return filterString;
    }

    @JsonSetter("filter-string")
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
        boolean result;
        switch (mode) {
            case Contains:
                result = path.toString().contains(filterString);
                break;
            case Glob:
                result = globPathMatcher.matches(path);
                break;
            case Regex:
                result = path.toString().matches(filterString);
                break;
            default:
                throw new RuntimeException("Unknown mode!");
        }
        if (!invert)
            return result;
        else
            return !result;
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
    public void reportValidity(JIPipeValidityReport report) {
        if (mode == Mode.Glob && globPathMatcher == null) {
            report.reportIsInvalid("Invalid filter string!",
                    "The glob file filter '" + filterString + "' is invalid!",
                    "Please change it to a valid filter string.",
                    this);
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
        PathPredicate that = (PathPredicate) o;
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

    /**
     * A collection of multiple {@link PathPredicate}
     * The filters are connected via "OR"
     */
    public static class List extends ListParameter<PathPredicate> {
        /**
         * Creates a new instance
         */
        public List() {
            super(PathPredicate.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(PathPredicate.class);
            for (PathPredicate pathPredicate : other) {
                add(new PathPredicate(pathPredicate));
            }
        }
    }
}
