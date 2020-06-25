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

package org.hkijena.acaq5.extensions.parameters.predicates;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

import java.util.function.Predicate;

/**
 * Can either filter a string or a double
 */
public class StringOrDoublePredicate implements ACAQParameterCollection, ACAQValidatable, Predicate<Object> {

    private EventBus eventBus = new EventBus();
    private FilterMode filterMode = FilterMode.Double;
    private StringPredicate stringPredicate = new StringPredicate();
    private DoublePredicate doublePredicate = new DoublePredicate();

    /**
     * Creates a new instance
     */
    public StringOrDoublePredicate() {
    }

    /**
     * Copies the object
     *
     * @param other the original
     */
    public StringOrDoublePredicate(StringOrDoublePredicate other) {
        this.filterMode = other.filterMode;
        this.stringPredicate = new StringPredicate(other.stringPredicate);
        this.doublePredicate = new DoublePredicate(other.doublePredicate);
    }

    @ACAQDocumentation(name = "Mode", description = "Which source is used")
    @ACAQParameter("mode")
    @JsonGetter("mode")
    public FilterMode getFilterMode() {
        return filterMode;
    }

    @ACAQParameter("mode")
    @JsonSetter("mode")
    public void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
        getEventBus().post(new ParameterChangedEvent(this, "mode"));
    }

    @ACAQDocumentation(name = "String filter", description = "The string filter")
    @ACAQParameter("string-filter")
    @JsonGetter("string-filter")
    public StringPredicate getStringPredicate() {
        return stringPredicate;
    }

    @ACAQParameter("string-filter")
    @JsonSetter("string-filter")
    public void setStringPredicate(StringPredicate stringPredicate) {
        this.stringPredicate = stringPredicate;
        getEventBus().post(new ParameterChangedEvent(this, "string-filter"));
    }

    @ACAQDocumentation(name = "Number filter", description = "The number filter")
    @ACAQParameter("number-filter")
    @JsonGetter("number-filter")
    public DoublePredicate getDoublePredicate() {
        return doublePredicate;
    }

    @ACAQParameter("number-filter")
    @JsonSetter("number-filter")
    public void setDoublePredicate(DoublePredicate doublePredicate) {
        this.doublePredicate = doublePredicate;
        getEventBus().post(new ParameterChangedEvent(this, "number-filter"));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (filterMode == FilterMode.Double) {
            report.report(stringPredicate);
        } else if (filterMode == FilterMode.String) {
            report.report(doublePredicate);
        }
    }

    @Override
    public boolean test(Object o) {
        if (filterMode == FilterMode.String) {
            return stringPredicate.test("" + o);
        } else {
            double value;
            if (o instanceof Number) {
                value = ((Number) o).doubleValue();
            } else {
                try {
                    value = Double.parseDouble("" + o);
                } catch (NumberFormatException e) {
                    value = 0;
                }
            }
            return doublePredicate.test(value);
        }
    }

    /**
     * Modes are that a column is picked or one is generated
     */
    public enum FilterMode {
        Double,
        String
    }

    /**
     * A collection of multiple {@link StringOrDoublePredicate}
     * The filters are connected via "OR"
     */
    public static class List extends ListParameter<StringOrDoublePredicate> implements Predicate<Object> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringOrDoublePredicate.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringOrDoublePredicate.class);
            for (StringOrDoublePredicate filter : other) {
                add(new StringOrDoublePredicate(filter));
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
            for (StringOrDoublePredicate filter : this) {
                if (filter.test(s))
                    return true;
            }
            return false;
        }
    }
}
