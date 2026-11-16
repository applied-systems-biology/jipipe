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

package org.hkijena.jipipe.api.automation.endpoint;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.servers.SharedServerDiscoveryFile;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;
import org.hkijena.jipipe.servers.mcp.MCPServerProcess;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client-side handle for the shared MCP broker: connects to an already-running broker via
 * the discovery file or spawns one ({@link MCPServerProcess}), then registers and
 * unregisters this JIPipe instance.
 * <p>
 * Mirrors the LLM proxy client ({@code LLMProxyClient}): discovery file read (PID alive
 * check plus {@code GET /health}), stale-file deletion, spawn via {@code java.class.path}
 * and an {@code @argfile}, 30 second health poll with stdout drain, and HTTP control calls.
 * The {@link Spawner} is constructor-injectable so tests never spawn a real JVM.
 * <p>
 * The registration payload is built from the endpoint ({@link #setEndpoint}), the project
 * source ({@link #setProjectSource}, default: the open project windows), and the tool source
 * ({@link #setToolSource}, default: the global automation operation registry). The instance
 * id is a UUID generated once per client.
 * <p>
 * Token authentication ({@code require-token} setting): the client ensures the shared
 * token file {@code <JIPipe user dir>/mcp/token} exists (generated once: a secure random
 * 32-byte base64url secret written with owner-only permissions, POSIX 0600 where the file
 * system supports it and a best-effort owner-only File API fallback elsewhere), passes
 * {@code --token-file} to spawned brokers, and presents the token as
 * {@code Authorization: Bearer} on all calls to the broker (health and control routes).
 * The token persists until the file is manually deleted: an existing file is never
 * regenerated, so the token survives broker restarts and is shared by all JIPipe
 * instances of the same user directory.
 */
public class MCPBrokerClient {

    /**
     * Spawns the broker process from the given command. Injectable so tests never spawn a
     * real JVM; the production implementation is {@link #defaultSpawner()}.
     */
    @FunctionalInterface
    public interface Spawner {
        Process spawn(List<String> command) throws Exception;
    }

    /**
     * A broker HTTP response: status code plus body. The status is checked by the caller:
     * a non-2xx response is a refusal even when the body carries no error marker.
     */
    private record HttpResult(int status, String body) {

        private boolean isSuccessful() {
            return status >= 200 && status < 300;
        }
    }

    private static final int SPAWN_TIMEOUT_MS = 30000;

    private final String instanceId = UUID.randomUUID().toString();
    private final Spawner spawner;
    private final int brokerPort;
    /**
     * Replacement for the {@code <JIPipe user dir>/mcp} directory (tests); null uses the
     * real user dir.
     */
    private final Path mcpDirectory;
    private volatile String baseUrl;
    private volatile boolean connected;
    private volatile boolean registered;
    private volatile AutomationInstanceEndpoint endpoint;
    private volatile ProjectSource projectSource = ProjectSource.openWindows();
    private volatile Supplier<List<MCPToolDescriptor>> toolSource = MCPBrokerClient::globalTools;
    private volatile String cachedToken;

    public MCPBrokerClient(Spawner spawner, int brokerPort) {
        this(spawner, brokerPort, null);
    }

    /**
     * Test constructor: redirects the shared MCP directory (discovery and token files)
     * away from the real JIPipe user dir.
     *
     * @param mcpDirectory the replacement directory, or null for the user dir
     */
    MCPBrokerClient(Spawner spawner, int brokerPort, Path mcpDirectory) {
        this.spawner = spawner;
        this.brokerPort = brokerPort;
        this.mcpDirectory = mcpDirectory;
    }

    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Builds the tool descriptors for the operations of the given registry, sorted by id.
     */
    public static List<MCPToolDescriptor> descriptorsOf(AutomationOperationRegistry registry) {
        List<MCPToolDescriptor> tools = new ArrayList<>();
        for (AutomationOperation operation : registry.getAll().values()) {
            tools.add(MCPToolDescriptor.of(operation));
        }
        tools.sort(Comparator.comparing(MCPToolDescriptor::getId));
        return tools;
    }

    /**
     * The production tool source: the global automation operation registry.
     */
    private static List<MCPToolDescriptor> globalTools() {
        return descriptorsOf(JIPipe.getAutomation().getOperationRegistry());
    }

    /**
     * The production spawner: starts the command (built by {@link #buildSpawnCommand}) with
     * merged stderr, like the LLM proxy client.
     */
    public static Spawner defaultSpawner() {
        return command -> {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            return processBuilder.start();
        };
    }

    /**
     * Connects to the broker: an existing healthy broker (discovery file, PID alive, health
     * check) is reused; otherwise the stale discovery file is deleted and a broker process
     * is spawned. Throws a {@link RuntimeException} when spawning or the health poll fails.
     */
    public synchronized void ensureConnected(JIPipeProgressInfo progressInfo) {
        if (connected && isBrokerAlive()) {
            return;
        }
        connected = false;
        registered = false;
        baseUrl = null;

        try {
            ensureTokenFile();
        } catch (IOException e) {
            System.err.println("MCP token file creation failed: " + e.getMessage());
        }

        Path discoveryFilePath = getDiscoveryFilePath();
        SharedServerDiscoveryFile.DiscoveryData data = SharedServerDiscoveryFile.read(discoveryFilePath);
        if (data != null && SharedServerDiscoveryFile.isPidAlive(data.getPid())) {
            String url = "http://127.0.0.1:" + data.getPort();
            if (checkBrokerHealth(url, currentToken())) {
                baseUrl = url;
                connected = true;
                return;
            }
        }

        if (data != null) {
            SharedServerDiscoveryFile.delete(discoveryFilePath);
        }

        spawnBroker(progressInfo);
        connected = true;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isRegistered() {
        return registered;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * True when the currently known broker answers {@code GET /health}.
     */
    public boolean isBrokerAlive() {
        return baseUrl != null && checkBrokerHealth(baseUrl, currentToken());
    }

    /**
     * The discovery file of the shared broker:
     * {@code <JIPipe user dir>/mcp/discovery.json}.
     */
    public Path getDiscoveryFilePath() {
        return mcpDirectoryPath("discovery.json");
    }

    /**
     * The shared broker token file: {@code <JIPipe user dir>/mcp/token}.
     */
    public Path getTokenFilePath() {
        return mcpDirectoryPath("token");
    }

    private Path mcpDirectoryPath(String fileName) {
        Path directory = mcpDirectory != null ? mcpDirectory : PathUtils.getJIPipeUserDir().resolve("mcp");
        return directory.resolve(fileName);
    }

    /**
     * Whether token authentication is required (the {@code require-token} setting).
     * Package private and overridable so tests can enable the token flow without the
     * application settings service. Fail-open (false) when the settings are unavailable:
     * the client then simply presents no token.
     */
    boolean isRequireToken() {
        try {
            MCPApplicationSettings settings = MCPApplicationSettings.getInstance();
            return settings != null && settings.isRequireToken();
        } catch (Exception e) {
            System.err.println("MCP settings unavailable: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ensures the token file exists (when {@link #isRequireToken()}): a missing file is
     * created with a secure random 32-byte base64url token; an existing file is never
     * regenerated (the token must persist across broker restarts).
     * <p>
     * Permissions: on POSIX file systems the file is created with {@code rw-------}
     * (0600). On non-POSIX file systems (e.g. Windows) the documented fallback restricts
     * read and write access to the owner via the File API (best effort; no ACL-precise
     * protection is attempted).
     */
    private void ensureTokenFile() throws IOException {
        if (!isRequireToken() || Files.exists(getTokenFilePath())) {
            return;
        }
        Path tokenFile = getTokenFilePath();
        Files.createDirectories(tokenFile.getParent());
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        if (tokenFile.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            FileAttribute<Set<PosixFilePermission>> permissions =
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
            Files.createFile(tokenFile, permissions);
        } else {
            Files.createFile(tokenFile);
            File file = tokenFile.toFile();
            // Owner-only, best effort: revoke for everyone, then restore for the owner.
            file.setReadable(false, false);
            file.setWritable(false, false);
            file.setExecutable(false, false);
            file.setReadable(true, true);
            file.setWritable(true, true);
        }
        Files.writeString(tokenFile, token, StandardCharsets.UTF_8);
    }

    /**
     * The token to present on client-to-broker calls, or null when token authentication is
     * off or the token file is currently unreadable (logged; a later call retries the
     * read). The token is cached after the first successful read.
     */
    private String currentToken() {
        if (!isRequireToken()) {
            return null;
        }
        String token = cachedToken;
        if (token != null) {
            return token;
        }
        try {
            String read = Files.readString(getTokenFilePath(), StandardCharsets.UTF_8).trim();
            if (!read.isEmpty()) {
                cachedToken = read;
                return read;
            }
            System.err.println("MCP token file is empty; presenting no token");
        } catch (IOException e) {
            System.err.println("MCP token file unavailable: " + e.getMessage());
        }
        return null;
    }

    /**
     * Registers this instance at the broker (or updates an existing registration).
     *
     * @throws RuntimeException when not connected or the broker refuses the registration
     *                          (non-2xx status or an error body)
     */
    public synchronized void register() {
        if (baseUrl == null) {
            throw new IllegalStateException("MCP broker client is not connected");
        }
        try {
            MCPInstanceRegistration registration = buildRegistration();
            String body = JsonUtils.getObjectMapper().writeValueAsString(registration);
            HttpResult result = httpPost(baseUrl + "/control/register", body, currentToken());
            if (!result.isSuccessful()) {
                throw new RuntimeException("MCP broker refused registration (HTTP "
                        + result.status() + "): " + result.body());
            }
            if (result.body().startsWith("{\"error\":")) {
                throw new RuntimeException("MCP broker refused registration: " + result.body());
            }
            registered = true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("MCP broker registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Unregisters this instance (best effort: the broker also reaps dead instance PIDs).
     */
    public synchronized void unregister() {
        if (baseUrl == null) {
            connected = false;
            registered = false;
            return;
        }
        try {
            String body = JsonUtils.getObjectMapper().writeValueAsString(Map.of("instance-id", instanceId));
            httpPost(baseUrl + "/control/unregister", body, currentToken());
        } catch (Exception e) {
            System.err.println("MCP unregister failed: " + e.getMessage());
        }
        connected = false;
        registered = false;
    }

    /**
     * Re-posts the registration with the given projects (used on every project window
     * open/close). Never throws: recurring failures are logged and must not reach the
     * Swing event thread.
     */
    public synchronized void updateProjects(List<MCPProjectEntry> projects) {
        if (baseUrl == null || !registered) {
            return;
        }
        try {
            MCPInstanceRegistration registration = buildRegistration();
            registration.setProjects(projects == null ? new ArrayList<>() : new ArrayList<>(projects));
            String body = JsonUtils.getObjectMapper().writeValueAsString(registration);
            httpPost(baseUrl + "/control/register", body, currentToken());
        } catch (Exception e) {
            System.err.println("MCP project update failed: " + e.getMessage());
        }
    }

    /**
     * Builds the current registration: instance id, JIPipe PID, endpoint coordinates and
     * secret, app version, projects, and tool descriptors.
     */
    public MCPInstanceRegistration buildRegistration() {
        MCPInstanceRegistration registration = new MCPInstanceRegistration();
        registration.setInstanceId(instanceId);
        registration.setJipipePid(ProcessHandle.current().pid());
        if (endpoint != null) {
            registration.setEndpointPort(endpoint.getBoundPort());
            registration.setEndpointSecret(endpoint.getSecret());
        }
        registration.setAppVersion(JIPipe.getJIPipeVersion());
        registration.setProjects(new ArrayList<>(projectSource.getProjects()));
        registration.setTools(new ArrayList<>(toolSource.get()));
        return registration;
    }

    public void setEndpoint(AutomationInstanceEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void setProjectSource(ProjectSource projectSource) {
        this.projectSource = projectSource;
    }

    public void setToolSource(Supplier<List<MCPToolDescriptor>> toolSource) {
        this.toolSource = toolSource;
    }

    /**
     * Spawns the broker process and polls the discovery file plus health endpoint for up to
     * 30 seconds. Sets {@code baseUrl} on success.
     */
    private void spawnBroker(JIPipeProgressInfo progressInfo) {
        Path discoveryFilePath = getDiscoveryFilePath();
        progressInfo.log("Spawning MCP broker process...");
        try {
            Files.createDirectories(discoveryFilePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create MCP discovery directory: " + e.getMessage(), e);
        }
        List<String> command;
        try {
            command = buildSpawnCommand(discoveryFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write MCP broker argument file: " + e.getMessage(), e);
        }
        progressInfo.log("MCP broker command: " + command.get(0) + " " + String.join(" ", command.subList(1, command.size())));

        Process process;
        try {
            process = spawner.spawn(command);
        } catch (Exception e) {
            throw new RuntimeException("Failed to spawn MCP broker: " + e.getMessage(), e);
        }

        Thread drainThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    progressInfo.log("[mcp] " + line);
                }
            } catch (IOException e) {
            }
        }, "MCP-Broker-Output-Drain");
        drainThread.setDaemon(true);
        drainThread.start();

        long deadline = System.currentTimeMillis() + SPAWN_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            SharedServerDiscoveryFile.DiscoveryData data = SharedServerDiscoveryFile.read(discoveryFilePath);
            if (data != null) {
                baseUrl = "http://127.0.0.1:" + data.getPort();
                if (checkBrokerHealth(baseUrl, currentToken())) {
                    progressInfo.log("MCP broker started at " + baseUrl);
                    return;
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        baseUrl = null;
        throw new RuntimeException("MCP broker failed to start within 30 seconds");
    }

    /**
     * Builds the broker spawn command: {@code java @argfile} where the argfile carries the
     * classpath, the {@link MCPServerProcess} main class, the discovery file, the parent
     * PID, the broker port from the settings, and (when token authentication is required
     * and the token file exists, which {@link #ensureConnected} ensures first) the token
     * file.
     */
    private List<String> buildSpawnCommand(Path discoveryFilePath) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaExecutable = javaHome + File.separator + "bin" + File.separator + "java";
        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add("-cp");
        jvmArgs.add(System.getProperty("java.class.path"));
        jvmArgs.add(MCPServerProcess.class.getName());
        jvmArgs.add("--discovery-file");
        jvmArgs.add(discoveryFilePath.toString());
        jvmArgs.add("--parent-pid");
        jvmArgs.add(String.valueOf(ProcessHandle.current().pid()));
        jvmArgs.add("--port");
        jvmArgs.add(String.valueOf(brokerPort));
        if (isRequireToken()) {
            // Only pass the token file when it exists: a failed creation (logged in
            // ensureConnected) degrades to a tokenless broker instead of a crashed spawn.
            Path tokenFile = getTokenFilePath();
            if (Files.exists(tokenFile)) {
                jvmArgs.add("--token-file");
                jvmArgs.add(tokenFile.toString());
            }
        }

        Path argFile = Files.createTempFile("mcp-broker-args", ".txt");
        argFile.toFile().deleteOnExit();
        Files.write(argFile, jvmArgs, StandardCharsets.UTF_8);

        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("@" + argFile);
        return command;
    }

    private static boolean checkBrokerHealth(String url, String bearerToken) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url + "/health").openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            if (bearerToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
            }
            int status = conn.getResponseCode();
            conn.disconnect();
            return status == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private static HttpResult httpPost(String urlStr, String body, String bearerToken) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        if (bearerToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int status = conn.getResponseCode();
        return new HttpResult(status, readResponse(conn, status));
    }

    private static String readResponse(HttpURLConnection conn, int status) throws IOException {
        // The error stream is null for some statuses (e.g. a 401 without a body).
        InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            conn.disconnect();
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
