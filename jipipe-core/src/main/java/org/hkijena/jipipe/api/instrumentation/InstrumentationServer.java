package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.instrumentation.events.*;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class InstrumentationServer extends WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentationServer.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile JIPipeProject selectedProject;
    private volatile String selectedProjectId;
    private volatile InstrumentationEventBus eventBus;
    private volatile InstrumentationJobManager jobManager;
    private final List<Runnable> activeSubscriptions = new ArrayList<>();
    private final CountDownLatch startLatch = new CountDownLatch(1);

    public InstrumentationServer(int port) {
        super(new InetSocketAddress(
                System.getProperty("jipipe.instrumentation.bind", "127.0.0.1"), port));
        setDaemon(true);
    }

    public boolean awaitStart(long timeout, TimeUnit unit) throws InterruptedException {
        return startLatch.await(timeout, unit);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("Instrumentation client connected: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("Instrumentation client disconnected: {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonNode msg = mapper.readTree(message);
            String type = msg.has("type") ? msg.get("type").asText() : "";
            String requestId = msg.has("requestId") ? msg.get("requestId").asText() : null;

            if (InstrumentationProtocol.CMD_SELECT_PROJECT.equals(type)) {
                handleSelectProject(conn, msg, requestId);
            } else if (InstrumentationProtocol.CMD_GET_JOB_STATUS.equals(type)) {
                handleGetJobStatus(conn, msg, requestId);
            } else if (InstrumentationProtocol.CMD_CANCEL_JOB.equals(type)) {
                handleCancelJob(conn, msg, requestId);
            } else {
                handleOperation(conn, type, msg, requestId);
            }
        } catch (Exception e) {
            logger.error("Error processing message", e);
            sendError(conn, null, e.getMessage());
        }
    }

    private void handleSelectProject(WebSocket conn, JsonNode msg, String requestId) {
        String projectId = msg.has("projectId") ? msg.get("projectId").asText() : null;
        unsubscribeFromProject();

        JIPipeDesktopProjectWindow window = findWindow(projectId);
        if (window == null) {
            sendError(conn, requestId, "Project not found: " + projectId);
            return;
        }
        selectedProject = window.getProject();
        selectedProjectId = projectId;
        eventBus = new InstrumentationEventBus();
        jobManager = new InstrumentationJobManager(eventBus, projectId);
        subscribeToProject();

        ObjectNode response = mapper.createObjectNode();
        response.put("type", InstrumentationProtocol.EVENT_PROJECT_CHANGED);
        response.put("projectId", projectId);
        response.put("name", selectedProject.getMetadata().getName());
        if (requestId != null) response.put("requestId", requestId);
        broadcast(response.toString());
    }

    private void handleGetJobStatus(WebSocket conn, JsonNode msg, String requestId) {
        String jobId = msg.get("jobId").asText();
        ObjectNode response = mapper.createObjectNode();
        response.put("type", InstrumentationProtocol.EVENT_OPERATION_RESULT);
        if (requestId != null) response.put("requestId", requestId);
        if (jobManager != null) {
            InstrumentationJob job = jobManager.getJob(jobId);
            if (job != null) {
                ObjectNode data = response.putObject("data");
                data.put("jobId", job.getId());
                data.put("status", job.getStatus().name());
                data.put("progress", job.getProgress());
                data.put("operation", job.getOperation());
                if (job.getError() != null) data.put("error", job.getError());
            } else {
                response.put("error", "Job not found: " + jobId);
            }
        } else {
            response.put("error", "No project selected");
        }
        conn.send(response.toString());
    }

    private void handleCancelJob(WebSocket conn, JsonNode msg, String requestId) {
        String jobId = msg.get("jobId").asText();
        ObjectNode response = mapper.createObjectNode();
        response.put("type", InstrumentationProtocol.EVENT_OPERATION_RESULT);
        if (requestId != null) response.put("requestId", requestId);
        if (jobManager != null) {
            boolean cancelled = jobManager.cancelJob(jobId);
            response.put("data", mapper.createObjectNode().put("success", cancelled));
        } else {
            response.put("error", "No project selected");
        }
        conn.send(response.toString());
    }

    private void handleOperation(WebSocket conn, String type, JsonNode msg, String requestId) {
        var registry = JIPipe.getInstrumentation().getOperationRegistry();
        InstrumentationOperation op = registry.get(type);
        if (op == null) {
            sendError(conn, requestId, "Unknown command: " + type);
            return;
        }
        if (selectedProject == null && !isProjectManagementOp(type)) {
            sendError(conn, requestId, "No project selected. Call list_projects and select_project first.");
            return;
        }
        try {
            InstrumentationContext ctx = new InstrumentationContext(
                    selectedProject, selectedProjectId,
                    new JIPipeProgressInfo(),
                    eventBus != null ? eventBus : new InstrumentationEventBus(),
                    jobManager != null ? jobManager : new InstrumentationJobManager(new InstrumentationEventBus(), selectedProjectId));

            if (op instanceof AsyncInstrumentationOperation asyncOp) {
                InstrumentationJob job = asyncOp.executeAsync(ctx, msg);
                ObjectNode response = mapper.createObjectNode();
                response.put("type", InstrumentationProtocol.EVENT_JOB_STARTED);
                response.put("jobId", job.getId());
                response.put("operation", type);
                response.put("description", job.getDescription());
                if (requestId != null) response.put("requestId", requestId);
                conn.send(response.toString());
            } else {
                JsonNode result = op.execute(ctx, msg);
                ObjectNode response = mapper.createObjectNode();
                response.put("type", InstrumentationProtocol.EVENT_OPERATION_RESULT);
                if (requestId != null) response.put("requestId", requestId);
                response.set("data", result != null ? result : mapper.createObjectNode());
                conn.send(response.toString());
            }
        } catch (Exception e) {
            logger.error("Error executing operation: {}", type, e);
            sendError(conn, requestId, e.getMessage());
        }
    }

    private boolean isProjectManagementOp(String type) {
        return "list_projects".equals(type) || "list_operations".equals(type);
    }

    private void sendError(WebSocket conn, String requestId, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", InstrumentationProtocol.EVENT_ERROR);
        if (requestId != null) response.put("requestId", requestId);
        response.put("message", message);
        conn.send(response.toString());
    }

    private void subscribeToProject() {
        if (eventBus == null) return;
        subscribe(NodeAddedEvent.class, e -> broadcastEvent(InstrumentationProtocol.EVENT_NODE_ADDED, serializeEvent(e)));
        subscribe(NodeRemovedEvent.class, e -> broadcastEvent(InstrumentationProtocol.EVENT_NODE_REMOVED, serializeEvent(e)));
        subscribe(ConnectionChangedEvent.class, e -> broadcastEvent(InstrumentationProtocol.EVENT_CONNECTION_CHANGED, serializeEvent(e)));
        subscribe(ParameterChangedEvent.class, e -> broadcastEvent(InstrumentationProtocol.EVENT_PARAMETER_CHANGED, serializeEvent(e)));
        subscribe(CompartmentChangedEvent.class, e -> broadcastEvent(InstrumentationProtocol.EVENT_COMPARTMENT_CHANGED, serializeEvent(e)));
        subscribe(JobStartedEvent.class, e -> broadcastEvent(InstrumentationProtocol.EVENT_JOB_STARTED, serializeEvent(e)));
        subscribe(JobProgressEvent.class, e -> broadcastEvent(InstrumentationProtocol.EVENT_JOB_PROGRESS, serializeEvent(e)));
        subscribe(JobCompletedEvent.class, e -> broadcastEvent(InstrumentationProtocol.EVENT_JOB_COMPLETED, serializeEvent(e)));
    }

    private <T extends InstrumentationEvent> void subscribe(Class<T> type, Consumer<T> listener) {
        eventBus.subscribe(type, listener);
        activeSubscriptions.add(() -> {
        });
    }

    private void unsubscribeFromProject() {
        if (eventBus != null) {
            eventBus.clear();
        }
        activeSubscriptions.clear();
    }

    private JsonNode serializeEvent(InstrumentationEvent event) {
        return mapper.valueToTree(event);
    }

    private void broadcastEvent(String eventType, JsonNode event) {
        ObjectNode msg = mapper.createObjectNode();
        msg.put("type", eventType);
        msg.set("data", event);
        broadcast(msg.toString());
    }

    private JIPipeDesktopProjectWindow findWindow(String windowId) {
        for (var window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            if (window.getSessionId().toString().equals(windowId)) return window;
        }
        return null;
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("Instrumentation server error", ex);
    }

    @Override
    public void onStart() {
        logger.info("Instrumentation server started on port {}", getPort());
        startLatch.countDown();
    }

    public void onProjectWindowsChanged() {
        broadcastProjectList();
    }

    private void broadcastProjectList() {
        try {
            var registry = JIPipe.getInstrumentation().getOperationRegistry();
            InstrumentationOperation op = registry.get("list_projects");
            if (op != null) {
                JsonNode result = op.execute(null, mapper.createObjectNode());
                ObjectNode msg = mapper.createObjectNode();
                msg.put("type", InstrumentationProtocol.EVENT_PROJECT_LIST);
                msg.set("projects", result.get("projects"));
                broadcast(msg.toString());
            }
        } catch (Exception e) {
            logger.error("Error broadcasting project list", e);
        }
    }
}
