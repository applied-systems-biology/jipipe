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

import org.hkijena.jipipe.api.service.components.JIPipeServerServiceComponent;

import java.util.UUID;

/**
 * An AutoCloseable lease that guards access to a running server instance.
 * 
 * <p>Inspired by the cache system's pin pattern, the lease provides:</p>
 * <ul>
 *     <li>Use-after-free protection via {@link #checkNotClosed()}</li>
 *     <li>Automatic release on {@link #close()} (for try-with-resources)</li>
 *     <li>Reference counting in the service component</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>
 * try (JIPipeServerLease<MyServerInstance> lease = service.acquireLease(...)) {
 *     lease.getInstance().doSomething();
 * } // automatically released
 * </pre>
 *
 * @param <TInst> the server instance type
 */
public class JIPipeServerLease<TInst extends JIPipeServerInstance<?>> implements AutoCloseable {
    private final TInst instance;
    private final String id;
    private final JIPipeServerServiceComponent serviceComponent;
    private volatile boolean closed = false;

    /**
     * Creates a new lease. Package-private: leases are created by the service component.
     *
     * @param instance         the server instance
     * @param serviceComponent the service component that manages this lease
     */
    public JIPipeServerLease(TInst instance, JIPipeServerServiceComponent serviceComponent) {
        this.instance = instance;
        this.id = UUID.randomUUID().toString();
        this.serviceComponent = serviceComponent;
    }

    /**
     * Gets the server instance. Throws {@link IllegalStateException} if the lease is closed.
     *
     * @return the server instance
     * @throws IllegalStateException if the lease has been closed
     */
    public TInst getInstance() {
        checkNotClosed();
        return instance;
    }

    /**
     * Returns the unique lease ID.
     *
     * @return the lease ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns whether this lease is still open (not yet closed).
     *
     * @return true if open
     */
    public boolean isOpen() {
        return !closed;
    }

    /**
     * Releases this lease. After calling close, any further calls to {@link #getInstance()}
     * will throw {@link IllegalStateException}.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            serviceComponent.releaseLease(this);
        }
    }

    /**
     * Returns the server instance without checking if the lease is closed.
     * This is intended for use by the service component during lease release,
     * where the instance is needed for state management even after the lease is closed.
     *
     * @return the server instance
     */
    public JIPipeServerInstance<?> getInstanceUnchecked() {
        return instance;
    }

    /**
     * Throws {@link IllegalStateException} if this lease has been closed.
     */
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Server lease '" + id + "' has been closed. " +
                    "The server instance can no longer be accessed through this lease.");
        }
    }
}
