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
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the HTTP surface of {@link AutomationInstanceEndpoint}: secret enforcement,
 * permission states, the error envelope, fail-closed schemas, and the call listener.
 */
public class AutomationInstanceEndpointTest {

    private static final String SECRET_HEADER = "X-JIPipe-Endpoint-Secret";
    private static final JsonNode ECHO_SCHEMA = AutomationSchemaUtils.parseJson(
            "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}},"
                    + "\"required\":[\"message\"],\"additionalProperties\":false}");

    private final HttpClient client = HttpClient.newHttpClient();
    private AutomationInstanceEndpoint endpoint;
    private RecordingListener listener;
    private RecordingAskHandler askHandler;

    @AfterEach
    public void tearDown() {
        if (endpoint != null) {
            endpoint.stop();
            endpoint = null;
        }
    }

    @Test
    public void healthRequiresNoSecret() throws Exception {
        startEndpoint();
        HttpResponse<String> response = getHealth();
        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
        assertTrue(listener.calls.isEmpty());
    }

    @Test
    public void operationWithCorrectSecretExecutes() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("echo_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{\"message\":\"hello\"}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertTrue(body.get("ok").asBoolean());
        assertTrue(body.get("result").get("echo").asBoolean());
        assertEquals(1, listener.calls.size());
        assertEquals("echo_test", listener.calls.get(0).operationId());
        assertEquals("proj1", listener.calls.get(0).projectKey());
        assertEquals("ok", listener.calls.get(0).outcome());
        assertTrue(listener.calls.get(0).durationMs() >= 0);
    }

    @Test
    public void wrongSecretIsUnauthorized() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("echo_test", "wrong-secret",
                "{\"project-key\":\"proj1\",\"params\":{\"message\":\"hello\"}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("unauthorized", body.get("code").asText());
        assertEquals(1, listener.calls.size());
        assertEquals("unauthorized", listener.calls.get(0).outcome());
    }

