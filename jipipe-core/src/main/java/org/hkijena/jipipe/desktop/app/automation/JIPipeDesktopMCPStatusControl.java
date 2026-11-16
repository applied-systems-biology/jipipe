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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.endpoint.MCPBrokerClient;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPIntegration.LayerState;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPIntegration.StatusSnapshot;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Status bar button that displays the MCP automation layer state and provides the layer
 * controls (pattern of {@link org.hkijena.jipipe.desktop.commons.components.ai.JIPipeDesktopAIStatusControl}).
 * <p>
 * States: off, starting, running (the label shows the last operation, a spinner runs while
 * a call is in flight), and paused. The popup menu offers copy server URL (and token when
 * token authentication is enabled), pause/resume, the operation log window, the MCP
 * settings section, the "Allow always" grants, and the pending Ask requests.
 * <p>
 * The Ask bubble is anchored below this button: it shows the operation id and project name
 * with the three answers (Allow once, Allow always (this project), Deny) and unblocks the
 * waiting endpoint thread. An ask whose project is this window shows here; an ask with an
 * unresolvable target shows in every open window (answering in one window answers for all,
 * first answer wins). One bubble is shown at a time; the queue continues when the current
 * one is answered.
 * <p>
 * All event handling marshals to the event dispatch thread: the status events are emitted
 * from the endpoint's HTTP executor and the MCP lifecycle executor.
 */
public class JIPipeDesktopMCPStatusControl extends JButton implements MCPStatusEventListener {

    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final ImageIcon defaultIcon;
    private final SpinnerIcon busyIcon;
    /**
     * The Ask request currently shown in the bubble (EDT-confined), or null.
     */
    private JIPipeDesktopMCPIntegration.PendingAsk displayedAsk;
    /**
     * The open ask bubble (EDT-confined), or null.
     */
    private JPopupMenu askBubble;

