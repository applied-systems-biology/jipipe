package org.hkijena.acaq5.extensions.parameters.filters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

import java.util.function.Predicate;

/**
 * Can either filter a string or a double
 */
public class StringOrDoubleFilter implements ACAQParameterCollection, ACAQValidatable, Predicate<Object> {

    private EventBus eventBus = new EventBus();
    private Mode mode = Mode.Double;
    private StringFilter stringFilter = new StringFilter();
    private DoubleFilter doubleFilter = new DoubleFilter();

    /**
     * Creates a new instance
     */
    public StringOrDoubleFilter() {
    }

    /**
     * Copies the object
     *
     * @param other the original
     */
    public StringOrDoubleFilter(StringOrDoubleFilter other) {
        this.mode = other.mode;
        this.stringFilter = new StringFilter(other.stringFilter);
        this.doubleFilter = new DoubleFilter(other.doubleFilter);
    }

    @ACAQDocumentation(name = "Mode", description = "Which source is used")
    @ACAQParameter("mode")
    @JsonGetter("mode")
    public Mode getMode() {
        return mode;
    }

    @ACAQParameter("mode")
    @JsonSetter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
        getEventBus().post(new ParameterChangedEvent(this, "mode"));
    }

    @ACAQDocumentation(name = "String filter", description = "The string filter")
    @ACAQParameter("string-filter")
    @JsonGetter("string-filter")
    public StringFilter getStringFilter() {
        return stringFilter;
    }

    @ACAQParameter("string-filter")
    @JsonSetter("string-filter")
    public void setStringFilter(StringFilter stringFilter) {
        this.stringFilter = stringFilter;
        getEventBus().post(new ParameterChangedEvent(this, "string-filter"));
    }

    @ACAQDocumentation(name = "Number filter", description = "The number filter")
    @ACAQParameter("number-filter")
    @JsonGetter("number-filter")
    public DoubleFilter getDoubleFilter() {
        return doubleFilter;
    }

    @ACAQParameter("number-filter")
    @JsonSetter("number-filter")
    public void setDoubleFilter(DoubleFilter doubleFilter) {
        this.doubleFilter = doubleFilter;
        getEventBus().post(new ParameterChangedEvent(this, "number-filter"));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (mode == Mode.Double) {
            report.report(stringFilter);
        } else if (mode == Mode.String) {
            report.report(doubleFilter);
        }
    }

    @Override
    public boolean test(Object o) {
        if(mode == Mode.String) {
            return stringFilter.test("" + o);
        }
        else {
            double value;
            if(o instanceof Number) {
                value = ((Number) o).doubleValue();
            }
            else {
                try {
                    value = Double.parseDouble("" + o);
                } catch (NumberFormatException e) {
                    value = 0;
                }
            }
            return doubleFilter.test(value);
        }
    }

    /**
     * Modes are that a column is picked or one is generated
     */
    public enum Mode {
        Double,
        String
    }
}
