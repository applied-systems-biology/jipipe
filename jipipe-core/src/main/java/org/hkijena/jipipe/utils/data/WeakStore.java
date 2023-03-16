package org.hkijena.jipipe.utils.data;

import java.lang.ref.WeakReference;

/**
 * A weakly owning store (using a {@link WeakReference}
 *
 * @param <T>
 */
public class WeakStore<T> implements Store<T> {
    private final WeakReference<T> reference;

    public WeakStore(T data) {
        reference = new WeakReference<>(data);
    }

    public WeakStore(WeakReference<T> reference) {
        this.reference = reference;
    }

    @Override
    public T get() {
        return reference.get();
    }

    @Override
    public boolean isPresent() {
        return reference.get() != null;
    }
}
