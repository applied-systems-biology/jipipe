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

package org.hkijena.jipipe.api.servers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates multiple server leases and releases them all on {@link #close()}, in LIFO order.
 * 
 * <p>Inspired by the cache system's scope pattern, this allows managing multiple
 * server leases within a single try-with-resources block:</p>
 * <pre>
 * try (JIPipeServerLeaseScope scope = new JIPipeServerLeaseScope()) {
 *     JIPipeServerLease<ServerA> a = scope.add(acquireLeaseA());
 *     JIPipeServerLease<ServerB> b = scope.add(acquireLeaseB());
 *     // use a and b ...
 * } // both released in LIFO order (b first, then a)
 * </pre>
 */
public class JIPipeServerLeaseScope implements AutoCloseable {
    private final List<JIPipeServerLease<?>> leases = new ArrayList<>();
    private volatile boolean closed = false;

    /**
     * Adds a lease to this scope. The lease will be released when this scope is closed.
     *
     * @param lease the lease to add
     * @param <TInst> the server instance type
     * @return the added lease (for convenient assignment)
     */
    public <TInst extends JIPipeServerInstance<?>> JIPipeServerLease<TInst> add(JIPipeServerLease<TInst> lease) {
        ensureOpen();
        leases.add(lease);
        return lease;
    }

    /**
     * Releases all managed leases in LIFO order. Best-effort: if any lease
     * throws during close, the error is suppressed and remaining leases are
     * still released.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            // Release in LIFO order
            Throwable firstError = null;
            for (int i = leases.size() - 1; i >= 0; i--) {
                try {
                    leases.get(i).close();
                } catch (Throwable e) {
                    if (firstError == null) {
                        firstError = e;
                    } else {
                        firstError.addSuppressed(e);
                    }
                }
            }
            leases.clear();
            if (firstError != null) {
                if (firstError instanceof RuntimeException re) throw re;
                throw new RuntimeException(firstError);
            }
        }
    }

    /**
     * Returns the list of currently managed leases.
     *
     * @return unmodifiable list of leases
     */
    public List<JIPipeServerLease<?>> getLeases() {
        return Collections.unmodifiableList(leases);
    }

    /**
     * Returns whether this scope is still open.
     *
     * @return true if open
     */
    public boolean isOpen() {
        return !closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Server lease scope has been closed");
        }
    }
}
