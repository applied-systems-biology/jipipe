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
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPIntegration;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEvent;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEventListener;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MCP operations page of the AI monitor window: the instance-side operation ring (one
 * record per dispatch attempt, fed by the endpoint's
 * {@link org.hkijena.jipipe.api.automation.endpoint.AutomationInstanceEndpoint.OperationCallListener})
 * as a table plus a Clear button. Refreshes on the monitor window's timer and on MCP
 * status events (ported from the former MCP operation log window).
 */
public class JIPipeDesktopAIMonitorMCPOperationsPage extends JIPipeDesktopAIMonitorPage implements MCPStatusEventListener {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final DefaultTableModel model;
    private final JPanel emptyState;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContent = new JPanel(cardLayout);

    public JIPipeDesktopAIMonitorMCPOperationsPage(JIPipeDesktopAIMonitorWindow window) {
        super(window);
        this.model = new DefaultTableModel(new Object[]{"Time", "Operation", "Project", "Outcome", "Duration"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.emptyState = new JPanel(new FlowLayout(FlowLayout.CENTER));
        this.emptyState.setOpaque(false);
        JLabel emptyLabel = new JLabel("MCP is not running");
        emptyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        this.emptyState.add(emptyLabel);

        initialize();
        refresh();

        // Status events refresh the table (push). Weak subscription; dispose() unsubscribes.
        JIPipeDesktopMCPIntegration.STATUS_EVENTS.subscribeWeak(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);

        cardContent.setOpaque(false);
        cardContent.add(scrollPane, "table");
        cardContent.add(emptyState, "empty");
        add(cardContent, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setOpaque(false);
        toolbar.add(UIUtils.createButton("Clear", "Clears the operation log",
                JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"), this::clearLog));
        add(toolbar, BorderLayout.NORTH);
    }

    private void showCard(String card) {
        cardLayout.show(cardContent, card);
    }

    private void clearLog() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        if (integration != null) {
            integration.getOperationLog().clear();
        }
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
        model.setRowCount(0);
        if (integration == null) {
            showCard("empty");
            return;
        }
        showCard("table");
        List<JIPipeDesktopMCPIntegration.MCPOperationLog.Entry> entries = integration.getOperationLog().getEntries();
        for (JIPipeDesktopMCPIntegration.MCPOperationLog.Entry entry : entries) {
            model.addRow(new Object[]{
                    TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp())),
                    entry.operationId(),
                    displayProjectName(entry.projectKey()),
                    entry.outcome(),
                    entry.durationMs() + " ms"});
        }
    }

    /**
     * Display name of a project key: the project name when the window is open, otherwise
     * the shortened key. Must run on the EDT (reads the open-window set).
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
