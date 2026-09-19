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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.servers.ServerStartException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * A generic server instance for any HTTP-speaking process.
 * 
 * <p>Provides basic HTTP GET/POST methods and health checking.
 * Suitable for wrapping any external HTTP server (e.g., REST APIs,
 * web services) as a managed JIPipe server instance.</p>
 */
public class GenericProcessServerInstance extends JIPipeServerInstance<JIPipeEnvironment> {

    /**
     * Factory ID for generic HTTP process servers.
     */
    public static final String FACTORY_ID = "generic-http-server";

    private final String healthCheckPath;
    private final int expectedHealthStatus;
    private final int healthCheckTimeoutMs;
    private final ProcessSupervisor processSupervisor = new ProcessSupervisor();

    /**
     * Creates a new generic process server instance.
     *
     * @param environment the environment configuration
     * @param port        the port the server will listen on
     */
    public GenericProcessServerInstance(JIPipeEnvironment environment, int port) {
        this(environment, port, "/health", 200, 5000);
    }

    /**
     * Creates a new generic process server instance with custom health check settings.
     *
     * @param environment          the environment configuration
     * @param port                 the port the server will listen on
     * @param healthCheckPath      the URL path for health checks
     * @param expectedHealthStatus the expected HTTP status code for healthy responses
     * @param healthCheckTimeoutMs the timeout for health check requests in milliseconds
     */
    public GenericProcessServerInstance(JIPipeEnvironment environment, int port,
                                         String healthCheckPath, int expectedHealthStatus,
                                         int healthCheckTimeoutMs) {
        super(environment, port);
        this.healthCheckPath = healthCheckPath;
        this.expectedHealthStatus = expectedHealthStatus;
        this.healthCheckTimeoutMs = healthCheckTimeoutMs;
    }

    @Override
    protected void startProcess() throws ServerStartException {
        // The actual process spawning is handled by subclasses or by the environment's
        // runExecutable method. This base implementation does nothing — subclasses
        // are expected to override and spawn the process before returning.
    }

    @Override
    protected void stopProcess() {
        Process process = getProcess();
        if (process != null && process.isAlive()) {
            processSupervisor.stopProcess(process, new JIPipeProgressInfo());
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            URL url = new URL("http://127.0.0.1:" + getPort() + healthCheckPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(healthCheckTimeoutMs);
            connection.setReadTimeout(healthCheckTimeoutMs);
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == expectedHealthStatus;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String getServerTypeId() {
        return FACTORY_ID;
    }

    /**
     * Returns the base URL for this server.
     *
     * @return the base URL (e.g., "http://127.0.0.1:8080")
     */
    public String getBaseUrl() {
        return "http://127.0.0.1:" + getPort();
    }

    /**
     * Performs an HTTP GET request to the given path.
     *
     * @param path the URL path (e.g., "/api/data")
     * @return the response body as a string
     * @throws IOException if the request fails
     */
    public String httpGet(String path) throws IOException {
        URL url = new URL(getBaseUrl() + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        return readResponse(connection);
    }

    /**
     * Performs an HTTP POST request to the given path.
     *
     * @param path the URL path (e.g., "/api/data")
     * @param body the request body
     * @return the response body as a string
     * @throws IOException if the request fails
     */
    public String httpPost(String path, String body) throws IOException {
        URL url = new URL(getBaseUrl() + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        return readResponse(connection);
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        status >= 200 && status < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Spawns the server process with the given command and environment variables.
     * This is a convenience method for subclasses that need to spawn a process.
     *
     * @param command     the command and arguments
     * @param environment additional environment variables
     * @param progressInfo the progress info for logging
     * @throws IOException if the process cannot be started
     */
    protected void spawnProcess(List<String> command, Map<String, String> environment,
                                 JIPipeProgressInfo progressInfo) throws IOException {
        Process process = processSupervisor.spawnProcess(command, environment,
                getDisplayName(), progressInfo);
        setProcess(process);
    }
}
