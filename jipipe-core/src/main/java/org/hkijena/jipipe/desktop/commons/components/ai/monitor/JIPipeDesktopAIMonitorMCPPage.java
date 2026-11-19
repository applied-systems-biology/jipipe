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
import org.hkijena.jipipe.desktop.commons.dialogs.Button;
import org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;
/**
 * MCP page of the AI monitor window: the layer status rows, the lifecycle controls
 * (start/stop through the launcher-installed lifecycle hooks), the broker server URL
 * (with copy), the access token row (a password-style field with Show/Hide, Copy and
 * Rotate; a muted note when token authentication is disabled or no broker URL is
 * available), the client configuration copy (the JSON block that is pasted into an
 * agent's MCP client configuration), the "Allow always" grants with per-entry revoke,
 * and the pending Ask requests (answering stays in the ask bubble anchored to the AI
 * status item).
 * <p>
 * Token reads and the rotation happen off the event dispatch thread (SwingWorker):
 * reading or rewriting the token file on the EDT would add disk I/O to every refresh.
 * The token is re-read on every refresh and before every copy: the token file is shared
 * between instances, so a cached value goes stale as soon as another JIPipe instance
 * rotates it. Rotation reconnects the broker connection through the lifecycle hooks (the
 * layer keeps running) so the replaced broker is picked up immediately.
 * <p>
 * Copy actions fail loud: when token authentication is on and the token is unavailable
 * (unreadable or empty token file), an error is shown instead of copying a headerless
 * client configuration or nothing at all.
 */
public class JIPipeDesktopAIMonitorMCPPage extends JIPipeDesktopAIMonitorPage implements MCPStatusEventListener {

    private static final String TOKEN_FIELD_CARD = "field";
    private static final String TOKEN_NOTE_CARD = "note";

    /**
     * The confirmation button that enables MCP in the application settings and starts
     * the layer (the default button of the disabled-start dialogue).
     */
    private static final Button ENABLE_AND_START = Button.custom("Enable and start");

    /**
     * The echo character of the password-style token field (the masked display).
     */
    private static final char TOKEN_ECHO_CHAR = '\u2022';

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
     * The token row wrapper: a {@link CardLayout} with the password-style field row
     * (token authentication on and a broker URL available) and the muted note (disabled
     * token authentication or not connected).
     */
    private final JPanel tokenRow;
    /**
     * The field card: a non-editable password-style field (masked via the echo character)
     * plus the Show/Hide, Copy and Rotate controls.
     */
    private JPanel tokenFieldRow;
    private JPasswordField tokenField;
    /**
     * The note card: a muted hint shown instead of the field (no field, no buttons).
     */
    private JLabel tokenDisabledLabel;
    private final JButton showTokenButton;
    private final JButton copyTokenButton;
    private final JButton rotateTokenButton;
    private final JButton copySettingsButton;
    private final JPanel grantsPanel;
    private final JPanel asksPanel;

    /**
     * Guards overlapping background token reads.
     */
    private boolean tokenFetchInFlight;
    /**
     * A copy action parked while a token read is in flight (single slot: the latest user
     * action wins), or null. Keeps the copy buttons responsive: the overlapping READ is
     * dropped (the shared guard), but the user action is not.
     */
    private Runnable pendingTokenAction;
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
        this.rotateTokenButton = UIUtils.createButton("Rotate", JIPipe.RESOURCES.getIcon16("actions/database-change-key.png"), this::rotateToken);
        this.copySettingsButton = UIUtils.createButton("Copy MCP settings", JIPipe.RESOURCES.getIcon16("actions/copy.png"), this::copyMcpSettings);
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

