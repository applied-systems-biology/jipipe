package org.hkijena.jipipe.api.instrumentation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple publish-subscribe event bus for instrumentation events.
 * Listeners subscribe by event type and are notified when events of that type are published.
 */
public class InstrumentationEventBus {
    private final Map<Class<? extends InstrumentationEvent>, List<Consumer<? extends InstrumentationEvent>>> listeners = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends InstrumentationEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public void publish(InstrumentationEvent event) {
        if (event == null) return;
        List<Consumer<? extends InstrumentationEvent>> listenersForType = listeners.get(event.getClass());
        if (listenersForType != null) {
            for (Consumer<? extends InstrumentationEvent> listener : listenersForType) {
                ((Consumer<InstrumentationEvent>) listener).accept(event);
            }
        }
    }

    public void clear() {
        listeners.clear();
    }
}
