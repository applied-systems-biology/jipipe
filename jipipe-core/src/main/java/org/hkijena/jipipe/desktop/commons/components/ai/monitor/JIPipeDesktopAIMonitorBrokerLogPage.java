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
import org.hkijena.jipipe.api.servers.ServerLogEntry;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPIntegration;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEvent;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEventListener;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MCP broker log page of the AI monitor window: the shared broker's log entries
 * ({@code GET /control/logs}), fetched through the broker client in a SwingWorker with
 * an in-flight guard (the HTTP call must never run on the event dispatch thread; ported
 * from the former MCP operation log window).
 * <p>
 * When the client or the broker is absent, the page shows a muted "MCP is not running"
 * state instead of errors.
 */
public class JIPipeDesktopAIMonitorBrokerLogPage extends JIPipeDesktopAIMonitorPage implements MCPStatusEventListener {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final DefaultTableModel model;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContent = new JPanel(cardLayout);
    private final JLabel emptyLabel = new JLabel("MCP is not running");

    /**
     * Guards against overlapping broker log fetches.
     */
    private boolean fetchInFlight;

    public JIPipeDesktopAIMonitorBrokerLogPage(JIPipeDesktopAIMonitorWindow window) {
        super(window);
        this.model = new DefaultTableModel(new Object[]{"Time", "Level", "Message"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        initialize();
        refresh();

        // Status events trigger a refresh (the client may have appeared or gone). Weak
        // subscription; dispose() unsubscribes.
        JIPipeDesktopMCPIntegration.STATUS_EVENTS.subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);

        emptyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel emptyState = new JPanel(new FlowLayout(FlowLayout.CENTER));
        emptyState.setOpaque(false);
        emptyState.add(emptyLabel);

        cardContent.setOpaque(false);
        cardContent.add(scrollPane, "table");
        cardContent.add(emptyState, "empty");
        add(cardContent, BorderLayout.CENTER);
    }

    @Override
    public void onMCPStatusChanged(MCPStatusEvent event) {
        // Status events arrive from background threads; marshal to the EDT.
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override
    public void refresh() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        MCPBrokerClient client = integration != null ? integration.getClient() : null;
        if (client == null || client.getBaseUrl() == null) {
            cardLayout.show(cardContent, "empty");
            return;
        }
        cardLayout.show(cardContent, "table");
        fetchBrokerLogsAsync(client);
    }

    /**
     * Fetches the broker log entries in the background and applies them on the EDT. A
     * fetch failure keeps the previously fetched entries; the next refresh retries.
     */
    private void fetchBrokerLogsAsync(MCPBrokerClient client) {
        if (fetchInFlight) {
            return;
        }
        fetchInFlight = true;
        SwingWorker<List<ServerLogEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ServerLogEntry> doInBackground() {
                // The bearer token is read off the EDT (the first read touches the token file).
                return client.fetchBrokerLogs(client.getToken());
            }

            @Override
            protected void done() {
                fetchInFlight = false;
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
        model.setRowCount(0);
        if (entries == null) {
            return;
        }
        for (ServerLogEntry entry : entries) {
            model.addRow(new Object[]{
                    TIME_FORMAT.format(Instant.ofEpochMilli(entry.getTimestamp())),
                    entry.getLevel(),
                    entry.getMessage()});
        }
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
