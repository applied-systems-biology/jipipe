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

package org.hkijena.jipipe.desktop.commons.components.ai.monitor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.endpoint.MCPBrokerClient;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPIntegration;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEvent;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEventListener;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
/**
 * MCP page of the AI monitor window: the layer status rows, the lifecycle controls
 * (start/stop through the launcher-installed lifecycle hooks), the broker server URL
 * and access token (with copy buttons), the "Allow always" grants with per-entry
 * revoke, and the pending Ask requests (answering stays in the ask bubble anchored to
 * the AI status item).
 * <p>
 * Token reads happen off the event dispatch thread (SwingWorker): reading the token
 * file on the EDT would add disk I/O to every refresh.
 */
public class JIPipeDesktopAIMonitorMCPPage extends JIPipeDesktopAIMonitorPage implements MCPStatusEventListener {

    private final JLabel statusLabel = new JLabel();
    private final JLabel brokerUrlLabel = new JLabel();
    private final JLabel copyUrlLabel = new JLabel();
    private final JLabel endpointPortLabel = new JLabel();
    private final JLabel lastOperationLabel = new JLabel();
    private final JLabel inFlightLabel = new JLabel();
    private final JButton startButton;
    private final JButton stopButton;
    private final JButton copyUrlButton;
    /**
     * The token row wrapper: hidden entirely when token authentication is off (a zero
     * preferred size keeps the form grid from reserving the row).
     */
    private final JPanel tokenRow;
    private final JLabel tokenLabel = new JLabel();
    private final JButton showTokenButton;
    private final JButton copyTokenButton;
    private final JPanel grantsPanel;
    private final JPanel asksPanel;

    /**
     * Guards overlapping background token reads.
     */
    private boolean tokenFetchInFlight;
    /**
     * The last fetched token (background read), or null when unknown/hidden.
     */
    private String fetchedToken;
    private boolean tokenShown;

    public JIPipeDesktopAIMonitorMCPPage(JIPipeDesktopAIMonitorWindow window) {
        super(window);
        this.startButton = UIUtils.createButton("Start MCP", JIPipe.RESOURCES.getIcon16("actions/circle-play.png"), this::startMcp);
        this.stopButton = UIUtils.createButton("Stop MCP", JIPipe.RESOURCES.getIcon16("actions/circle-stop.png"), this::stopMcp);
        this.copyUrlButton = UIUtils.createButton("Copy", JIPipe.RESOURCES.getIcon16("actions/copy.png"), this::copyServerUrl);
        this.showTokenButton = UIUtils.createButton("Show", JIPipe.RESOURCES.getIcon16("actions/key.png"), this::toggleTokenShown);
        this.copyTokenButton = UIUtils.createButton("Copy", JIPipe.RESOURCES.getIcon16("actions/copy.png"), this::copyToken);
        this.tokenRow = createTokenRow();
        this.grantsPanel = new JPanel();
        this.asksPanel = new JPanel();

        initialize();
        refresh();

        // Status events refresh the page (weak subscription; the static emitter outlives
        // the page). dispose() unsubscribes explicitly per the monitor page contract.
        JIPipeDesktopMCPIntegration.STATUS_EVENTS.subscribeWeak(this);
    }

