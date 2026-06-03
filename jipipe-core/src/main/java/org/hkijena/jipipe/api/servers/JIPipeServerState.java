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

/**
 * Lifecycle states for a server instance.
 * 
 * State transitions:
 * - NotRunning → Starting (start called)
 * - Starting → Running (health check passed)
 * - starting → Failed (startup timeout / health check failed)
 * - Failed → Starting (retry)
 * - Running → Idle (all leases released)
 * - Running → Busy (lease acquired)
 * - Idle → Busy (new lease acquired)
 * - Idle → Stopping (auto-shutdown triggered)
 * - Busy → Stopping (explicit stop called)
 * - Running → Stopping (explicit stop called)
 * - Stopping → NotRunning (process terminated)
 * - Stopping → Failed (process refused to terminate)
 */
public enum JIPipeServerState {
    NotRunning("No process, no resources held"),
    Starting("Process spawned, waiting for health check"),
    Running("Process alive, health check passing"),
    Idle("Process alive, no leases held"),
    Busy("Process alive, leases held"),
    Stopping("Shutdown initiated, waiting for process to terminate"),
    Failed("Unrecoverable error");

    private final String description;

    JIPipeServerState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
