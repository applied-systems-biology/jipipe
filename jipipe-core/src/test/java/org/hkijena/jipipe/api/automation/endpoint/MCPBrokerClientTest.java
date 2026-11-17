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

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.api.servers.SharedServerDiscoveryFile;
import org.hkijena.jipipe.servers.mcp.MCPServerProcess;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link MCPBrokerClient} against a stub broker HTTP server. The spawner is always
 * stubbed: no test spawns a real broker JVM.
 */
public class MCPBrokerClientTest {

    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    private HttpServer broker;
    private final List<String> registrationBodies = new CopyOnWriteArrayList<>();
    private final List<String> unregisterBodies = new CopyOnWriteArrayList<>();
    private final List<String> authorizationHeaders = new CopyOnWriteArrayList<>();
    private final List<String> healthAuthorizationHeaders = new CopyOnWriteArrayList<>();
    private final AtomicInteger registerResponseStatus = new AtomicInteger(200);
    private final AtomicReference<String> registerResponseBody = new AtomicReference<>("ok");
    private final List<List<String>> spawnedCommands = new ArrayList<>();
    private MCPBrokerClient client;
    private AutomationInstanceEndpoint endpoint;

    private final MCPBrokerClient.Spawner recordingSpawner = command -> {
        spawnedCommands.add(List.copyOf(command));
        throw new AssertionError("Spawner must not be invoked in this test");
    };

    private final MCPBrokerClient.Spawner spawningStubSpawner = command -> {
        spawnedCommands.add(List.copyOf(command));
        // Simulates the spawned broker process: rewrites the discovery file (the old one
        // was deleted by the client) and serves /health on the stub broker port.
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        return new StubProcess();
    };

    @AfterEach
    public void tearDown() {
        if (client != null) {
            try {
                client.unregister();
            } catch (Exception ignored) {
            }
            client = null;
        }
        if (endpoint != null) {
            endpoint.stop();
            endpoint = null;
        }
        if (broker != null) {
            broker.stop(0);
            broker = null;
        }
    }

    // ------------------------------------------------------------------ tests

