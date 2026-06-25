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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages automatic port assignment for server instances.
 *
 * <p>The primary strategy delegates to the operating system by binding a
 * {@link ServerSocket} to port {@code 0}, which makes the kernel pick a free
 * port from its own ephemeral range. This is the most reliable approach because
 * the OS is the single source of truth for which ports are free, and it
 * naturally respects system-specific restrictions on usable port ranges. For
 * example, on Linux the ephemeral range defaults to {@code 32768-60999}, which
 * only partially overlaps the IANA range {@code 49152-65535}; requiring ports
 * to fall inside the IANA range caused legitimate OS-assigned ports to be
 * rejected until all attempts were exhausted.</p>
 *
 * <p>Assigned ports are tracked in a thread-safe set so that they are not
 * handed out again while a managed server instance is still using them. If the
 * OS-assigned port collides with a tracked port, the assignment is retried. As
 * a last resort a bounded scan over a wide range is performed, skipping
 * privileged ports ({@code < 1024}) and already-assigned ports.</p>
 */
public class PortManager {
    /** Lowest port considered during the fallback scan. Ports below 1024 are privileged on Unix. */
    private static final int MIN_SCAN_PORT = 1024;
    /** Highest port considered during the fallback scan. */
    private static final int MAX_SCAN_PORT = 65535;
    /** Number of times to ask the OS for a free port before falling back to a scan. */
    private static final int MAX_OS_ATTEMPTS = 100;
    /** Number of random ports to probe during the fallback scan. */
    private static final int MAX_SCAN_ATTEMPTS = 64;
    private final Set<Integer> assignedPorts = ConcurrentHashMap.newKeySet();

    /**
     * Assigns a free port and tracks it so it is not re-assigned while in use.
     *
     * <p>First asks the operating system for a free port by binding a
     * {@link ServerSocket} to port {@code 0}. This returns a port from the
     * system's ephemeral range and is preferred over a manual scan because the
     * kernel guarantees the port is free and honours any system-specific
     * port-range restrictions.</p>
     *
     * <p>If the OS-assigned port collides with a tracked port (rare), the
     * request is retried. If the OS strategy keeps failing, a bounded fallback
     * scan over {@value #MIN_SCAN_PORT}-{@value #MAX_SCAN_PORT} is performed,
     * skipping privileged ports and already-assigned ports.</p>
     *
     * @return a free port number
     * @throws IllegalStateException if no free port can be found
     */
    public int assignFreePort() {
        // Strategy 1: let the OS pick a free port from its ephemeral range.
        // new ServerSocket(0) returns a port the kernel guarantees to be free
        // at bind time and naturally respects system port-range restrictions.
        for (int attempt = 0; attempt < MAX_OS_ATTEMPTS; attempt++) {
            try (ServerSocket socket = new ServerSocket(0)) {
                int port = socket.getLocalPort();
                if (assignedPorts.add(port)) {
                    return port;
                }
                // The OS reused a port we still track; retry.
            } catch (IOException e) {
                // Unable to bind; retry.
            }
        }

        // Strategy 2: bounded fallback scan. Skip privileged ports (< 1024) and
        // ports that are already tracked. Random sampling avoids getting stuck
        // probing a long contiguous block of busy ports.
        for (int attempt = 0; attempt < MAX_SCAN_ATTEMPTS; attempt++) {
            int port = MIN_SCAN_PORT + ThreadLocalRandom.current().nextInt(MAX_SCAN_PORT - MIN_SCAN_PORT + 1);
            if (assignedPorts.contains(port)) {
                continue;
            }
            try (ServerSocket socket = new ServerSocket(port)) {
                if (assignedPorts.add(port)) {
                    return port;
                }
            } catch (IOException e) {
                // Port in use; try another.
            }
        }

        throw new IllegalStateException("Unable to find a free port after " + MAX_OS_ATTEMPTS
                + " OS-assisted attempts and " + MAX_SCAN_ATTEMPTS + " fallback scan attempts");
    }

    /**
     * Reserves a specific port (e.g. one explicitly requested by the user) so
     * that it is not handed out again by {@link #assignFreePort()} while in use.
     *
     * @param port the port to reserve
     * @return {@code true} if the port was newly reserved, {@code false} if it was already tracked
     */
    public boolean reservePort(int port) {
        return assignedPorts.add(port);
    }

    /**
     * Releases a previously assigned port, allowing it to be reused.
     *
     * @param port the port to release
     */
    public void releasePort(int port) {
        assignedPorts.remove(port);
    }

    /**
     * Checks if a specific port is available (not assigned and not in use).
     *
     * @param port the port to check
     * @return true if the port is available
     */
    public boolean isPortAvailable(int port) {
        if (assignedPorts.contains(port)) {
            return false;
        }
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the set of currently assigned ports.
     *
     * @return unmodifiable set of assigned port numbers
     */
    public Set<Integer> getAssignedPorts() {
        return Set.copyOf(assignedPorts);
    }
}
