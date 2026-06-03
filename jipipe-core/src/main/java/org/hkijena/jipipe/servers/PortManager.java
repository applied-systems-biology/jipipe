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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages automatic port assignment for server instances.
 * 
 * <p>Uses the IANA ephemeral port range (49152-65535) for auto-assignment.
 * The bind-then-close strategy minimizes race conditions between port
 * assignment and actual server binding.</p>
 */
public class PortManager {
    private static final int MIN_PORT = 49152;
    private static final int MAX_PORT = 65535;
    private final Set<Integer> assignedPorts = ConcurrentHashMap.newKeySet();

    /**
     * Assigns a free port by binding to port 0 (OS-assigned), then closing the socket.
     * This minimizes the race condition window between finding a free port and using it.
     * The assigned port is tracked to avoid re-assignment.
     *
     * @return a free port number
     * @throws IllegalStateException if no free port can be found
     */
    public int assignFreePort() {
        // Try the bind-then-close strategy up to 10 times
        for (int attempt = 0; attempt < 10; attempt++) {
            try (ServerSocket socket = new ServerSocket(0)) {
                int port = socket.getLocalPort();
                if (port >= MIN_PORT && port <= MAX_PORT && assignedPorts.add(port)) {
                    return port;
                }
                // Port outside range or already assigned, try again
            } catch (IOException e) {
                // Unable to bind, try again
            }
        }
        throw new IllegalStateException("Unable to find a free port in the range " + MIN_PORT + "-" + MAX_PORT);
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
