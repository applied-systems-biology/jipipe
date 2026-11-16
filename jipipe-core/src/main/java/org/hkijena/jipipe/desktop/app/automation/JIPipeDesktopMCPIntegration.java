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

package org.hkijena.jipipe.desktop.app.automation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.automation.ScreenshotSource;
import org.hkijena.jipipe.api.automation.endpoint.AutomationEndpointHandlers;
import org.hkijena.jipipe.api.automation.endpoint.AutomationInstanceEndpoint;
import org.hkijena.jipipe.api.automation.endpoint.MCPBrokerClient;
import org.hkijena.jipipe.api.automation.endpoint.MCPProjectEntry;
import org.hkijena.jipipe.api.automation.endpoint.ProjectSource;
import org.hkijena.jipipe.api.automation.operations.ScreenshotOperations;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.utils.FakeTitlebarPainter;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.ui.ScreenImage;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The desktop wiring of the MCP automation layer: the real
 * {@link AutomationEndpointHandlers} (project-key resolution, EDT marshalling, dispatch
 * through the global operation registry), the {@link AutomationInstanceEndpoint.AskPromptHandler}
 * behind the status bar ask bubble, the {@link AutomationInstanceEndpoint.OperationCallListener}
 * feeding the operation log and the status controls, and the production {@link ScreenshotSource}.
 * <p>
 * <b>Ownership seam:</b> the endpoint and the broker client are created and shut down by the
 * GUI launcher (GuiCommand, jipipe-launcher) on its MCP lifecycle executor. This class never
 * starts or stops them on its own: {@link #createEndpoint()} only builds the endpoint with the
 * real registry and this integration as handlers, and {@link #install(AutomationInstanceEndpoint,
 * MCPBrokerClient)} stores the references so the status bar control can pause and resume
 * through {@link #pauseAsync()} and {@link #resumeAsync()} (endpoint stop/start plus client
 * unregister/register, mirroring the launcher startup path).
 * <p>
 * <b>Threading:</b> {@link #execute}, {@link #ask} and {@link #onCall} run on the endpoint's
 * HTTP executor thread. Project resolution always happens on the event dispatch thread
 * ({@link JIPipeDesktopProjectWindow#getOpenWindows()} is EDT-confined); operations that touch
 * Swing (graph modifications, runs, screenshots, window iteration) are dispatched on the EDT
 * (see {@link #EDT_OPERATIONS}), all others run directly on the endpoint thread. Status events
 * are emitted from background threads; the controls marshal to the EDT.
 */
public final class JIPipeDesktopMCPIntegration implements AutomationEndpointHandlers,
        AutomationInstanceEndpoint.AskPromptHandler,
        AutomationInstanceEndpoint.OperationCallListener,
        ScreenshotSource {

    /**
     * Fired on every state change the status controls display. Static so subscriptions
     * survive install and uninstall cycles (pause/resume does not uninstall).
     */
    public static final MCPStatusEventEmitter STATUS_EVENTS = new MCPStatusEventEmitter();

    /**
     * Operations whose dispatch runs on the event dispatch thread: they mutate project state
     * whose change events synchronously drive Swing (the graph modifications and runs), they
     * render or capture live components (the screenshots), or they iterate the EDT-confined
     * open-window set (list_projects, create_ro_crate). Everything else runs directly on the
     * endpoint thread.
     */
    private static final Set<String> EDT_OPERATIONS = Set.of(
            "add_node", "remove_node", "add_connection", "remove_connection", "set_parameter",
            "set_project_metadata", "add_compartment", "rename_compartment", "arrange_nodes",
            "run_node", "run_compartment", "run_pipeline", "update_cache",
            "screenshot_canvas", "screenshot_minimap", "screenshot_ui", "screenshot_window",
            "list_projects", "create_ro_crate");

    /**
     * How long an unanswered Ask request waits before it is denied, so the waiting endpoint
     * thread can never block forever (a closed window, an overlooked bubble). Shutdown
     * denies pending requests immediately.
     */
    private static final int ASK_TIMEOUT_SECONDS = 300;

    /**
     * Pure gap around the rendered minimap content.
     */
    private static final int MINIMAP_MARGIN = 16;

    /**
     * The curated UI surface catalog of {@link #findUiSurface(String)}.
     */
    private static final Set<String> UI_SURFACES = Set.of(
            "graph-canvas", "graph-editor", "compartments-editor", "project-tabs", "settings-mcp");

    private static volatile JIPipeDesktopMCPIntegration active;
    private static volatile boolean starting;

    private final MCPOperationLog operationLog = new MCPOperationLog(500);
    private final ConcurrentLinkedDeque<PendingAsk> pendingAsks = new ConcurrentLinkedDeque<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final InstrumentationEventBus eventBus = new InstrumentationEventBus();
    private final InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, null);
    private final ExecutorService lifecycleExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "JIPipe-MCP-UI-Lifecycle");
        thread.setDaemon(true);
        return thread;
    });

    private volatile AutomationInstanceEndpoint endpoint;
    private volatile MCPBrokerClient client;
    private volatile boolean paused;
    private volatile String lastOperationId;

    private JIPipeDesktopMCPIntegration() {
    }

    // ── Lifecycle (owned by the launcher; see the class Javadoc) ──

    /**
     * The live integration, or null when the MCP layer is not running.
     */
    public static JIPipeDesktopMCPIntegration getActive() {
        return active;
    }

    /**
     * Returns the live integration, creating it when absent (the handlers must exist before
     * the endpoint is constructed).
     */
    public static synchronized JIPipeDesktopMCPIntegration getOrCreate() {
        JIPipeDesktopMCPIntegration integration = active;
        if (integration == null) {
            integration = new JIPipeDesktopMCPIntegration();
            active = integration;
        }
        return integration;
    }

    /**
     * Marks the layer as starting (the launcher submitted the startup task). Displayed
     * until {@link #install(AutomationInstanceEndpoint, MCPBrokerClient)} or {@link #markStopped()}.
     */
    public static void markStarting() {
        starting = true;
        emit();
    }

    /**
     * Marks the layer as stopped (a startup attempt failed or the layer shut down).
     */
    public static void markStopped() {
        starting = false;
        emit();
    }

    /**
     * Creates the instance endpoint with the global operation registry, the shared
     * permission service, and this integration as handlers, ask handler, and call listener.
     * Also pushes the persisted permission states into the live permission service, so a
     * fresh instance starts with the states the user chose in the settings.
     *
     * @throws java.io.IOException when the endpoint's server socket cannot be opened
     */
    public static AutomationInstanceEndpoint createEndpoint() throws java.io.IOException {
        applyPersistedPermissions();
        JIPipeDesktopMCPIntegration integration = getOrCreate();
        return new AutomationInstanceEndpoint(JIPipe.getAutomation().getOperationRegistry(),
                JIPipe.getAutomation().getPermissionService(), integration, integration, integration);
    }

    /**
     * Stores the endpoint and client (created by the launcher) and activates the screenshot
     * source and the status display. Called once per startup, after the endpoint is built
     * and before the broker connection.
     */
    public static void install(AutomationInstanceEndpoint endpoint, MCPBrokerClient client) {
        JIPipeDesktopMCPIntegration integration = getOrCreate();
        integration.endpoint = endpoint;
        integration.client = client;
        integration.paused = false;
        starting = false;
        ScreenshotOperations.setSource(integration);
        emit();
    }

    /**
     * Deactivates the integration: the screenshot source is removed, pending Ask requests
     * are denied (their waiting endpoint threads must never block on a dead layer), and the
     * status falls back to off. Idempotent.
     */
    public static void uninstall() {
        JIPipeDesktopMCPIntegration integration = active;
        active = null;
        starting = false;
        ScreenshotOperations.clearSource();
        if (integration != null) {
            integration.denyAllPending();
            integration.endpoint = null;
            integration.client = null;
            integration.paused = false;
            integration.lastOperationId = null;
            integration.inFlight.set(0);
        }
        emit();
    }

    private static void emit() {
        STATUS_EVENTS.emit(new MCPStatusEvent(active));
    }

    /**
     * Pushes the persisted permission states (MCPApplicationSettings) into the live
     * permission service. The settings table applies changes live while it is open; this
     * covers the persisted states at layer start. CLI overrides stay more significant: the
     * resolution order is grant, then override, then user state.
     */
    private static void applyPersistedPermissions() {
        try {
            MCPApplicationSettings settings = MCPApplicationSettings.getInstance();
            if (settings == null) {
                return;
            }
            AutomationPermissionService service = JIPipe.getAutomation().getPermissionService();
            for (Map.Entry<String, String> entry : settings.getPermissions().asMap().entrySet()) {
                try {
                    service.setUserState(entry.getKey(), AutomationPermissionState.valueOf(entry.getValue()));
                } catch (IllegalArgumentException e) {
                    System.err.println("MCP persisted permission has unknown state '" + entry.getValue()
                            + "' for operation '" + entry.getKey() + "'");
                }
            }
        } catch (Exception e) {
            System.err.println("MCP persisted permissions unavailable: " + e.getMessage());
        }
    }

    // ── Status display (read by the status controls) ──

    /**
     * The lifecycle state shown by the status controls.
     */
    public enum LayerState {
        Off, Starting, Running, Paused
    }

    /**
     * A consistent view of the status display values.
     */
    public record StatusSnapshot(LayerState state, String lastOperationId, int inFlight, int pendingAsks) {
    }

    /**
     * The current lifecycle state.
     */
    public static LayerState currentLayerState() {
        if (starting) {
            return LayerState.Starting;
        }
        JIPipeDesktopMCPIntegration integration = active;
        if (integration == null) {
            return LayerState.Off;
        }
        return integration.paused ? LayerState.Paused : LayerState.Running;
    }

    /**
     * A consistent snapshot of the status display values.
     */
    public static StatusSnapshot currentSnapshot() {
        JIPipeDesktopMCPIntegration integration = active;
        if (integration == null) {
            return new StatusSnapshot(starting ? LayerState.Starting : LayerState.Off, null, 0, 0);
        }
        return new StatusSnapshot(currentLayerState(), integration.lastOperationId,
                integration.inFlight.get(), integration.pendingAsks.size());
    }

    /**
     * The broker client, or null when not installed (the controls use it for the server URL
     * and the token).
     */
    public MCPBrokerClient getClient() {
        return client;
    }

    /**
     * The instance-side operation log.
     */
    public MCPOperationLog getOperationLog() {
        return operationLog;
    }

    // ── AutomationEndpointHandlers ──

    @Override
    public JsonNode execute(String operationId, String projectKey, JsonNode params) throws Exception {
        lastOperationId = operationId;
        inFlight.incrementAndGet();
        try {
            JIPipeDesktopProjectWindow window = findProjectWindow(projectKey);
            AutomationContext context = new AutomationContext(window != null ? window.getProject() : null,
                    projectKey, new JIPipeProgressInfo(), eventBus, jobManager);
            if (EDT_OPERATIONS.contains(operationId)) {
                return callOnEdt(() -> JIPipe.getAutomation().getOperationRegistry()
                        .execute(operationId, context, params));
            }
            return JIPipe.getAutomation().getOperationRegistry().execute(operationId, context, params);
        } finally {
            emit();
        }
    }

    @Override
    public boolean isRegistered(String operationId) {
        return JIPipe.getAutomation().getOperationRegistry().has(operationId);
    }

    @Override
    public boolean isHidden(String operationId) {
        AutomationOperation operation = JIPipe.getAutomation().getOperationRegistry().get(operationId);
        return operation != null && JIPipe.getAutomation().getPermissionService()
                .resolve(operation, null) == AutomationPermissionState.Hide;
    }

    /**
     * Resolves the project window behind the project key: exactly the mapping the
     * registration announces ({@link ProjectSource#openWindows()}, key = window session id).
     * Runs on the EDT. Returns null for an unknown or stale key (the project-bound
     * operations then report "No project loaded").
     */
    private JIPipeDesktopProjectWindow findProjectWindow(String projectKey) throws Exception {
        return callOnEdt(() -> findProjectWindowOnEdt(projectKey));
    }

    private static JIPipeDesktopProjectWindow findProjectWindowOnEdt(String projectKey) {
        if (projectKey == null) {
            return null;
        }
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            if (window.getSessionId().toString().equals(projectKey)) {
                return window;
            }
        }
        return null;
    }

    // ── Ask prompt handling (the status bar bubble) ──

    @Override
    public boolean ask(String operationId, String projectKey) {
        PendingAsk pending = new PendingAsk(operationId, projectKey);
        pendingAsks.addLast(pending);
        emit();
        try {
            if (!pending.latch.await(ASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                // Timed out or interrupted: deny, so the endpoint thread never blocks forever.
                pending.answer(false, false);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pending.answer(false, false);
        } finally {
            pendingAsks.remove(pending);
            emit();
        }
        if (pending.isAllowed() && pending.isGrantAlways()) {
            JIPipe.getAutomation().getPermissionService().grantAlways(operationId, projectKey);
        }
        return pending.isAllowed();
    }

    /**
     * The queued Ask requests, oldest first.
     */
    public List<PendingAsk> getPendingAsks() {
        return new ArrayList<>(pendingAsks);
    }

    /**
     * Answers one Ask request. The first answer wins; later answers (from another window's
     * bubble) are no-ops. Safe on the EDT.
     */
    public void answerAsk(PendingAsk ask, boolean allow, boolean grantAlways) {
        if (ask == null) {
            return;
        }
        ask.answer(allow, grantAlways);
        emit();
    }

    /**
     * Denies all pending Ask requests (used on uninstall).
     */
    private void denyAllPending() {
        for (PendingAsk ask : pendingAsks) {
            ask.answer(false, false);
        }
        pendingAsks.clear();
    }

    /**
     * One pending "ask before running" request. The endpoint thread blocks on the latch
     * until a bubble answers it, the request times out, or the layer shuts down.
     */
    public static final class PendingAsk {

        private final String operationId;
        private final String projectKey;
        private final long createdAt = System.currentTimeMillis();
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean answered = new AtomicBoolean();
        private volatile boolean allowed;
        private volatile boolean grantAlways;

        private PendingAsk(String operationId, String projectKey) {
            this.operationId = operationId;
            this.projectKey = projectKey;
        }

        /**
         * Applies the answer; only the first one counts.
         */
        private boolean answer(boolean allow, boolean grantAlwaysState) {
            if (answered.compareAndSet(false, true)) {
                this.allowed = allow;
                this.grantAlways = allow && grantAlwaysState;
                latch.countDown();
                return true;
            }
            return false;
        }

        public String getOperationId() {
            return operationId;
        }

        public String getProjectKey() {
            return projectKey;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public boolean isGrantAlways() {
            return grantAlways;
        }

        public boolean isAnswered() {
            return answered.get();
        }
    }

    // ── OperationCallListener ──

    @Override
    public void onCall(String operationId, String projectKey, String outcome, long durationMs) {
        // execute() incremented for calls that reached dispatch; the clamp keeps the
        // counter non-negative for outcomes that never dispatch (unauthorized, unknown).
        inFlight.updateAndGet(value -> Math.max(0, value - 1));
        operationLog.add(new MCPOperationLog.Entry(operationId, projectKey, outcome, durationMs,
                System.currentTimeMillis()));
        if (!"unauthorized".equals(outcome)) {
            lastOperationId = operationId;
        }
        emit();
    }

    /**
     * The instance-side operation log: a bounded ring of one record per dispatch attempt.
     */
    public static final class MCPOperationLog {

        /**
         * One record: what ran (or was refused), for which project, with which outcome.
         */
        public record Entry(String operationId, String projectKey, String outcome, long durationMs, long timestamp) {
        }

        private final ArrayDeque<Entry> entries;
        private final int capacity;

        public MCPOperationLog(int capacity) {
            this.capacity = capacity;
            this.entries = new ArrayDeque<>(capacity);
        }

        public synchronized void add(Entry entry) {
            if (entries.size() >= capacity) {
                entries.removeFirst();
            }
            entries.addLast(entry);
        }

        public synchronized List<Entry> getEntries() {
            return new ArrayList<>(entries);
        }

        public synchronized void clear() {
            entries.clear();
        }
    }

    // ── Pause / resume (status bar popup) ──

    public boolean isPaused() {
        return paused;
    }

    /**
     * Pauses the layer: unregister from the broker and stop the endpoint. The broker
     * process itself stays alive. Runs on the integration's lifecycle executor.
     */
    public void pauseAsync() {
        lifecycleExecutor.execute(() -> {
            try {
                MCPBrokerClient brokerClient = client;
                AutomationInstanceEndpoint instanceEndpoint = endpoint;
                if (brokerClient != null) {
                    brokerClient.unregister();
                }
                if (instanceEndpoint != null) {
                    instanceEndpoint.stop();
                }
                paused = true;
                System.err.println("MCP paused: instance unregistered and endpoint stopped");
            } catch (Exception e) {
                System.err.println("MCP pause failed: " + e.getMessage());
            } finally {
                emit();
            }
        });
    }

    /**
     * Resumes the layer: start the endpoint, ensure the broker connection, register, and
     * push the fresh window list (mirroring the launcher startup path). Runs on the
     * integration's lifecycle executor.
     */
    public void resumeAsync() {
        lifecycleExecutor.execute(() -> {
            try {
                AutomationInstanceEndpoint instanceEndpoint = endpoint;
                MCPBrokerClient brokerClient = client;
                if (instanceEndpoint != null) {
                    instanceEndpoint.start();
                }
                if (brokerClient != null) {
                    brokerClient.ensureConnected(new JIPipeProgressInfo());
                    brokerClient.register();
                    // register() carries the startup snapshot; push the fresh window list
                    // right after (the window set is EDT-confined).
                    List<MCPProjectEntry> projects = callOnEdt(ProjectSource.openWindows()::getProjects);
                    brokerClient.updateProjects(projects);
                }
                paused = false;
                System.err.println("MCP resumed: endpoint started and instance registered");
            } catch (Exception e) {
                System.err.println("MCP resume failed: " + e.getMessage());
            } finally {
                emit();
            }
        });
    }

    // ── ScreenshotSource ──

    @Override
    public BufferedImage renderCanvas(String compartmentKey, double scale) {
        return callOnEdt(() -> {
            JIPipeDesktopGraphCanvasUI canvas = findCanvas(compartmentKey);
            if (canvas == null) {
                return null;
            }
            return renderComponentScaled(canvas, scale);
        });
    }

    @Override
    public BufferedImage renderMinimap(String compartmentKey, double scale) {
        return callOnEdt(() -> {
            JIPipeDesktopGraphCanvasUI canvas = findCanvas(compartmentKey);
            if (canvas == null) {
                return null;
            }
            Rectangle bounds = new Rectangle();
            for (JIPipeDesktopGraphNodeUI nodeUI : canvas.getNodeUIs().values()) {
                bounds.add(nodeUI.getBounds());
            }
            if (bounds.isEmpty()) {
                return null;
            }
            int width = Math.max((int) Math.round(bounds.width * scale) + 2 * MINIMAP_MARGIN, 1);
            int height = Math.max((int) Math.round(bounds.height * scale) + 2 * MINIMAP_MARGIN, 1);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(canvas.getBackground());
                graphics.fillRect(0, 0, width, height);
                int viewX = MINIMAP_MARGIN - (int) Math.round(bounds.x * scale);
                int viewY = MINIMAP_MARGIN - (int) Math.round(bounds.y * scale);
                canvas.getPaintManager().paintMiniMap(graphics, scale, viewX, viewY);
            } finally {
                graphics.dispose();
            }
            return image;
        });
    }

    @Override
    public Component findUiSurface(String surfaceId) {
        if (!UI_SURFACES.contains(surfaceId)) {
            return null;
        }
        return callOnEdt(() -> findUiSurfaceOnEdt(surfaceId));
    }

    /**
     * The curated catalog (v1): the first open pipeline editor's canvas or editor, the
     * compartments editor, the document tab pane, and a freshly constructed MCP settings
     * form (never shown, so the operation realizes it off-screen).
     */
    private static Component findUiSurfaceOnEdt(String surfaceId) {
        JIPipeDesktopProjectWorkbench workbench = currentWorkbench();
        if (workbench == null) {
            return null;
        }
        switch (surfaceId) {
            case "graph-canvas": {
                JIPipeDesktopPipelineGraphEditorUI editor = firstPipelineEditor(workbench);
                return editor != null ? editor.getCanvasUI() : null;
            }
            case "graph-editor": {
                return firstPipelineEditor(workbench);
            }
            case "compartments-editor": {
                for (JIPipeDesktopTabPane.DocumentTab tab : workbench.getDocumentTabPane().getTabs()) {
                    if (tab.getContent() instanceof JIPipeDesktopCompartmentsGraphEditorUI) {
                        return (Component) tab.getContent();
                    }
                }
                return null;
            }
            case "project-tabs": {
                return workbench.getDocumentTabPane();
            }
            case "settings-mcp": {
                return new JIPipeDesktopParameterFormPanel(workbench, MCPApplicationSettings.getInstance(),
                        MarkdownText.fromPluginResource("documentation/application-settings.md", new HashMap<>()),
                        JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION);
            }
            default:
                return null;
        }
    }

    private static JIPipeDesktopPipelineGraphEditorUI firstPipelineEditor(JIPipeDesktopProjectWorkbench workbench) {
        for (JIPipeDesktopTabPane.DocumentTab tab : workbench.getDocumentTabPane().getTabs()) {
            if (tab.getContent() instanceof JIPipeDesktopPipelineGraphEditorUI) {
                return (JIPipeDesktopPipelineGraphEditorUI) tab.getContent();
            }
        }
        return null;
    }

    /**
     * The workbench whose surfaces are shown: the active (focused) project window, or the
     * first open one. Must run on the EDT.
     */
    private static JIPipeDesktopProjectWorkbench currentWorkbench() {
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow != null) {
            JIPipeDesktopProjectWindow projectWindow = findProjectWindowFor(activeWindow);
            if (projectWindow != null) {
                return projectWindow.getProjectWorkbench();
            }
        }
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            return window.getProjectWorkbench();
        }
        return null;
    }

    private static JIPipeDesktopProjectWindow findProjectWindowFor(Window window) {
        for (Window current = window; current != null; current = current.getOwner()) {
            if (current instanceof JIPipeDesktopProjectWindow) {
                return (JIPipeDesktopProjectWindow) current;
            }
        }
        return null;
    }

    @Override
    public BufferedImage captureWindow(String projectKey, boolean titlebar) {
        return callOnEdt(() -> {
            JIPipeDesktopProjectWindow window = findProjectWindowOnEdt(projectKey);
            if (window == null) {
                return null;
            }
            BufferedImage capture;
            if (window.isShowing()) {
                try {
                    // Visible window: screen capture (the Robot-based ScreenImage variant).
                    capture = ScreenImage.createImage((Component) window);
                } catch (Exception e) {
                    // No screen access (e.g. headless or minimized): fall back to the
                    // print-based capture of the content pane.
                    capture = printCapture(window.getContentPane());
                }
            } else {
                // Off-screen realization: print the content pane in place (no reparenting,
                // no screen interaction).
                capture = printCapture(window.getContentPane());
            }
            if (capture == null) {
                return null;
            }
            if (!titlebar) {
                return capture;
            }
            return composeFakeTitlebar(capture, window.getTitle());
        });
    }

    /**
     * Print-based capture of a container (works for never-shown components; the component
     * stays attached to its hierarchy).
     */
    private static BufferedImage printCapture(Container container) {
        if (!(container instanceof JComponent)) {
            return null;
        }
        try {
            return ScreenImage.createImage((JComponent) container);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Composes the fake titlebar (KDE 6 Breeze style) on top of the capture. The bar
     * geometry comes from {@link FakeTitlebarPainter#layout(String, FontMetrics, int)} and
     * is therefore derived from the font metrics, never hardcoded.
     */
    private static BufferedImage composeFakeTitlebar(BufferedImage capture, String title) {
        Font font = UIManager.getDefaults().getFont("Label.font");
        if (font == null) {
            font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D scratchGraphics = scratch.createGraphics();
        scratchGraphics.setFont(font);
        FontMetrics metrics = scratchGraphics.getFontMetrics();
        scratchGraphics.dispose();

        FakeTitlebarPainter.FakeTitlebarLayout layout = FakeTitlebarPainter.layout(title, metrics, capture.getWidth());
        BufferedImage result = new BufferedImage(capture.getWidth(), capture.getHeight() + layout.barHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
            graphics.drawImage(capture, 0, layout.barHeight(), null);
            graphics.setFont(font);
            FakeTitlebarPainter.paint(graphics, layout, title);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    /**
     * The canvas of the compartment's open pipeline editor tab, or null.
     */
    private static JIPipeDesktopGraphCanvasUI findCanvas(String compartmentKey) {
        UUID compartmentUUID;
        try {
            compartmentUUID = UUID.fromString(compartmentKey);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();
            if (workbench == null) {
                continue;
            }
            for (JIPipeDesktopPipelineGraphEditorUI editor : workbench.findOpenPipelineEditorTabs(compartmentUUID)) {
                return editor.getCanvasUI();
            }
        }
        return null;
    }

    /**
     * Renders a component into an image at the given scale: the component is printed into
     * a scaled graphics context, so the result is a native (vector-sharp) render, not an
     * upscaled bitmap. Must run on the EDT.
     */
    private static BufferedImage renderComponentScaled(JComponent component, double scale) {
        int width = component.getWidth();
        int height = component.getHeight();
        if (width <= 0 || height <= 0) {
            Dimension preferred = component.getPreferredSize();
            width = Math.max(preferred.width, 1);
            height = Math.max(preferred.height, 1);
        }
        double clampedScale = Math.max(scale, Double.MIN_VALUE);
        BufferedImage image = new BufferedImage(
                (int) Math.round(width * clampedScale), (int) Math.round(height * clampedScale),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(component.getBackground());
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.scale(clampedScale, clampedScale);
            component.printAll(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    // ── EDT helpers ──

    /**
     * Runs the callable on the event dispatch thread and returns its result. Directly on
     * the EDT, otherwise via {@link SwingUtilities#invokeAndWait} (the endpoint and
     * lifecycle threads never hold a lock the EDT waits on). A {@link RuntimeException}
     * from the callable (an {@link IllegalArgumentException} from the registry's validating
     * execute among them) is rethrown unchanged, so the endpoint's error mapping sees the
     * original type.
     */
    private static <T> T callOnEdt(Callable<T> callable) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return callable.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        final Object[] result = new Object[1];
        final Exception[] failure = new Exception[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = callable.call();
                } catch (Exception e) {
                    failure[0] = e;
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the event dispatch thread", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
        }
        if (failure[0] != null) {
            if (failure[0] instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(failure[0].getMessage(), failure[0]);
        }
        //noinspection unchecked
        return (T) result[0];
    }
}
