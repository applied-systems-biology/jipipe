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

package org.hkijena.jipipe.desktop.app.running;

import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopLogViewer extends JIPipeDesktopProjectWorkbenchPanel implements JIPipeDesktopRunnableLogsCollection.LogClearedEventListener, JIPipeDesktopRunnableLogsCollection.LogEntryAddedEventListener, JIPipeDesktopRunnableLogsCollection.LogUpdatedEventListener {
    private final JList<JIPipeRunnableLogEntry> logEntryJList = new JList<>();
    private JIPipeRunnableLogEntry currentlyDisplayedLog;
    private AutoResizeSplitPane splitPane;

    public JIPipeDesktopLogViewer(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        JIPipeDesktopRunnableLogsCollection.getInstance().getLogClearedEventEmitter().subscribeWeak(this);
        JIPipeDesktopRunnableLogsCollection.getInstance().getLogEntryAddedEventEmitter().subscribeWeak(this);
        JIPipeDesktopRunnableLogsCollection.getInstance().getLogUpdatedEventEmitter().subscribeWeak(this);
        updateEntryList();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        logEntryJList.setCellRenderer(new LogEntryRenderer());

        // List panel
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(logEntryJList), BorderLayout.CENTER);
        logEntryJList.addListSelectionListener(e -> {
            JIPipeRunnableLogEntry selectedValue = logEntryJList.getSelectedValue();
            if (selectedValue != null) {
                showLog(selectedValue);
            }
        });

        JToolBar leftToolbar = new JToolBar();
        leftToolbar.setFloatable(false);
        leftPanel.add(leftToolbar, BorderLayout.NORTH);

        JButton clearButton = new JButton("Clear", UIUtils.getIconFromResources("actions/edit-clear.png"));
        clearButton.addActionListener(e -> JIPipeDesktopRunnableLogsCollection.getInstance().clear());
        leftToolbar.add(clearButton);

        leftToolbar.add(UIUtils.createButton("Mark all as read",
                UIUtils.getIconFromResources("actions/check-double.png"),
                () -> JIPipeDesktopRunnableLogsCollection.getInstance().markAllAsRead()));

        // Split pane
        splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                new JPanel(),
                new AutoResizeSplitPane.DynamicSidebarRatio(450, true));
        add(splitPane, BorderLayout.CENTER);
    }

    @Override
    public void onLogCleared(JIPipeDesktopRunnableLogsCollection.LogClearedEvent event) {
        currentlyDisplayedLog = null;
        updateEntryList();
    }

    @Override
    public void onLogEntryAdded(JIPipeDesktopRunnableLogsCollection.LogEntryAddedEvent event) {
        updateEntryList();
        if (currentlyDisplayedLog != null) {
            logEntryJList.setSelectedValue(currentlyDisplayedLog, true);
            showLog(currentlyDisplayedLog);
        }
    }

    private void updateEntryList() {
        DefaultListModel<JIPipeRunnableLogEntry> model = new DefaultListModel<>();
        for (JIPipeRunnableLogEntry logEntry : JIPipeDesktopRunnableLogsCollection.getInstance().getLogEntries()) {
            if (!model.contains(logEntry)) {
                model.add(0, logEntry);
            }
        }
        logEntryJList.setModel(model);
    }

    public void showLog(JIPipeRunnableLogEntry entry) {
        currentlyDisplayedLog = entry;
        splitPane.setRightComponent(new JIPipeDesktopLogViewLogUI(getDesktopWorkbench(), entry));
        JIPipeDesktopRunnableLogsCollection.getInstance().markAsRead(entry);
        splitPane.applyRatio();
    }

    @Override
    public void onLogUpdated(JIPipeDesktopRunnableLogsCollection.LogUpdatedEvent event) {
        logEntryJList.repaint(50);
    }

    public static class LogEntryRenderer extends JPanel implements ListCellRenderer<JIPipeRunnableLogEntry> {

        private JLabel nameLabel;
        private JLabel timeLabel;
        private JLabel successLabel;

        public LogEntryRenderer() {
            initialize();
        }

        private void initialize() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            Insets border = new Insets(2, 4, 2, 2);

            nameLabel = new JLabel();
            add(nameLabel, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 0;
                    anchor = WEST;
                    insets = border;
                }
            });

            timeLabel = new JLabel();
            timeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
            add(timeLabel, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 1;
                    anchor = WEST;
                    insets = border;
                }
            });

            successLabel = new JLabel();
            add(successLabel, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 2;
                    anchor = WEST;
                    insets = border;
                }
            });

            JPanel glue = new JPanel();
            glue.setOpaque(false);
            add(glue, new GridBagConstraints() {
                {
                    gridx = 2;
                    weightx = 1;
                }
            });
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends JIPipeRunnableLogEntry> list, JIPipeRunnableLogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            nameLabel.setText(value.getName());
            timeLabel.setText(StringUtils.formatDateTime(value.getDateTime()));
            if (value.isSuccess()) {
                if (value.getNotifications().isEmpty()) {
                    nameLabel.setIcon(UIUtils.getIconFromResources("emblems/emblem-rabbitvcs-normal.png"));
                    successLabel.setText("Successful");
                } else {
                    nameLabel.setIcon(UIUtils.getIconFromResources("emblems/warning.png"));
                    successLabel.setText("Successful (" + value.getNotifications().getNotifications().size() + " warnings)");
                }
            } else {
                nameLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
                successLabel.setText("Failed");
            }

            if (isSelected) {
                nameLabel.setForeground(UIManager.getColor("List.selectionForeground"));
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                if (value.isRead()) {
                    nameLabel.setForeground(Color.LIGHT_GRAY);
                } else {
                    nameLabel.setForeground(UIManager.getColor("List.selectionForeground"));
                }
                setBackground(UIManager.getColor("List.background"));
            }

            timeLabel.setForeground(nameLabel.getForeground());
            successLabel.setForeground(nameLabel.getForeground());

            return this;
        }
    }
}
