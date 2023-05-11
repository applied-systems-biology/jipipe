package org.hkijena.jipipe.api.events;

import org.scijava.Disposable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * An event emitter
 *
 * @param <Event>    the event type
 * @param <Listener> the listener interface
 */
public abstract class JIPipeEventEmitter<Event extends JIPipeEvent, Listener> implements Disposable {

    private boolean disposed = false;

    private final List<Subscriber<Event, Listener>> subscribers = new ArrayList<>();

    private final Map<Listener, Subscriber<Event, Listener>> listenerSubscriberMap = new IdentityHashMap<>();

    private final AtomicInteger emittingDepth = new AtomicInteger(0);

    private final AtomicBoolean copyOnWriteActive = new AtomicBoolean();

    private void addSubscriber(Subscriber<Event, Listener> subscriber) {
        if (disposed) {
            throw new UnsupportedOperationException("Event emitter is disposed!");
        }
        synchronized (subscribers) {
            subscribers.add(subscriber);
        }
    }

    private void removeSubscriber(Subscriber<Event, Listener> subscriber) {
        if (disposed) {
            throw new UnsupportedOperationException("Event emitter is disposed!");
        }
        synchronized (subscribers) {
            subscribers.remove(subscriber);
        }
    }

    public void subscribe(Listener listener) {
        Subscriber<Event, Listener> subscriber = new StrongObjectSubscriber<>(listener);
        addSubscriber(subscriber);
    }

    public void subscribeLambda(BiConsumer<JIPipeEventEmitter<Event, Listener>, Event> listener) {
        Subscriber<Event, Listener> subscriber = new LambdaSubscriber<>(listener, false);
        addSubscriber(subscriber);
    }

    public void subscribeLambdaOnce(BiConsumer<JIPipeEventEmitter<Event, Listener>, Event> listener) {
        Subscriber<Event, Listener> subscriber = new LambdaSubscriber<>(listener, true);
        addSubscriber(subscriber);
    }

    public void subscribeWeak(Listener listener) {
        Subscriber<Event, Listener> subscriber = new WeakObjectSubscriber<>(listener);
        addSubscriber(subscriber);
    }

    public void unsubscribe(Listener listener) {
        Subscriber<Event, Listener> subscriber = listenerSubscriberMap.getOrDefault(listener, null);
        if (subscriber != null) {
            removeSubscriber(subscriber);
        }
    }

    public synchronized void emit(Event event) {
        if (disposed) {
            throw new UnsupportedOperationException("Event emitter is disposed!");
        }
        if (event.getEmitter() == null) {
            event.setEmitter(this);
        }
        boolean needsGC = false;
        synchronized (subscribers) {
            synchronized (subscribers) {
                for (int i = 0; i < subscribers.size(); i++) {
                    if (i < subscribers.size()) {
                        Subscriber<Event, Listener> subscriber = subscribers.get(i);
                        if (subscriber.isPresent()) {
                            try {
                                subscriber.call(this, event);
                            }
                            catch (Throwable e) {
                                e.printStackTrace();
                            }
                            if(subscriber.requestGCImmediatelyAfterCall()) {
                                needsGC = true;
                            }
                        } else {
                            needsGC = true;
                        }
                    }
                }
            }
        }
        if (needsGC) {
            gc();
        }
    }

    protected abstract void call(Listener listener, Event event);

    public void gc() {
        synchronized (subscribers) {
            subscribers.removeIf(l -> !l.isPresent());
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        subscribers.clear();
        listenerSubscriberMap.clear();
    }

    public interface Subscriber<Event extends JIPipeEvent, Listener> {
        void call(JIPipeEventEmitter<Event, Listener> emitter, Event event);

        boolean isPresent();

        boolean requestGCImmediatelyAfterCall();
    }

    public static class StrongObjectSubscriber<Event extends JIPipeEvent, Listener> implements Subscriber<Event, Listener> {
        private final Listener listener;

        public StrongObjectSubscriber(Listener listener) {
            this.listener = Objects.requireNonNull(listener);
        }

        @Override
        public void call(JIPipeEventEmitter<Event, Listener> emitter, Event event) {
            emitter.call(listener, event);
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public boolean requestGCImmediatelyAfterCall() {
            return false;
        }
    }

    public static class WeakObjectSubscriber<Event extends JIPipeEvent, Listener> implements Subscriber<Event, Listener> {
        private final WeakReference<Listener> listener;

        public WeakObjectSubscriber(Listener listener) {
            this.listener = new WeakReference<>(Objects.requireNonNull(listener));
        }

        @Override
        public void call(JIPipeEventEmitter<Event, Listener> emitter, Event event) {
            Listener listener_ = listener.get();
            if (listener_ != null) {
                emitter.call(listener_, event);
            }
        }

        @Override
        public boolean isPresent() {
            return listener.get() != null;
        }

        @Override
        public boolean requestGCImmediatelyAfterCall() {
            return false;
        }
    }

    public static class LambdaSubscriber<Event extends JIPipeEvent, Listener> implements Subscriber<Event, Listener> {
        private final BiConsumer<JIPipeEventEmitter<Event, Listener>, Event> function;
        private final boolean once;

        private boolean triggered;

        public LambdaSubscriber(BiConsumer<JIPipeEventEmitter<Event, Listener>, Event> function, boolean once) {
            this.function = function;
            this.once = once;
        }

        @Override
        public void call(JIPipeEventEmitter<Event, Listener> emitter, Event event) {
            function.accept(emitter, event);
        }

        @Override
        public boolean isPresent() {
            return !once || !triggered;
        }

        @Override
        public boolean requestGCImmediatelyAfterCall() {
            return once;
        }
    }
}
