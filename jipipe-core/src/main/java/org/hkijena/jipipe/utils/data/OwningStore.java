package org.hkijena.jipipe.utils.data;

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
}
