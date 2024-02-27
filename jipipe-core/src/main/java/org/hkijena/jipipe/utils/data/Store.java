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