    public JIPipeDesktopMCPStatusControl(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.defaultIcon = JIPipe.RESOURCES.getIcon16("actions/robot.png");
        this.busyIcon = new SpinnerIcon(this);
        initialize();
        updateStatus();
        JIPipeDesktopMCPIntegration.STATUS_EVENTS.subscribeWeak(this);
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setIcon(defaultIcon);
        setText("MCP off");
        setToolTipText("MCP automation layer");
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadMenu);
    }

    @Override
    public void onMCPStatusChanged(MCPStatusEvent event) {
        // Status events arrive from background threads; every UI update marshals to the EDT.
        SwingUtilities.invokeLater(() -> {
            updateStatus();
            refreshAskBubble();
        });
    }

    private void updateStatus() {
        StatusSnapshot snapshot = JIPipeDesktopMCPIntegration.currentSnapshot();
        switch (snapshot.state()) {
            case Off:
                setText("MCP off");
                setIcon(defaultIcon);
                busyIcon.stop();
                break;
            case Starting:
                setText("MCP starting...");
                setIcon(busyIcon);
                busyIcon.start();
                break;
            case Paused:
                setText("MCP paused");
                setIcon(defaultIcon);
                busyIcon.stop();
                break;
            case Running:
                if (snapshot.inFlight() > 0) {
                    setText("MCP: " + (snapshot.lastOperationId() != null ? snapshot.lastOperationId() : "busy"));
                    setIcon(busyIcon);
                    busyIcon.start();
                } else if (snapshot.lastOperationId() != null) {
                    setText("MCP: " + snapshot.lastOperationId());
                    setIcon(defaultIcon);
                    busyIcon.stop();
                } else {
                    setText("MCP ready");
                    setIcon(defaultIcon);
                    busyIcon.stop();
                }
                break;
        }
        setToolTipText(getToolTipFor(snapshot));
    }

    private String getToolTipFor(StatusSnapshot snapshot) {
        switch (snapshot.state()) {
            case Off:
                return "MCP automation layer is off. Start it with the --mcp flag or the MCP settings.";
            case Starting:
                return "MCP automation layer is starting (connecting to or spawning the broker)";
            case Paused:
                return "MCP automation layer is paused. Resume it from the menu.";
            case Running:
                if (snapshot.inFlight() > 0) {
                    return "MCP automation layer is running an operation: "
                            + (snapshot.lastOperationId() != null ? snapshot.lastOperationId() : "unknown");
                }
                return "MCP automation layer is running";
            default:
                return "MCP automation layer";
        }
    }

    // ── Popup menu ──

    private void reloadMenu() {
        popupMenu.removeAll();
        StatusSnapshot snapshot = JIPipeDesktopMCPIntegration.currentSnapshot();
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();

        JMenuItem stateItem = new JMenuItem(describeState(snapshot));
        stateItem.setEnabled(false);
        popupMenu.add(stateItem);
        popupMenu.addSeparator();

        addConnectionMenuItems(integration);
        addLifecycleItems(snapshot, integration);

        popupMenu.addSeparator();
        popupMenu.add(UIUtils.createMenuItem("Open operation log", "Opens the MCP operation log window",
                JIPipe.RESOURCES.getIcon16("actions/history.png"), this::openOperationLog));
        popupMenu.add(UIUtils.createMenuItem("Configure ...", "Opens the settings page for the MCP server",
                JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openSettingsSection));

        popupMenu.addSeparator();
        popupMenu.add(createGrantsMenu());
        popupMenu.add(createPendingAsksMenu());
    }

    private String describeState(StatusSnapshot snapshot) {
        switch (snapshot.state()) {
            case Off:
                return "MCP: off";
            case Starting:
                return "MCP: starting...";
            case Paused:
                return "MCP: paused";
            case Running:
                return snapshot.inFlight() > 0 ? "MCP: running " + snapshot.lastOperationId() : "MCP: running";
            default:
                return "MCP";
        }
    }

    private void addConnectionMenuItems(JIPipeDesktopMCPIntegration integration) {
        MCPBrokerClient client = integration != null ? integration.getClient() : null;
        boolean hasUrl = client != null && client.getBaseUrl() != null;
        JMenuItem copyUrl = UIUtils.createMenuItem("Copy server URL", "Copies the MCP broker URL to the clipboard",
                JIPipe.RESOURCES.getIcon16("actions/copy.png"),
                () -> UIUtils.copyToClipboard(client.getBaseUrl()));
        copyUrl.setEnabled(hasUrl);
        popupMenu.add(copyUrl);

        boolean requireToken = false;
        try {
            requireToken = MCPApplicationSettings.getInstance() != null
                    && MCPApplicationSettings.getInstance().isRequireToken();
        } catch (Exception ignored) {
            // Settings unavailable: no token entry.
        }
        if (hasUrl && requireToken) {
            String token = client.getToken();
            JMenuItem copyToken = UIUtils.createMenuItem("Copy access token",
                    "Copies the MCP broker access token to the clipboard",
                    JIPipe.RESOURCES.getIcon16("actions/key.png"),
                    () -> UIUtils.copyToClipboard(token));
            copyToken.setEnabled(token != null && !token.isEmpty());
            popupMenu.add(copyToken);
        }
    }

    private void addLifecycleItems(StatusSnapshot snapshot, JIPipeDesktopMCPIntegration integration) {
        if (integration == null) {
            return;
        }
        if (snapshot.state() == LayerState.Paused) {
            popupMenu.add(UIUtils.createMenuItem("Resume", "Starts the endpoint and registers this instance at the broker",
                    JIPipe.RESOURCES.getIcon16("actions/circle-play.png"), integration::resumeAsync));
        } else {
            popupMenu.add(UIUtils.createMenuItem("Pause", "Unregisters this instance and stops the endpoint (the broker stays alive)",
                    JIPipe.RESOURCES.getIcon16("actions/circle-pause.png"), integration::pauseAsync));
        }
    }

    /**
     * The "Allow always" grants with per-entry revoke and revoke all.
     */
    private JMenu createGrantsMenu() {
        JMenu menu = new JMenu("Allow-always grants");
        AutomationPermissionService service = JIPipe.getAutomation().getPermissionService();
        List<AutomationPermissionService.AutomationGrant> grants = service.getGrants();
        if (grants.isEmpty()) {
            JMenuItem empty = new JMenuItem("No grants");
            empty.setEnabled(false);
            menu.add(empty);
        } else {
            for (AutomationPermissionService.AutomationGrant grant : grants) {
                menu.add(UIUtils.createMenuItem(grant.operationId() + " in " + displayProjectName(grant.projectKey()),
                        "Revokes this grant: the operation asks again in that project",
                        JIPipe.RESOURCES.getIcon16("actions/ban.png"), () -> service.revokeGrant(grant)));
            }
            menu.addSeparator();
            menu.add(UIUtils.createMenuItem("Revoke all grants", "All operations ask again",
                    JIPipe.RESOURCES.getIcon16("actions/ban.png"), service::revokeAllGrants));
        }
        return menu;
    }

    /**
     * The pending Ask requests. Answering happens in the bubble; the entries jump to it.
     */
    private JMenu createPendingAsksMenu() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        List<JIPipeDesktopMCPIntegration.PendingAsk> asks =
                integration != null ? integration.getPendingAsks() : List.of();
        JMenu menu = new JMenu("Pending Ask requests" + (asks.isEmpty() ? "" : " (" + asks.size() + ")"));
        if (asks.isEmpty()) {
            JMenuItem empty = new JMenuItem("No pending requests");
            empty.setEnabled(false);
            menu.add(empty);
        } else {
            for (JIPipeDesktopMCPIntegration.PendingAsk ask : asks) {
                menu.add(UIUtils.createMenuItem(ask.getOperationId() + " in " + displayProjectName(ask.getProjectKey()),
                        "Shows the ask bubble for this request",
                        JIPipe.RESOURCES.getIcon16("actions/robot.png"), () -> showAskBubble(ask)));
            }
        }
        return menu;
    }

    /**
     * Display name of a project key: the project name when the window is open, otherwise
     * the shortened key. Must run on the EDT (reads the open-window set).
     */
    private String displayProjectName(String projectKey) {
        JIPipeDesktopProjectWindow window = findWindowByKey(projectKey);
        if (window != null && window.getProject() != null && window.getProject().getMetadata() != null
                && window.getProject().getMetadata().getName() != null) {
            return window.getProject().getMetadata().getName();
        }
        if (projectKey != null && projectKey.length() > 8) {
            return "..." + projectKey.substring(projectKey.length() - 8);
        }
        return projectKey != null ? projectKey : "unknown project";
    }

    private JIPipeDesktopProjectWindow findWindowByKey(String projectKey) {
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

    private void openSettingsSection() {
        workbench.openSettingsSection(org.hkijena.jipipe.api.settings.SettingsScope.APPLICATION, MCPApplicationSettings.ID);
    }

    private void openOperationLog() {
        JIPipeDesktopMCPOperationLogUI window = new JIPipeDesktopMCPOperationLogUI(workbench);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
    }

    // ── Ask bubble ──

    /**
     * Updates the bubble after a status change: hides it when the displayed ask is gone,
     * then shows the next queued ask that targets this window (or any window, when the
     * target is unresolvable). EDT only.
     */
    private void refreshAskBubble() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        if (displayedAsk != null && (integration == null || displayedAsk.isAnswered()
                || !integration.getPendingAsks().contains(displayedAsk))) {
            hideAskBubble();
        }
        if (displayedAsk == null && integration != null) {
            for (JIPipeDesktopMCPIntegration.PendingAsk ask : integration.getPendingAsks()) {
                if (isTargetOf(ask)) {
                    showAskBubble(ask);
                    break;
                }
            }
        }
    }

    /**
     * Whether this control should display the ask: its project targets this window, or the
     * target is unresolvable (the brief allows showing those in every window's button).
     */
    private boolean isTargetOf(JIPipeDesktopMCPIntegration.PendingAsk ask) {
        if (ask.getProjectKey() == null) {
            return true;
        }
        JIPipeDesktopProjectWindow target = findWindowByKey(ask.getProjectKey());
        return target == null || target == workbench.getWindow();
    }

    /**
     * Shows the ask bubble anchored below this button. EDT only. One bubble at a time; an
     * already shown ask is not re-shown.
     */
    private void showAskBubble(JIPipeDesktopMCPIntegration.PendingAsk ask) {
        if (displayedAsk == ask && askBubble != null && askBubble.isVisible()) {
            return;
        }
        hideAskBubble();
        displayedAsk = ask;

        askBubble = new JPopupMenu();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.weightx = 1.0;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.anchor = GridBagConstraints.LINE_START;

        JLabel title = new JLabel("MCP asks to run:");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        panel.add(title, labelConstraints);

        labelConstraints.gridy = 1;
        panel.add(new JLabel(ask.getOperationId()), labelConstraints);

        labelConstraints.gridy = 2;
        panel.add(new JLabel("Project: " + displayProjectName(ask.getProjectKey())), labelConstraints);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.add(UIUtils.createButton("Allow once", null, null, () -> answerAsk(ask, true, false)));
        buttons.add(UIUtils.createButton("Allow always (this project)", null, null, () -> answerAsk(ask, true, true)));
        buttons.add(UIUtils.createButton("Deny", null, null, () -> answerAsk(ask, false, false)));

        labelConstraints.gridy = 3;
        labelConstraints.insets = new Insets(8, 0, 0, 0);
        panel.add(buttons, labelConstraints);

        askBubble.add(panel);
        askBubble.show(this, 0, getHeight());
    }

    private void hideAskBubble() {
        if (askBubble != null) {
            askBubble.setVisible(false);
            askBubble = null;
        }
        displayedAsk = null;
    }

    private void answerAsk(JIPipeDesktopMCPIntegration.PendingAsk ask, boolean allow, boolean grantAlways) {
        hideAskBubble();
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        if (integration != null) {
            integration.answerAsk(ask, allow, grantAlways);
        }
    }
}
