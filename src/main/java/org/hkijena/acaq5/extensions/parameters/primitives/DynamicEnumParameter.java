package org.hkijena.acaq5.extensions.parameters.primitives;

import javax.swing.*;
import java.util.List;

/**
 * Parameter that acts as dynamic enum.
 * Use {@link DynamicEnumParameterSettings} to define a supplier for the
 * items. Alternatively, use allowedValues to supply items.
 * allowedValues is preferred. If allowedValues is null, you have to use {@link DynamicEnumParameterSettings}.
 * The JSON serialization must be done manually.
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

    /**
     * Function that renders the label. This is used in UI.
     *
     * @param value the value
     * @return the rendered text
     */
    public String renderLabel(Object value) {
        return "" + value;
    }

    /**
     * Function that renders the tooltip. This is used in UI.
     *
     * @param value the tooltip
     * @return the rendered tooltip
     */
    public String renderTooltip(Object value) {
        return null;
    }

    /**
     * Function that renders the icon. This is used in UI.
     *
     * @param value the value
     * @return the rendered icon
     */
    public Icon renderIcon(Object value) {
        return null;
    }
}
