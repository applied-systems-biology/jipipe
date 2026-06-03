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

package org.hkijena.jipipe.servers;

import org.hkijena.jipipe.api.servers.JIPipeServerInstance;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reference-counted entry for an active server instance.
 * Used internally by {@link org.hkijena.jipipe.api.service.components.JIPipeServerServiceComponent}
 * to track lease counts per instance.
 */
public class ManagedInstanceEntry {
    private final JIPipeServerInstance<?> instance;
    private final AtomicInteger leaseCount = new AtomicInteger(0);

    /**
     * @param instance the server instance to track
     */
    public ManagedInstanceEntry(JIPipeServerInstance<?> instance) {
        this.instance = instance;
    }

    /**
     * Increments the lease count.
     *
     * @return the new lease count
     */
    public int acquire() {
        return leaseCount.incrementAndGet();
    }

    /**
     * Decrements the lease count.
     *
     * @return the new lease count
     */
    public int release() {
        return leaseCount.decrementAndGet();
    }

    /**
     * Returns the current lease count.
     *
     * @return the lease count
     */
    public int getLeaseCount() {
        return leaseCount.get();
    }

    /**
     * Returns the server instance.
     *
     * @return the instance
     */
    public JIPipeServerInstance<?> getInstance() {
        return instance;
    }
}
