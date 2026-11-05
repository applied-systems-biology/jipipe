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

package org.hkijena.jipipe.desktop.app.running.logs;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopGenericNotificationInboxUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Displays one log entry of the Tools &gt; Logs collection. The log is rendered from the entry's
 * NDJSON data file on demand (when the entry is selected/displayed) — the collection holds no
 * rendered text. The default view keeps the last {@link #LOG_LINE_LIMIT} lines (bounded memory,
 * independent of the file size); "Show full log" renders everything.
 */
public class JIPipeDesktopLogViewLogUI extends JIPipeDesktopWorkbenchPanel {
    public static int LOG_LINE_LIMIT = 500;
    private final JIPipeRunnableLogEntry logEntry;
    private final JCheckBox displayFullLog = new JCheckBox("Show full log");
    private final JTextPane logReader = new JTextPane();
    private JScrollPane scrollPane;

    public JIPipeDesktopLogViewLogUI(JIPipeDesktopWorkbench workbench, JIPipeRunnableLogEntry logEntry) {
        super(workbench);
        this.logEntry = logEntry;
        initialize();
        reloadLog();
    }

    private void reloadLog() {
        // Render on demand from the entry's data. The bounded view streams the render through a
        // ring buffer of the last LOG_LINE_LIMIT lines (memory stays bounded for any file size);
        // the full-log view renders everything, as before.
        StringBuilder full = displayFullLog.isSelected() ? new StringBuilder() : null;
        Deque<String> tail = displayFullLog.isSelected() ? null : new ArrayDeque<>(LOG_LINE_LIMIT);
        int[] totalLines = {0};
        logEntry.renderText(JIPipeLogLevel.INFO, line -> {
            totalLines[0]++;
            if (full != null) {
                full.append(line).append('\n');
            } else {
                if (tail.size() >= LOG_LINE_LIMIT) {
                    tail.pollFirst();
                }
                tail.addLast(line);
            }
        });
        if (full != null) {
            logReader.setText(full.toString());
        } else if (totalLines[0] <= LOG_LINE_LIMIT) {
            logReader.setText(joinLines(tail));
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
            builder.append("Only displaying the last ").append(LOG_LINE_LIMIT).append(" lines\n\n\n");
            builder.append(joinLines(tail));
            logReader.setText(builder.toString());
        }
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()));
    }

    private static String joinLines(Deque<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }
        return builder.toString();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Init base layout
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        add(toolBar, BorderLayout.NORTH);
        JIPipeDesktopTabPane tabPane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.Style.Right);
        add(tabPane, BorderLayout.CENTER);

        // Init reader
        logReader.setFont(new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal()));
        scrollPane = new JScrollPane(logReader);
        tabPane.addTab("Log", JIPipe.RESOURCES.getIcon32("actions/rabbitvcs-show_log.png"), scrollPane, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Init notifications
        tabPane.addTab("Notifications",
                logEntry.getNotifications().isEmpty() ? JIPipe.RESOURCES.getIcon32("actions/notifications.png") : JIPipe.RESOURCES.getIcon32("status/dialog-error.png"),
                new JIPipeDesktopGenericNotificationInboxUI(getDesktopWorkbench(), logEntry.getNotifications()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Init toolbar
        toolBar.add(displayFullLog);
        displayFullLog.addActionListener(e -> reloadLog());

        toolBar.add(Box.createHorizontalGlue());

        JButton exportDataButton = new JButton("Export data file", JIPipe.RESOURCES.getIcon16("actions/document-export.png"));
        exportDataButton.addActionListener(e -> exportDataFile());
        // Entries without a data file (silent/stdout stores) have nothing to export
        exportDataButton.setEnabled(logEntry.getPath() != null);
        exportDataButton.setToolTipText("Saves the NDJSON data file of this log");
        toolBar.add(exportDataButton);

        JButton exportButton = new JButton("Export text", JIPipe.RESOURCES.getIcon16("actions/document-export.png"));
        exportButton.addActionListener(e -> exportLog());
        toolBar.add(exportButton);

        JButton openInExternalToolButton = new JButton("Open in external editor", JIPipe.RESOURCES.getIcon16("actions/open-in-new-window.png"));
        openInExternalToolButton.addActionListener(e -> openLogInExternalTool());
        toolBar.add(openInExternalToolButton);
    }

    private void openLogInExternalTool() {
        Path tempFile = JIPipe.getTemporaryFile("log", ".txt");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                // Streamed render — no full-buffer copy of the log text
                logEntry.renderText(JIPipeLogLevel.INFO, line -> {
                    try {
                        writer.write(line);
                        writer.write('\n');
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
            UIUtils.desktopOpenFile(tempFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves the entry's NDJSON data file (the primary export — the raw log data).
     */
    private void exportDataFile() {
        Path path = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export log data file", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_NDJSON);
        if (path != null) {
            try {
                Files.copy(logEntry.getPath(), path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Saves the entry's log as rendered text (streamed from the data file).
     */
    private void exportLog() {
        Path path = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, "Export log as text", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_TXT);
        if (path != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                logEntry.renderText(JIPipeLogLevel.INFO, line -> {
                    try {
                        writer.write(line);
                        writer.write('\n');
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
