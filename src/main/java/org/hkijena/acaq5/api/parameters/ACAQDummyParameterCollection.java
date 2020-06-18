package org.hkijena.acaq5.api.parameters;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link ACAQParameterCollection} that stores exactly one value (an object)
 * This is used in conjunction with {@link ACAQManualParameterAccess}
 */
public class ACAQDummyParameterCollection implements ACAQParameterCollection, Consumer<Object>, Supplier<Object> {

    private final  EventBus eventBus = new EventBus();
    private Object value;

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public void accept(Object o) {
        this.value = o;
        eventBus.post(new ParameterChangedEvent(this, "value"));
    }

    @Override
    public Object get() {
        return value;
    }
}
