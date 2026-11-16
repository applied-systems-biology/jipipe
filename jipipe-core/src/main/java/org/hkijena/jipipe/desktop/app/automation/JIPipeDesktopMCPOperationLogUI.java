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
import org.hkijena.jipipe.api.servers.ServerLogEntry;
import org.hkijena.jipipe.api.automation.endpoint.MCPBrokerClient;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Window listing the MCP operation records: the instance-side operation log (one record
 * per dispatch attempt, fed by the endpoint's {@link org.hkijena.jipipe.api.automation.endpoint.AutomationInstanceEndpoint.OperationCallListener})
 * and the shared broker's log entries ({@code GET /control/logs}, fetched through the
 * broker client).
 * <p>
 * The instance-side table refreshes on every status event (push) and on the refresh timer;
 * the broker table is fetched in the background (the HTTP call must not run on the event
 * dispatch thread). Window pattern of {@link org.hkijena.jipipe.desktop.commons.components.ai.monitor.JIPipeDesktopAIMonitorWindow}.
 */
public class JIPipeDesktopMCPOperationLogUI extends JFrame implements MCPStatusEventListener {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final JIPipeDesktopProjectWorkbench workbench;
    private final DefaultTableModel instanceModel;
    private final DefaultTableModel brokerModel;
    private Timer refreshTimer;
    /**
     * Guards against overlapping broker log fetches.
     */
    private boolean brokerFetchInFlight;

    public JIPipeDesktopMCPOperationLogUI(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.instanceModel = new DefaultTableModel(new Object[]{"Time", "Operation", "Project", "Outcome", "Duration"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.brokerModel = new DefaultTableModel(new Object[]{"Time", "Level", "Message"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.refreshTimer = new Timer(5000, e -> {
            if (isDisplayable()) {
                refreshInstanceLog();
                refreshBrokerLogs();
            } else {
                refreshTimer.stop();
            }
        });
        initialize();
        refreshInstanceLog();
        refreshBrokerLogs();
        refreshTimer.start();
        JIPipeDesktopMCPIntegration.STATUS_EVENTS.subscribeWeak(this);
    }

    private void initialize() {
        setTitle(UIUtils.getWindowTitle("MCP Operation Log"));
        setSize(800, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setIconImage(UIUtils.getJIPipeIcon128());
        setLayout(new BorderLayout());

        JTable instanceTable = new JTable(instanceModel);
        instanceTable.setAutoCreateRowSorter(true);
        JTable brokerTable = new JTable(brokerModel);
        brokerTable.setAutoCreateRowSorter(true);

        JPanel instancePanel = new JPanel(new BorderLayout(8, 8));
        instancePanel.setBorder(BorderFactory.createTitledBorder("Instance operations"));
        instancePanel.add(new JScrollPane(instanceTable), BorderLayout.CENTER);

        JPanel brokerPanel = new JPanel(new BorderLayout(8, 8));
        brokerPanel.setBorder(BorderFactory.createTitledBorder("Broker log"));
        brokerPanel.add(new JScrollPane(brokerTable), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, instancePanel, brokerPanel);
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.RIGHT));
        toolbar.add(UIUtils.createButton("Refresh", "Reloads the operation log and the broker log",
                JIPipe.RESOURCES.getIcon16("actions/view-refresh.png"), () -> {
                    refreshInstanceLog();
                    refreshBrokerLogs();
                }));
        toolbar.add(UIUtils.createButton("Clear", "Clears the instance-side operation log",
                JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), this::clearInstanceLog));
        add(toolbar, BorderLayout.NORTH);
    }

    @Override
    public void onMCPStatusChanged(MCPStatusEvent event) {
        // Status events arrive from background threads; the table update marshals to the EDT.
        SwingUtilities.invokeLater(this::refreshInstanceLog);
    }

    private void refreshInstanceLog() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        instanceModel.setRowCount(0);
        if (integration == null) {
            return;
        }
        for (JIPipeDesktopMCPIntegration.MCPOperationLog.Entry entry : integration.getOperationLog().getEntries()) {
            instanceModel.addRow(new Object[]{
                    TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())),
                    entry.operationId(),
                    displayProjectName(entry.projectKey()),
                    entry.outcome(),
                    entry.durationMs() + " ms"});
        }
    }

    private void clearInstanceLog() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        if (integration != null) {
            integration.getOperationLog().clear();
        }
        refreshInstanceLog();
    }

    /**
     * Fetches the broker log entries in the background and applies them on the EDT.
     */
    private void refreshBrokerLogs() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        MCPBrokerClient client = integration != null ? integration.getClient() : null;
        if (client == null || brokerFetchInFlight) {
            return;
        }
        brokerFetchInFlight = true;
        SwingWorker<List<ServerLogEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ServerLogEntry> doInBackground() {
                return client.fetchBrokerLogs(client.getToken());
            }

            @Override
            protected void done() {
                brokerFetchInFlight = false;
                try {
                    applyBrokerLogs(get());
                } catch (Exception e) {
                    // Keep the previously fetched entries; the next refresh retries.
                }
            }
        };
        worker.execute();
    }

    private void applyBrokerLogs(List<ServerLogEntry> entries) {
        brokerModel.setRowCount(0);
        for (ServerLogEntry entry : entries) {
            brokerModel.addRow(new Object[]{
                    TIME_FORMAT.format(Instant.ofEpochMilli(entry.getTimestamp())),
                    entry.getLevel(),
                    entry.getMessage()});
        }
    }

    /**
     * Display name of a project key: the project name when the window is open, otherwise
     * the shortened key. Runs on the EDT (reads the open-window set).
     */
    private String displayProjectName(String projectKey) {
        if (projectKey == null) {
            return "";
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
}