    @Test
    public void missingSecretIsUnauthorized() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("echo_test", null,
                "{\"project-key\":\"proj1\",\"params\":{\"message\":\"hello\"}}");
        assertEquals("unauthorized", json(response.body()).get("code").asText());
    }

    @Test
    public void unknownOperationIsUnknown() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("does_not_exist", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("unknown", body.get("code").asText());
        assertEquals(1, listener.calls.size());
        assertEquals("does_not_exist", listener.calls.get(0).operationId());
        assertEquals("unknown", listener.calls.get(0).outcome());
    }

    @Test
    public void hiddenOperationIsHidden() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("hidden_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("hidden", body.get("code").asText());
        assertEquals(1, listener.calls.size());
        assertEquals("hidden", listener.calls.get(0).outcome());
    }

    @Test
    public void deniedOperationIsDeniedWithoutAsking() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("deny_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("denied", body.get("code").asText());
        assertTrue(askHandler.calls.isEmpty());
        assertEquals(1, listener.calls.size());
        assertEquals("denied", listener.calls.get(0).outcome());
    }

    @Test
    public void askHandlerReturningTrueAllowsExecution() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("ask_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertTrue(body.get("ok").asBoolean());
        assertTrue(body.get("result").get("asked").asBoolean());
        assertEquals(List.of("ask_test:proj1"), askHandler.calls);
        assertEquals(1, listener.calls.size());
        assertEquals("ok", listener.calls.get(0).outcome());
    }

    @Test
    public void askHandlerReturningFalseIsDenied() throws Exception {
        startEndpoint();
        askHandler.answer = false;
        HttpResponse<String> response = postOperation("ask_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("denied", body.get("code").asText());
        assertEquals(1, askHandler.calls.size());
        assertEquals(1, listener.calls.size());
        assertEquals("denied", listener.calls.get(0).outcome());
    }

    @Test
    public void schemaViolationIsInvalid() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("echo_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("invalid", body.get("code").asText());
        assertTrue(body.get("error").asText().contains("required"));
        assertEquals(1, listener.calls.size());
        assertEquals("invalid", listener.calls.get(0).outcome());
    }

    @Test
    public void malformedBodyIsInvalid() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("echo_test", endpoint.getSecret(), "this is not json");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("invalid", body.get("code").asText());
        assertEquals(1, listener.calls.size());
        assertEquals("invalid", listener.calls.get(0).outcome());
    }

    @Test
    public void missingProjectKeyIsInvalid() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("echo_test", endpoint.getSecret(),
                "{\"params\":{\"message\":\"hello\"}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("invalid", body.get("code").asText());
        assertEquals(1, listener.calls.size());
        assertEquals("invalid", listener.calls.get(0).outcome());
    }

    @Test
    public void operationWithoutUsableSchemaIsRefused() throws Exception {
        startEndpoint();
        HttpResponse<String> nullSchema = postOperation("noschema_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        assertEquals("invalid", json(nullSchema.body()).get("code").asText());
        HttpResponse<String> nonObjectSchema = postOperation("badschema_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        assertEquals("invalid", json(nonObjectSchema.body()).get("code").asText());
        assertEquals(2, listener.calls.size());
        assertEquals("invalid", listener.calls.get(0).outcome());
        assertEquals("invalid", listener.calls.get(1).outcome());
    }

    @Test
    public void handlerThrowingIsFailed() throws Exception {
        startEndpoint();
        HttpResponse<String> response = postOperation("boom_test", endpoint.getSecret(),
                "{\"project-key\":\"proj1\",\"params\":{}}");
        JsonNode body = json(response.body());
        assertEquals(200, response.statusCode());
        assertFalse(body.get("ok").asBoolean());
        assertEquals("failed", body.get("code").asText());
        assertTrue(body.get("error").asText().contains("boom"));
        assertEquals(1, listener.calls.size());
        assertEquals("failed", listener.calls.get(0).outcome());
    }

    @Test
    public void listenerRecordsExactlyOneEntryPerDispatch() throws Exception {
        startEndpoint();
        postOperation("echo_test", endpoint.getSecret(), "{\"project-key\":\"proj1\",\"params\":{\"message\":\"hello\"}}");
        postOperation("does_not_exist", endpoint.getSecret(), "{\"project-key\":\"proj1\",\"params\":{}}");
        postOperation("echo_test", "wrong-secret", "{\"project-key\":\"proj1\",\"params\":{}}");
        getHealth();
        assertEquals(3, listener.calls.size());
        assertEquals("ok", listener.calls.get(0).outcome());
        assertEquals("unknown", listener.calls.get(1).outcome());
        assertEquals("unauthorized", listener.calls.get(2).outcome());
        assertTrue(listener.calls.get(0).durationMs() >= 0);
        assertTrue(listener.calls.get(1).durationMs() >= 0);
        assertTrue(listener.calls.get(2).durationMs() >= 0);
    }

    @Test
    public void registrationDtoUsesKebabCaseKeys() throws Exception {
        MCPProjectEntry entry = new MCPProjectEntry();
        entry.setKey("proj1");
        entry.setName("Project One");
        entry.setPath("/data/proj1");
        MCPInstanceRegistration registration = new MCPInstanceRegistration();
        registration.setInstanceId("instance-1");
        registration.setJipipePid(4242);
        registration.setEndpointPort(8781);
        registration.setEndpointSecret("s3cret");
        registration.setAppVersion("6.0.0");
        registration.setProjects(List.of(entry));

        String json = JsonUtils.getObjectMapper().writeValueAsString(registration);
        JsonNode node = JsonUtils.getObjectMapper().readTree(json);
        assertEquals("instance-1", node.get("instance-id").asText());
        assertEquals(4242, node.get("jipipe-pid").asLong());
        assertEquals(8781, node.get("endpoint-port").asInt());
        assertEquals("s3cret", node.get("endpoint-secret").asText());
        assertEquals("6.0.0", node.get("app-version").asText());
        assertEquals("proj1", node.get("projects").get(0).get("key").asText());
        assertEquals("Project One", node.get("projects").get(0).get("name").asText());
        assertEquals("/data/proj1", node.get("projects").get(0).get("path").asText());

        MCPInstanceRegistration parsed = JsonUtils.getObjectMapper().readValue(json, MCPInstanceRegistration.class);
        assertEquals("instance-1", parsed.getInstanceId());
        assertEquals(4242, parsed.getJipipePid());
        assertEquals(8781, parsed.getEndpointPort());
        assertEquals("s3cret", parsed.getEndpointSecret());
        assertEquals("6.0.0", parsed.getAppVersion());
        assertEquals(1, parsed.getProjects().size());
        assertEquals("/data/proj1", parsed.getProjects().get(0).getPath());
    }

    private void startEndpoint() throws Exception {
        AutomationPermissionService permissions = new AutomationPermissionService();
        permissions.setUserState("hidden_test", AutomationPermissionState.Hide);
        permissions.setUserState("deny_test", AutomationPermissionState.Deny);
        permissions.setUserState("ask_test", AutomationPermissionState.Ask);
        listener = new RecordingListener();
        askHandler = new RecordingAskHandler();
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        registry.register("echo_test", stubOperation("echo_test", ECHO_SCHEMA,
                (ctx, params) -> JsonUtils.getObjectMapper().createObjectNode().put("echo", true)));
        registry.register("boom_test", stubOperation("boom_test", AutomationSchemaUtils.emptyObjectSchema(),
                (ctx, params) -> {
                    throw new IllegalStateException("boom");
                }));
        registry.register("hidden_test", stubOperation("hidden_test", AutomationSchemaUtils.emptyObjectSchema(),
                (ctx, params) -> JsonUtils.getObjectMapper().createObjectNode().put("hidden", true)));
        registry.register("deny_test", stubOperation("deny_test", AutomationSchemaUtils.emptyObjectSchema(),
                (ctx, params) -> JsonUtils.getObjectMapper().createObjectNode().put("denied", true)));
        registry.register("ask_test", stubOperation("ask_test", AutomationSchemaUtils.emptyObjectSchema(),
                (ctx, params) -> JsonUtils.getObjectMapper().createObjectNode().put("asked", true)));
        registry.register("noschema_test", stubOperation("noschema_test", null,
                (ctx, params) -> JsonUtils.getObjectMapper().createObjectNode().put("done", true)));
        registry.register("badschema_test", stubOperation("badschema_test", AutomationSchemaUtils.parseJson("\"not an object\""),
                (ctx, params) -> JsonUtils.getObjectMapper().createObjectNode().put("done", true)));
        AutomationEndpointHandlers handlers = new StubHandlers(registry);
        endpoint = new AutomationInstanceEndpoint(registry, permissions, handlers, askHandler, listener);
        endpoint.start();
    }

    private static AutomationOperation stubOperation(String id, JsonNode schema, OpBody body) {
        return new AutomationOperation() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getDescription() {
                return "stub operation " + id;
            }

            @Override
            public JsonNode getInputSchema() {
                return schema;
            }

            @Override
            public JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception {
                return body.execute(ctx, params);
            }
        };
    }

    @FunctionalInterface
    private interface OpBody {
        JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception;
    }

    /**
     * Emulates the GUI wiring: dispatch goes through the registry's validating execute.
     */
    private static class StubHandlers implements AutomationEndpointHandlers {
        private final AutomationOperationRegistry registry;

        private StubHandlers(AutomationOperationRegistry registry) {
            this.registry = registry;
        }

        @Override
        public JsonNode execute(String operationId, String projectKey, JsonNode params) throws Exception {
            return registry.execute(operationId, null, params);
        }

        @Override
        public boolean isRegistered(String operationId) {
            return registry.has(operationId);
        }

        @Override
        public boolean isHidden(String operationId) {
            return operationId.equals("hidden_test");
        }
    }

    private record Call(String operationId, String projectKey, String outcome, long durationMs) {
    }

    private static class RecordingListener implements AutomationInstanceEndpoint.OperationCallListener {
        private final List<Call> calls = new ArrayList<>();

        @Override
        public void onCall(String operationId, String projectKey, String outcome, long durationMs) {
            calls.add(new Call(operationId, projectKey, outcome, durationMs));
        }
    }

    private static class RecordingAskHandler implements AutomationInstanceEndpoint.AskPromptHandler {
        private final List<String> calls = new ArrayList<>();
        private boolean answer = true;

        @Override
        public boolean ask(String operationId, String projectKey) {
            calls.add(operationId + ":" + projectKey);
            return answer;
        }
    }

    private HttpResponse<String> getHealth() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + endpoint.getBoundPort() + "/health"))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postOperation(String operationId, String secret, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + endpoint.getBoundPort() + "/op/" + operationId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (secret != null) {
            builder.header(SECRET_HEADER, secret);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode json(String text) throws Exception {
        return JsonUtils.getObjectMapper().readTree(text);
    }
}
