package org.hkijena.acaq5.api.parameters;

import org.hkijena.acaq5.extensions.parametereditors.editors.DynamicEnumParameterSettings;

import java.util.List;

/**
 * Parameter that acts as dynamic enum.
 * Use {@link DynamicEnumParameterSettings} to define a supplier for the
 * items. Alternatively, use allowedValues to supply items.
 * allowedValues is preferred. If allowedValues is null, you have to use {@link DynamicEnumParameterSettings}.
 */
public abstract class DynamicEnumParameter {
    private Object value;
    private List<Object> allowedValues;

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
    public DynamicEnumParameter(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<Object> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<Object> allowedValues) {
        this.allowedValues = allowedValues;
    }
}
