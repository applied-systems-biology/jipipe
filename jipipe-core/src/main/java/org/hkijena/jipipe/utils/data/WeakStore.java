package org.hkijena.jipipe.utils.data;

import java.lang.ref.WeakReference;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store<?> weakStore = (Store<?>) o;
        T o1 = reference.get();
        Object o2 = weakStore.get();
        return Objects.equals(o1, o2);
    }

    @Override
    public int hashCode() {
        T t = reference.get();
        if (t != null) {
            return t.hashCode();
        } else {
            return 0;
        }
    }
}
