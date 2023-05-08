package org.hkijena.jipipe.api.events;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.utils.data.OwningStore;
import org.hkijena.jipipe.utils.data.Store;
import org.hkijena.jipipe.utils.data.WeakStore;
import org.scijava.Disposable;

import java.util.*;

/**
 * An event emitter
 * @param <T> the event type
 */
public class JIPipeEventEmitter<T extends JIPipeEvent> implements Disposable {

    private final Set<Store<JIPipeEventListener<T>>> subscribers = new LinkedHashSet<>();

    public synchronized void register(JIPipeEventListener<T> listener) {
        subscribers.add(new OwningStore<>(listener));
    }

    public synchronized void registerWeak(JIPipeEventListener<T> listener) {
        subscribers.add(new WeakStore<>(listener));
    }

    public synchronized void unsubscribe(JIPipeEventListener<T> listener) {
        subscribers.removeIf(l -> l.get() == listener);
    }

    public synchronized void publish() {
        boolean needsGC = false;
        for (Store<JIPipeEventListener<T>> subscriber : subscribers) {
            JIPipeEventListener<T> listener = subscriber.get();
            if(listener != null) {

            }
            else {
                needsGC = true;
            }
        }
        if(needsGC) {
            gc();
        }
    }

    public synchronized void gc() {
        subscribers.removeIf(l -> !l.isPresent());
    }

    @Override
    public void dispose() {
        subscribers.clear();
    }
}
