package org.hkijena.jipipe.desktop.commons.components.servers.monitor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.servers.JIPipeServerInstance;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Overview page for the server monitor window.
 *
 * <p>Displays all active server instances in a {@link JXTable} with their details
 * (name, type, state, port, PID, uptime, and health) and provides a clickable
 * "Stop" button per instance via a {@link MouseListener} on the action column.</p>
 *
 * <p>A header component (title label and "Stop all" button) is placed in
 * {@link BorderLayout#NORTH}, the table inside a {@link JScrollPane} in
 * {@link BorderLayout#CENTER}.</p>
 */
public class JIPipeDesktopServerMonitorOverviewPage extends JIPipeDesktopServerMonitorPage {

    /**
     * Index of the "Action" column that hosts the stop button.
     */
    private static final int ACTION_COLUMN_INDEX = 7;

    private JXTable table;
    private InstanceTableModel tableModel;
    private Timer refreshTimer;

    public JIPipeDesktopServerMonitorOverviewPage(JIPipeDesktopServerMonitorWindow window) {
        super(window);
        initialize();
        this.refreshTimer = new Timer(5000, this::onRefreshTimer);
        refreshTimer.start();
    }

    private void onRefreshTimer(ActionEvent e) {
        if (isDisplayable()) {
            refresh();
        } else {
            refreshTimer.stop();
        }
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        // ---- Header (NORTH) ----
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Active Server Instances",
                JIPipe.RESOURCES.getIcon16("actions/database.png"), SwingConstants.LEFT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 8));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton stopAllButton = UIUtils.createButton("Stop all",
                JIPipe.RESOURCES.getIcon16("actions/process-stop.png"), this::stopAll);
        headerPanel.add(stopAllButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // ---- Table (CENTER) ----
        tableModel = new InstanceTableModel();
        table = new JXTable(tableModel);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSortable(true);
        table.setHighlighters(HighlighterFactory.createSimpleStriping(UIManager.getColor("Panel.background")));
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        // Button renderer for the action column
        table.getColumn(tableModel.getColumnName(ACTION_COLUMN_INDEX))
                .setCellRenderer(new StopButtonRenderer());

        // Column widths
        setPreferredColumnWidths();

        // Handle clicks on the stop button (single click, via MouseListener)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewColumn = table.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewColumn < 0) {
                    return;
                }
                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                if (modelColumn != ACTION_COLUMN_INDEX) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(viewRow);
                JIPipeServerInstance<?> instance = tableModel.getInstance(modelRow);
                if (instance != null && isRunning(instance)) {
                    JIPipe.getInstance().getServerService().stopInstance(instance);
                    refresh();
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setPreferredColumnWidths() {
        int[] widths = {220, 140, 90, 60, 70, 110, 70, 90};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumn(table.getColumnName(i)).setPreferredWidth(widths[i]);
        }
    }

    @Override
    public void refresh() {
        List<JIPipeServerInstance<?>> instances = JIPipe.getInstance().getServerService().getActiveInstances();
        tableModel.setInstances(instances);
    }

    /**
     * Stops all currently running instances.
     */
    private void stopAll() {
        List<JIPipeServerInstance<?>> instances = JIPipe.getInstance().getServerService().getActiveInstances();
        for (JIPipeServerInstance<?> instance : instances) {
            if (isRunning(instance)) {
                JIPipe.getInstance().getServerService().stopInstance(instance);
            }
        }
        refresh();
    }

    /**
     * Returns whether the instance is in a state where it can be stopped.
     *
     * @param instance the instance
     * @return true if a stop can be issued
     */
    private static boolean isRunning(JIPipeServerInstance<?> instance) {
        MicroserviceState state = instance.getState();
        return state != MicroserviceState.Stopped && state != MicroserviceState.Stopping;
    }

    /**
     * Formats the instance uptime based on {@link JIPipeServerInstance#getStartedAt()}.
     *
     * @param instance the instance
     * @return a human-readable uptime string, or "N/A"
     */
    private static String formatUptime(JIPipeServerInstance<?> instance) {
        Instant startedAt = instance.getStartedAt();
        if (startedAt == null) {
            return "N/A";
        }
        Duration uptime = Duration.between(startedAt, Instant.now());
        long hours = uptime.toHours();
        long minutes = uptime.minusHours(hours).toMinutes();
        long seconds = uptime.minusHours(hours).minusMinutes(minutes).getSeconds();
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        refreshTimer.stop();
    }

    /**
     * Table model backed by {@link JIPipe#getServerService()} active instances.
     */
    private static class InstanceTableModel extends AbstractTableModel {

        private final String[] columnNames = {"Name", "Type", "State", "Port", "PID", "Uptime", "Healthy", "Action"};
        private List<JIPipeServerInstance<?>> instances = Collections.emptyList();

        public void setInstances(List<JIPipeServerInstance<?>> instances) {
            this.instances = instances != null ? instances : Collections.emptyList();
            fireTableDataChanged();
        }

        /**
         * Returns the instance backing the given model row.
         *
         * @param row the model row index
         * @return the instance, or null if out of range
         */
        public JIPipeServerInstance<?> getInstance(int row) {
            if (row < 0 || row >= instances.size()) {
                return null;
            }
            return instances.get(row);
        }

        @Override
        public int getRowCount() {
            return instances.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            switch (column) {
                case 3:
                    return String.class; // Port (as String to avoid locale-specific thousands separators)
                case 6:
                    return Boolean.class; // Healthy
                case 7:
                    return JIPipeServerInstance.class; // Action (instance used by the renderer)
                default:
                    return String.class;
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            JIPipeServerInstance<?> instance = instances.get(row);
            switch (column) {
                case 0:
                    return instance.getDisplayName();
                case 1:
                    return instance.getServerTypeId();
                case 2:
                    return instance.getState().name();
                case 3:
                    return String.valueOf(instance.getPort()); // Port (as String to avoid locale-specific thousands separators)
                case 4:
                    return instance.getProcess() != null ? String.valueOf(instance.getProcess().pid()) : "N/A";
                case 5:
                    return formatUptime(instance);
                case 6:
                    return instance.isHealthy();
                case 7:
                    return instance;
                default:
                    return null;
            }
        }
    }

    /**
     * Renders the "Stop" button in the action column. The button is enabled only
     * when the backing instance is in a stoppable state.
     */
    private static class StopButtonRenderer extends JButton implements TableCellRenderer {

        StopButtonRenderer() {
            super("Stop", JIPipe.RESOURCES.getIcon16("actions/process-stop.png"));
            setOpaque(true);
            setBorderPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof JIPipeServerInstance) {
                setEnabled(isRunning((JIPipeServerInstance<?>) value));
            } else {
                setEnabled(false);
            }
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }
}
