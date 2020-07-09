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

package org.hkijena.pipelinej.extensions.parameters.predicates;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.pipelinej.api.ACAQValidatable;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.extensions.parameters.collections.ListParameter;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A filter for path filenames that can handle multiple filter modes
 */
public class PathPredicate implements Predicate<Path>, ACAQValidatable {

    private Mode mode = Mode.Contains;
    private String filterString;
    private PathMatcher globPathMatcher;

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
                return path.toString().contains(filterString);
            case Glob:
                return globPathMatcher.matches(path);
            case Regex:
                return path.toString().matches(filterString);
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
        PathPredicate that = (PathPredicate) o;
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
