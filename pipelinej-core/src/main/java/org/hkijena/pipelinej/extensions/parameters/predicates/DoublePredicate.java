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

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A filter for {@link Double}
 */
public class DoublePredicate implements Predicate<Double>, ACAQValidatable {

    private Mode mode = Mode.Equals;
    private double reference = 0;

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
     */
    public DoublePredicate(Mode mode, double reference) {
        this.mode = mode;
        this.reference = reference;
    }

    /**
     * Copies the filter
     *
     * @param other the original
     */
    public DoublePredicate(DoublePredicate other) {
        this.mode = other.mode;
        this.reference = other.reference;
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
    public double getReference() {
        return reference;
    }

    @JsonSetter
    public void setReference(double reference) {
        this.reference = reference;
    }

    @Override
    public boolean test(Double other) {
        switch (mode) {
            case Equals:
                return other == reference;
            case LessThan:
                return other < reference;
            case GreaterThan:
                return other > reference;
            case LessThanOrEquals:
                return other <= reference;
            case GreaterThanOrEquals:
                return other >= reference;
            default:
                throw new RuntimeException("Unknown mode!");
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
    }

    @Override
    public String toString() {
        return "(x " + mode.getStringRepresentation() + " " + reference + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoublePredicate that = (DoublePredicate) o;
        return mode == that.mode &&
                Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, reference);
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
     * A collection of multiple {@link PathPredicate}
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
