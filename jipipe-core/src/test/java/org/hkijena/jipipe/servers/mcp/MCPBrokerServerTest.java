package org.hkijena.jipipe.servers.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link MCPBrokerServer} HTTP surface with a real MCP client flow (raw JSON-RPC
 * over the streamable HTTP transport) against a stub instance endpoint.
 */
public class MCPBrokerServerTest {

    private static final String SECRET = "test-endpoint-secret";
    private static final String INSTANCE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_INSTANCE_ID = "22222222-2222-2222-2222-222222222222";
    private static final String PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
    private static final String SECRET_HEADER = "X-JIPipe-Endpoint-Secret";

    private final HttpClient client = HttpClient.newHttpClient();
    private HttpServer stub;
    private MCPBrokerServer broker;

    @AfterEach
    public void tearDown() {
        if (stub != null) {
            stub.stop(0);
            stub = null;
        }
        if (broker != null) {
            broker.stop();
            broker = null;
        }
    }

    // ------------------------------------------------------------------ tests

    @Test
    public void healthIsOpenAndReturnsOk() throws Exception {
        startBroker(null);
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + broker.getPort() + "/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
    }

    @Test
    public void registerLogsAndListsToolsWithReadOnlyHint() throws Exception {
        startBroker(null);
        assertEquals(200, registerInstance().statusCode());
        initialize(null);

        JsonNode tools = listTools();
        List<String> names = new ArrayList<>();
        JsonNode echoSpec = null;
        JsonNode renderSpec = null;
        for (JsonNode tool : tools.get("result").get("tools")) {
            names.add(tool.get("name").asText());
            if ("echo_test".equals(tool.get("name").asText())) {
                echoSpec = tool;
            }
            if ("render_image_test".equals(tool.get("name").asText())) {
                renderSpec = tool;
            }
        }
        assertTrue(names.contains("echo_test"));
        assertTrue(names.contains("render_image_test"));
        // Controller ruling: list_operations is a registry meta-operation, never an MCP tool.
        assertFalse(names.contains("list_operations"));
        assertNotNull(echoSpec);
        assertNotNull(renderSpec);
        assertTrue(echoSpec.get("annotations").get("readOnlyHint").asBoolean());
        assertFalse(renderSpec.get("annotations").get("readOnlyHint").asBoolean());

        // Lifecycle events land in the ring buffer (register + tool rebuild).
        HttpResponse<String> logs = client.send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + broker.getPort() + "/control/logs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, logs.statusCode());
        JsonNode logArray = JsonUtils.getObjectMapper().readTree(logs.body());
        assertTrue(logArray.isArray());
        assertFalse(logArray.isEmpty());
        boolean hasRegister = false;
        for (JsonNode entry : logArray) {
            assertTrue(entry.has("level"));
            assertTrue(entry.has("message"));
            if (entry.get("message").asText().contains("Registered instance")) {
                hasRegister = true;
            }
        }
        assertTrue(hasRegister);
    }

    @Test
    public void callEchoToolRoutesToInstanceProject() throws Exception {
        startBroker(null);
        registerInstance();
        initialize(null);

        ObjectNode arguments = JsonUtils.getObjectMapper().createObjectNode();
        arguments.put("project", INSTANCE_ID + ":p1");
        arguments.put("message", "hello");
        JsonNode result = callTool("echo_test", arguments);
        assertNull(result.get("error"));
        JsonNode content = result.get("content").get(0);
        assertEquals("text", content.get("type").asText());
        assertTrue(content.get("text").asText().contains("\"echo\":true"));
        assertTrue(content.get("text").asText().contains("hello"));
    }

    @Test
    public void callImageToolReturnsImageContent() throws Exception {
        startBroker(null);
        registerInstance();
        initialize(null);

        ObjectNode arguments = JsonUtils.getObjectMapper().createObjectNode();
        arguments.put("project", INSTANCE_ID + ":p1");
        JsonNode result = callTool("render_image_test", arguments);
        assertNull(result.get("error"));
        JsonNode content = result.get("content").get(0);
        assertEquals("image", content.get("type").asText());
        assertEquals(PNG_BASE64, content.get("data").asText());
        assertEquals("image/png", content.get("mimeType").asText());
    }

    @Test
    public void unknownProjectKeyListsInstanceProjects() throws Exception {
        startBroker(null);
        registerInstance();
        initialize(null);

        ObjectNode arguments = JsonUtils.getObjectMapper().createObjectNode();
        arguments.put("project", INSTANCE_ID + ":nope");
        arguments.put("message", "hello");
        JsonNode result = callTool("echo_test", arguments);
        assertTrue(result.get("isError").asBoolean());
        assertTrue(result.get("content").get(0).get("text").asText().contains("p1"));
    }

