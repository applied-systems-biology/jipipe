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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP endpoint that exposes the automation operations of one JIPipe instance on 127.0.0.1.
 * <p>
 * Requests are authenticated with a per-endpoint secret sent as the {@code X-JIPipe-Endpoint-Secret}
 * header (constant-time comparison); the secret check runs before any request body byte is read
 * or parsed, so unauthorized requests are rejected without buffering their payload.
 * Responses always use HTTP 200: errors are carried by the JSON envelope
 * ({@code ok:false} plus a code of {@code unauthorized|hidden|denied|unknown|invalid|failed}).
 * <p>
 * Fail-closed schemas: an operation whose input schema is null or not a JSON object is refused
 * with code {@code invalid} instead of being dispatched without validation.
 * <p>
 * Handling order: secret check, operation existence (registry lookup), permission resolution
 * (Hide, then Deny, then Ask), body and parameter validation, dispatch. A missing or blank
 * {@code project-key} is NOT rejected: the request is dispatched with a null project key, so
 * the registry-global operations run project-less while project-bound operations fail with
 * their own "No project loaded" style error results (E2E ruling 2026-11-16). Parameter validation is
 * done once, by the registry's validating execute invoked from
 * {@link AutomationEndpointHandlers#execute}; its {@link IllegalArgumentException} maps to
 * {@code invalid}, and an {@link IllegalArgumentException} thrown by the handler implementation
 * itself is indistinguishable from a validation failure and also maps to {@code invalid}
 * (single-validator path, by design). Dispatch runs on the HTTP executor thread; EDT
 * marshalling belongs to the {@link AutomationEndpointHandlers} implementation.
 * <p>
 * {@link AutomationEndpointHandlers#isRegistered(String)} and
 * {@link AutomationEndpointHandlers#isHidden(String)} are NOT consulted by the endpoint:
 * operation existence comes from the {@link AutomationOperationRegistry} lookup and hidden
 * state from the {@link AutomationPermissionService}; the two handler methods exist for
 * GUI-side wiring only.
 */
public class AutomationInstanceEndpoint {

    /**
     * The origin metadata this endpoint reports for its asks: every ask that reaches the
     * {@link AskPromptHandler} through this endpoint was requested by an MCP client, and
     * the endpoint has no richer origin information (session, agent id) today. A future
     * ask source (the ACP integration, #1376) passes its own origin through the
     * three-argument {@link AskPromptHandler#ask(String, String, String)}.
     */
    public static final String ASK_ORIGIN_MCP = "mcp";

    /**
     * Decides one "ask before running" prompt (true = allow once).
     */
    public interface AskPromptHandler {

        boolean ask(String operationId, String projectKey);

        /**
         * Same as {@link #ask(String, String)}, with origin metadata naming who asked
         * (free-form; see {@link #ASK_ORIGIN_MCP}). The endpoint always passes
         * {@link #ASK_ORIGIN_MCP}: it has no richer origin information today. Handlers
         * that do not care about the origin only implement the two-argument form; this
         * default delegates to it.
         */
        default boolean ask(String operationId, String projectKey, String origin) {
            return ask(operationId, projectKey);
        }
    }

    /**
     * Notified once after every dispatch attempt, including unauthorized ones.
     * <p>
     * {@link #onCall} fires on the HTTP executor thread, not the EDT: GUI listeners must
     * marshal to the EDT themselves.
     */
    public interface OperationCallListener {
        void onCall(String operationId, String projectKey, String outcome, long durationMs);
    }

    /**
     * One dispatch outcome: the envelope code and the prebuilt response body.
     */
    private record DispatchResult(String code, ObjectNode body) {

        private static DispatchResult of(String code, String error) {
            ObjectNode body = JsonUtils.getObjectMapper().createObjectNode();
            body.put("ok", false);
            body.put("code", code);
            body.put("error", error);
            return new DispatchResult(code, body);
        }

        private static DispatchResult success(JsonNode result) {
            ObjectNode body = JsonUtils.getObjectMapper().createObjectNode();
            body.put("ok", true);
            body.set("result", result);
            return new DispatchResult("ok", body);
        }
    }

    private static final String SECRET_HEADER = "X-JIPipe-Endpoint-Secret";
    private static final String OP_PREFIX = "/op/";

    private final AutomationOperationRegistry registry;
    private final AutomationPermissionService permissions;
    private final AutomationEndpointHandlers handlers;
    private final AskPromptHandler askHandler;
    private final OperationCallListener callListener;
    private final String secret;
    private final HttpServer server;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean();

    public AutomationInstanceEndpoint(AutomationOperationRegistry registry,
                                      AutomationPermissionService permissions,
                                      AutomationEndpointHandlers handlers,
                                      AskPromptHandler askHandler,
                                      OperationCallListener callListener) throws IOException {
        this.registry = registry;
        this.permissions = permissions;
        this.handlers = handlers;
        this.askHandler = askHandler;
        this.callListener = callListener;
        this.secret = generateSecret();
        this.executor = Executors.newCachedThreadPool(this::createWorkerThread);
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext(OP_PREFIX, this::handleOperation);
        server.setExecutor(executor);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            server.start();
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    public int getBoundPort() {
        return server.getAddress().getPort();
    }

    public String getSecret() {
        return secret;
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private void handleOperation(HttpExchange exchange) throws IOException {
        long startNanos = System.nanoTime();
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        String operationId = extractOperationId(exchange.getRequestURI().getPath());
        String providedSecret = exchange.getRequestHeaders().getFirst(SECRET_HEADER);
        if (providedSecret == null || !constantTimeEquals(secret, providedSecret)) {
            // The secret check runs before any request body byte is read or parsed: an
            // unauthorized request is rejected without buffering its payload, so the
            // listener reports no project key. Closing the exchange discards the unread body.
            complete(exchange, startNanos, operationId, null,
                    DispatchResult.of("unauthorized", "Missing or invalid endpoint secret"));
            return;
        }
        byte[] request = exchange.getRequestBody().readAllBytes();
        JsonNode lenientBody = tryParse(request);
        String projectKey = extractProjectKey(lenientBody);
        DispatchResult result;
        try {
            result = dispatch(operationId, lenientBody, projectKey);
        } catch (Exception e) {
            result = DispatchResult.of("failed", describe(e));
        }
        complete(exchange, startNanos, operationId, projectKey, result);
    }

    private DispatchResult dispatch(String operationId, JsonNode body, String projectKey) throws Exception {
        AutomationOperation operation = registry.get(operationId);
        if (operation == null) {
            return DispatchResult.of("unknown", "Unknown operation: " + operationId);
        }
        AutomationPermissionState state = permissions.resolve(operation, projectKey);
        if (state == AutomationPermissionState.Hide) {
            return DispatchResult.of("hidden", "Operation '" + operationId + "' is hidden");
        }
        if (state == AutomationPermissionState.Deny) {
            return DispatchResult.of("denied", "Operation '" + operationId + "' is denied");
        }
        if (state == AutomationPermissionState.Ask && (askHandler == null
                || !askHandler.ask(operationId, projectKey, ASK_ORIGIN_MCP))) {
            return DispatchResult.of("denied", "Operation '" + operationId + "' was not approved");
        }
        if (body == null || !body.isObject()) {
            return DispatchResult.of("invalid", "Request body must be a JSON object");
        }
        // A null project key is forwarded to the handler: the registry-global operations
        // (list_projects, list_algorithms, get_node_docs) are dispatched with a null-project
        // context, while project-bound operations fail with their own "No project loaded"
        // style error results (E2E ruling 2026-11-16 - the broker routes global tools without
        // a project). No blanket rejection here.
        JsonNode schema = operation.getInputSchema();
        if (schema == null || schema.isNull() || !schema.isObject()) {
            // Fail-closed: never dispatch an operation whose schema cannot be validated.
            return DispatchResult.of("invalid", "Operation '" + operationId + "' does not declare a valid input schema");
        }
        try {
            return DispatchResult.success(handlers.execute(operationId, projectKey, body.get("params")));
        } catch (IllegalArgumentException e) {
            // The registry's validating execute is the single validator; its violations map to
            // invalid, and an IllegalArgumentException from the handler implementation itself
            // is indistinguishable and also maps to invalid (see class Javadoc).
            return DispatchResult.of("invalid", describe(e));
        } catch (Exception e) {
            return DispatchResult.of("failed", describe(e));
        }
    }

    private void complete(HttpExchange exchange, long startNanos, String operationId,
                          String projectKey, DispatchResult result) throws IOException {
        if (callListener != null) {
            try {
                callListener.onCall(operationId, projectKey, result.code(), (System.nanoTime() - startNanos) / 1_000_000);
            } catch (Exception e) {
                // A misbehaving listener must never prevent the HTTP response.
            }
        }
        respondJson(exchange, 200, result.body());
    }

    private static void respondJson(HttpExchange exchange, int status, JsonNode body) throws IOException {
        byte[] bytes = JsonUtils.getObjectMapper().writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static String extractOperationId(String path) {
        return path.length() > OP_PREFIX.length() ? path.substring(OP_PREFIX.length()) : "";
    }

    private static String extractProjectKey(JsonNode body) {
        if (body == null || !body.isObject()) {
            return null;
        }
        JsonNode key = body.get("project-key");
        if (key == null || !key.isTextual()) {
            return null;
        }
        String value = key.asText();
        return value.isBlank() ? null : value;
    }

    private static JsonNode tryParse(byte[] request) {
        if (request.length == 0) {
            return null;
        }
        try {
            return JsonUtils.getObjectMapper().readTree(request);
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }

    private static String describe(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getName();
    }

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Thread createWorkerThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "jipipe-automation-endpoint");
        thread.setDaemon(true);
        return thread;
    }
}