        // Client configuration
        formPanel.addGroupHeader("Client configuration", JIPipe.RESOURCES.getIcon16("actions/code-block.png"));
        JLabel clientConfigHint = new JLabel("Paste this into your agent's MCP client configuration");
        clientConfigHint.setForeground(UIManager.getColor("Label.disabledForeground"));
        JPanel clientConfigRow = new JPanel(new BorderLayout(8, 0));
        clientConfigRow.setOpaque(false);
        clientConfigRow.add(clientConfigHint, BorderLayout.CENTER);
        clientConfigRow.add(copySettingsButton, BorderLayout.EAST);
        formPanel.addToForm(clientConfigRow, new JLabel("MCP settings"));

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
     * Builds the token row: a card layout with the password-style field row (masked
     * token plus Show/Hide, Copy and Rotate) and the muted note shown when token
     * authentication is disabled or no broker URL is available.
     */
    private JPanel createTokenRow() {
        tokenField = new JPasswordField();
        tokenField.setEditable(false);
        tokenField.setEchoChar(TOKEN_ECHO_CHAR);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        buttons.add(showTokenButton);
        buttons.add(copyTokenButton);
        buttons.add(rotateTokenButton);

        tokenFieldRow = new JPanel(new BorderLayout(8, 0));
        tokenFieldRow.setOpaque(false);
        tokenFieldRow.add(tokenField, BorderLayout.CENTER);
        tokenFieldRow.add(buttons, BorderLayout.EAST);

        tokenDisabledLabel = new JLabel();
        tokenDisabledLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        JPanel row = new JPanel(new CardLayout(0, 0));
        row.setOpaque(false);
        row.add(tokenFieldRow, TOKEN_FIELD_CARD);
        row.add(tokenDisabledLabel, TOKEN_NOTE_CARD);
        return row;
    }

    /**
     * The start control: starts the layer through the lifecycle hooks. The start request
     * is only swallowed silently by the settings when the layer never started (state
     * off) and "Enable MCP" is disabled while the AI feature is on: in that state a
     * confirmation offers to enable MCP and start it now. The confirmed write goes
     * through the parameter access (not the setter), so the parameter change event
     * starts the layer via the lifecycle owner's subscriber: exactly the one start path
     * the settings checkbox uses, and the checkbox flips consistently. Every other
     * state (no hooks, AI feature off, paused or running layer, MCP already enabled)
     * keeps the previous behavior.
     */
    private void startMcp() {
        JIPipeDesktopMCPIntegration.LifecycleHooks hooks = JIPipeDesktopMCPIntegration.getLifecycleHooks();
        if (hooks == null) {
            return;
        }
        MCPApplicationSettings settings = MCPApplicationSettings.getInstance();
        JIPipeDesktopMCPIntegration.StatusSnapshot snapshot = JIPipeDesktopMCPIntegration.currentSnapshot();
        boolean settingsWouldSwallowStart = settings != null
                && snapshot.state() == JIPipeDesktopMCPIntegration.LayerState.Off
                && AIApplicationSettings.getInstance().isEnableAI()
                && !settings.isEnable();
        if (settingsWouldSwallowStart) {
            // asDefault() returns a copy; keep that instance for the identity
            // comparison (the chrome records the exact instance).
            Button enableAndStart = ENABLE_AND_START.asDefault();
            Optional<Button> choice = JIPipeDesktopDialogs.choose(this, "MCP is disabled",
                    "MCP is currently disabled in the application settings. Enable MCP and start it now?",
                    enableAndStart, Button.CANCEL);
            if (choice.filter(button -> button == enableAndStart).isEmpty()) {
                return;
            }
            settings.setParameter("enable", true);
            return;
        }
        hooks.startMcpAsync();
    }

    private void stopMcp() {
        JIPipeDesktopMCPIntegration.LifecycleHooks hooks = JIPipeDesktopMCPIntegration.getLifecycleHooks();
        if (hooks != null) {
            hooks.stopMcpAsync();
        }
    }

    private void copyServerUrl() {
        MCPBrokerClient client = activeClient();
        if (client != null && client.getBaseUrl() != null) {
            UIUtils.copyToClipboard(client.getBaseUrl());
        }
    }