    @Test
    public void unknownInstanceListsAvailableProjects() throws Exception {
        startBroker(null);
        registerInstance();
        initialize(null);

        ObjectNode arguments = JsonUtils.getObjectMapper().createObjectNode();
        arguments.put("project", OTHER_INSTANCE_ID + ":p1");
        arguments.put("message", "hello");
        JsonNode result = callTool("echo_test", arguments);
        assertTrue(result.get("isError").asBoolean());
        String text = result.get("content").get(0).get("text").asText();
        assertTrue(text.contains(INSTANCE_ID + ":p1"));
    }

    @Test
    public void projectKeyWithColonRoutesToInstance() throws Exception {
        startBroker(null);
        registerInstance();
        initialize(null);

        // The id is split on the FIRST colon: instance id is the UUID, project key is
        // "p1:inner". Pins the split rule against a regression to lastIndexOf.
        ObjectNode arguments = JsonUtils.getObjectMapper().createObjectNode();
        arguments.put("project", INSTANCE_ID + ":p1:inner");
        arguments.put("message", "hello");
        JsonNode result = callTool("echo_test", arguments);
        assertNull(result.get("error"));
        String text = result.get("content").get(0).get("text").asText();
        assertTrue(text.contains("\"project-key\":\"p1:inner\""));
    }

    @Test
    public void bearerTokenRejectsWrongAndAllowsRight() throws Exception {
        startBroker("s3cret");

        // Health stays open (process supervision probes it without credentials).
        HttpResponse<String> health = client.send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + broker.getPort() + "/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, health.statusCode());

        // Wrong bearer on /mcp and /control/* -> 401 before any processing.
        assertEquals(401, postRpc("/mcp", initializeBody(), "wrong", null).status());
        assertEquals(401, postRpc("/control/register", registrationJson(), "wrong", null).status());
        assertEquals(401, postRpc("/control/logs", null, "wrong", null).status());

        // Correct bearer passes and the MCP handshake succeeds.
        assertEquals(200, registerInstance("s3cret").statusCode());
        initialize("s3cret");
        JsonNode tools = listTools("s3cret");
        assertTrue(tools.get("result").get("tools").size() > 0);
    }

    @Test
    public void openAccessWithoutToken() throws Exception {
        startBroker(null);
        // No Authorization header at all: the full flow works.
        registerInstance();
        initialize(null);
        JsonNode tools = listTools(null);
        assertTrue(tools.get("result").get("tools").size() > 0);
        assertNotEquals(401, postRpc("/control/logs", null, null, null).status());
    }

    // ------------------------------------------------------- stub instance

    private void startBroker(String bearerToken) throws Exception {
        startStub();
        AutomationOperationRegistry metadata = new AutomationOperationRegistry();
        metadata.register("echo_test", new EchoTestOperation());
        metadata.register("render_image_test", new RenderImageTestOperation());
        broker = new MCPBrokerServer(0, new MCPInstanceRegistry(), bearerToken, metadata);
        broker.start();
    }

    private void startStub() throws IOException {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/op", this::handleStubOperation);
        stub.start();
    }