    private void initialize() {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        add(formPanel, BorderLayout.CENTER);

        // Status
        formPanel.addGroupHeader("Status", JIPipe.RESOURCES.getIcon16("actions/robot.png"));
        formPanel.addToForm(statusLabel, new JLabel("Layer state"));
        formPanel.addToForm(brokerUrlLabel, new JLabel("Broker URL"));
        formPanel.addToForm(endpointPortLabel, new JLabel("Endpoint port"));
        formPanel.addToForm(lastOperationLabel, new JLabel("Last operation"));
        formPanel.addToForm(inFlightLabel, new JLabel("In-flight operations"));

        // Controls
        formPanel.addGroupHeader("Controls", JIPipe.RESOURCES.getIcon16("actions/system-run.png"));
        formPanel.addToForm(new JLabel("Start the MCP automation layer (connect to or spawn the broker, register this instance)"),
                startButton);
        formPanel.addToForm(new JLabel("Stop the MCP automation layer (unregister and stop the endpoint; the broker stays alive)"),
                stopButton);

        JPanel urlRow = new JPanel(new BorderLayout(8, 0));
        urlRow.setOpaque(false);
        urlRow.add(copyUrlLabel, BorderLayout.CENTER);
        urlRow.add(copyUrlButton, BorderLayout.EAST);
        formPanel.addToForm(urlRow, new JLabel("Server URL"));

        formPanel.addToForm(tokenRow, new JLabel("Access token"));

        // Grants
        formPanel.addGroupHeader("Allow-always grants", JIPipe.RESOURCES.getIcon16("actions/check.png"));
        grantsPanel.setOpaque(false);
        grantsPanel.setLayout(new GridLayout(0, 1, 0, 4));
        formPanel.addWideToForm(grantsPanel);

        // Pending asks
        formPanel.addGroupHeader("Pending Ask requests", JIPipe.RESOURCES.getIcon16("actions/robot.png"));
        asksPanel.setOpaque(false);
        asksPanel.setLayout(new GridLayout(0, 1, 0, 4));
        formPanel.addWideToForm(asksPanel);
        JLabel askHint = new JLabel("Answer requests in the ask bubble anchored to the AI status item "
                + "(Allow once, Allow always, Deny). Unanswered requests are denied after 300 seconds.");
        askHint.setForeground(UIManager.getColor("Label.disabledForeground"));
        formPanel.addWideToForm(askHint);
    }

    /**
     * Builds the token row: masked or shown token text plus Show and Copy buttons. The
     * wrapper collapses to zero size when token authentication is off.
     */
    private JPanel createTokenRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override
            public Dimension getPreferredSize() {
                return isVisible() ? super.getPreferredSize() : new Dimension();
            }

