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

package org.hkijena.jipipe.api.service.components;

import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEvent;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEventEmitter;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeListener;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.servers.*;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service component that manages the lifecycle of external server instances.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *     <li>Registry of server instance factories (mapping environment types to server types)</li>
 *     <li>Lifecycle management: start, stop, health check, auto-shutdown</li>
 *     <li>Lease acquisition and release with reference counting</li>
 *     <li>Port assignment via {@link PortManager}</li>
 *     <li>Event-driven state change notifications (via each instance's own emitter)</li>
 * </ul>
 *
 * <p>Server instances now extend {@link org.hkijena.jipipe.api.microservice.AbstractMicroservice},
 * so lifecycle state transitions (Starting → Ready → Stopping → Stopped) are handled
 * internally by {@code start()}/{@code stop()}. The service component simply delegates
 * to those methods and uses {@code setStateDetail()} for Busy/Idle nuance.</p>
 */
public class JIPipeServerServiceComponent extends JIPipeServiceComponent {

    private final Map<String, FactoryEntry<?, ?>> factories = new ConcurrentHashMap<>();
    private final Map<String, ManagedInstanceEntry> activeInstances = new ConcurrentHashMap<>();
    private final JIPipeServerInstanceManager appWideManager;
    private final PortManager portManager = new PortManager();
    private final JIPipeQueuedRunnableExecutor lifecycleQueue = new JIPipeQueuedRunnableExecutor("Server Lifecycle");
    private final MicroserviceStateChangeEventEmitter stateChangedEventEmitter = new MicroserviceStateChangeEventEmitter();

    public JIPipeServerServiceComponent(JIPipeService service) {
        super(service);
        this.lifecycleQueue.setSilent(true);
        this.appWideManager = new JIPipeServerInstanceManager(this);
    }

    // ===== Factory Registration =====

    /**
     * Registers a server instance factory.
     *
     * @param factoryId the unique factory ID (e.g., "ipython-server")
     * @param envClass  the environment class this factory accepts
     * @param instClass the server instance class this factory produces
     * @param factory   the factory function
     * @param icon      the icon for this server type
     * @param <TEnv>    the environment type
     * @param <TInst>   the server instance type
     */
    public <TEnv extends JIPipeEnvironment, TInst extends JIPipeServerInstance<TEnv>>
    void registerServerInstanceFactory(String factoryId, Class<TEnv> envClass,
                                       Class<TInst> instClass,
                                       ServerInstanceFactory<TEnv, TInst> factory,
                                       Icon icon) {
        factories.put(factoryId, new FactoryEntry<>(factoryId, envClass, instClass, factory, icon));
    }

    /**
     * Returns the factory entry for the given factory ID.
     *
     * @param factoryId the factory ID
     * @return the factory entry, or null if not found
     */
    public FactoryEntry<?, ?> getFactory(String factoryId) {
        return factories.get(factoryId);
    }

    /**
     * Returns all registered factory IDs.
     *
     * @return unmodifiable set of factory IDs
     */
    public Set<String> getFactoryIds() {
        return Collections.unmodifiableSet(factories.keySet());
    }

    // ===== Lease Acquisition =====

    /**
     * Acquires a lease for a server instance. If an instance for the given environment
     * already exists and is running, increments the reference count and returns a new lease.
     * If not, creates a new instance, starts it, and returns a lease.
     *
     * @param factoryId   the server instance factory ID
     * @param instClass   the expected server instance class
     * @param environment the environment to configure the server
     * @param <TInst>     the server instance type
     * @return a lease for the server instance
     * @throws IllegalArgumentException if the factory is not registered or the instance class doesn't match
     * @throws ServerStartException     if the server fails to start
     */
    public <TInst extends JIPipeServerInstance<?>>
    JIPipeServerLease<TInst> acquireLease(String factoryId, Class<TInst> instClass, JIPipeEnvironment environment) {
        FactoryEntry<?, ?> entry = factories.get(factoryId);
        if (entry == null) {
            throw new IllegalArgumentException("No server instance factory registered for ID: " + factoryId);
        }

        String instanceKey = createInstanceKey(factoryId, environment);

        ManagedInstanceEntry managedEntry = activeInstances.computeIfAbsent(instanceKey, k -> {
            // Create new instance
            int port = resolvePort(environment);
            @SuppressWarnings("unchecked")
            ServerInstanceFactory<JIPipeEnvironment, JIPipeServerInstance<JIPipeEnvironment>> typedFactory =
                    (ServerInstanceFactory<JIPipeEnvironment, JIPipeServerInstance<JIPipeEnvironment>>) entry.factory;
            JIPipeServerInstance<JIPipeEnvironment> instance = typedFactory.create(environment, port);
            // Relay state change events from this instance to the service-level emitter
            instance.getStateChangeEventEmitter().subscribeLambda((emitter, event) ->
                    stateChangedEventEmitter.emit(event));
            return new ManagedInstanceEntry(instance);
        });

        @SuppressWarnings("unchecked")
        TInst instance = (TInst) managedEntry.getInstance();

        // Validate type
        if (!instClass.isInstance(instance)) {
            throw new IllegalArgumentException("Factory '" + factoryId + "' produces " +
                    instance.getClass().getName() + " but " + instClass.getName() + " was requested");
        }

        // Start the instance if not ready (AbstractMicroservice.start() blocks until Ready/Failed)
        if (!instance.isReady()) {
            instance.start();
            if (!instance.isReady()) {
                throw new ServerStartException("Server failed to start: " + instance.getStateDetail(),
                        ServerStartException.Reason.Unknown);
            }
        }

        // Increment reference count
        managedEntry.acquire();

        // Update state detail to indicate busy
        instance.updateStateDetail("Busy (port " + instance.getPort() + ")");

        return new JIPipeServerLease<>(instance, this);
    }

    /**
     * Releases a lease. Called by {@link JIPipeServerLease#close()}.
     * Decrements the reference count and updates the instance state detail.
     *
     * @param lease the lease to release
     */
    public void releaseLease(JIPipeServerLease<?> lease) {
        JIPipeServerInstance<?> instance = lease.getInstanceUnchecked();
        String instanceKey = findInstanceKey(instance);
        if (instanceKey == null) {
            return; // Instance no longer tracked
        }

        ManagedInstanceEntry managedEntry = activeInstances.get(instanceKey);
        if (managedEntry == null) {
            return;
        }

        int newCount = managedEntry.release();
        if (newCount <= 0) {
            // No more leases — update state detail to idle
            instance.updateStateDetail("Idle (port " + instance.getPort() + ")");
        }
    }

    // ===== Lifecycle =====

    /**
     * Stops a server instance. Delegates to {@link JIPipeServerInstance#stop()},
     * which is the {@link org.hkijena.jipipe.api.microservice.AbstractMicroservice}
     * lifecycle method (handles Stopping → Stopped transitions internally).
     *
     * @param instance the instance to stop
     */
    public void stopInstance(JIPipeServerInstance<?> instance) {
        if (instance.getState() == MicroserviceState.Stopped) {
            return;
        }
        try {
            instance.stop();
        } finally {
            portManager.releasePort(instance.getPort());
            String key = findInstanceKey(instance);
            if (key != null) {
                activeInstances.remove(key);
            }
        }
    }

    /**
     * Stops all active server instances. Called during application shutdown.
     */
    public void releaseAll() {
        List<JIPipeServerInstance<?>> instancesToStop = new ArrayList<>();
        for (ManagedInstanceEntry entry : activeInstances.values()) {
            instancesToStop.add(entry.getInstance());
        }

        // Stop in reverse order (LIFO)
        for (int i = instancesToStop.size() - 1; i >= 0; i--) {
            try {
                stopInstance(instancesToStop.get(i));
            } catch (Exception e) {
                // Best effort during shutdown
            }
        }
        activeInstances.clear();
    }

    // ===== Queries =====

    /**
     * Returns all active server instances.
     *
     * @return unmodifiable list of active instances
     */
    public List<JIPipeServerInstance<?>> getActiveInstances() {
        List<JIPipeServerInstance<?>> instances = new ArrayList<>();
        for (ManagedInstanceEntry entry : activeInstances.values()) {
            instances.add(entry.getInstance());
        }
        return Collections.unmodifiableList(instances);
    }

    /**
     * Returns the state of the instance identified by the given key.
     *
     * @param instanceKey the instance key
     * @return the state, or null if no such instance exists
     */
    public MicroserviceState getState(String instanceKey) {
        ManagedInstanceEntry entry = activeInstances.get(instanceKey);
        return entry != null ? entry.getInstance().getState() : null;
    }

    /**
     * Returns the port manager.
     *
     * @return the port manager
     */
    public PortManager getPortManager() {
        return portManager;
    }

    /**
     * Returns the app-wide server instance manager.
     *
     * @return the app-wide manager
     */
    public JIPipeServerInstanceManager getAppWideManager() {
        return appWideManager;
    }

    /**
     * Returns the lifecycle queue.
     *
     * @return the lifecycle queue
     */
    public JIPipeQueuedRunnableExecutor getLifecycleQueue() {
        return lifecycleQueue;
    }

    // ===== Event System =====

    /**
     * Returns the service-level event emitter that relays state change events
     * from all managed server instances. UI components can subscribe to this
     * emitter to receive notifications when any instance changes state.
     *
     * @return the relay event emitter
     */
    public MicroserviceStateChangeEventEmitter getStateChangedEventEmitter() {
        return stateChangedEventEmitter;
    }

    // ===== Internal Helpers =====

    /**
     * Creates an instance key from the factory ID and environment.
     */
    private String createInstanceKey(String factoryId, JIPipeEnvironment environment) {
        return factoryId + ":" + environment.getClass().getName() + ":" + environment.getName();
    }

    /**
     * Finds the instance key for a given instance.
     */
    private String findInstanceKey(JIPipeServerInstance<?> instance) {
        for (Map.Entry<String, ManagedInstanceEntry> entry : activeInstances.entrySet()) {
            if (entry.getValue().getInstance() == instance) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Resolves the port for a server instance from the environment.
     * If the environment has an OptionalIntegerParameter port field that is enabled,
     * uses that port. Otherwise, auto-assigns via PortManager.
     *
     * <p>This method uses the parameter collection API to look for a "port" parameter
     * of type {@link OptionalIntegerParameter} in the environment. If found and enabled,
     * the specified port is used. Otherwise, a free port is auto-assigned.</p>
     *
     * @param environment the environment
     * @return the resolved port number
     * @throws ServerStartException if the requested port is unavailable
     */
    private int resolvePort(JIPipeEnvironment environment) {
        // Try to find an OptionalIntegerParameter named "port" via the parameter collection
        try {
            JIPipeParameterAccess portAccess = environment.getParameterAccess("port");
            if (portAccess != null) {
                OptionalIntegerParameter optionalPort = portAccess.get(OptionalIntegerParameter.class);
                if (optionalPort != null && optionalPort.isEnabled()) {
                    int requestedPort = optionalPort.getContent();
                    if (portManager.isPortAvailable(requestedPort)) {
                        portManager.reservePort(requestedPort);
                        return requestedPort;
                    } else {
                        throw new ServerStartException("Requested port " + requestedPort + " is not available",
                                ServerStartException.Reason.PortUnavailable);
                    }
                }
            }
        } catch (ServerStartException e) {
            throw e;
        } catch (Exception e) {
            // No port parameter found, fall through to auto-assignment
        }

        return portManager.assignFreePort();
    }

    // ===== Factory Entry =====

    /**
     * Holds metadata about a registered server instance factory.
     */
    public static class FactoryEntry<TEnv extends JIPipeEnvironment, TInst extends JIPipeServerInstance<TEnv>> {
        private final String factoryId;
        private final Class<TEnv> envClass;
        private final Class<TInst> instClass;
        private final ServerInstanceFactory<TEnv, TInst> factory;
        private final Icon icon;

        public FactoryEntry(String factoryId, Class<TEnv> envClass, Class<TInst> instClass,
                            ServerInstanceFactory<TEnv, TInst> factory, Icon icon) {
            this.factoryId = factoryId;
            this.envClass = envClass;
            this.instClass = instClass;
            this.factory = factory;
            this.icon = icon;
        }

        public String getFactoryId() {
            return factoryId;
        }

        public Class<TEnv> getEnvClass() {
            return envClass;
        }

        public Class<TInst> getInstClass() {
            return instClass;
        }

        public ServerInstanceFactory<TEnv, TInst> getFactory() {
            return factory;
        }

        public Icon getIcon() {
            return icon;
        }
    }
}
