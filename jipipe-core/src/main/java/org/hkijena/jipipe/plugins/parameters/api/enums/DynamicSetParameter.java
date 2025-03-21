/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.api.enums;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import javax.swing.*;
import java.util.*;

/**
 * Parameter that acts as dynamic enum where a set of items can be selected
 * Use {@link DynamicEnumParameterSettings} to define a supplier for the
 * items. Alternatively, use allowedValues to supply items.
 * allowedValues is preferred. If allowedValues is null, you have to use {@link DynamicEnumParameterSettings}.
 */
public abstract class DynamicSetParameter<T> {
    private Set<T> values = new HashSet<>();
    private Set<T> allowedValues = new TreeSet<>();
    private boolean collapsed = false;

    /**
     * Creates a new instance with null value
     */
    public DynamicSetParameter() {
    }

    public DynamicSetParameter(DynamicSetParameter<T> other) {
        this.values = new HashSet<>(other.values);
        this.allowedValues = other.allowedValues;
        this.collapsed = other.collapsed;
    }

    public DynamicSetParameter(Set<T> values) {
        this.values = values;
    }

    @JsonGetter("values")
    public Set<T> getValues() {
        return values;
    }

    @JsonSetter("values")
    public void setValues(Set<T> values) {
        this.values = new HashSet<>(values);
    }

    @JsonGetter("collapsed")
    public boolean isCollapsed() {
        return collapsed;
    }

    @JsonSetter("collapsed")
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public Set<T> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(Set<T> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public void setAllowedValues(List<T> allowedValues) {
        this.allowedValues = new TreeSet<>(allowedValues);
    }

    /**
     * Function that renders the label. This is used in UI.
     *
     * @param value the value
     * @return the rendered text
     */
    public String renderLabel(T value) {
        return "" + value;
    }

    /**
     * Function that renders the tooltip. This is used in UI.
     *
     * @param value the tooltip
     * @return the rendered tooltip
     */
    public String renderTooltip(T value) {
        return null;
    }

    /**
     * Function that renders the icon. This is used in UI.
     *
     * @param value the value
     * @return the rendered icon
     */
    public Icon renderIcon(T value) {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicSetParameter<?> that = (DynamicSetParameter<?>) o;
        return Objects.equals(values, that.values) && Objects.equals(allowedValues, that.allowedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values, allowedValues);
    }

    public boolean contains(T value) {
        return values.contains(value);
    }
}
