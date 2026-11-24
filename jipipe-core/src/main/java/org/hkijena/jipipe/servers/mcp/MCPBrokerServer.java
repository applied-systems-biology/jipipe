package org.hkijena.jipipe.servers.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.AutomationToolScope;
import org.hkijena.jipipe.api.automation.endpoint.MCPInstanceRegistration;
import org.hkijena.jipipe.api.automation.endpoint.MCPToolDescriptor;
import org.hkijena.jipipe.api.servers.ServerLogRingBuffer;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * MCP broker: a Jetty server on 127.0.0.1 that exposes the automation operations of
 * registered JIPipe instances as MCP tools.
 * <p>
 * Routes: {@code GET /health} (open, no bearer token required), MCP endpoint {@code /mcp}
 * (streamable HTTP via {@link HttpServletStreamableServerTransportProvider}), and control
 * routes {@code POST /control/register}, {@code POST /control/unregister},
 * {@code GET /control/logs}. When a bearer token is configured, every request to
 * {@code /mcp} and {@code /control/*} must present {@code Authorization: Bearer <token>}
 * (constant-time comparison) or is rejected with 401 before any processing.
 * <p>
 * Tool specs are rebuilt on every registration change. Registrations carry per-tool
 * {@link MCPToolDescriptor}s (id, description, verbatim input schema, read-only flag,
 * declared tool scope), and the broker serves {@code tools/list} directly from the
 * aggregated descriptors (name = descriptor id), excluding the operations whose declared
 * scope is {@code NOT_A_TOOL} (today the {@code list_operations} meta-operation). When no
 * registration carries descriptors, a broker constructed with a local {@link AutomationOperationRegistry}
 * (test/standalone path) falls back to building specs from that registry
 * ({@link AutomationOperation#getAgentDocumentation()}, {@link AutomationOperation#getInputSchema()},
 * {@link AutomationOperation#isReadOnly()}); without a local registry the tool list stays
 * empty, which is correct while no instance is registered.
 * <p>
 * Every operation declares its {@link AutomationToolScope}, carried to the broker on the
 * descriptor (the registry convention: the registered item carries all information about
 * itself, so the broker derives routing from the declaration instead of a hardcoded id
 * set). {@code PROJECT_BOUND} tools get the {@code project} argument injected into their
 * published schema; tools of any other scope publish no project argument.
 * {@code tools/call} routes via the {@code project} argument ({@code <instanceId>:<projectKey>},
 * split on the first colon) and forwards the call to
 * {@code POST http://127.0.0.1:<endpointPort>/op/<operationId>} with the endpoint secret
 * header and body {@code {"project-key": k, "params": <args minus project>}}. The
 * {@code project} argument is optional: when it is missing or blank, the call is routed to
 * the most-recently registered live instance that carries the tool with a null forwarded
 * project key, and the instance resolves the target project window (E2E ruling 2026-11-16;
 * the job tools and {@code search_nodes} joined the project-free set in the 2026-11-17
 * follow-up wave because job ids address the shared job manager, not a project, and
 * {@code search_nodes} searches the node type registry, which does not involve a project).
 * An explicit {@code project} argument always routes by name.
 * {@code INSTANCE_GLOBAL_AGGREGATED} tools (today {@code list_projects}) are the exception
 * to the single-instance routing: the broker aggregates them across EVERY live instance that
 * carries the tool (routing it to one instance would hide every other instance's projects)
 * and rewrites each returned project id to the full routable {@code <instanceId>:<projectKey>}
 * form that it owns; instances that do not answer are skipped, and the call fails only when
 * no instance answers. Results whose
 * JSON contains non-null {@code image-base64} and {@code mime} fields become MCP image
 * content, everything else becomes JSON text content. Operations classified as jobs (category
 * Execute, except {@code get_job_status}/{@code cancel_job}) return a {@code job-id}
 * envelope from the instance immediately; the broker passes it through unchanged and does
 * not wait for job completion.
 * <p>
 * Progress: when a {@code tools/call} request carries {@code _meta.progressToken}, the broker
 * emits a single start-of-dispatch progress notification (progress 0 of 1, message = operation
 * id) through the SDK exchange. Tool calls return at job-ack by architecture, so there is no
 * long-running tools/call phase: fine-grained job progress is delivered via the returned
 * job-id and {@code get_job_status}, while instance-to-broker progress streaming is a
 * follow-up work item.
 * <p>
 * All routing outcomes and lifecycle events are recorded in the {@link ServerLogRingBuffer}
 * served by {@code /control/logs}.
 */
public class MCPBrokerServer {

    /**
     * The one {@link AutomationToolScope#INSTANCE_GLOBAL_AGGREGATED} operation today: the
     * broker answers its project-free calls itself instead of routing them to a single
     * instance ({@link #aggregateListProjects()}). The routing decision is made from the
     * declared tool scope, not from this id; the constant only names the operation whose
     * envelope ({@code projects} entries) the aggregation rewrites, in logs and errors.
     */
    private static final String LIST_PROJECTS_TOOL_ID = "list_projects";

    /**
     * The schema description of the broker-injected {@code project} argument (per the
     * sub-project C design as corrected by the 2026-11-20 review: the argument is optional, a
     * missing one routes to the focused project window of the routed instance).
     */
    private static final String PROJECT_ARGUMENT_DESCRIPTION =
            "Optional. Omit to target the focused project window of the routed instance (an explicit "
                    + "error lists the candidates when several are open and none is focused). Pass "
                    + "'<instanceId>:<projectKey>' as returned by list_projects to target a specific window "
                    + "or instance - needed to target a specific instance or window";

    private static final String SECRET_HEADER = "X-JIPipe-Endpoint-Secret";

    /**
     * How long the broker waits for the instance endpoint to answer a forwarded tools/call.
     * <p>
     * Invariant: this timeout must exceed the instance-side Ask bound
     * ({@code JIPipeDesktopMCPIntegration.ASK_TIMEOUT_SECONDS}, 300 s) plus a margin, so an
     * Ask-classified tool whose permission bubble is answered late is still delivered instead
     * of dying on a transport timeout while the operation may still execute (a lost result
     * invites an agent retry, which can double-execute a mutating operation). The Ask bound
     * lives in the desktop integration and is deliberately NOT referenced in code: the broker
     * must not depend on the desktop package. 360 s = 300 s Ask bound + 60 s margin.
     */
    private static final Duration FORWARD_TIMEOUT = Duration.ofSeconds(360);

    /**
     * Accepts every tool input: the instance endpoint validates parameters against the
     * operation schema, so the broker must not run the SDK's own validator (whose
     * json-schema-validator dependency is incompatible with the version pinned by the
     * RO-Crate module and therefore excluded).
     */
    private static final JsonSchemaValidator ACCEPT_ALL_VALIDATOR = new JsonSchemaValidator() {
        @Override
        public JsonSchemaValidator.ValidationResponse validate(Map<String, Object> schema, Object args) {
            return JsonSchemaValidator.ValidationResponse.asValid(null);
        }
    };

    private final int port;
    private final MCPInstanceRegistry registry;
    private final String bearerToken;
    private final AutomationOperationRegistry operationMetadata;
    private final ServerLogRingBuffer logs = new ServerLogRingBuffer(2000);
    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();
    private final HttpClient forwardClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Set<String> registeredTools = new LinkedHashSet<>();

    private Server jettyServer;
    private ServerConnector connector;
    private McpSyncServer syncServer;

    public MCPBrokerServer(int port, MCPInstanceRegistry registry, String bearerToken) {
        this(port, registry, bearerToken, null);
    }

    /**
     * @param operationMetadata local operation definitions used for tool descriptions,
     *                          schemas, and read-only hints (nullable)
     */
    public MCPBrokerServer(int port, MCPInstanceRegistry registry, String bearerToken,
                           AutomationOperationRegistry operationMetadata) {
        this.port = port;
        this.registry = registry;
        this.bearerToken = bearerToken;
        this.operationMetadata = operationMetadata;
    }

    public void start() throws Exception {
        HttpServletStreamableServerTransportProvider transportProvider =
                HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(McpJsonDefaults.getMapper())
                        .mcpEndpoint("/mcp")
                        .build();
        syncServer = McpServer.sync(transportProvider)
                .serverInfo("jipipe", JIPipe.getJIPipeVersion())
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .jsonSchemaValidator(ACCEPT_ALL_VALIDATOR)
                .validateToolInputs(false)
                .build();
        rebuildTools();

        jettyServer = new Server();
        connector = new ServerConnector(jettyServer);
        connector.setHost("127.0.0.1");
        connector.setPort(port);
        jettyServer.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.addFilter(new FilterHolder(new BearerTokenFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));
        handler.addServlet(new ServletHolder(transportProvider), "/mcp");
        handler.addServlet(new ServletHolder(new HealthServlet()), "/health");
        handler.addServlet(new ServletHolder(new ControlServlet()), "/control/*");
        jettyServer.setHandler(handler);

        jettyServer.start();
        logs.add("info", "MCP broker listening on 127.0.0.1:" + getPort()
                + (bearerToken != null ? " (bearer token required)" : " (open access)"));
    }

    public void stop() {
        // Close the MCP server first, then Jetty: the SDK server owns the open sessions
        // and the tools/list change notifications, so it must shut down before its
        // transport servlet disappears. Idempotent and exception-safe: a close failure
        // is logged and must not prevent the Jetty stop.
        McpSyncServer mcpServer = this.syncServer;
        this.syncServer = null;
        if (mcpServer != null) {
            try {
                mcpServer.close();
            } catch (Exception e) {
                logs.add("warn", "MCP server close failed: " + describe(e));
            }
        }
        if (jettyServer != null) {
            try {
                jettyServer.stop();
            } catch (Exception e) {
                logs.add("warn", "Broker stop failed: " + describe(e));
            }
            jettyServer = null;
        }
    }

    /**
     * The actually bound port (differs from the constructor port when it was 0).
     */
    public int getPort() {
        return connector == null ? port : connector.getLocalPort();
    }

    public ServerLogRingBuffer getLogs() {
        return logs;
    }

    /**
     * Removes all previously registered tools and re-adds the aggregated tool set.
     * <p>
     * Registration-carried descriptors take precedence; the local operation metadata
     * registry is only consulted when no registration carries descriptors (legacy
     * {@code tool-ids}-only path). Mixed fleet: registrations without descriptors
     * contribute no tools while any descriptor-bearing registration exists.
     */
    private synchronized void rebuildTools() {
        for (String toolId : new ArrayList<>(registeredTools)) {
            syncServer.removeTool(toolId);
        }
        registeredTools.clear();
        List<MCPToolDescriptor> descriptors = registry.aggregateToolDescriptors();
        if (!descriptors.isEmpty()) {
            for (MCPToolDescriptor descriptor : descriptors) {
                syncServer.addTool(buildToolSpecification(descriptor));
                registeredTools.add(descriptor.getId());
            }
        } else if (operationMetadata != null) {
            for (String toolId : registry.aggregateToolIds()) {
                syncServer.addTool(buildToolSpecification(toolId));
                registeredTools.add(toolId);
            }
        }
        syncServer.notifyToolsListChanged();
        logs.add("info", "Tool list rebuilt: " + registeredTools.size() + " tool(s)");
    }

    private McpServerFeatures.SyncToolSpecification buildToolSpecification(MCPToolDescriptor descriptor) {
        AutomationToolScope scope = descriptor.getToolScope();
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder(descriptor.getId());
        toolBuilder.description(descriptor.getDescription() != null ? descriptor.getDescription()
                : fallbackDescription(descriptor.getId(), scope));
        toolBuilder.inputSchema(withProjectArgument(inputSchemaMapOf(descriptor.getInputSchema()), scope));
        toolBuilder.annotations(McpSchema.ToolAnnotations.builder()
                .readOnlyHint(descriptor.isReadOnly())
                .build());
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(toolBuilder.build())
                .callHandler((exchange, request) -> handleToolCall(descriptor.getId(), scope, exchange, request))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification buildToolSpecification(String operationId) {
        AutomationOperation operation = operationMetadata != null ? operationMetadata.get(operationId) : null;
        // Legacy path: tool-ids-only registrations carry no scope, so the scope comes from
        // the local operation metadata; an operation unknown to it defaults to PROJECT_BOUND.
        AutomationToolScope scope = operation != null ? operation.getToolScope() : AutomationToolScope.PROJECT_BOUND;
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder(operationId);
        if (operation != null) {
            toolBuilder.description(operation.getAgentDocumentation());
            toolBuilder.inputSchema(withProjectArgument(inputSchemaMapOf(operation.getInputSchema()), scope));
            toolBuilder.annotations(McpSchema.ToolAnnotations.builder()
                    .readOnlyHint(operation.isReadOnly())
                    .build());
        } else {
            toolBuilder.description(fallbackDescription(operationId, scope));
            toolBuilder.inputSchema(withProjectArgument(new LinkedHashMap<>(Map.of("type", "object")), scope));
        }
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(toolBuilder.build())
                .callHandler((exchange, request) -> handleToolCall(operationId, scope, exchange, request))
                .build();
    }

    /**
     * Adds the broker-routed {@code project} argument to the published input schema of every
     * {@link AutomationToolScope#PROJECT_BOUND} tool: the property is added into the schema's
     * {@code properties} object (created when the schema has none), so
     * {@code additionalProperties:false} keeps holding. Tools with any other declared scope
     * are returned unchanged: they are callable without a project.
     */
    private static Map<String, Object> withProjectArgument(Map<String, Object> schema, AutomationToolScope scope) {
        if (scope != AutomationToolScope.PROJECT_BOUND) {
            return schema;
        }
        Map<String, Object> result = new LinkedHashMap<>(schema);
        Map<String, Object> properties = new LinkedHashMap<>();
        if (result.get("properties") instanceof Map<?, ?> existing) {
            for (Map.Entry<?, ?> entry : existing.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    properties.put(key, entry.getValue());
                }
            }
        }
        properties.put("project", Map.of(
                "type", "string",
                "description", PROJECT_ARGUMENT_DESCRIPTION));
        result.put("properties", properties);
        return result;
    }

    private static String fallbackDescription(String operationId, AutomationToolScope scope) {
        if (scope != AutomationToolScope.PROJECT_BOUND) {
            return "Executes the operation '" + operationId + "' on a JIPipe instance "
                    + "(registry-global: callable without the 'project' argument).";
        }
        return "Executes the operation '" + operationId
                + "' on a JIPipe instance. The optional 'project' argument selects the target as "
                + "'<instanceId>:<projectKey>'.";
    }

    private Map<String, Object> inputSchemaMapOf(JsonNode schema) {
        if (schema == null || schema.isNull() || !schema.isObject()) {
            return Map.of("type", "object");
        }
        return objectMapper.convertValue(schema, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
        });
    }

    private McpSchema.CallToolResult handleToolCall(String operationId, AutomationToolScope scope,
                                                    McpSyncServerExchange exchange,
                                                    McpSchema.CallToolRequest request) {
        try {
            notifyProgressStart(exchange, operationId, request);
            Map<String, Object> forwardedParams = request.arguments() == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(request.arguments());
            Object projectArgument = forwardedParams.remove("project");
            String projectId = projectArgument instanceof String text && !text.isBlank() ? text : null;
            MCPInstanceRegistration registration;
            if (projectId != null) {
                try {
                    registration = registry.findByProjectId(projectId);
                } catch (NoSuchElementException e) {
                    return toolError(operationId, projectId, e.getMessage());
                }
                if (registration == null) {
                    return toolError(operationId, projectId, "Unknown instance in project id '" + projectId
                            + "'. Available projects: " + availableProjectIds());
                }
            } else {
                // Missing or blank project: every scope is callable without one (E2E ruling
                // 2026-11-16, now driven by the declared tool scope). The aggregated scope is
                // answered by the broker itself; every other scope routes deterministically
                // to the most-recently registered live instance that carries the tool, else
                // the first one, and the instance resolves the target project window.
                if (scope == AutomationToolScope.INSTANCE_GLOBAL_AGGREGATED) {
                    // 2026-11-17 follow-up: not routed to one instance. The broker aggregates
                    // the project lists of every live instance that carries the tool and
                    // returns fully-qualified routable ids.
                    return aggregateListProjects();
                }
                registration = registry.findAnyByToolId(operationId);
                if (registration == null) {
                    return toolError(operationId, null, "No registered JIPipe instance carries the tool '"
                            + operationId + "'. Available projects: " + availableProjectIds());
                }
                projectId = registration.getInstanceId();
            }
            String projectKey = projectId.indexOf(':') >= 0 ? projectId.substring(projectId.indexOf(':') + 1) : null;
            return forward(operationId, projectId, projectKey, registration, forwardedParams);
        } catch (Exception e) {
            logs.add("warn", "Tool '" + operationId + "' call failed: " + describe(e));
            return errorResult(describe(e));
        }
    }

    /**
     * Emits the single start-of-dispatch progress notification when the incoming
     * {@code tools/call} request carries {@code _meta.progressToken} (progress 0 of 1,
     * message = operation id). A failing notification channel is logged and does not fail
     * the tool call itself.
     */
    private void notifyProgressStart(McpSyncServerExchange exchange, String operationId,
                                     McpSchema.CallToolRequest request) {
        Object progressToken = request.progressToken();
        if (progressToken == null) {
            return;
        }
        try {
            exchange.progressNotification(new McpSchema.ProgressNotification(progressToken, 0.0, 1.0, operationId));
        } catch (Exception e) {
            logs.add("warn", "Progress notification for tool '" + operationId + "' failed: " + describe(e));
        }
    }

    private McpSchema.CallToolResult forward(String operationId, String projectId, String projectKey,
                                             MCPInstanceRegistration registration,
                                             Map<String, Object> forwardedParams) throws JsonProcessingException {
        JsonNode envelope;
        try {
            envelope = postToInstance(operationId, projectId, registration, projectKey, forwardedParams);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "Failed to reach instance '" + registration.getInstanceId() + "': " + describe(e);
            logs.add("warn", "Tool '" + operationId + "' project '" + projectId + "': " + message);
            return errorResult(message);
        } catch (IOException e) {
            String message = e.getMessage() != null ? e.getMessage() : describe(e);
            logs.add("warn", "Tool '" + operationId + "' project '" + projectId + "': " + message);
            return errorResult(message);
        }
        // path(): a 200 + ok:true envelope without a "result" key serializes as "null"
        // text content instead of NPE-ing on the absent node.
        JsonNode result = envelope.path("result");
        JsonNode image = result.path("image-base64");
        JsonNode mime = result.path("mime");
        if (image.isTextual() && mime.isTextual()) {
            return McpSchema.CallToolResult.builder()
                    .addContent(new McpSchema.ImageContent(null, image.asText(), mime.asText()))
                    .isError(false)
                    .build();
        }
        return McpSchema.CallToolResult.builder()
                .addContent(new McpSchema.TextContent(objectMapper.writeValueAsString(result)))
                .isError(false)
                .build();
    }

    /**
     * Aggregates the project-free {@code list_projects} call across ALL live instances that
     * carry the tool (2026-11-17 follow-up: routing it to a single instance hid every other
     * instance's projects, and the returned instance-local ids were not routable by the
     * project-bound tools). Selected by the declared
     * {@link AutomationToolScope#INSTANCE_GLOBAL_AGGREGATED} scope of the tool, not by its
     * id. Each returned project entry's {@code id} (the instance-local
     * project key) is rewritten to the full {@code <instanceId>:<projectKey>} form owned by
     * the broker; identical full ids are deduplicated. An instance that does not answer
     * (unreachable, non-200, or error envelope) is skipped with a ring-buffer log entry; the
     * call fails only when no instance answers.
     */
    private McpSchema.CallToolResult aggregateListProjects() throws JsonProcessingException {
        List<MCPInstanceRegistration> carriers = new ArrayList<>();
        for (MCPInstanceRegistration registration : registry.getAll()) {
            if (MCPInstanceRegistry.carriesTool(registration, LIST_PROJECTS_TOOL_ID)) {
                carriers.add(registration);
            }
        }
        if (carriers.isEmpty()) {
            return toolError(LIST_PROJECTS_TOOL_ID, null, "No registered JIPipe instance carries the tool '"
                    + LIST_PROJECTS_TOOL_ID + "'. Available projects: " + availableProjectIds());
        }
        Map<String, JsonNode> projects = new LinkedHashMap<>();
        boolean anyAnswered = false;
        for (MCPInstanceRegistration registration : carriers) {
            String instanceId = registration.getInstanceId();
            JsonNode envelope;
            try {
                envelope = postToInstance(LIST_PROJECTS_TOOL_ID, null, registration, null, new LinkedHashMap<>());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return toolError(LIST_PROJECTS_TOOL_ID, null, "The '" + LIST_PROJECTS_TOOL_ID
                        + "' aggregation was interrupted while reaching instance '" + instanceId + "'");
            } catch (IOException e) {
                // The instance is unreachable or rejected the call: skip it and let the
                // remaining instances answer (already logged by postToInstance).
                logs.add("warn", "Tool '" + LIST_PROJECTS_TOOL_ID + "' instance '" + instanceId
                        + "' skipped: " + (e.getMessage() != null ? e.getMessage() : describe(e)));
                continue;
            }
            anyAnswered = true;
            for (JsonNode entry : envelope.path("result").path("projects")) {
                if (!entry.isObject()) {
                    continue;
                }
                ObjectNode rewritten = ((ObjectNode) entry).deepCopy();
                String localId = rewritten.path("id").asText(null);
                if (localId == null || localId.isBlank()) {
                    continue;
                }
                String fullId = instanceId + ":" + localId;
                rewritten.put("id", fullId);
                projects.putIfAbsent(fullId, rewritten);
            }
        }
        if (!anyAnswered) {
            return toolError(LIST_PROJECTS_TOOL_ID, null, "No registered JIPipe instance answered the '"
                    + LIST_PROJECTS_TOOL_ID + "' call. Available projects: " + availableProjectIds());
        }
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode merged = result.putArray("projects");
        for (JsonNode entry : projects.values()) {
            merged.add(entry);
        }
        logs.add("info", "Tool '" + LIST_PROJECTS_TOOL_ID + "' aggregated " + merged.size()
                + " project(s) from the live instances");
        return McpSchema.CallToolResult.builder()
                .addContent(new McpSchema.TextContent(objectMapper.writeValueAsString(result)))
                .isError(false)
                .build();
    }

    /**
     * Forwards one operation call to an instance endpoint and returns the parsed response
     * envelope of an {@code ok:true} answer. Any failure is turned into an {@link IOException}
     * whose message describes it (unreachable instance, non-200 status, or an
     * {@code ok:false} envelope) and is logged to the ring buffer, so callers decide between
     * failing the tool result ({@link #forward}) and skipping the instance
     * ({@link #aggregateListProjects}).
     *
     * @throws InterruptedException when the forward was interrupted (not wrapped)
     */
    private JsonNode postToInstance(String operationId, String projectId, MCPInstanceRegistration registration,
                                    String projectKey, Map<String, Object> forwardedParams)
            throws IOException, InterruptedException {
        String instanceId = registration.getInstanceId();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("project-key", projectKey);
        body.set("params", objectMapper.valueToTree(forwardedParams));
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                        + registration.getEndpointPort() + "/op/" + operationId))
                .header(SECRET_HEADER, registration.getEndpointSecret())
                .header("Content-Type", "application/json")
                .timeout(FORWARD_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body),
                        StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response;
        try {
            response = forwardClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            String message = "Failed to reach instance '" + instanceId + "': " + describe(e);
            logs.add("warn", "Tool '" + operationId + "' project '" + projectId + "': " + message);
            if (e instanceof InterruptedException) {
                throw (InterruptedException) e;
            }
            throw new IOException(message, e);
        }
        if (response.statusCode() != 200) {
            String message = "Instance '" + instanceId + "' returned HTTP " + response.statusCode()
                    + " for operation '" + operationId + "'";
            logs.add("warn", "Tool '" + operationId + "' project '" + projectId + "': " + message);
            throw new IOException(message);
        }
        JsonNode envelope = objectMapper.readTree(response.body());
        if (!envelope.path("ok").asBoolean(false)) {
            String message = "Instance '" + instanceId + "' rejected operation '" + operationId + "': "
                    + envelope.path("error").asText("unknown error");
            logs.add("warn", "Tool '" + operationId + "' project '" + projectId + "': " + message);
            throw new IOException(message);
        }
        logs.add("info", "Tool '" + operationId + "' project '" + projectId + "' -> instance '"
                + instanceId + "' ok");
        return envelope;
    }

    private McpSchema.CallToolResult toolError(String operationId, Object projectId, String message) {
        logs.add("warn", "Tool '" + operationId + "' project '" + projectId + "' routing failed: " + message);
        return errorResult(message);
    }

    private static McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .addContent(new McpSchema.TextContent(message))
                .isError(true)
                .build();
    }

    private String availableProjectIds() {
        List<String> ids = new ArrayList<>();
        for (MCPInstanceRegistration registration : registry.getAll()) {
            for (var project : registration.getProjects()) {
                ids.add(registration.getInstanceId() + ":" + project.getKey());
            }
        }
        return String.join(", ", ids);
    }

    private static String describe(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getName();
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Rejects requests without a valid bearer token on {@code /mcp} and {@code /control/*}
     * before any processing; {@code /health} and all other paths pass through.
     * <p>
     * Matching uses {@link HttpServletRequest#getServletPath()}, not the raw request URI:
     * under the {@code "/"} context both are identical, but the servlet path stays correct
     * if the context path ever changes.
     */
    private class BearerTokenFilter implements Filter {

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
                throws IOException, ServletException {
            if (bearerToken != null) {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                String path = request.getServletPath();
                if (path.equals("/mcp") || path.startsWith("/mcp/") || path.startsWith("/control")) {
                    HttpServletResponse response = (HttpServletResponse) servletResponse;
                    String header = request.getHeader("Authorization");
                    if (header == null || !header.startsWith("Bearer ")
                            || !constantTimeEquals(bearerToken, header.substring("Bearer ".length()))) {
                        response.sendError(401);
                        return;
                    }
                }
            }
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    private static class HealthServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/plain; charset=utf-8");
            response.setStatus(200);
            response.getWriter().write("ok");
        }
    }

    private class ControlServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            if (request.getRequestURI().equals("/control/logs")) {
                respondJson(response, 200, objectMapper.valueToTree(logs.getAll()));
                return;
            }
            respondJson(response, 404, objectMapper.createObjectNode().put("error", "Unknown control route"));
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            byte[] body = request.getInputStream().readAllBytes();
            if (request.getRequestURI().equals("/control/register")) {
                MCPInstanceRegistration registration;
                try {
                    registration = objectMapper.readValue(body, MCPInstanceRegistration.class);
                } catch (IOException e) {
                    respondJson(response, 400, objectMapper.createObjectNode()
                            .put("error", "Invalid registration payload: " + describe(e)));
                    return;
                }
                if (registration.getInstanceId() == null || registration.getInstanceId().isBlank()) {
                    respondJson(response, 400, objectMapper.createObjectNode()
                            .put("error", "Registration payload requires an instance-id"));
                    return;
                }
                registry.register(registration);
                rebuildTools();
                logs.add("info", "Registered instance '" + registration.getInstanceId() + "' ("
                        + registration.getProjects().size() + " project(s))");
                respondText(response, 200, "ok");
                return;
            }
            if (request.getRequestURI().equals("/control/unregister")) {
                JsonNode payload = objectMapper.readTree(body);
                String instanceId = payload.path("instance-id").asText(null);
                if (instanceId == null || instanceId.isBlank()) {
                    respondJson(response, 400, objectMapper.createObjectNode()
                            .put("error", "Unregister payload requires an instance-id"));
                    return;
                }
                registry.unregister(instanceId);
                rebuildTools();
                logs.add("info", "Unregistered instance '" + instanceId + "'");
                respondText(response, 200, "ok");
                return;
            }
            respondJson(response, 404, objectMapper.createObjectNode().put("error", "Unknown control route"));
        }

        private void respondText(HttpServletResponse response, int status, String text) throws IOException {
            response.setContentType("text/plain; charset=utf-8");
            response.setStatus(status);
            response.getWriter().write(text);
        }

        private void respondJson(HttpServletResponse response, int status, JsonNode body) throws IOException {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            response.setContentType("application/json");
            response.setStatus(status);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        }
    }
}
