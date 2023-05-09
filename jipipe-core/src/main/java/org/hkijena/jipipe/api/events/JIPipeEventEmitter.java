package org.hkijena.jipipe.api.events;

import org.hkijena.jipipe.utils.data.OwningStore;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.scijava.Disposable;

import java.util.*;

/**
 * An event emitter
 * @param <Event> the event type
 * @param <Listener> the listener interface
 */
public abstract class JIPipeEventEmitter<Event extends JIPipeEvent, Listener> implements Disposable {

    private final Set<Store<Listener>> subscribers = new LinkedHashSet<>();

    public synchronized void subscribe(Listener listener) {
        subscribers.add(new OwningStore<>(listener));
    }

    public synchronized void subscribeWeak(Listener listener) {
        subscribers.add(new WeakStore<>(listener));
    }

    public synchronized void unsubscribe(Listener listener) {
        subscribers.removeIf(l -> l.get() == listener);
    }

    public synchronized void emit(Event event) {
        if(event.getEmitter() == null) {
            event.setEmitter(this);
        }
        boolean needsGC = false;
        for (Store<Listener> subscriber : subscribers) {
            Listener listener = subscriber.get();
            if(listener != null) {
                call(listener, event);
            }
            else {
                needsGC = true;
            }
        }
        if(needsGC) {
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
