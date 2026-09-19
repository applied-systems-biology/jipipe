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
import org.hkijena.jipipe.api.microservice.AbstractMicroservice;

import java.time.Instant;

/**
 * Abstract base class for a managed server instance.
 *
 * <p>A server instance represents a live connection to a running external process.
 * It is typed on the environment class that configured it, enabling strongly-typed
 * domain-specific APIs in subclasses.</p>
 *
 * <p>Lifecycle is managed by {@link AbstractMicroservice}: {@code Stopped → Starting → Ready → Stopping → Stopped}.
 * Subclasses implement {@link #startProcess()} and {@link #stopProcess()} for actual
 * process management. The {@link #onStart()} hook calls {@code startProcess()} and
 * performs a health check before transitioning to {@code Ready}.</p>
 *
 * @param <TEnv> the environment type that configures this instance
 */
public abstract class JIPipeServerInstance<TEnv extends JIPipeEnvironment> extends AbstractMicroservice {
    private final TEnv environment;
    private final int port;
    private Instant startedAt;
    private Process process;

    /**
     * @param environment the environment that configures this instance
     * @param port        the port the server is (or will be) listening on
     */
    protected JIPipeServerInstance(TEnv environment, int port) {
        super(environment.getName() + " (port " + port + ")");
        this.environment = environment;
        this.port = port;
    }

    @Override
    protected void onStart() throws Exception {
        setStateDetail("Starting on port " + port + "...");
        startProcess();
        setStateDetail("Waiting for health check...");
        if (!waitForHealth()) {
            throw new ServerStartException("Health check failed for " + getServerTypeId() + " on port " + port,
                    ServerStartException.Reason.HealthCheckFailed);
        }
        startedAt = Instant.now();
        setStateDetail("Running on port " + port);
    }

    @Override
    protected void onStop() throws Exception {
        setStateDetail("Stopping...");
        stopProcess();
        startedAt = null;
    }

    private boolean waitForHealth() {
        long deadline = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadline) {
            if (Thread.currentThread().isInterrupted()) return false;
            if (isHealthy()) return true;
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Spawns the server process.
     *
     * @throws ServerStartException if the server fails to start
     */
    protected abstract void startProcess() throws ServerStartException;

    /**
     * Stops the server process.
     * Implementations should attempt graceful shutdown first,
     * then force-kill if the process does not terminate within a timeout.
     */
    protected abstract void stopProcess();

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

    /**
     * Updates the state detail string for this instance.
     * Public wrapper around {@link AbstractMicroservice#setStateDetail(String)}
     * so the service component (in a different package) can convey
     * Busy/Idle nuance via the detail string.
     *
     * @param detail the new detail string
     */
    public void updateStateDetail(String detail) {
        setStateDetail(detail);
    }
}
