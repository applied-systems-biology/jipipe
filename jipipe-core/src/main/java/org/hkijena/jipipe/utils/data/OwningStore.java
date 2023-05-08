package org.hkijena.jipipe.utils.data;

import java.util.Objects;

/**
 * A store that keeps a strong reference to the data
 *
 * @param <T>
 */
public class OwningStore<T> implements Store<T> {
    private final T data;

    public OwningStore(T data) {
        this.data = data;
    }

    @Override
    public T get() {
        return data;
    }

    @Override
    public boolean isPresent() {
        return data != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store<?> that = (Store<?>) o;
        return Objects.equals(get(), that.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
