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

import org.hkijena.jipipe.api.servers.JIPipeServerHealthCheck;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Health check strategy that uses HTTP GET requests.
 * 
 * <p>Considers a server healthy if the HTTP response status code
 * matches the expected status (default: 200).</p>
 */
public class HttpHealthCheck implements JIPipeServerHealthCheck {
    private final String path;
    private final int expectedStatus;
    private final int timeoutMs;

    /**
     * Creates an HTTP health check with default settings.
     * Path: "/health", expected status: 200, timeout: 5000ms
     */
    public HttpHealthCheck() {
        this("/health", 200, 5000);
    }

    /**
     * Creates an HTTP health check with custom settings.
     *
     * @param path          the URL path to check (e.g., "/health")
     * @param expectedStatus the expected HTTP status code
     * @param timeoutMs     the connection timeout in milliseconds
     */
    public HttpHealthCheck(String path, int expectedStatus, int timeoutMs) {
        this.path = path;
        this.expectedStatus = expectedStatus;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public boolean isHealthy(String host, int port) {
        try {
            URL url = new URL("http://" + host + ":" + port + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == expectedStatus;
        } catch (IOException e) {
            return false;
        }
    }
}
