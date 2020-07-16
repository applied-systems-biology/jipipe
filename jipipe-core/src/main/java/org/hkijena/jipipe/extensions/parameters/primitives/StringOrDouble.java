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

package org.hkijena.jipipe.extensions.parameters.primitives;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;

/**
 * Can either filter a string or a double
 */
public class StringOrDouble implements JIPipeParameterCollection {

    private EventBus eventBus = new EventBus();
    private Mode mode = Mode.Double;
    private String stringValue = "";
    private double doubleValue = 0;

    /**
     * Creates a new instance
     */
    public StringOrDouble() {
    }

    /**
     * Copies the object
     *
     * @param other the original
     */
    public StringOrDouble(StringOrDouble other) {
        this.mode = other.mode;
        this.stringValue = other.stringValue;
        this.doubleValue = other.doubleValue;
    }

    @JIPipeDocumentation(name = "Mode", description = "Which source is used")
    @JIPipeParameter("mode")
    @JsonGetter("mode")
    public Mode getMode() {
        return mode;
    }

    @JIPipeParameter("mode")
    @JsonSetter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;

    }

    @JIPipeDocumentation(name = "String", description = "The string")
    @JIPipeParameter("string")
    @JsonGetter("string")
    public String getStringValue() {
        return stringValue;
    }

    @JIPipeParameter("string")
    @JsonSetter("string")
    public void setStringValue(String String) {
        this.stringValue = String;

    }

    @JIPipeDocumentation(name = "Number", description = "The number")
    @JIPipeParameter("number")
    @JsonGetter("number")
    public double getDoubleValue() {
        return doubleValue;
    }

    @JIPipeParameter("number")
    @JsonSetter("number")
    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;

    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the current value
     *
     * @return the value according the the current mode
     */
    public Object getValue() {
        return mode == Mode.String ? stringValue : doubleValue;
    }

    /**
     * Modes are that a column is picked or one is generated
     */
    public enum Mode {
        Double,
        String
    }

    /**
     * A collection of multiple {@link StringOrDouble}
     * The filters are connected via "OR"
     */
    public static class List extends ListParameter<StringOrDouble> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringOrDouble.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringOrDouble.class);
            for (StringOrDouble filter : other) {
                add(new StringOrDouble(filter));
            }
        }
    }
}
