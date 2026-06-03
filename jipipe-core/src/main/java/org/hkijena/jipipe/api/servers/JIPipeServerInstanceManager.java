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

import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.service.components.JIPipeServerServiceComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An {@link AutoCloseable} manager that holds a block of server instance leases.
 * 
 * <p>Two common usage patterns:</p>
 * <ul>
 *     <li><b>Run-scoped:</b> Created at start of pipeline run, holds all servers needed
 *         by that run, released at end.</li>
 *     <li><b>App-wide:</b> Long-lived manager holding servers shared across multiple runs.</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>
 * try (JIPipeServerInstanceManager servers = new JIPipeServerInstanceManager(serviceComponent)) {
 *     JIPipeServerLease<MyServer> lease = servers.acquireLease("my-server", MyServer.class, env);
 *     lease.getInstance().doWork();
 * } // all leases released
 * </pre>
 */
public class JIPipeServerInstanceManager implements AutoCloseable {
    private final Map<String, JIPipeServerLease<?>> activeLeases = new LinkedHashMap<>();
    private final JIPipeServerServiceComponent serviceComponent;
    private volatile boolean closed = false;

    /**
     * @param serviceComponent the service component that manages server instances
     */
    public JIPipeServerInstanceManager(JIPipeServerServiceComponent serviceComponent) {
        this.serviceComponent = serviceComponent;
    }

    /**
     * Acquires a lease for a server instance. If an instance for the same key
     * is already managed by this manager, returns the existing lease.
     * Otherwise, acquires a new lease from the service component.
     *
     * @param factoryId     the server instance factory ID
     * @param instanceClass the expected server instance class
     * @param environment   the environment to configure the server
     * @param <TInst>       the server instance type
     * @return a lease for the server instance
     */
    public <TInst extends JIPipeServerInstance<?>> JIPipeServerLease<TInst> acquireLease(
            String factoryId, Class<TInst> instanceClass, JIPipeEnvironment environment) {
        ensureOpen();
        String key = factoryId + ":" + environment.getClass().getName() + ":" + environment.getName();
        @SuppressWarnings("unchecked")
        JIPipeServerLease<TInst> existing = (JIPipeServerLease<TInst>) activeLeases.get(key);
        if (existing != null && existing.isOpen()) {
            return existing;
        }
        JIPipeServerLease<TInst> lease = serviceComponent.acquireLease(factoryId, instanceClass, environment);
        activeLeases.put(key, lease);
        return lease;
    }

    /**
     * Releases a specific lease from this manager.
     *
     * @param lease the lease to release
     */
    public void releaseLease(JIPipeServerLease<?> lease) {
        ensureOpen();
        activeLeases.values().removeIf(l -> l.getId().equals(lease.getId()));
        lease.close();
    }

    /**
     * Releases all managed leases in LIFO order. Best-effort cleanup.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Throwable firstError = null;
            // Release in LIFO order
            Object[] leaseArray = activeLeases.values().toArray();
            for (int i = leaseArray.length - 1; i >= 0; i--) {
                try {
                    ((JIPipeServerLease<?>) leaseArray[i]).close();
                } catch (Throwable e) {
                    if (firstError == null) {
                        firstError = e;
                    } else {
                        firstError.addSuppressed(e);
                    }
                }
            }
            activeLeases.clear();
            if (firstError != null) {
                if (firstError instanceof RuntimeException re) throw re;
                throw new RuntimeException(firstError);
            }
        }
    }

    /**
     * Returns the currently active leases.
     *
     * @return unmodifiable collection of active leases
     */
    public Collection<JIPipeServerLease<?>> getActiveLeases() {
        return Collections.unmodifiableCollection(activeLeases.values());
    }

    /**
     * Returns whether this manager is still open.
     *
     * @return true if open
     */
    public boolean isOpen() {
        return !closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Server instance manager has been closed");
        }
    }
}
