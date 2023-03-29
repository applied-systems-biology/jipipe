package org.hkijena.jipipe.utils.data;

/**
 * Stores data. Acts as abstraction layer between strong and weak references. See {@link OwningStore} and {@link WeakStore}
 */
public interface Store<T> {
    /**
     * Gets the stored data or null if the data is not present
     *
     * @return the stored data or null
     */
    T get();

    /**
     * Returns true if the stored data is present
     *
     * @return true if the stored data is present
     */
    boolean isPresent();
}