    @Test
    public void ensureConnectedUsesHealthyBrokerWithoutSpawning() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());

        client.ensureConnected(new JIPipeProgressInfo());

        assertTrue(client.isConnected());
        assertEquals("http://127.0.0.1:" + brokerPort(), client.getBaseUrl());
        assertTrue(client.isBrokerAlive());
        assertTrue(spawnedCommands.isEmpty());
    }

    @Test
    public void registerSendsFullRegistrationJsonIncludingToolDescriptors() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        startStubEndpoint();
        client.setEndpoint(endpoint);
        client.setProjectSource(() -> List.of(projectEntry("p1", "Project One", "/tmp/p1")));
        client.setToolSource(() -> MCPBrokerClient.descriptorsOf(stubOperationRegistry(),
                new AutomationPermissionService()));

        client.ensureConnected(new JIPipeProgressInfo());
        client.register();

        assertTrue(client.isRegistered());
        assertEquals(1, registrationBodies.size());
        JsonNode json = JsonUtils.getObjectMapper().readTree(registrationBodies.get(0));
        assertFalse(json.get("instance-id").asText().isBlank());
        assertEquals(client.getInstanceId(), json.get("instance-id").asText());
        assertEquals(ProcessHandle.current().pid(), json.get("jipipe-pid").asLong());
        assertEquals(endpoint.getBoundPort(), json.get("endpoint-port").asInt());
        assertEquals(endpoint.getSecret(), json.get("endpoint-secret").asText());
        assertFalse(json.get("app-version").asText().isBlank());
        assertEquals("p1", json.get("projects").get(0).get("key").asText());
        assertEquals("Project One", json.get("projects").get(0).get("name").asText());

        JsonNode tools = json.get("tools");
        assertNotNull(tools);
        assertEquals(2, tools.size());
        JsonNode echo = toolById(tools, "echo_stub");
        assertNotNull(echo);
        assertEquals("Echoes the message back", echo.get("description").asText());
        assertEquals("string", echo.get("inputSchema").get("properties").get("message").get("type").asText());
        assertTrue(echo.get("read-only").asBoolean());
        JsonNode render = toolById(tools, "render_stub");
        assertNotNull(render);
        assertFalse(render.get("read-only").asBoolean());
    }

    @Test
    public void descriptorsExcludeOperationsResolvedToHide() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        startStubEndpoint();
        client.setEndpoint(endpoint);
        client.setProjectSource(() -> List.of(projectEntry("p1", "Project One", "/tmp/p1")));

        // Spec section 6: a user-configured Hide must keep the operation out of the
        // registration payload entirely, so it never appears in tools/list. Resolved with a
        // null project key: grants do not apply, CLI override and user state keep precedence.
        AutomationPermissionService permissions = new AutomationPermissionService();
        permissions.setUserState("hidden_stub", AutomationPermissionState.Hide);
        AutomationOperationRegistry registry = stubOperationRegistry();
        registry.register("hidden_stub", new HiddenStubOperation());
        client.setToolSource(() -> MCPBrokerClient.descriptorsOf(registry, permissions));

        client.ensureConnected(new JIPipeProgressInfo());
        client.register();

        assertTrue(client.isRegistered());
        assertEquals(1, registrationBodies.size());
        JsonNode json = JsonUtils.getObjectMapper().readTree(registrationBodies.get(0));
        JsonNode tools = json.get("tools");
        assertNotNull(tools);
        assertNull(toolById(tools, "hidden_stub"), "a Hide operation must not be registered as a tool");
        assertNotNull(toolById(tools, "echo_stub"), "the visible operations must remain registered");
        assertNotNull(toolById(tools, "render_stub"), "the visible operations must remain registered");
        assertEquals(2, tools.size());
    }

    @Test
    public void spawnCommandQuotesArgFileTokensContainingSpaces() throws Exception {
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        // A discovery path containing spaces (and a nested directory) must survive the JDK
        // argfile tokenizer: the rendered token is double-quoted.
        Path discoveryFile = tempDir.resolve("discovery dir with space").resolve("discovery.json");
        List<String> command = client.buildSpawnCommand(discoveryFile);
        List<String> jvmArgs = readJvmArgs(command);

        String discoveryLine = jvmArgs.stream()
                .filter(line -> line.endsWith("discovery.json\""))
                .findFirst().orElse(null);
        assertNotNull(discoveryLine, "the argfile must carry the discovery file: " + jvmArgs);
        assertEquals("\"" + discoveryFile + "\"", discoveryLine,
                "a path containing spaces must be written quoted");
        // The classpath (typically space-free in the test environment) stays verbatim.
        int classpathIndex = jvmArgs.indexOf("-cp");
        assertEquals(System.getProperty("java.class.path"), jvmArgs.get(classpathIndex + 1),
                "a space-free entry must be unchanged");
    }

    @Test
    public void argFileTokenQuotingRoundTripsThroughTheJdkLauncher() throws Exception {
        // True round-trip: the JDK launcher must read back exactly the tokens we rendered,
        // including whitespace, a literal double quote, and a Windows-style backslash path.
        String value = "C:\\Program Files\\dir with space\\a \"quoted\" name";
        Path argFile = tempDir.resolve("roundtrip-args.txt");
        Files.writeString(argFile, "-Dmcp.argfile.test=" + MCPBrokerClient.argFileToken(value) + "\n",
                StandardCharsets.UTF_8);
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        Process process = new ProcessBuilder(java, "@" + argFile, "-XshowSettings:properties", "-version")
                .redirectErrorStream(true).start();
        String output;
        try (InputStream stream = process.getInputStream()) {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(0, process.waitFor(), "the launcher must accept the argfile: " + output);
        assertTrue(output.contains("mcp.argfile.test = " + value),
                "the quoted token must round-trip through the JDK argfile parser: " + output);
    }

    @Test
    public void argFileTokenLeavesSpaceFreeEntriesUnchanged() {
        assertEquals("-cp", MCPBrokerClient.argFileToken("-cp"));
        assertEquals("/opt/jipipe/lib.jar", MCPBrokerClient.argFileToken("/opt/jipipe/lib.jar"));
        assertEquals("C:\\opt\\jipipe", MCPBrokerClient.argFileToken("C:\\opt\\jipipe"),
                "backslashes pass through verbatim when quoting is not needed");
    }

    @Test
    public void deadBrokerPidTriggersSpawnAndConnects() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(spawningStubSpawner, 8781, tempDir);
        writeDiscoveryFile(deadPid(), brokerPort());

        client.ensureConnected(new JIPipeProgressInfo());

        assertTrue(client.isConnected());
        assertEquals(1, spawnedCommands.size());
        List<String> command = spawnedCommands.get(0);
        assertTrue(command.get(0).endsWith("java"));
        assertTrue(command.get(1).startsWith("@"));
        // The JVM arguments live in the @argfile.
        List<String> jvmArgs = Files.readAllLines(Path.of(command.get(1).substring(1)), StandardCharsets.UTF_8);
        assertTrue(jvmArgs.contains(MCPServerProcess.class.getName()));
        assertTrue(jvmArgs.contains("--discovery-file"));
        assertTrue(jvmArgs.contains(client.getDiscoveryFilePath().toString()));
        assertTrue(jvmArgs.contains("--parent-pid"));
        assertTrue(jvmArgs.contains(String.valueOf(ProcessHandle.current().pid())));
        assertTrue(jvmArgs.contains("--port"));
        assertTrue(jvmArgs.contains("8781"));
        // The stale file was deleted and rewritten by the "spawned" process.
        assertEquals(brokerPort(), SharedServerDiscoveryFile.read(client.getDiscoveryFilePath()).getPort());
        assertEquals("http://127.0.0.1:" + brokerPort(), client.getBaseUrl());
    }

    @Test
    public void deadBrokerPidAndSpawnFailureThrowsRuntimeException() throws Exception {
        client = new MCPBrokerClient(command -> {
            throw new IOException("spawn refused");
        }, 8781, tempDir);
        writeDiscoveryFile(deadPid(), 1);

        assertThrows(RuntimeException.class, () -> client.ensureConnected(new JIPipeProgressInfo()));
        assertFalse(client.isConnected());
    }

    @Test
    public void unregisterPostsInstanceId() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        client.setToolSource(List::of);
        client.ensureConnected(new JIPipeProgressInfo());
        client.register();
        client.unregister();

        assertEquals(1, unregisterBodies.size());
        JsonNode json = JsonUtils.getObjectMapper().readTree(unregisterBodies.get(0));
        assertEquals(client.getInstanceId(), json.get("instance-id").asText());
        assertFalse(client.isConnected());
    }

    @Test
    public void updateProjectsRepostsRegistrationWithNewProjects() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        startStubEndpoint();
        client.setEndpoint(endpoint);
        client.setProjectSource(() -> List.of(projectEntry("p1", "Project One", "/tmp/p1")));
        client.setToolSource(List::of);
        client.ensureConnected(new JIPipeProgressInfo());
        client.register();

        client.updateProjects(List.of(projectEntry("p2", "Project Two", "/tmp/p2")));

        assertEquals(2, registrationBodies.size());
        JsonNode json = JsonUtils.getObjectMapper().readTree(registrationBodies.get(1));
        assertEquals("p2", json.get("projects").get(0).get("key").asText());
        assertEquals(client.getInstanceId(), json.get("instance-id").asText());
    }

    @Test
    public void registerRefusalOnNon2xxStatusReportsStatusAndBody() throws Exception {
        startStubBroker();
        registerResponseStatus.set(500);
        registerResponseBody.set("broker unavailable");
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        client.setToolSource(List::of);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());

        client.ensureConnected(new JIPipeProgressInfo());

        // The body carries no error marker, so only the HTTP status exposes the refusal.
        RuntimeException exception = assertThrows(RuntimeException.class, client::register);
        assertTrue(exception.getMessage().contains("HTTP 500"), exception.getMessage());
        assertTrue(exception.getMessage().contains("broker unavailable"), exception.getMessage());
        assertFalse(client.isRegistered());
    }

    @Test
    public void registerRefusalViaErrorBodyOnSuccessStatus() throws Exception {
        startStubBroker();
        registerResponseStatus.set(200);
        registerResponseBody.set("{\"error\": \"unknown operation\"}");
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        client.setToolSource(List::of);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());

        client.ensureConnected(new JIPipeProgressInfo());

        RuntimeException exception = assertThrows(RuntimeException.class, client::register);
        assertTrue(exception.getMessage().contains("refused registration"), exception.getMessage());
        assertTrue(exception.getMessage().contains("unknown operation"), exception.getMessage());
        assertFalse(client.isRegistered());
    }

    @Test
    public void requireTokenGeneratesPersistentOwnerOnlyTokenFileAndPassesItToBroker() throws Exception {
        startStubBroker();
        client = tokenClient(spawningStubSpawner);

        client.ensureConnected(new JIPipeProgressInfo());

        // The spawn command carries --token-file <token path>.
        assertEquals(1, spawnedCommands.size());
        List<String> jvmArgs = readJvmArgs(spawnedCommands.get(0));
        int tokenFlagIndex = jvmArgs.indexOf("--token-file");
        assertTrue(tokenFlagIndex >= 0, "spawn command must carry --token-file: " + jvmArgs);
        assertEquals(client.getTokenFilePath().toString(), jvmArgs.get(tokenFlagIndex + 1));

        // The token file holds 32 random bytes (base64url) with owner-only permissions.
        Path tokenFile = client.getTokenFilePath();
        String token = Files.readString(tokenFile, StandardCharsets.UTF_8).trim();
        assertEquals(32, Base64.getUrlDecoder().decode(token).length);
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(tokenFile);
        assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), permissions);

        // A second client in the same directory reuses the file: never regenerated.
        SharedServerDiscoveryFile.delete(client.getDiscoveryFilePath());
        spawnedCommands.clear();
        client = tokenClient(spawningStubSpawner);
        client.ensureConnected(new JIPipeProgressInfo());
        assertEquals(1, spawnedCommands.size());
        assertTrue(readJvmArgs(spawnedCommands.get(0)).contains("--token-file"));
        assertEquals(token, Files.readString(tokenFile, StandardCharsets.UTF_8).trim());
    }

    @Test
    public void requireTokenSendsBearerHeaderOnHealthAndControlCalls() throws Exception {
        startStubBroker();
        client = tokenClient(spawningStubSpawner);
        client.setToolSource(List::of);

        client.ensureConnected(new JIPipeProgressInfo());
        String expected = "Bearer " + Files.readString(client.getTokenFilePath(), StandardCharsets.UTF_8).trim();
        assertFalse(healthAuthorizationHeaders.isEmpty());
        assertEquals(expected, healthAuthorizationHeaders.get(healthAuthorizationHeaders.size() - 1));

        client.register();
        assertEquals(1, authorizationHeaders.size());
        assertEquals(expected, authorizationHeaders.get(0));
        assertTrue(client.isRegistered());

        client.unregister();
        assertEquals(2, authorizationHeaders.size());
        assertEquals(expected, authorizationHeaders.get(1));
    }

    @Test
    public void rotateTokenReplacesTokenFileAndClearsCachedToken() throws Exception {
        startStubBroker();
        client = tokenClient(recordingSpawner);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        client.ensureConnected(new JIPipeProgressInfo());

        Path tokenFile = client.getTokenFilePath();
        String oldToken = Files.readString(tokenFile, StandardCharsets.UTF_8).trim();
        assertEquals(oldToken, client.getToken()); // prime the cache

        String rotated = client.rotateToken();

        // The file holds a new 32-byte base64url token without padding.
        assertNotEquals(oldToken, rotated);
        assertEquals(rotated, Files.readString(tokenFile, StandardCharsets.UTF_8).trim());
        assertFalse(rotated.contains("="));
        assertEquals(32, Base64.getUrlDecoder().decode(rotated).length);

        // Owner-only permissions are kept.
        assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                Files.getPosixFilePermissions(tokenFile));

        // The cached token was cleared (not repopulated with the rotated value): the file
        // is authoritative again, so an external edit is picked up.
        Files.writeString(tokenFile, "external-value", StandardCharsets.UTF_8);
        assertEquals("external-value", client.getToken());

        // The atomic replacement left no temp files behind.
        try (Stream<Path> entries = Files.list(tempDir)) {
            assertTrue(entries.noneMatch(entry -> entry.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    public void rotateTokenFailsClosedWhenTokenAuthenticationIsDisabled() {
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        assertThrows(IllegalStateException.class, client::rotateToken);
    }

    @Test
    public void brokerRejectionInvalidatesCachedTokenSoTheNextConnectionPicksUpTheRotatedFile() throws Exception {
        // After a rotate the running broker still holds the old token until it is replaced:
        // /health accepts only the NEW token here. A client that cached the old token must
        // recover on its next connection by re-reading the rotated token file (what other
        // running JIPipe instances do after a rotate).
        AtomicReference<String> acceptedToken = new AtomicReference<>("old-token");
        broker = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        broker.createContext("/health", exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            healthAuthorizationHeaders.add(authorization);
            if (("Bearer " + acceptedToken.get()).equals(authorization)) {
                respond(exchange, 200, "text/plain; charset=utf-8", "ok");
            } else {
                respond(exchange, 401, "text/plain; charset=utf-8", "unauthorized");
            }
        });
        broker.start();

        client = tokenClient(command -> {
            spawnedCommands.add(List.copyOf(command));
            // The spawned broker runs with the rotated token: rewrite the token file the
            // way a rotate left it, then serve the discovery file.
            Files.writeString(client.getTokenFilePath(), acceptedToken.get(), StandardCharsets.UTF_8);
            writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
            return new StubProcess();
        });

        Path tokenFile = client.getTokenFilePath();
        Files.writeString(tokenFile, "old-token", StandardCharsets.UTF_8);
        assertEquals("old-token", client.getToken()); // prime the cache with the stale token
        acceptedToken.set("rotated-token");

        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        client.ensureConnected(new JIPipeProgressInfo());

        assertTrue(client.isConnected());
        // The first health check presented the stale cached token (a 401, which clears the
        // cache); the spawn poll re-read the rotated file and succeeded.
        assertEquals("Bearer old-token", healthAuthorizationHeaders.get(0));
        assertTrue(healthAuthorizationHeaders.contains("Bearer rotated-token"));
        assertEquals("rotated-token", client.getToken());
    }

    @Test
    public void invalidateConnectionReconnectsThroughTheFullDiscoveryPath() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(recordingSpawner, 8781, tempDir);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        client.ensureConnected(new JIPipeProgressInfo());
        assertEquals("http://127.0.0.1:" + brokerPort(), client.getBaseUrl());
        assertTrue(client.isConnected());

        // A replacement broker takes over the discovery file at a different port while
        // the old broker is still reachable: only an invalidated connection state makes
        // the next connect re-read the discovery file instead of reusing the cached
        // connection.
        HttpServer replacement = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger replacementHealthHits = new AtomicInteger();
        replacement.createContext("/health", exchange -> {
            replacementHealthHits.incrementAndGet();
            respond(exchange, 200, "text/plain; charset=utf-8", "ok");
        });
        replacement.start();
        try {
            int replacementPort = replacement.getAddress().getPort();
            writeDiscoveryFile(ProcessHandle.current().pid(), replacementPort);

            client.invalidateConnection();
            assertFalse(client.isConnected());
            assertNull(client.getBaseUrl());

            client.ensureConnected(new JIPipeProgressInfo());

            assertTrue(client.isConnected());
            assertEquals("http://127.0.0.1:" + replacementPort, client.getBaseUrl());
            assertEquals(1, replacementHealthHits.get(), "the replacement broker's health endpoint must be re-checked");
            assertTrue(spawnedCommands.isEmpty());
        } finally {
            replacement.stop(0);
        }
    }

    @Test
    public void invalidateConnectionAfterRotationReplacesOldTokenBrokerOn401() throws Exception {
        // The old broker holds the OLD token (it read the file at startup): /health
        // accepts only that token. After a rotate plus invalidateConnection, the next
        // connect must replace it: the 401 deletes the stale discovery file and a fresh
        // broker spawns with the rotated token.
        AtomicReference<String> acceptedToken = new AtomicReference<>("old-token");
        broker = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        broker.createContext("/health", exchange -> {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            healthAuthorizationHeaders.add(authorization);
            if (("Bearer " + acceptedToken.get()).equals(authorization)) {
                respond(exchange, 200, "text/plain; charset=utf-8", "ok");
            } else {
                respond(exchange, 401, "text/plain; charset=utf-8", "unauthorized");
            }
        });
        broker.start();

        client = tokenClient(command -> {
            spawnedCommands.add(List.copyOf(command));
            // The spawned broker runs with the rotated token file and takes over the
            // discovery file (the client deleted the stale one).
            acceptedToken.set(Files.readString(client.getTokenFilePath(), StandardCharsets.UTF_8).trim());
            writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
            return new StubProcess();
        });

        Path tokenFile = client.getTokenFilePath();
        Files.writeString(tokenFile, "old-token", StandardCharsets.UTF_8);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());

        client.ensureConnected(new JIPipeProgressInfo());
        assertTrue(client.isConnected());
        assertEquals("old-token", client.getToken());

        String rotated = client.rotateToken();
        assertNotEquals("old-token", rotated);
        client.invalidateConnection();
        assertFalse(client.isConnected());

        client.ensureConnected(new JIPipeProgressInfo());

        // The 401 from the old-token broker deleted the discovery file and spawned a
        // fresh broker, which accepted the rotated token.
        assertEquals(1, spawnedCommands.size());
        assertTrue(client.isConnected());
        assertEquals(brokerPort(), SharedServerDiscoveryFile.read(client.getDiscoveryFilePath()).getPort());
        assertEquals("Bearer old-token", healthAuthorizationHeaders.get(0));
        assertTrue(healthAuthorizationHeaders.contains("Bearer " + rotated));
        assertEquals(rotated, client.getToken());
    }

    /**
     * A client with token authentication forced on (without the application settings
     * service) and the MCP directory redirected into the test temp dir.
     */
    private MCPBrokerClient tokenClient(MCPBrokerClient.Spawner spawner) {
        return new MCPBrokerClient(spawner, 8781, tempDir) {
            @Override
            boolean isRequireToken() {
                return true;
            }
        };
    }

    private static List<String> readJvmArgs(List<String> command) throws IOException {
        // The JVM arguments live in the @argfile.
        return Files.readAllLines(Path.of(command.get(1).substring(1)), StandardCharsets.UTF_8);
    }

    // --------------------------------------------------------------- helpers

    private int brokerPort() {
        return broker.getAddress().getPort();
    }

    private void startStubBroker() throws IOException {
        broker = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        broker.createContext("/health", exchange -> {
            healthAuthorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "text/plain; charset=utf-8", "ok");
        });
        broker.createContext("/control/register", exchange -> {
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            registrationBodies.add(body);
            respond(exchange, registerResponseStatus.get(), "text/plain; charset=utf-8", registerResponseBody.get());
        });
        broker.createContext("/control/unregister", exchange -> {
            authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            unregisterBodies.add(body);
            respond(exchange, 200, "text/plain; charset=utf-8", "ok");
        });
        broker.start();
    }

    private void startStubEndpoint() throws IOException {
        endpoint = new AutomationInstanceEndpoint(new AutomationOperationRegistry(),
                new AutomationPermissionService(),
                minimalHandlers(),
                null,
                null);
        endpoint.start();
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void writeDiscoveryFile(long pid, int port) {
        Path discoveryFile = client.getDiscoveryFilePath();
        SharedServerDiscoveryFile.write(discoveryFile, pid, port, Instant.now().toString());
    }

    private static MCPProjectEntry projectEntry(String key, String name, String path) {
        MCPProjectEntry entry = new MCPProjectEntry();
        entry.setKey(key);
        entry.setName(name);
        entry.setPath(path);
        return entry;
    }

    private static JsonNode toolById(JsonNode tools, String id) {
        for (JsonNode tool : tools) {
            if (id.equals(tool.get("id").asText())) {
                return tool;
            }
        }
        return null;
    }

    private static AutomationOperationRegistry stubOperationRegistry() {
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        registry.register("echo_stub", new EchoStubOperation());
        registry.register("render_stub", new RenderStubOperation());
        return registry;
    }

    /**
     * Long-running stub process: empty streams, never exits on its own.
     */
    private static final class StubProcess extends Process {
        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * A pid that is guaranteed to be dead: a finished helper process.
     */
    private static long deadPid() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("true").start();
        process.waitFor();
        assertNotEquals(ProcessHandle.current().pid(), process.pid());
        return process.pid();
    }

    private static AutomationEndpointHandlers minimalHandlers() {
        return new AutomationEndpointHandlers() {
            @Override
            public JsonNode execute(String operationId, String projectKey, JsonNode params) {
                throw new IllegalStateException("Not used in this test");
            }

            @Override
            public boolean isRegistered(String operationId) {
                return false;
            }

            @Override
            public boolean isHidden(String operationId) {
                return false;
            }
        };
    }

    private static class EchoStubOperation implements AutomationOperation {
        @Override
        public String getId() {
            return "echo_stub";
        }

        @Override
        public String getDescription() {
            return "Echoes the message back";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson(
                    "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}},\"additionalProperties\":false}");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            return params;
        }
    }

    private static class RenderStubOperation implements AutomationOperation {
        @Override
        public String getId() {
            return "render_stub";
        }

        @Override
        public String getDescription() {
            return "Renders a test image";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("{\"type\":\"object\",\"additionalProperties\":false}");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Render;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            return params;
        }
    }

    /**
     * An operation the tests configure as Hide in the permission service.
     */
    private static class HiddenStubOperation implements AutomationOperation {
        @Override
        public String getId() {
            return "hidden_stub";
        }

        @Override
        public String getDescription() {
            return "A hidden test operation";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("{\"type\":\"object\",\"additionalProperties\":false}");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            return params;
        }
    }
}
