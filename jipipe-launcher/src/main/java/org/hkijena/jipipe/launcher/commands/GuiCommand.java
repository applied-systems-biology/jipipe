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
     * Starts the MCP automation layer on the first window (when requested via {@code --mcp}
     * or the auto-start setting): CLI permission overrides, instance endpoint, broker
     * connection, registration. Runs on the MCP background executor (see
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
                    && MCPApplicationSettings.getInstance().isAutoStart());
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
