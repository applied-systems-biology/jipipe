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

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A filter for {@link Double}
 */
public class DoublePredicate implements Predicate<Double>, JIPipeValidatable {

    private Mode mode = Mode.Equals;
    private double reference = 0;
    private boolean invert = false;

    /**
     * Initializes a new filter. Defaults to no filter string and Mode.Contains
     */
    public DoublePredicate() {

    }

    /**
     * Initializes a new filter
     *
     * @param mode      filter mode
     * @param reference reference value
     * @param invert    if the result is inverted
     */
    public DoublePredicate(Mode mode, double reference, boolean invert) {
        this.mode = mode;
        this.reference = reference;
        this.invert = invert;
    }

    /**
     * Copies the filter
     *
     * @param other the original
     */
    public DoublePredicate(DoublePredicate other) {
        this.mode = other.mode;
        this.reference = other.reference;
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

    @JsonGetter("reference")
    public double getReference() {
        return reference;
    }

    @JsonSetter("reference")
    public void setReference(double reference) {
        this.reference = reference;
    }

    @Override
    public boolean test(Double other) {
        boolean result;
        switch (mode) {
            case Equals:
                result = other == reference;
                break;
            case LessThan:
                result = other < reference;
                break;
            case GreaterThan:
                result = other > reference;
                break;
            case LessThanOrEquals:
                result = other <= reference;
                break;
            case GreaterThanOrEquals:
                result = other >= reference;
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
    }

    @Override
    public String toString() {
        return (invert ? "!" : "") + "(x " + mode.getStringRepresentation() + " " + reference + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoublePredicate that = (DoublePredicate) o;
        return mode == that.mode && invert == that.invert &&
                Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, reference, invert);
    }

    /**
     * Available filter modes
     */
    public enum Mode {
        Equals("="),
        LessThan("<"),
        GreaterThan(">"),
        LessThanOrEquals("≤"),
        GreaterThanOrEquals("≥");

        private final String stringRepresentation;

        Mode(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        public String getStringRepresentation() {
            return stringRepresentation;
        }
    }

    /**
     * A collection of multiple {@link DoublePredicate}
     * The filters are connected via "OR"
     */
    public static class List extends ListParameter<DoublePredicate> {
        /**
         * Creates a new instance
         */
        public List() {
            super(DoublePredicate.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(DoublePredicate.class);
            for (DoublePredicate pathPredicate : other) {
                add(new DoublePredicate(pathPredicate));
            }
        }
    }

}