    /**
     * The broker client of the active MCP integration, or null when the layer is not
     * installed.
     */
    private static MCPBrokerClient activeClient() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        return integration != null ? integration.getClient() : null;
    }

    /**
     * Toggles the masked/shown token. The token is read off the EDT; the display updates
     * when the background read completes.
     */
    private void toggleTokenShown() {
        tokenShown = !tokenShown;
        showTokenButton.setText(tokenShown ? "Hide" : "Show");
        applyTokenDisplay();
        if (tokenShown && fetchedToken == null) {
            fetchTokenAsync(null);
        }
    }

    /**
     * Applies the shown/hidden state to the password-style field: the mask is the echo
     * character, the clear text is only rendered while shown. The text is always the
     * last fetched token (masked unless shown), so the field tracks the current token
     * even after another instance rotated it. Without a token, the placeholder is shown
     * unmasked (it is a status, not a secret).
     */
    private void applyTokenDisplay() {
        if (fetchedToken != null && !fetchedToken.isEmpty()) {
            tokenField.setEchoChar(tokenShown ? (char) 0 : TOKEN_ECHO_CHAR);
            tokenField.setText(fetchedToken);
        } else {
            tokenField.setEchoChar((char) 0);
            tokenField.setText(tokenFetchInFlight ? TOKEN_LOADING : TOKEN_UNAVAILABLE);
        }
    }

    private void copyToken() {
        fetchTokenAsync(this::copyFetchedToken);
    }

    /**
     * Copies the fetched token, or shows the fail-loud error when the background read
     * returned nothing (the token file is unreadable or empty).
     */
    private void copyFetchedToken() {
        if (fetchedToken != null && !fetchedToken.isEmpty()) {
            UIUtils.copyToClipboard(fetchedToken);
        } else {
            showTokenUnavailableError("no access token can be copied.");
        }
    }

    /**
     * Copies the MCP client configuration (the JSON block pasted into an agent's MCP
     * client configuration): the broker URL with the {@code /mcp} path, plus the shared
     * token as an Authorization header when token authentication is on. The token is
     * always re-read in the background (the read touches the token file, which other
     * JIPipe instances share and may have rotated without this instance noticing), so
     * the copied configuration carries the current token; the cached fast path was
     * removed for exactly that reason. When token authentication is on and the token is
     * unavailable, an error is shown instead of copying a headerless configuration.
     */
    private void copyMcpSettings() {
        MCPBrokerClient client = activeClient();
        if (client == null || client.getBaseUrl() == null) {
            return;
        }
        if (tokenAuthEnabled()) {
            fetchTokenAsync(() -> {
                // The connection can drop while the token is being read.
                if (client.getBaseUrl() == null) {
                    return;
                }
                if (fetchedToken == null || fetchedToken.isEmpty()) {
                    showTokenUnavailableError("no client configuration can be copied.");
                    return;
                }
                copyMcpSettingsJson(client, fetchedToken);
            });
        } else {
            copyMcpSettingsJson(client, null);
        }
    }

    /**
     * The fail-loud notice for a copy action when the token is unavailable (unreadable
     * or empty token file): nothing is copied, because a headerless client configuration
     * would fail the token-authenticated broker.
     */
    private void showTokenUnavailableError(String consequence) {
        JIPipeDesktopDialogs.error(this, "MCP token unavailable",
                "<html>The MCP token is unavailable, so " + consequence + "<br/>"
                        + "The token file is shared between JIPipe instances: another instance may have "
                        + "rotated it, or the file is currently unreadable.</html>", null);
    }

    /**
     * Renders and copies the client configuration with the given token (null when token
     * authentication is off or the token is unavailable).
     */
    private void copyMcpSettingsJson(MCPBrokerClient client, String token) {
        UIUtils.copyToClipboard(MCPBrokerClient.clientConfigurationJson(client.getBaseUrl(), token));
    }

    /**
     * Whether token authentication is required (the {@code require-token} setting).
     */
    private static boolean tokenAuthEnabled() {
        MCPApplicationSettings settings = MCPApplicationSettings.getInstance();
        return settings != null && settings.isRequireToken();
    }

    /**
     * Rotates the shared broker access token after an explicit confirmation. The file
     * rewrite and the reconnect run off the EDT:
     * <ol>
     *     <li>A confirmation dialog warns that rotating invalidates the current token,
     *     that agents configured with the old token must be updated, and that the shared
     *     broker is replaced on the next connection.</li>
     *     <li>On confirm, {@link MCPBrokerClient#rotateToken()} atomically regenerates the
     *     token file, clears the cached token, and returns the new token (background
     *     thread).</li>
     *     <li>The broker connection is re-established through the lifecycle hooks
     *     (reconnectMcpAsync; the layer keeps running): the connection state is
     *     invalidated, so the next connect re-runs the full discovery. The running broker
     *     still holds the old token, the health check fails (401), the stale discovery
     *     file is deleted, and a fresh broker spawns with the new token. Other running
     *     JIPipe instances converge the same way on their next connection (the 401 clears
     *     their cached token, so they re-read the rotated file).</li>
     *     <li>The page state is refreshed afterwards; the status events refresh it again
     *     while the layer reconnects.</li>
     * </ol>
     */
    private void rotateToken() {
        if (!JIPipeDesktopDialogs.confirm(this, "Rotate access token",
                "<html><b>Rotating invalidates the current access token.</b><br/>"
                        + "Agents configured with the old token must be updated with the new token.<br/>"
                        + "The shared broker is replaced on the next connection (this instance and other "
                        + "running JIPipe instances reconnect automatically).<br/><br/>"
                        + "Rotate the token now?</html>")) {
            return;
        }
        rotateTokenButton.setEnabled(false);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                MCPBrokerClient client = activeClient();
                return client != null ? client.rotateToken() : null;
            }

            @Override
            protected void done() {
                rotateTokenButton.setEnabled(true);
                String rotated;
                try {
                    rotated = get();
                } catch (Exception e) {
                    UIUtils.showErrorDialog(getDesktopWorkbench(), JIPipeDesktopAIMonitorMCPPage.this, e);
                    refresh();
                    return;
                }
                if (rotated == null) {
                    refresh();
                    return;
                }
                // The field immediately renders the new token (still masked unless shown).
                fetchedToken = rotated;
                applyTokenDisplay();
                // Reconnect the broker connection so the replaced broker is picked up
                // (the layer keeps running; only the connection is re-established).
                JIPipeDesktopMCPIntegration.LifecycleHooks hooks = JIPipeDesktopMCPIntegration.getLifecycleHooks();
                if (hooks != null) {
                    hooks.reconnectMcpAsync();
                }
                refresh();
            }
        };
        worker.execute();
    }

    /**
     * Reads the token in the background (the read touches the token file) and then runs
     * the given action on the EDT (display the token, copy it, or copy the client
     * configuration). The action may be null. Overlapping reads are guarded: while a
     * read is in flight, a given action is parked in the {@link #pendingTokenAction}
     * single slot (the latest action wins) and runs after the in-flight read completes,
     * so a copy click during the read is not silently dropped.
     */
    private void fetchTokenAsync(Runnable onComplete) {
        MCPBrokerClient client = activeClient();
        if (client == null) {
            return;
        }
        if (tokenFetchInFlight) {
            if (onComplete != null) {
                pendingTokenAction = onComplete;
            }
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
                applyTokenDisplay();
                Runnable parked = pendingTokenAction;
                pendingTokenAction = null;
                if (onComplete != null) {
                    onComplete.run();
                }
                if (parked != null) {
                    parked.run();
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
        copySettingsButton.setEnabled(baseUrl != null);
        copySettingsButton.setToolTipText(baseUrl != null
                ? "Copies the MCP client configuration (JSON) to the clipboard"
                : "MCP is not running");

        // Token row: the password-style field when token authentication is on and a broker
        // URL is available; otherwise a muted note (no field, no buttons).
        boolean requireToken = tokenAuthEnabled();
        boolean tokenUiAvailable = requireToken && baseUrl != null;
        CardLayout tokenCards = (CardLayout) tokenRow.getLayout();
        tokenCards.show(tokenRow, tokenUiAvailable ? TOKEN_FIELD_CARD : TOKEN_NOTE_CARD);
        if (tokenUiAvailable) {
            showTokenButton.setEnabled(true);
            copyTokenButton.setEnabled(true);
            rotateTokenButton.setEnabled(hooks != null);
            rotateTokenButton.setToolTipText(hooks != null
                    ? "Replaces the shared token (agents configured with the old token must be updated)"
                    : "The MCP lifecycle is owned by the GUI launcher");
            // The field always renders the real token (masked unless shown). The token is
            // re-read on EVERY refresh, not only when never fetched: the token file is
            // shared between instances, so another JIPipe instance may have rotated it
            // since the last read. The read is off-EDT and overlap-guarded; the display
            // updates on completion.
            tokenField.setText("");
            fetchTokenAsync(null);
        } else {
            tokenShown = false;
            fetchedToken = null;
            showTokenButton.setText("Show");
            tokenDisabledLabel.setText(requireToken
                    ? "Not connected (no broker URL is available)"
                    : "Token authentication is disabled (enable 'Require token' in the MCP settings)");
        }

        rebuildGrants();
        rebuildAsks(integration);
    }

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
