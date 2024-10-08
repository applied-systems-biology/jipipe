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
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parameter that acts as dynamic enum.
 * Use {@link DynamicEnumParameterSettings} to define a supplier for the
 * items. Alternatively, use allowedValues to supply items.
 * allowedValues is preferred. If allowedValues is null, you have to use {@link DynamicEnumParameterSettings}.
 */
public abstract class DynamicEnumParameter<T> {
    private T value;
    private List<T> allowedValues = new ArrayList<>();

    /**
     * Creates a new instance with null value
     */
    public DynamicEnumParameter() {

    }

    /**
     * Creates a new instance
     *
     * @param value initial value
     */
    public DynamicEnumParameter(T value) {
        this.value = value;
    }

    @JsonGetter("value")
    public T getValue() {
        return value;
    }

    @JsonSetter("value")
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Returns the list of allowed choices
     * Please note that if isEditable() is true, other options might be available
     *
     * @return the allowed values
     */
    public List<T> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<T> allowedValues) {
        this.allowedValues = allowedValues;
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

    /**
     * The search string used for the searchable mode
     *
     * @return the search string
     */
    public String getSearchString(T value) {
        return StringUtils.nullToEmpty(value);
    }

    /**
     * If this returns true, the user can freely edit the entry
     *
     * @return if the user can freely edit the entry
     */
    public boolean isEditable() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicEnumParameter<?> that = (DynamicEnumParameter<?>) o;
        return Objects.equals(value, that.value) && Objects.equals(allowedValues, that.allowedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, allowedValues);
    }
}
