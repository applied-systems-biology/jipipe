package org.hkijena.jipipe.api.events;

import org.hkijena.jipipe.utils.data.OwningStore;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.scijava.Disposable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * An event emitter
 *
 * @param <Event>    the event type
 * @param <Listener> the listener interface
 */
public abstract class JIPipeEventEmitter<Event extends JIPipeEvent, Listener> implements Disposable {

    private final Set<Store<Listener>> subscribers = new LinkedHashSet<>();

    private final Set<BiConsumer<JIPipeEventEmitter<Event, Listener>, Event>> actionSubscribers = new LinkedHashSet<>();

    private final Set<BiConsumer<JIPipeEventEmitter<Event, Listener>, Event>> actionSubscribersOnce = new LinkedHashSet<>();

    public synchronized void subscribe(Listener listener) {
        subscribers.add(new OwningStore<>(listener));
    }

    public synchronized void subscribeLambda(BiConsumer<JIPipeEventEmitter<Event, Listener>, Event> listener) {
        actionSubscribers.add(listener);
    }

    public synchronized void subscribeLambdaOnce(BiConsumer<JIPipeEventEmitter<Event, Listener>, Event> listener) {
        actionSubscribers.add(listener);
        actionSubscribersOnce.add(listener);
    }

    public synchronized void subscribeWeak(Listener listener) {
        subscribers.add(new WeakStore<>(listener));
    }

    public synchronized void unsubscribe(Listener listener) {
        subscribers.removeIf(l -> l.get() == listener || l.get() == null);
    }

    public synchronized void emit(Event event) {
        if (event.getEmitter() == null) {
            event.setEmitter(this);
        }
        boolean needsGC = false;
        for (Store<Listener> subscriber : subscribers) {
            Listener listener = subscriber.get();
            if (listener != null) {
                call(listener, event);
            } else {
                needsGC = true;
            }
        }
        List<BiConsumer<JIPipeEventEmitter<Event, Listener>, Event>> toDelete = null;
        if (!actionSubscribersOnce.isEmpty()) {
            toDelete = new ArrayList<>();
        }
        for (BiConsumer<JIPipeEventEmitter<Event, Listener>, Event> subscriber : actionSubscribers) {
            subscriber.accept(this, event);
            if (actionSubscribersOnce.contains(subscriber)) {
                toDelete.add(subscriber);
            }
        }
        if (toDelete != null) {
            actionSubscribersOnce.removeAll(toDelete);
            actionSubscribers.removeAll(toDelete);
        }
        if (needsGC) {
            gc();
        }
    }

    protected abstract void call(Listener listener, Event event);

    public synchronized void gc() {
        subscribers.removeIf(l -> !l.isPresent());
    }

    @Override
    public void dispose() {
        subscribers.clear();
    }
}
