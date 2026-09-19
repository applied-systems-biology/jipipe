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

package org.hkijena.jipipe.api.microservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base implementation of {@link Microservice} that manages state transitions,
 * dependency resolution, and event emission.
 *
 * <p>Subclasses implement {@link #onStart()} and {@link #onStop()} for actual
 * resource management. The {@link #start()} and {@link #stop()} methods are
 * final and orchestrate state transitions, dependency checking, and events.</p>
 *
 * <p>Thread safety: state is volatile, dependencies use CopyOnWriteArrayList,
 * start()/stop() are synchronized on this instance.</p>
 */
public abstract class AbstractMicroservice implements Microservice {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMicroservice.class);

    /**
     * Timeout for waiting on a dependency to transition out of Starting state (seconds).
     */
    private static final long DEPENDENCY_START_TIMEOUT_SECONDS = 120;

    /**
     * Polling interval when waiting for a dependency (milliseconds).
     */
    private static final long DEPENDENCY_POLL_INTERVAL_MS = 500;

    private final String name;
    private volatile MicroserviceState state = MicroserviceState.Stopped;
    private volatile String stateDetail = "";
    private final List<Microservice> dependencies = new CopyOnWriteArrayList<>();
    private final MicroserviceStateChangeEventEmitter stateChangeEventEmitter = new MicroserviceStateChangeEventEmitter();

    protected AbstractMicroservice(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public final synchronized void start() {
        if (state == MicroserviceState.Starting || state == MicroserviceState.Ready) {
            return;
        }

        // Start dependencies first
        for (Microservice dep : dependencies) {
            if (dep.getState() == MicroserviceState.Ready) {
                continue;
            }
            if (dep.getState() == MicroserviceState.Starting) {
                if (!waitForDependency(dep)) {
                    MicroserviceState oldState = state;
                    state = MicroserviceState.Failed;
                    stateDetail = "Dependency '" + dep.getName() + "' failed to start";
                    fireStateChange(oldState, MicroserviceState.Failed);
                    return;
                }
                continue;
            }
            // Dependency is Stopped or Failed — start it
            dep.start();
            if (!waitForDependency(dep)) {
                MicroserviceState oldState = state;
                state = MicroserviceState.Failed;
                stateDetail = "Dependency '" + dep.getName() + "' failed to start";
                fireStateChange(oldState, MicroserviceState.Failed);
                return;
            }
        }

        MicroserviceState oldState = state;
        state = MicroserviceState.Starting;
        fireStateChange(oldState, MicroserviceState.Starting);

        try {
            onStart();
            oldState = state;
            state = MicroserviceState.Ready;
            fireStateChange(oldState, MicroserviceState.Ready);
        } catch (Exception e) {
            LOGGER.error("Failed to start microservice '{}': {}", name, e.getMessage(), e);
            oldState = state;
            state = MicroserviceState.Failed;
            stateDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            fireStateChange(oldState, MicroserviceState.Failed);
        }
    }

    @Override
    public final synchronized void stop() {
        if (state == MicroserviceState.Stopped || state == MicroserviceState.Stopping) {
            return;
        }

        MicroserviceState oldState = state;
        state = MicroserviceState.Stopping;
        fireStateChange(oldState, MicroserviceState.Stopping);

        try {
            onStop();
        } catch (Exception e) {
            LOGGER.error("Error stopping microservice '{}': {}", name, e.getMessage(), e);
        }

        oldState = state;
        state = MicroserviceState.Stopped;
        stateDetail = "";
        fireStateChange(oldState, MicroserviceState.Stopped);
    }

    @Override
    public MicroserviceState getState() {
        return state;
    }

    @Override
    public String getStateDetail() {
        return stateDetail;
    }

    @Override
    public boolean isReady() {
        return state == MicroserviceState.Ready;
    }

    @Override
    public List<Microservice> getDependencies() {
        return new ArrayList<>(dependencies);
    }

    @Override
    public void addDependency(Microservice dependency) {
        Objects.requireNonNull(dependency, "Dependency must not be null");
        if (createsCycle(dependency)) {
            throw new IllegalArgumentException(
                    "Adding dependency '" + dependency.getName() + "' would create a circular dependency");
        }
        dependencies.add(dependency);
    }

    @Override
    public MicroserviceStateChangeEventEmitter getStateChangeEventEmitter() {
        return stateChangeEventEmitter;
    }

    // ===== Subclass hooks =====

    protected abstract void onStart() throws Exception;

    protected void onStop() throws Exception {
    }

    protected void setStateDetail(String detail) {
        this.stateDetail = detail != null ? detail : "";
    }

    protected void markFailed(String detail) {
        MicroserviceState oldState = state;
        if (oldState == MicroserviceState.Failed) {
            return;
        }
        state = MicroserviceState.Failed;
        stateDetail = detail != null ? detail : "";
        fireStateChange(oldState, MicroserviceState.Failed);
    }

    // ===== Internal =====

    private boolean waitForDependency(Microservice dep) {
        long deadline = System.currentTimeMillis() + DEPENDENCY_START_TIMEOUT_SECONDS * 1000;
        while (System.currentTimeMillis() < deadline) {
            MicroserviceState depState = dep.getState();
            if (depState == MicroserviceState.Ready) {
                return true;
            }
            if (depState == MicroserviceState.Failed) {
                return false;
            }
            try {
                Thread.sleep(DEPENDENCY_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean createsCycle(Microservice newDep) {
        Set<Microservice> visited = new HashSet<>();
        collectTransitiveDependencies(newDep, visited);
        return visited.contains(this);
    }

    private void collectTransitiveDependencies(Microservice service, Set<Microservice> visited) {
        if (!visited.add(service)) {
            return;
        }
        for (Microservice dep : service.getDependencies()) {
            collectTransitiveDependencies(dep, visited);
        }
    }

    private void fireStateChange(MicroserviceState oldState, MicroserviceState newState) {
        stateChangeEventEmitter.emit(new MicroserviceStateChangeEvent(this, this, oldState, newState));
    }
}
