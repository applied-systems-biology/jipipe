package org.hkijena.acaq5.extensions.parameters.filters;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A parameter that renames a matching string into another string
 */
public class StringRenaming implements ACAQParameterCollection, Predicate<String>, Function<String, String> {

    private EventBus eventBus = new EventBus();
    private StringFilter filter = new StringFilter();
    private String target;

    /**
     * Creates a new instance
     */
    public StringRenaming() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringRenaming(StringRenaming other) {
        this.filter = new StringFilter(other.filter);
        this.target = other.target;
    }

    @Override
    public String apply(String s) {
        if (test(s)) {
            return target;
        } else {
            return s;
        }
    }

    @Override
    public boolean test(String s) {
        return filter.test(s);
    }

    @ACAQDocumentation(name = "Filter", description = "The filter to test the input")
    @ACAQParameter("filter")
    public StringFilter getFilter() {
        return filter;
    }

    @ACAQParameter("filter")
    public void setFilter(StringFilter filter) {
        this.filter = filter;
        eventBus.post(new ParameterChangedEvent(this, "filter"));
    }

    @ACAQDocumentation(name = "Target", description = "The string that is returned if the filter applies")
    @ACAQParameter("target")
    public String getTarget() {
        return target;
    }

    @ACAQParameter("target")
    public void setTarget(String target) {
        this.target = target;
        eventBus.post(new ParameterChangedEvent(this, "target"));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
