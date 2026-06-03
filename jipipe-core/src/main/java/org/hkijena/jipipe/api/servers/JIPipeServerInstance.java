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

import java.time.Instant;

/**
 * Abstract base class for a managed server instance.
 * 
 * <p>A server instance represents a live connection to a running external process.
 * It is typed on the environment class that configured it, enabling strongly-typed
 * domain-specific APIs in subclasses.</p>
 * 
 * <p>Lifecycle: {@link JIPipeServerState#NotRunning} → {@link JIPipeServerState#Starting}
 * → {@link JIPipeServerState#Running} → {@link JIPipeServerState#Stopping}
 * → {@link JIPipeServerState#NotRunning}.</p>
 * 
 * @param <TEnv> the environment type that configures this instance
 */
public abstract class JIPipeServerInstance<TEnv extends JIPipeEnvironment> {
    private final TEnv environment;
    private final int port;
    private volatile JIPipeServerState state = JIPipeServerState.NotRunning;
    private Instant startedAt;
    private Process process;

    /**
     * @param environment the environment that configures this instance
     * @param port        the port the server is (or will be) listening on
     */
    protected JIPipeServerInstance(TEnv environment, int port) {
        this.environment = environment;
        this.port = port;
    }

    /**
     * Starts the server process.
     * Implementations should:
     * 1. Set state to Starting
     * 2. Spawn the process
     * 3. Wait for health check to pass
     * 4. Set state to Running
     *
     * @throws ServerStartException if the server fails to start
     */
    public abstract void start() throws ServerStartException;

    /**
     * Stops the server process.
     * Implementations should attempt graceful shutdown first,
     * then force-kill if the process does not terminate within a timeout.
     */
    public abstract void stop();

    /**
     * Checks if the server is healthy and responsive.
     *
     * @return true if the server is healthy
     */
    public abstract boolean isHealthy();

    /**
     * Returns the server type identifier (e.g., "ipython-server", "llamacpp-server").
     *
     * @return the server type ID
     */
    public abstract String getServerTypeId();

    /**
     * Returns the environment that configured this instance.
     *
     * @return the environment
     */
    public TEnv getEnvironment() {
        return environment;
    }

    /**
     * Returns the port the server is listening on.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the current lifecycle state.
     *
     * @return the state
     */
    public JIPipeServerState getState() {
        return state;
    }

    /**
     * Sets the lifecycle state. Public: the service component needs to
     * manage state transitions from a different package.
     *
     * @param state the new state
     */
    public void setState(JIPipeServerState state) {
        JIPipeServerState oldState = this.state;
        this.state = state;
        if (state == JIPipeServerState.Running || state == JIPipeServerState.Idle || state == JIPipeServerState.Busy) {
            if (startedAt == null) {
                startedAt = Instant.now();
            }
        }
        if (state == JIPipeServerState.NotRunning) {
            startedAt = null;
        }
    }

    /**
     * Returns the time when the server entered a running state, or null if not running.
     *
     * @return the start time, or null
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Returns a display name for this instance.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return environment.getName() + " (" + getServerTypeId() + ")";
    }

    /**
     * Returns the managed process, or null if not started.
     *
     * @return the process, or null
     */
    public Process getProcess() {
        return process;
    }

    /**
     * Sets the managed process.
     *
     * @param process the process
     */
    protected void setProcess(Process process) {
        this.process = process;
    }
}
