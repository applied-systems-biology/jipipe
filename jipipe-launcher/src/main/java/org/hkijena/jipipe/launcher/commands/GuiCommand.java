package org.hkijena.jipipe.launcher.commands;

import com.fasterxml.jackson.databind.JsonNode;
import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeGUICommand;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.endpoint.AutomationEndpointHandlers;
import org.hkijena.jipipe.api.automation.endpoint.AutomationInstanceEndpoint;
import org.hkijena.jipipe.api.automation.endpoint.MCPBrokerClient;
import org.hkijena.jipipe.api.automation.endpoint.MCPProjectEntry;
import org.hkijena.jipipe.api.automation.endpoint.ProjectSource;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.api.instrumentation.InstrumentationApplicationSettings;
import org.hkijena.jipipe.api.instrumentation.InstrumentationServer;
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

    private static InstrumentationServer instrumentationServer;
    private static int pendingInstrumentationPort = -1;
    private static boolean instrumentationFlag = false;

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
    private static volatile AutomationInstanceEndpoint automationEndpoint;
    private static volatile Future<?> mcpStartupTask;

    public static InstrumentationServer getInstrumentationServer() {
        return instrumentationServer;
    }

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
            if ("--instrumentation".equals(argsList.get(i))) {
                instrumentationFlag = true;
                if (i + 1 < argsList.size()) {
                    try {
                        pendingInstrumentationPort = Integer.parseInt(argsList.get(i + 1));
                        i++;
                    } catch (NumberFormatException e) {
                        // Port will be resolved from settings in onWindowOpened()
                    }
                }
                // else: port will be resolved from settings in onWindowOpened()
            } else {
                remainingArgs.add(argsList.get(i));
            }
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
            // Minimal pre-Task-13 wiring: the endpoint registry is empty, so the endpoint
            // refuses every operation with the unknown code before reaching the handlers.
            // Task 13 replaces the registry and handlers with the real GUI dispatch.
            automationEndpoint = new AutomationInstanceEndpoint(new AutomationOperationRegistry(),
                    permissions, minimalMcpHandlers(), null, null);
            automationEndpoint.start();
            int brokerPort = MCPApplicationSettings.getInstance() != null
                    ? MCPApplicationSettings.getInstance().getPort() : 8781;
            MCPBrokerClient client = new MCPBrokerClient(MCPBrokerClient.defaultSpawner(), brokerPort);
            client.setEndpoint(automationEndpoint);
            client.setProjectSource(() -> projectSnapshot);
            mcpBrokerClient = client;
            client.ensureConnected(new JIPipeProgressInfo());
            client.register();
            System.err.println("MCP broker connected at " + client.getBaseUrl());
        } catch (Exception e) {
            System.err.println("MCP automation layer failed to start: " + e.getMessage());
            shutdownMcp();
        }
    }

    /**
     * Minimal endpoint handlers for the pre-Task-13 state. The endpoint's operation registry
     * is empty, so every operation is refused as unknown before these handlers run; they
     * exist so the endpoint is fully functional without GUI wiring.
     */
    private static AutomationEndpointHandlers minimalMcpHandlers() {
        return new AutomationEndpointHandlers() {
            @Override
            public JsonNode execute(String operationId, String projectKey, JsonNode params) {
                throw new IllegalStateException("GUI operation dispatch is not wired yet (Task 13): " + operationId);
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
        if (automationEndpoint != null) {
            try {
                automationEndpoint.stop();
            } catch (Exception e) {
                System.err.println("MCP endpoint stop failed: " + e.getMessage());
            }
            automationEndpoint = null;
        }
    }

    public static class WindowWatcher implements WindowOpenedEventListener, WindowClosedEventListener {
        public WindowWatcher() {
            JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this);
            JIPipeDesktopProjectWindow.WINDOW_CLOSED_EVENT_EMITTER.subscribe(this);
        }

        @Override
        public void onWindowClosed(WindowClosedEvent event) {
            if (instrumentationServer != null) {
                SwingUtilities.invokeLater(() -> instrumentationServer.onProjectWindowsChanged());
            }
            submitMcpProjectPush();
            int windowsOpen = JIPipeDesktopProjectWindow.getOpenWindows().size();
            if (windowsOpen == 0) {
                shutdownMcpOrdered();
                if (instrumentationServer != null) {
                    try {
                        instrumentationServer.stop(0);
                    } catch (Exception ignored) {
                    }
                }
                JIPipe.exitLater(0);
            }
        }

        @Override
        public void onWindowOpened(WindowOpenedEvent event) {
            if (instrumentationServer == null) {
                int port = -1;
                if (pendingInstrumentationPort > 0) {
                    port = pendingInstrumentationPort;
                } else if (instrumentationFlag || InstrumentationApplicationSettings.getInstance().isAutoStart()) {
                    port = InstrumentationApplicationSettings.getInstance().getPort();
                }
                if (port > 0) {
                    instrumentationServer = new InstrumentationServer(port);
                    try {
                        instrumentationServer.start();
                        System.err.println("Instrumentation server started on port " + port);
                    } catch (Exception e) {
                        System.err.println("Failed to start instrumentation server: " + e.getMessage());
                        instrumentationServer = null;
                    }
                    pendingInstrumentationPort = -1;
                }
            }
            if (instrumentationServer != null) {
                SwingUtilities.invokeLater(() -> instrumentationServer.onProjectWindowsChanged());
            }
            startMcpIfNeededAsync();
            submitMcpProjectPush();
        }
    }
}
