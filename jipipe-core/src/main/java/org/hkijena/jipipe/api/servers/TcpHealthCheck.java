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

import org.hkijena.jipipe.api.servers.JIPipeServerHealthCheck;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Health check strategy that uses TCP socket connection.
 * 
 * <p>Considers a server healthy if a TCP connection can be established
 * to the given host and port within the timeout period.</p>
 */
public class TcpHealthCheck implements JIPipeServerHealthCheck {
    private final int timeoutMs;

    /**
     * Creates a TCP health check with default timeout of 5000ms.
     */
    public TcpHealthCheck() {
        this(5000);
    }

    /**
     * Creates a TCP health check with custom timeout.
     *
     * @param timeoutMs the connection timeout in milliseconds
     */
    public TcpHealthCheck(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public boolean isHealthy(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