            @Override
            public Dimension getMinimumSize() {
                return isVisible() ? super.getMinimumSize() : new Dimension();
            }
        };
        row.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        buttons.add(showTokenButton);
        buttons.add(copyTokenButton);
        row.add(tokenLabel, BorderLayout.CENTER);
        row.add(buttons, BorderLayout.EAST);
        return row;
    }

    private void startMcp() {
        JIPipeDesktopMCPIntegration.LifecycleHooks hooks = JIPipeDesktopMCPIntegration.getLifecycleHooks();
        if (hooks != null) {
            hooks.startMcpAsync();
        }
    }

    private void stopMcp() {
        JIPipeDesktopMCPIntegration.LifecycleHooks hooks = JIPipeDesktopMCPIntegration.getLifecycleHooks();
        if (hooks != null) {
            hooks.stopMcpAsync();
        }
    }

    private void copyServerUrl() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        MCPBrokerClient client = integration != null ? integration.getClient() : null;
        if (client != null && client.getBaseUrl() != null) {
            UIUtils.copyToClipboard(client.getBaseUrl());
        }
    }

    /**
     * Toggles the masked/shown token. The token is read off the EDT; the display updates
     * when the background read completes.
     */
    private void toggleTokenShown() {
        tokenShown = !tokenShown;
        showTokenButton.setText(tokenShown ? "Hide" : "Show");
        if (tokenShown) {
            fetchTokenAsync(false);
        } else {
            tokenLabel.setText(TOKEN_MASK);
        }
    }

    private void copyToken() {
        fetchTokenAsync(true);
    }

    /**
     * Reads the token in the background (the read touches the token file) and then
     * displays or copies it on the EDT.
     */
    private void fetchTokenAsync(boolean copy) {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        MCPBrokerClient client = integration != null ? integration.getClient() : null;
        if (client == null || tokenFetchInFlight) {
            return;
        }
        tokenFetchInFlight = true;
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return client.getToken();
            }

            @Override
            protected void done() {
                tokenFetchInFlight = false;
                try {
                    fetchedToken = get();
                } catch (Exception e) {
                    fetchedToken = null;
                }
                if (tokenShown) {
                    tokenLabel.setText(fetchedToken != null && !fetchedToken.isEmpty() ? fetchedToken : TOKEN_UNAVAILABLE);
                }
                if (copy && fetchedToken != null && !fetchedToken.isEmpty()) {
                    UIUtils.copyToClipboard(fetchedToken);
                }
            }
        };
        worker.execute();
    }

    private void revokeGrant(AutomationPermissionService.AutomationGrant grant) {
        JIPipe.getAutomation().getPermissionService().revokeGrant(grant);
        refresh();
    }

    private void revokeAllGrants() {
        JIPipe.getAutomation().getPermissionService().revokeAllGrants();
        refresh();
    }

    @Override
    public void onMCPStatusChanged(MCPStatusEvent event) {
        // Status events arrive from background threads; marshal to the EDT.
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override
    public void refresh() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        JIPipeDesktopMCPIntegration.StatusSnapshot snapshot = JIPipeDesktopMCPIntegration.currentSnapshot();
        JIPipeDesktopMCPIntegration.LifecycleHooks hooks = JIPipeDesktopMCPIntegration.getLifecycleHooks();
        MCPBrokerClient client = integration != null ? integration.getClient() : null;

        statusLabel.setText(describeState(snapshot));
        String baseUrl = client != null ? client.getBaseUrl() : null;
        brokerUrlLabel.setText(baseUrl != null ? baseUrl : "Not connected");
        copyUrlLabel.setText(baseUrl != null ? baseUrl : "Not connected");
        int boundPort = integration != null && integration.getEndpoint() != null
                ? integration.getEndpoint().getBoundPort() : -1;
        endpointPortLabel.setText(boundPort > 0 ? String.valueOf(boundPort) : "Not running");
        lastOperationLabel.setText(snapshot.lastOperationId() != null ? snapshot.lastOperationId() : "None");
        inFlightLabel.setText(String.valueOf(snapshot.inFlight()));

        // Lifecycle buttons: the hooks are installed by the GUI launcher; without them
        // nothing can start or stop the layer.
        boolean hasHooks = hooks != null;
        startButton.setEnabled(hasHooks && (snapshot.state() == JIPipeDesktopMCPIntegration.LayerState.Off
                || snapshot.state() == JIPipeDesktopMCPIntegration.LayerState.Paused));
        startButton.setToolTipText(hasHooks ? "Connects to or spawns the broker and registers this instance"
                : "The MCP lifecycle is owned by the GUI launcher");
        stopButton.setEnabled(hasHooks && snapshot.state() == JIPipeDesktopMCPIntegration.LayerState.Running);
        stopButton.setToolTipText(hasHooks ? "Unregisters this instance and stops the endpoint (the broker stays alive)"
                : "The MCP lifecycle is owned by the GUI launcher");
        copyUrlButton.setEnabled(baseUrl != null);

        // Token row: only meaningful when token authentication is on.
        boolean requireToken = MCPApplicationSettings.getInstance() != null
                && MCPApplicationSettings.getInstance().isRequireToken();
        tokenRow.setVisible(requireToken && baseUrl != null);
        copyTokenButton.setEnabled(requireToken && baseUrl != null);
        showTokenButton.setEnabled(requireToken && baseUrl != null);
        if (!tokenRow.isVisible()) {
            tokenShown = false;
            fetchedToken = null;
            showTokenButton.setText("Show");
        }
        if (!tokenRow.isVisible()) {
            tokenLabel.setText("");
        } else if (!tokenShown) {
            tokenLabel.setText(TOKEN_MASK);
        } else {
            tokenLabel.setText(fetchedToken != null && !fetchedToken.isEmpty() ? fetchedToken : TOKEN_LOADING);
            // A shown-but-unfetched token (the fetch failed or never ran) recovers here
            // instead of staying on "Loading..." until the next Show click.
            if (fetchedToken == null) {
                fetchTokenAsync(false);
            }
        }

        rebuildGrants();
        rebuildAsks(integration);
    }

    private static final String TOKEN_MASK = "Hidden. Use Show to display, Copy to copy.";
    private static final String TOKEN_UNAVAILABLE = "Unavailable";
    private static final String TOKEN_LOADING = "Loading...";

    private String describeState(JIPipeDesktopMCPIntegration.StatusSnapshot snapshot) {
        switch (snapshot.state()) {
            case Off:
                return "Off";
            case Starting:
                return "Starting...";
            case Paused:
                return "Paused (unregistered, endpoint stopped)";
            case Running:
            default:
                return snapshot.inFlight() > 0 ? "Running (operation in flight)" : "Running";
        }
    }

    /**
     * Rebuilds the grants list: one row per "Allow always" grant with a revoke button,
     * plus a revoke-all button.
     */
    private void rebuildGrants() {
        grantsPanel.removeAll();
        AutomationPermissionService service = JIPipe.getAutomation().getPermissionService();
        List<AutomationPermissionService.AutomationGrant> grants = service.getGrants();
        if (grants.isEmpty()) {
            JLabel empty = new JLabel("No grants");
            empty.setForeground(UIManager.getColor("Label.disabledForeground"));
            grantsPanel.add(empty);
        } else {
            for (AutomationPermissionService.AutomationGrant grant : grants) {
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.add(new JLabel(grant.operationId() + " in " + displayProjectName(grant.projectKey())), BorderLayout.CENTER);
                row.add(UIUtils.createButton("Revoke", JIPipe.RESOURCES.getIcon16("actions/ban.png"), () -> revokeGrant(grant)),
                        BorderLayout.EAST);
                grantsPanel.add(row);
            }
            JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            row.setOpaque(false);
            row.add(UIUtils.createButton("Revoke all grants", JIPipe.RESOURCES.getIcon16("actions/ban.png"), this::revokeAllGrants));
            grantsPanel.add(row);
        }
        grantsPanel.revalidate();
        grantsPanel.repaint();
    }

    /**
     * Rebuilds the pending asks list (count and entries). Answering stays in the bubble.
     */
    private void rebuildAsks(JIPipeDesktopMCPIntegration integration) {
        asksPanel.removeAll();
        List<JIPipeDesktopMCPIntegration.PendingAsk> asks =
                integration != null ? integration.getPendingAsks() : List.of();
        JLabel count = new JLabel(asks.isEmpty() ? "No pending requests" : asks.size() + " pending request(s)");
        count.setForeground(asks.isEmpty() ? UIManager.getColor("Label.disabledForeground")
                : UIManager.getColor("Label.foreground"));
        asksPanel.add(count);
        for (JIPipeDesktopMCPIntegration.PendingAsk ask : asks) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.add(new JLabel(ask.getOperationId() + " in " + displayProjectName(ask.getProjectKey())), BorderLayout.CENTER);
            asksPanel.add(row);
        }
        asksPanel.revalidate();
        asksPanel.repaint();
    }

    /**
     * Display name of a project key: the project name when the window is open, otherwise
     * the shortened key. Must run on the EDT (reads the open-window set).
     */
    private String displayProjectName(String projectKey) {
        if (projectKey == null) {
            return "unknown project";
        }
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            if (window.getSessionId().toString().equals(projectKey)) {
                if (window.getProject() != null && window.getProject().getMetadata() != null
                        && window.getProject().getMetadata().getName() != null) {
                    return window.getProject().getMetadata().getName();
                }
                break;
            }
        }
        if (projectKey.length() > 8) {
            return "..." + projectKey.substring(projectKey.length() - 8);
        }
        return projectKey;
    }

    /**
     * Releases the status event subscription (the monitor window's dispose chain calls
     * this for every page).
     */
    @Override
    public void dispose() {
        JIPipeDesktopMCPIntegration.STATUS_EVENTS.unsubscribe(this);
        super.dispose();
    }
}
