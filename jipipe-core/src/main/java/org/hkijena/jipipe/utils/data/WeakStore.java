/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

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
