/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.running;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeProjectRun;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class JIPipeLogViewer extends JIPipeProjectWorkbenchPanel {
    private final JList<JIPipeLogs.LogEntry> logEntryJList = new JList<>();
    private final JTextPane logReader = new JTextPane();
    private JIPipeLogs.LogEntry currentlyDisplayedLog;

    public JIPipeLogViewer(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        JIPipeLogs.getInstance().getEventBus().register(this);
        updateEntryList();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        logReader.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logEntryJList.setCellRenderer(new LogEntryRenderer());

        // List panel
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(logEntryJList), BorderLayout.CENTER);
        logEntryJList.addListSelectionListener(e -> {
            JIPipeLogs.LogEntry selectedValue = logEntryJList.getSelectedValue();
            if (selectedValue != null) {
                showLog(selectedValue);
            }
        });

        JToolBar leftToolbar = new JToolBar();
        leftToolbar.setFloatable(false);
        leftPanel.add(leftToolbar, BorderLayout.NORTH);

        JButton clearButton = new JButton("Clear", UIUtils.getIconFromResources("actions/edit-clear.png"));
        clearButton.addActionListener(e -> JIPipeLogs.getInstance().clear());
        leftToolbar.add(clearButton);

        // Viewer panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JScrollPane(logReader), BorderLayout.CENTER);

        JToolBar rightToolbar = new JToolBar();
        rightToolbar.setFloatable(false);
        rightPanel.add(rightToolbar, BorderLayout.NORTH);

        JButton exportButton = new JButton("Export", UIUtils.getIconFromResources("actions/document-export.png"));
        exportButton.addActionListener(e -> exportLog());
        rightToolbar.add(exportButton);

        JButton openInExternalToolButton = new JButton("Open log in external editor", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        openInExternalToolButton.addActionListener(e -> openLogInExternalTool());
        rightToolbar.add(openInExternalToolButton);

        // Split pane
        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                rightPanel, AutoResizeSplitPane.RATIO_1_TO_3);
        add(splitPane, BorderLayout.CENTER);
    }

    @Subscribe
    public void onLogCleared(JIPipeLogs.LogClearedEvent event) {
        logReader.setText("");
        currentlyDisplayedLog = null;
        updateEntryList();
    }

    @Subscribe
    public void onLogEntryAdded(JIPipeLogs.LogEntryAddedEvent event) {
        updateEntryList();
        if(currentlyDisplayedLog != null) {
            logEntryJList.setSelectedValue(currentlyDisplayedLog, true);
            showLog(currentlyDisplayedLog);
        }
    }

    private void openLogInExternalTool() {
        Path tempFile = RuntimeSettings.generateTempFile("log", ".txt");
        try {
            Files.write(tempFile, logReader.getText().getBytes(StandardCharsets.UTF_8));
            Desktop.getDesktop().open(tempFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void exportLog() {
        Path path = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Data, "Export log", UIUtils.EXTENSION_FILTER_TXT);
        if (path != null) {
            try {
                Files.write(path, logReader.getText().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void showLog(String log) {
        currentlyDisplayedLog = null;
        logEntryJList.clearSelection();
        logReader.setText(log);
    }

    private void updateEntryList() {
        DefaultListModel<JIPipeLogs.LogEntry> model = new DefaultListModel<>();
        for (JIPipeLogs.LogEntry logEntry : JIPipeLogs.getInstance().getLogEntries()) {
            model.add(0, logEntry);
        }
        logEntryJList.setModel(model);
    }

    private void showLog(JIPipeLogs.LogEntry entry) {
        currentlyDisplayedLog = entry;
        logReader.setText(entry.getLog());
    }

    public static class LogEntryRenderer extends JPanel implements ListCellRenderer<JIPipeLogs.LogEntry> {

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

            nameLabel = new JLabel(UIUtils.getIconFromResources("actions/show_log.png"));
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
        public Component getListCellRendererComponent(JList<? extends JIPipeLogs.LogEntry> list, JIPipeLogs.LogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            nameLabel.setText(value.getName());
            timeLabel.setText(StringUtils.formatDateTime(value.getDateTime()));
            successLabel.setText(value.isSuccess() ? "Successful" : "<html><span style=\"color: red;\">Failed</span></html>");
            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