    private void handleStubOperation(HttpExchange exchange) throws IOException {
        try {
            String providedSecret = exchange.getRequestHeaders().getFirst(SECRET_HEADER);
            byte[] request = exchange.getRequestBody().readAllBytes();
            String response;
            if (!SECRET.equals(providedSecret)) {
                response = "{\"ok\":false,\"code\":\"unauthorized\",\"error\":\"Missing or invalid endpoint secret\"}";
            } else {
                JsonNode body = JsonUtils.getObjectMapper().readTree(request);
                String opId = exchange.getRequestURI().getPath().substring("/op/".length());
                response = switch (opId) {
                    case "echo_test" -> stubResult(echoResult(body));
                    case "render_image_test" -> "{\"ok\":true,\"result\":{\"image-base64\":\"" + PNG_BASE64
                            + "\",\"mime\":\"image/png\"}}";
                    default -> "{\"ok\":false,\"code\":\"unknown\",\"error\":\"Unknown operation: " + opId + "\"}";
                };
            }
            respond(exchange, 200, response);
        } catch (Exception e) {
            respond(exchange, 500, "{\"ok\":false,\"code\":\"failed\",\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private ObjectNode echoResult(JsonNode body) {
        ObjectNode result = JsonUtils.getObjectMapper().createObjectNode();
        result.put("echo", true);
        result.set("message", body.path("params").path("message"));
        result.set("project-key", body.path("project-key"));
        return result;
    }

    private String stubResult(JsonNode result) {
        ObjectNode envelope = JsonUtils.getObjectMapper().createObjectNode();
        envelope.put("ok", true);
        envelope.set("result", result);
        return envelope.toString();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String registrationJson() {
        ObjectNode registration = JsonUtils.getObjectMapper().createObjectNode();
        registration.put("instance-id", INSTANCE_ID);
        registration.put("jipipe-pid", ProcessHandle.current().pid());
        registration.put("endpoint-port", stub.getAddress().getPort());
        registration.put("endpoint-secret", SECRET);
        registration.put("app-version", "test");
        registration.set("tool-ids", JsonUtils.getObjectMapper().createArrayNode()
                .add("echo_test").add("render_image_test").add("list_operations"));
        ObjectNode project = JsonUtils.getObjectMapper().createObjectNode();
        project.put("key", "p1");
        project.put("name", "P1");
        project.put("path", "/tmp/p1");
        ObjectNode colonProject = JsonUtils.getObjectMapper().createObjectNode();
        colonProject.put("key", "p1:inner");
        colonProject.put("name", "P1 Inner");
        colonProject.put("path", "/tmp/p1/inner");
        registration.set("projects", JsonUtils.getObjectMapper().createArrayNode().add(project).add(colonProject));
        return registration.toString();
    }

    private HttpResponse<String> registerInstance() throws Exception {
        return registerInstance(null);
    }

    private HttpResponse<String> registerInstance(String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + broker.getPort() + "/control/register"))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(registrationJson(),
                        StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    // ------------------------------------------------------------ MCP flow

    private record RpcResponse(int status, JsonNode body, String sessionId) {
    }

    private String initializeBody() {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":"
                + "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
                + "\"clientInfo\":{\"name\":\"test\",\"version\":\"0\"}}}";
    }

    private RpcResponse postRpc(String path, String json, String token, String session) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + broker.getPort() + path));
        if (json != null) {
            builder.header("Content-Type", "application/json");
            builder.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        builder.header("Accept", "application/json, text/event-stream");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (session != null) {
            builder.header("mcp-session-id", session);
        }
        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        JsonNode parsed = parseMcpBody(response.headers().firstValue("Content-Type").orElse(""), response.body());
        return new RpcResponse(response.statusCode(), parsed,
                response.headers().firstValue("mcp-session-id").orElse(null));
    }

    private JsonNode parseMcpBody(String contentType, byte[] bytes) throws IOException {
        String text = new String(bytes, StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) {
            return null;
        }
        boolean eventStream = contentType.contains("text/event-stream") || text.startsWith("event:")
                || text.startsWith("data:");
        if (!eventStream) {
            return JsonUtils.getObjectMapper().readTree(text);
        }
        JsonNode best = null;
        for (String line : text.split("\n")) {
            if (!line.startsWith("data:")) {
                continue;
            }
            String data = line.substring(5).stripLeading();
            if (data.isEmpty()) {
                continue;
            }
            JsonNode parsed = JsonUtils.getObjectMapper().readTree(data);
            if (best == null || parsed.has("result") || parsed.has("error")) {
                best = parsed;
            }
        }
        return best;
    }

    private void initialize(String token) throws Exception {
        RpcResponse init = postRpc("/mcp", initializeBody(), token, null);
        assertEquals(200, init.status());
        assertNotNull(init.sessionId());
        currentSession = init.sessionId();
        RpcResponse notified = postRpc("/mcp", "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}",
                token, currentSession);
        assertEquals(202, notified.status());
    }

    private JsonNode listTools(String token) throws Exception {
        RpcResponse response = postRpc("/mcp", "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}",
                token, currentSession);
        assertEquals(200, response.status());
        return response.body();
    }

    private JsonNode listTools() throws Exception {
        return listTools(null);
    }

    private String currentSession;

    private JsonNode callTool(String name, ObjectNode arguments) throws Exception {
        ObjectNode params = JsonUtils.getObjectMapper().createObjectNode();
        params.put("name", name);
        params.set("arguments", arguments);
        String json = "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":"
                + JsonUtils.getObjectMapper().writeValueAsString(params) + "}";
        RpcResponse response = postRpc("/mcp", json, null, currentSession);
        assertEquals(200, response.status());
        assertNotNull(response.body());
        assertNull(response.body().get("error"));
        return response.body().get("result");
    }

    // --------------------------------------------------- metadata stub ops

    private static class EchoTestOperation implements AutomationOperation {
        @Override
        public String getId() {
            return "echo_test";
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

    private static class RenderImageTestOperation implements AutomationOperation {
        @Override
        public String getId() {
            return "render_image_test";
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
