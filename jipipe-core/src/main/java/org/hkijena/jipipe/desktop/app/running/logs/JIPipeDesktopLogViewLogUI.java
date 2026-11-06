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
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopProgressTreePanel;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.notifications.JIPipeDesktopGenericNotificationInboxUI;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
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

/**
 * Displays one log entry of the Tools &gt; Logs collection. The "Log" tab hosts the v2 progress
 * tree panel ({@link JIPipeDesktopProgressTreePanel}) instead of the former flat monospace text
 * view: the entry's progress data is bound as a store, and the panel renders the node tree with
 * expandable record details, level filters, and the status footer.
 * <p>
 * The store binding: an entry with an NDJSON data file reopens the file read-only
 * ({@link JIPipeProgressLog#openReadOnly(Path)} - no writer, no mutation); an entry of a
 * silent/stdout run has no data file, and the collection keeps its store, which is bound directly.
 * Both kinds are finished and closed before the entry exists, so no batches ever arrive - the
 * panel renders statically from its initial catch-up scan. The "Notifications" tab, "Export data
 * file", and "Export text" are kept.
 */
public class JIPipeDesktopLogViewLogUI extends JIPipeDesktopWorkbenchPanel implements JIPipeProgressLog.ProgressBatchEventListener {
    private final JIPipeRunnableLogEntry logEntry;
    private final JIPipeDesktopProgressTreePanel treePanel;
    private JIPipeDesktopTabPane tabPane;
    private JIPipeProgressLog boundStore;

    public JIPipeDesktopLogViewLogUI(JIPipeDesktopWorkbench workbench, JIPipeRunnableLogEntry logEntry) {
        super(workbench);
        this.logEntry = logEntry;
        this.treePanel = new JIPipeDesktopProgressTreePanel(workbench);
        // The progress tree fills the tab (the checkbox is a run-manager convenience; the log
        // viewer always shows the tree)
        treePanel.setShowLogSelected(true);
        initialize();
        bindLog();
    }

    /**
     * Binds the entry's progress data to the tree panel. The batch listener is subscribed before
     * the store is set - the safe ordering for any store that could still emit.
     */
    private void bindLog() {
        Path dataFile = logEntry.getPath();
        JIPipeProgressLog store = null;
        if (dataFile != null && Files.exists(dataFile)) {
            // Finished file-backed run: reopen the completed NDJSON file read-only
            store = JIPipeProgressLog.openReadOnly(dataFile);
        } else if (dataFile == null) {
            // Silent/stdout run: the collection keeps the store itself
            store = logEntry.getStore();
        }
        if (store == null) {
            // No data file (removed, e.g. evicted) and no store - the placeholder of the former
            // flat view (renderText's unavailable-data contract)
            remove(tabPane);
            add(new JLabel(JIPipeRunnableLogEntry.DATA_UNAVAILABLE_LINE, SwingConstants.CENTER), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }
        boundStore = store;
        store.getBatchEventEmitter().subscribe(this);
        treePanel.setProgressStore(store);
    }

    @Override
    public void onProgressBatch(JIPipeProgressLog.ProgressBatchEvent event) {
        treePanel.onProgressBatch(event);
    }

    /**
     * Unsubscribes the batch listener and stops the tree panel's timer and background reads.
     * Called before the viewer shows a different entry (one panel per opened entry - the
     * subscription must not outlive the panel).
     */
    public void dispose() {
        if (boundStore != null) {
            boundStore.getBatchEventEmitter().unsubscribe(this);
            boundStore = null;
        }
        treePanel.dispose();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Init base layout
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        add(toolBar, BorderLayout.NORTH);
        tabPane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.Style.Right);
        add(tabPane, BorderLayout.CENTER);

        // Init progress tree
        tabPane.addTab("Log", JIPipe.RESOURCES.getIcon32("actions/rabbitvcs-show_log.png"), treePanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Init notifications
        tabPane.addTab("Notifications",
                logEntry.getNotifications().isEmpty() ? JIPipe.RESOURCES.getIcon32("actions/notifications.png") : JIPipe.RESOURCES.getIcon32("status/dialog-error.png"),
                new JIPipeDesktopGenericNotificationInboxUI(getDesktopWorkbench(), logEntry.getNotifications()),
                JIPipeDesktopTabPane.CloseMode.withoutCloseButton);

        // Init toolbar
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
                // Streamed render - no full-buffer copy of the log text
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
     * Saves the entry's NDJSON data file (the primary export - the raw log data).
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
