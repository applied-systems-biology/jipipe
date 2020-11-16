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
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.jipipe.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JIPipeLogViewer extends JIPipeProjectWorkbenchPanel {
    private final RuntimeSettings runtimeSettings = RuntimeSettings.getInstance();
    private List<LogEntry> logEntries = new ArrayList<>();
    private JList<LogEntry> logEntryJList = new JList<>();
    private MarkdownReader logReader = new MarkdownReader(true);

    public JIPipeLogViewer(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        logEntryJList.setCellRenderer(new LogEntryRenderer());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                logEntryJList,
                logReader);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        add(splitPane, BorderLayout.CENTER);

        logEntryJList.addListSelectionListener(e -> {
            LogEntry selectedValue = logEntryJList.getSelectedValue();
            if (selectedValue != null) {
                showLog(selectedValue);
            }
        });
    }

    private void showLog(LogEntry entry) {
        logReader.setDocument(new MarkdownDocument(entry.getLog().replace("\n", "\n\n")));
    }

    private void pushToLog(JIPipeRunnable run, boolean success) {
        if (run instanceof JIPipeRun && ((JIPipeRun) run).getProject() != getProject())
            return;
        StringBuilder log = run.getProgressInfo().getLog();
        if (log != null && log.length() > 0) {
            if (logEntries.size() + 1 > runtimeSettings.getLogLimit())
                logEntries.remove(0);
            logEntries.add(new LogEntry(run.getTaskLabel(), LocalDateTime.now(), log.toString(), success));
            DefaultListModel<LogEntry> model = new DefaultListModel<>();
            for (LogEntry logEntry : logEntries) {
                model.add(0, logEntry);
            }
            logEntryJList.setModel(model);
        }
    }

    @Subscribe
    public void onRunFinished(RunUIWorkerFinishedEvent event) {
        pushToLog(event.getRun(), true);
    }

    @Subscribe
    public void onRunCancelled(RunUIWorkerInterruptedEvent event) {
        pushToLog(event.getRun(), false);
    }

    public static class LogEntry {
        private final String name;
        private final LocalDateTime dateTime;
        private final String log;
        private final boolean success;

        public LogEntry(String name, LocalDateTime dateTime, String log, boolean success) {
            this.name = name;
            this.dateTime = dateTime;
            this.log = log;
            this.success = success;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public String getLog() {
            return log;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getName() {
            return name;
        }
    }

    public static class LogEntryRenderer extends JPanel implements ListCellRenderer<LogEntry> {

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
        public Component getListCellRendererComponent(JList<? extends LogEntry> list, LogEntry value, int index, boolean isSelected, boolean cellHasFocus) {
            nameLabel.setText(value.getName());
            timeLabel.setText(StringUtils.formatDateTime(value.dateTime));
            successLabel.setText(value.success ? "Successful" : "<html><span style=\"color: red;\">Failed</span></html>");
            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
