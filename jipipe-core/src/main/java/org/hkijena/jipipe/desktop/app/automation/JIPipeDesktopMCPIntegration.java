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
import org.hkijena.jipipe.api.ai.llm.activity.LLMActivityPhase;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationProgressHandles;
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
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
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
import org.hkijena.jipipe.utils.ui.ScreenImageSVG;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The desktop wiring of the MCP automation layer: the real
 * {@link AutomationEndpointHandlers} (project-key resolution, EDT marshalling, dispatch
 * through the global operation registry), the {@link AutomationInstanceEndpoint.AskPromptHandler}
 * whose asks are presented by the pluggable {@link MCPAskPresenter} (the default presents
 * them as the status bar ask balloon; see {@link #setAskPresenter}), the
 * {@link AutomationInstanceEndpoint.OperationCallListener}
 * feeding the operation log and the status controls, and the production {@link ScreenshotSource}.
 * <p>
 * <b>Ownership seam:</b> the endpoint and the broker client are created and shut down by the
 * GUI launcher (GuiCommand, jipipe-launcher) on its MCP lifecycle executor. This class never
 * starts or stops them on its own: {@link #createEndpoint()} only builds the endpoint with the
 * real registry and this integration as handlers, and {@link #install(AutomationInstanceEndpoint,
 * MCPBrokerClient)} stores the references so the status bar control can pause and resume
 * through {@link #pauseAsync()} and {@link #resumeAsync()} (endpoint recreation plus client
 * unregister/register, mirroring the launcher startup path). A paused endpoint cannot simply be
 * restarted: a JDK {@code HttpServer} forbids start-after-stop, so every resume builds a fresh
 * endpoint with the same wiring ({@link #getEndpoint()} always returns the current one, and the
 * launcher's shutdown stops the current endpoint through it).
 * <p>
 * <b>Threading:</b> {@link #execute}, {@link #ask} and {@link #onCall} run on the endpoint's
 * HTTP executor thread. Project resolution always happens on the event dispatch thread
 * ({@link JIPipeDesktopProjectWindow#getOpenWindows()} is EDT-confined); operations that touch
 * Swing without bridging it themselves (graph modifications, runs, window iteration) are
 * dispatched on the EDT (each operation declares this via
 * {@link AutomationOperation#requiresEdt()}), all others run
 * directly on the endpoint thread. The screenshot window/ui operations dispatch off the EDT
 * and bridge their Swing phases themselves ({@link #findUiSurface(String)},
 * {@link #captureWindow(String, boolean)}); the Robot-based screen capture never runs on the
 * event dispatch thread: on Wayland/XWayland hosts it goes through the xdg-desktop-portal
 * screencast, which can block indefinitely (no exception, no timeout), so running it on the
 * EDT freezes the whole application (observed live). It therefore runs on
 * {@link #captureExecutor} with the wait bounded at {@link #CAPTURE_TIMEOUT_SECONDS}; every
 * Swing state read and every print-based capture stays on the EDT. Status events are emitted
 * from background threads; the controls marshal to the EDT.
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
     * Core-side hook for the MCP lifecycle: starts and stops the whole layer (broker
     * client, endpoint, registration) and reconnects the broker connection after a token
     * rotation (the layer keeps running). Installed by the GUI launcher at startup
     * because the lifecycle executor and the broker client live there; the AI monitor's
     * start and stop buttons, the token rotation, and the "Enable MCP" settings toggle
     * call through it. All implementations must be non-blocking: the work runs on
     * background executors.
     */
    public interface LifecycleHooks {

        /**
         * Starts the MCP layer (a no-op when it already runs). Never blocks the caller.
         */
        void startMcpAsync();

        /**
         * Stops the MCP layer with pause semantics (unregister and endpoint stop; the
         * broker stays alive for other instances). Never blocks the caller.
         */
        void stopMcpAsync();

        /**
         * Re-establishes the broker connection without stopping the layer (used after
         * token rotation: the connection state is invalidated, so the next connect
         * replaces the old-token broker). A stopped layer is started. Never blocks the
         * caller.
         */
        void reconnectMcpAsync();
    }

    /**
     * The installed lifecycle hooks, or null when no launcher owns the lifecycle.
     */
    private static volatile LifecycleHooks lifecycleHooks;

    /**
     * Installs the lifecycle hooks (called once by the GUI launcher at startup).
     */
    public static void setLifecycleHooks(LifecycleHooks hooks) {
        lifecycleHooks = hooks;
    }

    /**
     * The installed lifecycle hooks, or null when no launcher owns the lifecycle
     * (start/stop controls then have nothing to call and must stay disabled).
     */
    public static LifecycleHooks getLifecycleHooks() {
        return lifecycleHooks;
    }

    /**
     * How long an unanswered Ask request waits before it is denied, so the waiting endpoint
     * thread can never block forever (a closed window, an overlooked bubble). Shutdown
     * denies pending requests immediately.
     * <p>
     * Deviation from the original design: the design specified an unbounded wait; the wait is
     * bounded at 300 seconds so a paused or unattended UI cannot hold endpoint threads forever,
     * and the agent receives a denied outcome after the timeout.
     * <p>
     * Invariant: the broker's forward timeout (MCPBrokerServer.FORWARD_TIMEOUT, 360 s) must
     * exceed this Ask bound plus a margin, so a late bubble answer is still delivered to the
     * agent instead of dying on a broker transport timeout. The relationship is documented on
     * both constants; the broker deliberately does not depend on this desktop class in code.
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

    /**
     * How long the Robot-based screen capture may take before it is abandoned (treated as
     * "no capture" and answered by the print fallback). On Wayland/XWayland hosts the JDK's
     * Robot capture goes through the xdg-desktop-portal screencast, which can block
     * indefinitely and without an exception; the bound turns that hang into a degraded
     * capture instead of a frozen application.
     */
    private static final long CAPTURE_TIMEOUT_SECONDS = 15;

    private static final AtomicInteger captureThreadId = new AtomicInteger();

    /**
     * Executes the Robot-based screen captures off the event dispatch thread: a capture
     * blocked inside the native screencast portal would otherwise freeze the whole
     * application (observed live: the EDT stuck inside
     * {@code sun.awt.screencast.ScreencastHelper}). Daemon threads. A hung native capture
     * can pin its pool thread until the portal responds; the pool bound keeps that leak
     * bounded, and when both threads hang, further captures time out after
     * {@link #CAPTURE_TIMEOUT_SECONDS} and take the print fallback: the tool degrades, the
     * application never freezes.
     */
    private static final ExecutorService captureExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "jipipe-mcp-capture-" + captureThreadId.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private static volatile JIPipeDesktopMCPIntegration active;
    private static volatile boolean starting;

    private final MCPOperationLog operationLog = new MCPOperationLog(500);

    /**
     * Presents the asks (the presentation half of the ask flow, see {@link MCPAskPresenter}).
     * Defaults to the status bar balloon presentation; replaceable through
     * {@link #setAskPresenter} (the seam for the ACP agent-chat presentation, #1376).
     * Volatile: the presenter methods run on the endpoint's HTTP executor thread while the
     * setter can run anywhere.
     */
    private volatile MCPAskPresenter askPresenter = new DefaultAskPresenter();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final InstrumentationEventBus eventBus = new InstrumentationEventBus();
    private final InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, null);
    private final AutomationProgressHandles progressHandles = new AutomationProgressHandles();
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
     * fresh instance starts with the states the user chose in the settings. Resume reuses
     * the same wiring through {@link #newEndpoint()} (without the permission push, which
     * the settings table applies live).
     *
     * @throws java.io.IOException when the endpoint's server socket cannot be opened
     */
    public static AutomationInstanceEndpoint createEndpoint() throws java.io.IOException {
        applyPersistedPermissions();
        return getOrCreate().newEndpoint();
    }

    /**
     * Builds a fresh endpoint wired to the global operation registry, the shared permission
     * service, and this integration as handlers, ask handler, and call listener (the same
     * wiring the launcher's startup endpoint has). Resume must call this on every attempt:
     * the JDK forbids starting an {@code HttpServer} again after {@code stop()}.
     *
     * @throws java.io.IOException when the endpoint's server socket cannot be opened
     */
    private AutomationInstanceEndpoint newEndpoint() throws java.io.IOException {
        return new AutomationInstanceEndpoint(JIPipe.getAutomation().getOperationRegistry(),
                JIPipe.getAutomation().getPermissionService(), this, this, this);
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
     * Emits a status event from outside the integration (used by the launcher's reconnect
     * path: a successful reconnect changes the broker URL without any pause, resume, or
     * install transition that would emit on its own).
     */
    public static void emitStatus() {
        emit();
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
                integration.inFlight.get(), integration.askPresenter.getPendingAsks().size());
    }

    /**
     * The broker client, or null when not installed (the controls use it for the server URL
     * and the token).
     */
    public MCPBrokerClient getClient() {
        return client;
    }

    /**
     * The current endpoint, or null when not installed. Resume recreates the endpoint (a JDK
     * {@code HttpServer} cannot restart after {@code stop()}), so this is always the live
     * instance; the launcher's shutdown stops the endpoint through this accessor instead of
     * a stale reference.
     */
    public AutomationInstanceEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * The instance-side operation log.
     */
    public MCPOperationLog getOperationLog() {
        return operationLog;
    }

    // ── AutomationEndpointHandlers ──

    /**
     * {@code projectKey} may be null (registry-global operations routed without a project by
     * the broker): {@link #findProjectWindow} then returns null and the context carries a null
     * project. The registry-global operations (list_projects, list_algorithms, get_node_docs)
     * never read the project, while a project-bound operation reached this way fails with its
     * own "No project loaded" style error result (E2E ruling 2026-11-16).
     * <p>
     * The progress handle comes from the shared per-project chain
     * ({@link AutomationProgressHandles}): a run triggered through this context adopts its
     * file-backed store into the chain, so a later get_log/get_errors call for the same project
     * reads the run's records (review fix 2026-11-16; see the trace conclusion there).
     */
    @Override
    public JsonNode execute(String operationId, String projectKey, JsonNode params) throws Exception {
        lastOperationId = operationId;
        inFlight.incrementAndGet();
        recordToolCallBegin(operationId);
        try {
            JIPipeDesktopProjectWindow window = findProjectWindow(projectKey);
            AutomationContext context = new AutomationContext(window != null ? window.getProject() : null,
                    projectKey, progressHandles.create(projectKey), eventBus, jobManager);
            AutomationOperation operation = JIPipe.getAutomation().getOperationRegistry().get(operationId);
            if (operation != null && operation.requiresEdt()) {
                return callOnEdt(() -> JIPipe.getAutomation().getOperationRegistry()
                        .execute(operationId, context, params));
            }
            return JIPipe.getAutomation().getOperationRegistry().execute(operationId, context, params);
        } finally {
            recordToolCallEnd();
            emit();
        }
    }

    /**
     * Records the begin of one MCP operation dispatch as a ToolCall phase in the AI
     * activity tracker (agent tool calls ARE tool calls), so the AI status graph shows
     * MCP activity. Cheap and failure-proof: any tracker problem is swallowed and never
     * affects the dispatch. Runs on the endpoint's HTTP executor thread (the tracker is
     * synchronized, so any thread is fine). Overlapping dispatches share one phase
     * interval: the graph is a glance, not an audit log.
     */
    private static void recordToolCallBegin(String operationId) {
        try {
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            if (aiService != null) {
                aiService.getLLMActivityTracker().beginPhase(LLMActivityPhase.ToolCall, operationId);
            }
        } catch (Throwable ignored) {
            // The status graph must never affect dispatch.
        }
    }

    /**
     * Records the end of the MCP operation dispatch begun by {@link #recordToolCallBegin}.
     * Failure-proof for the same reason.
     */
    private static void recordToolCallEnd() {
        try {
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            if (aiService != null) {
                aiService.getLLMActivityTracker().finalizeCurrentPhase();
            }
        } catch (Throwable ignored) {
            // The status graph must never affect dispatch.
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

    // ── Ask prompt handling (resolution here, presentation via MCPAskPresenter) ──

    /**
     * The two-argument ask handler: the origin defaults to the endpoint's MCP origin
     * ({@link AutomationInstanceEndpoint#ASK_ORIGIN_MCP}), because every ask that reaches
     * this handler arrives through the automation endpoint.
     */
    @Override
    public boolean ask(String operationId, String projectKey) {
        return ask(operationId, projectKey, AutomationInstanceEndpoint.ASK_ORIGIN_MCP);
    }

    /**
     * Queues one Ask request (the installed {@link MCPAskPresenter} presents it) and
     * blocks the endpoint thread until a presentation answers it, the layer shuts down
     * (all pending requests are denied), or {@link #ASK_TIMEOUT_SECONDS} elapse (the
     * request is then denied). Deviation from the original design: the design specified
     * an unbounded wait; the wait is bounded so a paused or unattended UI cannot hold
     * endpoint threads forever, and the agent receives a denied outcome after the
     * timeout. Runs on the endpoint's HTTP executor thread.
     * <p>
     * {@code origin} is free-form metadata naming who asked; it is carried on the
     * {@link PendingAsk} so a presenter can tell ask sources apart. Today every ask
     * arrives through the automation endpoint and reports
     * {@link AutomationInstanceEndpoint#ASK_ORIGIN_MCP} (the endpoint has no richer
     * origin information). Invariant: the broker's forward timeout
     * (MCPBrokerServer.FORWARD_TIMEOUT, 360 s) must exceed the ask bound, so a late
     * answer is still delivered to the agent.
     */
    @Override
    public boolean ask(String operationId, String projectKey, String origin) {
        PendingAsk pending = new PendingAsk(operationId, projectKey, origin);
        askPresenter.askPosted(pending);
        try {
            if (!pending.latch.await(ASK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                // Timed out or interrupted: deny, so the endpoint thread never blocks forever.
                pending.answer(false, false);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pending.answer(false, false);
        } finally {
            askPresenter.askRemoved(pending);
        }
        if (pending.isAllowed() && pending.isGrantAlways()) {
            JIPipe.getAutomation().getPermissionService().grantAlways(operationId, projectKey);
        }
        return pending.isAllowed();
    }

    /**
     * The presented (queued) Ask requests, oldest first, as reported by the installed
     * {@link MCPAskPresenter}.
     */
    public List<PendingAsk> getPendingAsks() {
        return askPresenter.getPendingAsks();
    }

    /**
     * Installs the ask presenter: the presentation half of the ask flow (see
     * {@link MCPAskPresenter}). This is the seam for alternative ask presentations; the
     * ACP integration (#1376) will register a presenter that redirects asks into the
     * active agent's chat. {@code null} restores the default status bar balloon
     * presenter. The resolution half (the PendingAsk latch, the ask bound, the grant
     * writeback) is not affected. A presenter swap does not migrate the asks queued with
     * the previous presenter: install a presenter before the layer serves asks, or deny
     * the pending asks first.
     */
    public void setAskPresenter(MCPAskPresenter presenter) {
        askPresenter = presenter != null ? presenter : new DefaultAskPresenter();
    }

    /**
     * The installed ask presenter (the default presents asks as the status bar ask
     * balloon).
     */
    public MCPAskPresenter getAskPresenter() {
        return askPresenter;
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
        askPresenter.askAnswered(ask);
    }

    /**
     * Denies all presented Ask requests (used on uninstall).
     */
    private void denyAllPending() {
        askPresenter.denyAll();
    }

    /**
     * One pending "ask before running" request. The endpoint thread blocks on the latch
     * until a presentation (the default: the status bar balloon) answers it, the request
     * times out, or the layer shuts down.
     */
    public static final class PendingAsk {

        private final String operationId;
        private final String projectKey;

        /**
         * Free-form metadata naming who asked. "mcp" today: every ask arrives through the
         * automation endpoint ({@link AutomationInstanceEndpoint#ASK_ORIGIN_MCP}); a
         * future ask source passes its own origin.
         */
        private final String origin;
        private final long createdAt = System.currentTimeMillis();
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean answered = new AtomicBoolean();
        private volatile boolean allowed;
        private volatile boolean grantAlways;

        private PendingAsk(String operationId, String projectKey, String origin) {
            this.operationId = operationId;
            this.projectKey = projectKey;
            this.origin = origin;
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

        /**
         * Free-form metadata naming who asked (see {@link AutomationInstanceEndpoint#ASK_ORIGIN_MCP}).
         */
        public String getOrigin() {
            return origin;
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

    /**
     * The default ask presenter: the pre-seam presentation, moved verbatim. Owns the
     * pending-ask queue and fires {@link #STATUS_EVENTS} exactly where the ask flow fired
     * it before the seam (on queue, on answer, on removal; not in {@link #denyAll},
     * whose denies are followed by the uninstall path's own final status event). With
     * this presenter installed, the behavior is unchanged: STATUS_EVENTS drives the ask
     * balloon of the {@code JIPipeDesktopAIManager} and the ask lists of the AI monitor
     * pages.
     */
    private static final class DefaultAskPresenter implements MCPAskPresenter {

        private final ConcurrentLinkedDeque<PendingAsk> pendingAsks = new ConcurrentLinkedDeque<>();

        @Override
        public void askPosted(PendingAsk ask) {
            pendingAsks.addLast(ask);
            emit();
        }

        @Override
        public void askAnswered(PendingAsk ask) {
            emit();
        }

        @Override
        public void askRemoved(PendingAsk ask) {
            pendingAsks.remove(ask);
            emit();
        }

        @Override
        public List<PendingAsk> getPendingAsks() {
            return new ArrayList<>(pendingAsks);
        }

        @Override
        public void denyAll() {
            for (PendingAsk ask : pendingAsks) {
                ask.answer(false, false);
            }
            pendingAsks.clear();
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
     * Resumes the layer: builds a fresh endpoint (a JDK {@code HttpServer} cannot restart
     * after {@code stop()}), starts it, ensures the broker connection, registers, and pushes
     * the fresh window list (mirroring the launcher startup path). Runs on the integration's
     * lifecycle executor. A Resume click can race the launcher's shutdown: if the layer was
     * uninstalled meanwhile (the {@code active} flag cleared), the freshly created endpoint
     * is stopped again and the resume is abandoned instead of resurrecting the layer.
     */
    public void resumeAsync() {
        lifecycleExecutor.execute(() -> {
            AutomationInstanceEndpoint freshEndpoint = null;
            try {
                MCPBrokerClient brokerClient = client;
                if (brokerClient == null) {
                    return;
                }
                freshEndpoint = newEndpoint();
                // A failed earlier resume can leave its endpoint running behind the paused
                // flag; stop it (a stop on the paused, already stopped endpoint is a no-op).
                AutomationInstanceEndpoint previous = endpoint;
                if (previous != null) {
                    previous.stop();
                }
                endpoint = freshEndpoint;
                brokerClient.setEndpoint(freshEndpoint);
                freshEndpoint.start();
                if (getActive() != this) {
                    // The launcher's uninstall won the race before registration: discard the
                    // fresh endpoint, never resurrect the shut-down layer.
                    freshEndpoint.stop();
                    freshEndpoint = null;
                    System.err.println("MCP resume aborted: the layer was shut down while resuming");
                    return;
                }
                brokerClient.ensureConnected(new JIPipeProgressInfo());
                brokerClient.register();
                // register() carries the startup snapshot; push the fresh window list
                // right after (the window set is EDT-confined).
                List<MCPProjectEntry> projects = callOnEdt(ProjectSource.openWindows()::getProjects);
                brokerClient.updateProjects(projects);
                if (getActive() != this) {
                    // The shutdown won the race while the broker calls ran: undo the
                    // registration and discard the fresh endpoint.
                    brokerClient.unregister();
                    freshEndpoint.stop();
                    freshEndpoint = null;
                    System.err.println("MCP resume aborted: the layer was shut down while resuming");
                    return;
                }
                paused = false;
                System.err.println("MCP resumed: endpoint started and instance registered");
            } catch (Exception e) {
                System.err.println("MCP resume failed: " + e.getMessage());
            } finally {
                if (freshEndpoint != null && getActive() != this) {
                    // Exception paths (e.g. a refused registration) must not leak a running
                    // endpoint when the layer is already gone.
                    try {
                        freshEndpoint.stop();
                    } catch (Exception stopError) {
                        // Best effort: the JVM exit or the next shutdown stops it anyway.
                    }
                }
                emit();
            }
        });
    }

    // ── ScreenshotSource ──

    @Override
    public BufferedImage renderCanvas(String compartmentKey, double scale) {
        return callOnEdt(() -> {
            AutoOpenedCanvas opened = findOrOpenCanvas(compartmentKey);
            if (opened == null) {
                return null;
            }
            try {
                if (opened.tab() != null) {
                    // Auto-opened tab: the editor was created a moment ago and its first
                    // layout and paint cycle are still pending on the EDT. Run that cycle
                    // now, or the capture below renders a blank image (2026-11-17
                    // follow-up: the live test captured only 43 non-white pixels).
                    forceLayoutAndPaintCycle(opened.canvas());
                }
                return renderComponentScaled(opened.canvas(), scale);
            } finally {
                closeAutoOpenedCanvas(opened);
            }
        });
    }

    @Override
    public BufferedImage renderMinimap(String compartmentKey, double scale) {
        return callOnEdt(() -> {
            AutoOpenedCanvas opened = findOrOpenCanvas(compartmentKey);
            if (opened == null) {
                return null;
            }
            try {
                JIPipeDesktopGraphCanvasUI canvas = opened.canvas();
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
            } finally {
                closeAutoOpenedCanvas(opened);
            }
        });
    }

    @Override
    public BufferedImage renderFullCanvas(String compartmentKey, double scale) {
        return callOnEdt(() -> {
            AutoOpenedCanvas opened = findOrOpenCanvas(compartmentKey);
            if (opened == null) {
                return null;
            }
            try {
                JIPipeDesktopGraphCanvasUI canvas = opened.canvas();
                double oldZoom = canvas.getZoom();
                if (oldZoom != 1.0) {
                    canvas.setZoom(1.0);
                }
                try {
                    if (opened.tab() != null) {
                        // Auto-opened tab: run the pending layout and paint cycle now, or the
                        // capture below renders a blank image (same hazard as renderCanvas).
                        forceLayoutAndPaintCycle(canvas);
                    }
                    return renderComponentScaled(canvas, scale);
                } finally {
                    if (oldZoom != 1.0) {
                        canvas.setZoom(oldZoom);
                    }
                }
            } finally {
                closeAutoOpenedCanvas(opened);
            }
        });
    }

    @Override
    public String renderFullCanvasSvg(String compartmentKey) {
        return callOnEdt(() -> {
            AutoOpenedCanvas opened = findOrOpenCanvas(compartmentKey);
            if (opened == null) {
                return null;
            }
            try {
                JIPipeDesktopGraphCanvasUI canvas = opened.canvas();
                double oldZoom = canvas.getZoom();
                if (oldZoom != 1.0) {
                    canvas.setZoom(1.0);
                }
                try {
                    if (opened.tab() != null) {
                        // Auto-opened tab: run the pending layout and paint cycle now, or the
                        // render below draws components that were never laid out (same hazard
                        // as renderCanvas).
                        forceLayoutAndPaintCycle(canvas);
                    }
                    return ScreenImageSVG.createImage(canvas).getSVGElement();
                } finally {
                    if (oldZoom != 1.0) {
                        canvas.setZoom(oldZoom);
                    }
                }
            } finally {
                closeAutoOpenedCanvas(opened);
            }
        });
    }

    /**
     * One find-or-open outcome: the canvas to render from, plus the auto-opened editor tab
     * (null when the editor was already open, so nothing is closed afterwards).
     */
    private record AutoOpenedCanvas(JIPipeDesktopGraphCanvasUI canvas, JIPipeDesktopTabPane.DocumentTab tab) {
    }

    /**
     * The canvas of the compartment's pipeline editor tab. When no editor tab is open for the
     * compartment, one is opened on demand through the project workbench (the same
     * {@link JIPipeDesktopProjectWorkbench#getOrOpenPipelineEditorTab} the Compartments tab
     * uses; E2E ruling 2026-11-16: the documentation use case must capture without manual UI
     * steps) and the caller must close it afterwards via {@link #closeAutoOpenedCanvas} so the
     * prior UI state is restored. Returns null when the compartment does not exist in any open
     * project. Must run on the EDT.
     */
    private static AutoOpenedCanvas findOrOpenCanvas(String compartmentKey) {
        UUID compartmentUUID;
        try {
            compartmentUUID = UUID.fromString(compartmentKey);
        } catch (IllegalArgumentException e) {
            return null;
        }
        // Already open: render from the live editor and keep it open.
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();
            if (workbench == null) {
                continue;
            }
            List<JIPipeDesktopPipelineGraphEditorUI> editors = workbench.findOpenPipelineEditorTabs(compartmentUUID);
            if (!editors.isEmpty()) {
                return new AutoOpenedCanvas(editors.getFirst().getCanvasUI(), null);
            }
        }
        // Not open: open the editor without switching tabs (the user's current view stays),
        // render from the now-live editor, and close the tab afterwards.
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            JIPipeDesktopProjectWorkbench workbench = window.getProjectWorkbench();
            if (workbench == null || window.getProject() == null) {
                continue;
            }
            JIPipeProjectCompartment compartment = window.getProject().getCompartments().get(compartmentUUID);
            if (compartment == null) {
                continue;
            }
            JIPipeDesktopTabPane.DocumentTab tab = workbench.getOrOpenPipelineEditorTab(compartment, false);
            if (tab == null || !(tab.getContent() instanceof JIPipeDesktopPipelineGraphEditorUI editor)) {
                return null;
            }
            return new AutoOpenedCanvas(editor.getCanvasUI(), tab);
        }
        return null;
    }

    /**
     * Closes the auto-opened editor tab (a silent close, no user interaction). A tab that was
     * already open before the render is left untouched.
     */
    private static void closeAutoOpenedCanvas(AutoOpenedCanvas opened) {
        if (opened == null || opened.tab() == null) {
            return;
        }
        opened.tab().getDocumentTabPane().forceCloseTab(opened.tab());
    }

    /**
     * Forces the full layout and paint pass of a freshly auto-opened editor before a
     * {@code printAll} capture. The freshly opened tab marks its hierarchy dirty with
     * {@code revalidate}, and the EDT would only run that layout and the first paint on a
     * later cycle - an immediate capture renders components that were never laid out
     * (zero-sized node content, a blank image).
     * <p>
     * Every step runs and completes synchronously on the calling (EDT) thread, so the
     * component state at return time is deterministic - there is no event-queue wait and no
     * sleep involved:
     * <ol>
     *     <li>the pending layout pass is performed right here by invalidating and validating
     *     the top-level ancestor (the same full pass the EDT would run on its next cycle;
     *     the canvas itself has no "laid out" signal - the editor UI exposes none), and</li>
     *     <li>one full paint cycle of the canvas is performed right here through
     *     {@link JComponent#paintImmediately} (the documented immediate paint through the
     *     repaint manager), followed by {@link Toolkit#sync} to flush the graphics
     *     pipeline.</li>
     * </ol>
     */
    private static void forceLayoutAndPaintCycle(JComponent canvas) {
        canvas.revalidate();
        Container root = canvas.getTopLevelAncestor();
        if (root != null) {
            root.invalidate();
            root.validate();
        } else {
            canvas.invalidate();
            canvas.validate();
        }
        if (canvas.getWidth() > 0 && canvas.getHeight() > 0) {
            canvas.paintImmediately(0, 0, canvas.getWidth(), canvas.getHeight());
            Toolkit.getDefaultToolkit().sync();
        }
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
     * compartments editor's canvas (the compartment overview), the document tab pane, and
     * a freshly constructed MCP settings form (never shown, so the operation realizes it
     * off-screen).
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
                    if (tab.getContent() instanceof JIPipeDesktopCompartmentsGraphEditorUI editor) {
                        return editor.getCanvasUI();
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

    /**
     * Captures the project window. Visible (non-iconified) windows are captured from the
     * screen via the Robot-based {@link ScreenImage#createImage(Rectangle)}; off-screen or
     * iconified windows are realized in place via the print-based capture of the content
     * pane. The Robot capture never runs on the event dispatch thread: on Wayland/XWayland
     * hosts it goes through the xdg-desktop-portal screencast, which can block indefinitely
     * (no exception, no timeout) and would freeze the whole application. Instead, the window
     * state and the screen region are read on the EDT (phase 1), the Robot capture runs on
     * {@link #captureExecutor} with a wait bounded at {@link #CAPTURE_TIMEOUT_SECONDS}
     * (phase 2), and a capture without a result falls back to the EDT print path (phase 3).
     * The fake titlebar compositing is pure graphics and runs on the calling thread.
     */
    @Override
    public BufferedImage captureWindow(String projectKey, boolean titlebar) {
        // Phase 1 (EDT): resolve the window, read the shown/iconified state, compute the
        // screen region (the exact math of ScreenImage.createImage(Component)) and the title.
        WindowCapturePreparation preparation = callOnEdt(() -> {
            JIPipeDesktopProjectWindow window = findProjectWindowOnEdt(projectKey);
            if (window == null) {
                return null;
            }
            boolean iconified = window instanceof Frame frame
                    && (frame.getExtendedState() & Frame.ICONIFIED) != 0;
            if (window.isShowing() && !iconified) {
                try {
                    Point location = new Point(0, 0);
                    SwingUtilities.convertPointToScreen(location, window);
                    Rectangle region = window.getBounds();
                    region.x = location.x;
                    region.y = location.y;
                    return new WindowCapturePreparation(window, true, region, window.getTitle());
                } catch (Exception e) {
                    // The window is showing but has no usable screen location (no native
                    // peer): the Robot capture cannot address it, so go straight to the
                    // print fallback (the same outcome as the previous single-phase code).
                    return new WindowCapturePreparation(window, false, null, window.getTitle());
                }
            }
            // Off-screen realization: print the content pane in place (no reparenting,
            // no screen interaction). This also covers the iconified (minimized) state:
            // the window is still showing, but a Robot capture would grab whatever
            // desktop pixels now cover its bounds.
            return new WindowCapturePreparation(window, false, null, window.getTitle());
        });
        if (preparation == null) {
            return null;
        }
        // Phase 2 (off the EDT): the Robot capture with a bounded wait.
        BufferedImage capture = null;
        if (preparation.robotCapture()) {
            capture = robotCaptureOffEdt(preparation.region());
        }
        // Phase 3: no screen capture (no screen access such as headless, a blocked portal,
        // the timeout, or an off-screen/iconified window): the print-based capture of the
        // content pane, on the EDT.
        if (capture == null) {
            capture = callOnEdt(() -> printCapture(preparation.window().getContentPane()));
        }
        if (capture == null) {
            return null;
        }
        if (!titlebar) {
            return capture;
        }
        return composeFakeTitlebar(capture, preparation.title());
    }

    /**
     * The EDT phase result of {@link #captureWindow(String, boolean)}: the resolved window
     * (for the print fallback), whether the Robot-based screen capture applies, the screen
     * region (when it does), and the window title (for the fake titlebar).
     */
    private record WindowCapturePreparation(JIPipeDesktopProjectWindow window, boolean robotCapture,
                                            Rectangle region, String title) {
    }

    /**
     * Runs the Robot-based capture of the screen region on {@link #captureExecutor} (never
     * on the event dispatch thread) and waits at most {@link #CAPTURE_TIMEOUT_SECONDS}. Any
     * failure (no screen access, the portal blocking the native call, the timeout) is
     * treated as "no capture": the caller takes the print fallback. An interrupted wait
     * restores the interrupt flag.
     */
    private static BufferedImage robotCaptureOffEdt(Rectangle region) {
        Future<BufferedImage> future = captureExecutor.submit(() -> ScreenImage.createImage(region));
        try {
            return future.get(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return null;
        } catch (ExecutionException | TimeoutException e) {
            future.cancel(true);
            return null;
        }
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
