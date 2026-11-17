package org.hkijena.jipipe.launcher.commands;

import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeGUICommand;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.endpoint.AutomationInstanceEndpoint;
import org.hkijena.jipipe.api.automation.endpoint.MCPBrokerClient;
import org.hkijena.jipipe.api.automation.endpoint.MCPProjectEntry;
import org.hkijena.jipipe.api.automation.endpoint.ProjectSource;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPIntegration;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowClosedEventListener;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEvent;
import org.hkijena.jipipe.desktop.commons.events.WindowOpenedEventListener;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GuiCommand {

    /**
     * Single-threaded daemon executor for the whole MCP lifecycle: startup, project
     * pushes, and shutdown run serialized here. The startup alone can block for up to
     * 30 seconds (broker health poll plus HTTP calls with 10-30 second timeouts), so it
     * must never run on the EDT that receives the window events triggering it.
     */
    private static final ExecutorService MCP_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "JIPipe-MCP-Lifecycle");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * How long {@link #shutdownMcpOrdered()} waits for the queued shutdown before the
     * application exits anyway (the broker also reaps dead instance PIDs).
     */
    private static final int MCP_SHUTDOWN_AWAIT_SECONDS = 10;

    private static volatile McpCliOptions mcpOptions = McpCliOptions.parse(List.of());
    private static volatile MCPBrokerClient mcpBrokerClient;
    private static volatile Future<?> mcpStartupTask;

    /**
     * Pure parser for the {@code --mcp} CLI arguments (delegates to {@link McpCliOptions}).
     */
    public static McpCliOptions parseMcpArgs(List<String> args) {
        return McpCliOptions.parse(args);
    }

    public static MCPBrokerClient getMcpBrokerClient() {
        return mcpBrokerClient;
    }

    public static void startGui(List<String> argsList) {
        mcpOptions = McpCliOptions.parse(argsList);
        // The lifecycle hooks route the MCP start/stop requests of the AI monitor and the
        // "Enable MCP" settings toggle through this class's MCP executor (the lifecycle
        // owner). Installed before the GUI starts; never blocks the caller.
        JIPipeDesktopMCPIntegration.setLifecycleHooks(new JIPipeDesktopMCPIntegration.LifecycleHooks() {
            @Override
            public void startMcpAsync() {
                startMcpFromHooks();
            }

            @Override
            public void stopMcpAsync() {
                stopMcpFromHooks();
            }

            @Override
            public void reconnectMcpAsync() {
                reconnectMcpFromHooks();
            }
        });
        List<String> remainingArgs = new ArrayList<>();
        for (int i = 0; i < argsList.size(); i++) {
            // MCP arguments are consumed as a block (single source of truth in McpCliOptions).
            int consumed = McpCliOptions.consumedCount(argsList, i);
            if (consumed > 0) {
                i += consumed - 1;
                continue;
            }
            remainingArgs.add(argsList.get(i));
        }

        if (!remainingArgs.isEmpty()) {
            JIPipeDesktop.tryAddOpenProjectOnLoad(remainingArgs.getLast());
        }

        final ImageJ ij = new ImageJ();
        final WindowWatcher windowWatcher = new WindowWatcher();
        SwingUtilities.invokeLater(() -> ij.command().run(JIPipeGUICommand.class, true));
    }

    /**
     * Starts the MCP automation layer on the first window (when requested via {@code --mcp},
     * the auto-start setting, or the enable setting): CLI permission overrides, instance
     * endpoint, broker connection, registration. Runs on the MCP background executor (see
     * {@link #startMcpIfNeededAsync()}), never on the EDT. Every failure is logged and
     * never crashes the GUI startup.
     * <p>
     * A {@code --mcp <port>} override applies to the broker SPAWN only: {@link MCPBrokerClient}
     * passes the port to the spawned broker process, while a broker that is already reachable
     * through the discovery file is connected to regardless of the requested port (see
     * {@link MCPBrokerClient#ensureConnected}).
     *
     * @param projectSnapshot the open project windows, snapshotted on the EDT by the
     *                        submitter (the window set is EDT-confined)
     */
    private static synchronized void startMcpIfNeeded(List<MCPProjectEntry> projectSnapshot) {
        if (mcpBrokerClient != null) {
            return;
        }
        boolean requested;
        try {
            requested = mcpOptions.requested()
                    || (MCPApplicationSettings.getInstance() != null
                    && (MCPApplicationSettings.getInstance().isAutoStart()
                    || MCPApplicationSettings.getInstance().isEnable()));
        } catch (Exception e) {
            System.err.println("MCP automation layer unavailable: " + e.getMessage());
            return;
        }
        if (!requested) {
            return;
        }
        JIPipeDesktopMCPIntegration.markStarting();
        try {
            // CLI overrides are applied non-persistently to the shared permission service
            // (JIPipeAutomationServiceComponent, reachable from the endpoint, the GUI
            // launcher, and the automation permission UI).
            AutomationPermissionService permissions = JIPipe.getAutomation().getPermissionService();
            if (!mcpOptions.allowIds().isEmpty()) {
                permissions.setCliOverride(AutomationPermissionState.Allow, mcpOptions.allowIds());
            }
            if (!mcpOptions.denyIds().isEmpty()) {
                permissions.setCliOverride(AutomationPermissionState.Deny, mcpOptions.denyIds());
            }
            // Task 13 wiring: the endpoint serves the global operation registry and the
            // desktop integration is the real dispatch (project resolution, EDT
            // marshalling, ask bubble, operation log, screenshot source). The endpoint is
            // tracked by the integration (getEndpoint): resume recreates it, so this
            // startup reference must never be reused for shutdown.
            AutomationInstanceEndpoint startupEndpoint = JIPipeDesktopMCPIntegration.createEndpoint();
            startupEndpoint.start();
            // The CLI --mcp port overrides the settings port for the spawn; without it the
            // settings port (default 8781) is used.
            int brokerPort;
            if (mcpOptions.port() != null) {
                brokerPort = mcpOptions.port();
            } else {
                brokerPort = MCPApplicationSettings.getInstance() != null
                        ? MCPApplicationSettings.getInstance().getPort() : 8781;
            }
            MCPBrokerClient client = new MCPBrokerClient(MCPBrokerClient.defaultSpawner(), brokerPort);
            client.setEndpoint(startupEndpoint);
            client.setProjectSource(() -> projectSnapshot);
            mcpBrokerClient = client;
            JIPipeDesktopMCPIntegration.install(startupEndpoint, client);
            client.ensureConnected(new JIPipeProgressInfo());
            client.register();
            System.err.println("MCP broker connected at " + client.getBaseUrl());
        } catch (Exception e) {
            System.err.println("MCP automation layer failed to start: " + e.getMessage());
            shutdownMcp();
        }
    }

    /**
     * Snapshots the open project windows on the EDT (the window set is EDT-confined) and
     * pushes the snapshot to the broker on the MCP executor: the event thread stays cheap
     * and the POST is serialized with the other MCP tasks.
     */
    private static void submitMcpProjectPush() {
        List<MCPProjectEntry> projects = ProjectSource.openWindows().getProjects();
        MCP_EXECUTOR.execute(loggedMcpTask("project update", () -> pushMcpProjects(projects)));
    }

    private static void pushMcpProjects(List<MCPProjectEntry> projects) {
        MCPBrokerClient client = mcpBrokerClient;
        if (client == null) {
            return;
        }
        try {
            client.updateProjects(projects);
        } catch (Exception e) {
            System.err.println("MCP project update failed: " + e.getMessage());
        }
    }

    /**
     * Submits {@link #startMcpIfNeeded(List)} to the MCP executor: the startup (broker
     * health poll up to 30 seconds plus HTTP calls) must never run on the EDT, where it
     * would freeze first-window construction. The single executor serializes startup,
     * project updates, and shutdown to keep them ordered.
     */
    private static void startMcpIfNeededAsync() {
        if (mcpBrokerClient != null) {
            return;
        }
        List<MCPProjectEntry> projectSnapshot = ProjectSource.openWindows().getProjects();
        mcpStartupTask = MCP_EXECUTOR.submit(loggedMcpTask("startup",
                () -> startMcpIfNeeded(projectSnapshot)));
    }

    /**
     * The lifecycle hook start path (called by the AI monitor and the "Enable MCP"
     * settings toggle; never blocks the caller): a stopped layer starts on the MCP
     * executor, a paused layer resumes (fresh endpoint, re-register). A running layer
     * is left alone. Marshals to the EDT when needed: the startup snapshots the open
     * project windows, and the window set is EDT-confined.
     */
    private static void startMcpFromHooks() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(GuiCommand::startMcpFromHooks);
            return;
        }
        if (mcpBrokerClient != null) {
            JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
            if (integration != null && integration.isPaused()) {
                integration.resumeAsync();
            }
            return;
        }
        startMcpIfNeededAsync();
    }

    /**
     * The lifecycle hook stop path (pause semantics: unregister and endpoint stop; the
     * broker stays alive for other instances, so a later start resumes). A stopped or
     * starting layer is left alone (a startup failure cleans up itself). Runs on any
     * thread; the pause runs on the integration's lifecycle executor.
     */
    private static void stopMcpFromHooks() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        if (integration != null) {
            integration.pauseAsync();
        }
    }

    /**
     * The lifecycle hook reconnect path (called by the AI monitor after a token
     * rotation; never blocks the caller): re-establishes the broker connection without
     * stopping the layer. With no client (layer not started), this delegates to the
     * startup path. A paused layer is resumed instead (its endpoint must not be
     * registered while stopped); the pause unregistration already cleared the client's
     * connection state, so the resume connect re-runs the full discovery path. Otherwise
     * the current windows are snapshotted here (the window set is EDT-confined) and the
     * reconnect runs on the MCP executor, serialized with the other MCP tasks.
     */
    private static void reconnectMcpFromHooks() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(GuiCommand::reconnectMcpFromHooks);
            return;
        }
        if (mcpBrokerClient == null) {
            startMcpIfNeededAsync();
            return;
        }
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        if (integration != null && integration.isPaused()) {
            startMcpFromHooks();
            return;
        }
        List<MCPProjectEntry> projectSnapshot = ProjectSource.openWindows().getProjects();
        MCP_EXECUTOR.execute(loggedMcpTask("reconnect", () -> reconnectMcp(projectSnapshot)));
    }

    /**
     * The reconnect sequence on the MCP executor, mirroring the startup path after
     * connect (register, then push the current window list): the invalidated client
     * re-runs the full discovery, and a 401 from the old-token broker deletes the stale
     * discovery file and spawns a fresh broker with the rotated token. Failures are
     * logged and non-fatal.
     */
    private static void reconnectMcp(List<MCPProjectEntry> projectSnapshot) {
        MCPBrokerClient client = mcpBrokerClient;
        if (client == null) {
            return;
        }
        try {
            client.invalidateConnection();
            client.ensureConnected(new JIPipeProgressInfo());
            client.register();
            System.err.println("MCP broker reconnected at " + client.getBaseUrl());
            client.updateProjects(projectSnapshot);
        } catch (Exception e) {
            System.err.println("MCP reconnect failed: " + e.getMessage());
        } finally {
            // The broker URL may have changed without any other lifecycle transition.
            JIPipeDesktopMCPIntegration.emitStatus();
        }
    }

    /**
     * The "Enable MCP" settings toggle, applied live: on starts the layer (a no-op when
     * it already runs), off pauses it (unregister and endpoint stop; the broker stays
     * alive). Runs on the EDT (the parameter change event source).
     */
    private static void onEnableSettingChanged(boolean enabled) {
        if (enabled) {
            startMcpFromHooks();
        } else {
            stopMcpFromHooks();
        }
    }

    /**
     * Wraps an MCP task so that anything thrown on the executor thread is logged: a
     * failure must never kill the thread silently without a trace.
     */
    private static Runnable loggedMcpTask(String name, Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                String message = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
                System.err.println("MCP background task '" + name + "' crashed: " + message);
            }
        };
    }

    /**
     * Runs {@link #shutdownMcp()} on the MCP executor, ordered after the queued MCP tasks,
     * and waits briefly so the unregister POST is not lost to {@link JIPipe#exitLater}.
     * A still-running startup is cancelled first: its poll loop is interruptible and its
     * failure path cleans up. {@link #shutdownMcp()} is idempotent, so a shutdown racing
     * a cancelled startup is a no-op.
     */
    private static void shutdownMcpOrdered() {
        Future<?> startupTask = mcpStartupTask;
        if (startupTask != null) {
            startupTask.cancel(true);
        }
        try {
            MCP_EXECUTOR.submit(loggedMcpTask("shutdown", GuiCommand::shutdownMcp))
                    .get(MCP_SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.err.println("MCP shutdown did not complete within " + MCP_SHUTDOWN_AWAIT_SECONDS
                    + " seconds; exiting without confirmed unregister");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            System.err.println("MCP shutdown failed: " + (cause != null && cause.getMessage() != null
                    ? cause.getMessage() : String.valueOf(cause)));
        }
    }

    /**
     * The integration's current endpoint (resume recreates it), or null when the MCP layer
     * is not installed. Shutdown must stop this one, not the startup-time endpoint.
     */
    private static AutomationInstanceEndpoint currentMcpEndpoint() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        return integration != null ? integration.getEndpoint() : null;
    }

    /**
     * Unregisters from the broker and stops the instance endpoint. Never throws. Runs on
     * the MCP executor only (via {@link #shutdownMcpOrdered()} or the startup failure
     * path), so it is always ordered against the other MCP tasks.
     */
    private static synchronized void shutdownMcp() {
        if (mcpBrokerClient != null) {
            try {
                mcpBrokerClient.unregister();
            } catch (Exception e) {
                System.err.println("MCP unregister failed: " + e.getMessage());
            }
            mcpBrokerClient = null;
        }
        AutomationInstanceEndpoint endpoint = currentMcpEndpoint();
        if (endpoint != null) {
            try {
                endpoint.stop();
            } catch (Exception e) {
                System.err.println("MCP endpoint stop failed: " + e.getMessage());
            }
        }
        // Deactivates the desktop integration (screenshot source, pending asks, status).
        JIPipeDesktopMCPIntegration.uninstall();
    }

    public static class WindowWatcher implements WindowOpenedEventListener, WindowClosedEventListener, JIPipeDesktopProjectWindow.ProjectLoadedEventListener {
        public WindowWatcher() {
            JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this);
            JIPipeDesktopProjectWindow.WINDOW_CLOSED_EVENT_EMITTER.subscribe(this);
            JIPipeDesktopProjectWindow.PROJECT_LOADED_EVENT_EMITTER.subscribe(this);
            // The "Enable MCP" settings toggle starts/stops the layer live while the app
            // runs. The lifecycle owner subscribes (this watcher outlives the GUI run: the
            // static window emitters hold it). A settings read failure leaves the toggle
            // inert; the launch-time requests (--mcp, auto-start, enable) still apply.
            try {
                MCPApplicationSettings settings = MCPApplicationSettings.getInstance();
                if (settings != null) {
                    settings.getParameterChangedEventEmitter().subscribeLambda((emitter, event) -> {
                        if ("enable".equals(event.getKey())) {
                            try {
                                onEnableSettingChanged(MCPApplicationSettings.getInstance().isEnable());
                            } catch (Exception e) {
                                System.err.println("MCP enable toggle failed: " + e.getMessage());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("MCP enable toggle unavailable: " + e.getMessage());
            }
        }

        @Override
        public void onWindowClosed(WindowClosedEvent event) {
            submitMcpProjectPush();
            int windowsOpen = JIPipeDesktopProjectWindow.getOpenWindows().size();
            if (windowsOpen == 0) {
                shutdownMcpOrdered();
                JIPipe.exitLater(0);
            }
        }

        @Override
        public void onWindowOpened(WindowOpenedEvent event) {
            startMcpIfNeededAsync();
            submitMcpProjectPush();
        }

        /**
         * {@link JIPipeDesktopProjectWindow#loadProject(JIPipeProject, boolean, boolean)}
         * regenerates the window session id, so a registration that captured the key at
         * window-open time (before the project file loaded) is stale. Refresh the broker
         * registration with the new key and the loaded project's name/path. Safe when the
         * MCP layer is not started (the push is a no-op) and ordered behind the startup
         * task on the MCP executor, so the refresh cannot be lost to a still-running
         * startup.
         */
        @Override
        public void onProjectLoaded(JIPipeDesktopProjectWindow.ProjectLoadedEvent event) {
            submitMcpProjectPush();
        }
    }
}
