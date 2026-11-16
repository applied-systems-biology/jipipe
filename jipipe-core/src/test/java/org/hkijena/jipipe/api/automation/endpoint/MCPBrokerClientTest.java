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
import org.hkijena.jipipe.api.servers.SharedServerDiscoveryFile;
import org.hkijena.jipipe.servers.mcp.MCPServerProcess;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link MCPBrokerClient} against a stub broker HTTP server. The spawner is always
 * stubbed: no test spawns a real broker JVM.
 */
public class MCPBrokerClientTest {

    private HttpServer broker;
    private final List<String> registrationBodies = new CopyOnWriteArrayList<>();
    private final List<String> unregisterBodies = new CopyOnWriteArrayList<>();
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
        client = new MCPBrokerClient(recordingSpawner, 8781);
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
        client = new MCPBrokerClient(recordingSpawner, 8781);
        writeDiscoveryFile(ProcessHandle.current().pid(), brokerPort());
        startStubEndpoint();
        client.setEndpoint(endpoint);
        client.setProjectSource(() -> List.of(projectEntry("p1", "Project One", "/tmp/p1")));
        client.setToolSource(() -> MCPBrokerClient.descriptorsOf(stubOperationRegistry()));

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
    public void deadBrokerPidTriggersSpawnAndConnects() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(spawningStubSpawner, 8781);
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
        }, 8781);
        writeDiscoveryFile(deadPid(), 1);

        assertThrows(RuntimeException.class, () -> client.ensureConnected(new JIPipeProgressInfo()));
        assertFalse(client.isConnected());
    }

    @Test
    public void unregisterPostsInstanceId() throws Exception {
        startStubBroker();
        client = new MCPBrokerClient(recordingSpawner, 8781);
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
        client = new MCPBrokerClient(recordingSpawner, 8781);
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

    // --------------------------------------------------------------- helpers

    private int brokerPort() {
        return broker.getAddress().getPort();
    }

    private void startStubBroker() throws IOException {
        broker = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        broker.createContext("/health", exchange -> respond(exchange, 200, "text/plain; charset=utf-8", "ok"));
        broker.createContext("/control/register", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            registrationBodies.add(body);
            respond(exchange, 200, "text/plain; charset=utf-8", "ok");
        });
        broker.createContext("/control/unregister", exchange -> {
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
}
