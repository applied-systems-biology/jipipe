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

public class GuiCommand {

    private static InstrumentationServer instrumentationServer;
    private static int pendingInstrumentationPort = -1;
    private static boolean instrumentationFlag = false;
    private static McpCliOptions mcpOptions = McpCliOptions.parse(List.of());
    private static MCPBrokerClient mcpBrokerClient;
    private static AutomationInstanceEndpoint automationEndpoint;

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
     * connection, registration. Every failure is logged and never crashes the GUI startup.
     */
    private static synchronized void startMcpIfNeeded() {
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

    private static void pushMcpProjects() {
        try {
            mcpBrokerClient.updateProjects(ProjectSource.openWindows().getProjects());
        } catch (Exception e) {
            System.err.println("MCP project update failed: " + e.getMessage());
        }
    }

    /**
     * Unregisters from the broker and stops the instance endpoint. Never throws.
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
            if (mcpBrokerClient != null) {
                pushMcpProjects();
            }
            int windowsOpen = JIPipeDesktopProjectWindow.getOpenWindows().size();
            if (windowsOpen == 0) {
                shutdownMcp();
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
            startMcpIfNeeded();
            if (mcpBrokerClient != null) {
                pushMcpProjects();
            }
        }
    }
}
