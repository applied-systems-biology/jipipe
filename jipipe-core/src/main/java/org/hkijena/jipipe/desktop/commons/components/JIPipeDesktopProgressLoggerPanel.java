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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNode;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeUtils;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A panel that monitors one or multiple {@link JIPipeProgressInfo} instances and displays their progress.
 * The panel aggregates progress from all sources and displays log messages with source prefixes.
 * <p>
 * Updates arrive as coalesced batches from each source's progress store: per batch, only the new
 * records are read, rendered with the store's render shape, and appended in ONE EDT runnable.
 * Sources without a writer thread (silent stores) stay idle; they are populated via the
 * tail-populate path of {@link #addProgressSource(String, JIPipeProgressInfo, boolean)}.
 */
public class JIPipeDesktopProgressLoggerPanel extends JIPipeDesktopWorkbenchPanel {

    private final Map<String, ProgressSource> sources = new HashMap<>();
    private final ReentrantLock sourcesLock = new ReentrantLock();

    private final JLabel titleLabel = new JLabel();
    private final JButton clearButton = new JButton("Clear", JIPipe.RESOURCES.getIcon16("actions/clear-brush.png"));
    private final JButton toggleAutoScrollButton = new JButton("Auto-scroll", JIPipe.RESOURCES.getIcon16("emblems/checkbox-checked.png"));
    private final LoggerPanel loggerPanel = new LoggerPanel();
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel("Ready ...");
    private final JPanel headerButtonPanel = UIUtils.boxHorizontal();
    private final boolean showProgressBar;

    private boolean autoScrollEnabled = true;

    /**
     * Creates a new progress logger panel with default title.
     *
     * @param desktopWorkbench The desktop workbench to use
     */
    public JIPipeDesktopProgressLoggerPanel(JIPipeDesktopWorkbench desktopWorkbench) {
        this(desktopWorkbench, "Progress Monitor");
    }

    /**
     * Creates a new progress logger panel with custom title.
     *
     * @param desktopWorkbench The desktop workbench to use
     * @param title            The title to display in the header
     */
    public JIPipeDesktopProgressLoggerPanel(JIPipeDesktopWorkbench desktopWorkbench, String title) {
        this(desktopWorkbench, title, true);
    }

    /**
     * Creates a new progress logger panel with custom title and progress bar visibility.
     *
     * @param desktopWorkbench The desktop workbench to use
     * @param title            The title to display in the header
     * @param showProgressBar  Whether to show the progress bar
     */
    public JIPipeDesktopProgressLoggerPanel(JIPipeDesktopWorkbench desktopWorkbench, String title, boolean showProgressBar) {
        super(desktopWorkbench);
        this.showProgressBar = showProgressBar;
        titleLabel.setText(title);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Header panel (NORTH)
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        JPanel headerPanel = UIUtils.boxHorizontal(titleLabel, Box.createHorizontalGlue(), headerButtonPanel);
        headerPanel.setBorder(UIUtils.createEmptyBorder(8));
        add(headerPanel, BorderLayout.NORTH);

        // Add default header buttons
        toggleAutoScrollButton.addActionListener(e -> {
            autoScrollEnabled = !autoScrollEnabled;
            loggerPanel.setAutoScrollEnabled(autoScrollEnabled);
            updateAutoScrollButton();
        });
        toggleAutoScrollButton.setToolTipText("Toggle automatic scrolling to the latest log entry");
        addHeaderPanelComponent(toggleAutoScrollButton);

        clearButton.addActionListener(e -> clearLog());
        clearButton.setToolTipText("Clear the log display");
        addHeaderPanelComponent(clearButton);

        // Logger panel (CENTER)
        if (ThemeUtils.isUsingModernTheme()) {
            UIUtils.makeNonOpaque(loggerPanel, true);
        }
        Component visibleLoggerComponent = UIUtils.wrapInBackgroundIslandPanelIfNeeded(loggerPanel);
        add(visibleLoggerComponent, BorderLayout.CENTER);

        // Status panel (SOUTH)
        statusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        statusLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());

        JPanel bottomPanel = new JPanel(new BorderLayout(16, 16));
        bottomPanel.setBorder(UIUtils.createEmptyBorder(8));

        JPanel statusInfoPanel = new JPanel(new GridBagLayout());
        int gridY = 0;
        if (showProgressBar) {
            progressBar.setMaximumSize(new Dimension(Short.MAX_VALUE, 4));
            progressBar.setMinimumSize(new Dimension(32, 4));
            progressBar.setPreferredSize(new Dimension(100, 4));
            statusInfoPanel.add(progressBar, new GridBagConstraints(0, gridY++, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, UIUtils.UI_PADDING, 0, 0));
        }
        statusInfoPanel.add(statusLabel, new GridBagConstraints(0, gridY, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, UIUtils.UI_PADDING, 0, 0));

        bottomPanel.add(statusInfoPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        updateAutoScrollButton();
    }

    private void updateAutoScrollButton() {
        if (autoScrollEnabled) {
            toggleAutoScrollButton.setIcon(JIPipe.RESOURCES.getIcon16("emblems/checkbox-checked.png"));
        } else {
            toggleAutoScrollButton.setIcon(JIPipe.RESOURCES.getIcon16("emblems/checkbox-unchecked.png"));
        }
    }

    /**
     * Adds a progress info source to monitor.
     *
     * @param name         Identifier for this source (used as log prefix)
     * @param progressInfo The progress info to monitor
     */
    public void addProgressSource(String name, JIPipeProgressInfo progressInfo) {
        addProgressSource(name, progressInfo, false);
    }

    /**
     * Adds a progress info source to monitor with an option to populate existing log entries.
     *
     * @param name                Identifier for this source (used as log prefix)
     * @param progressInfo        The progress info to monitor
     * @param populateExistingLog If true, existing log entries from the progress info will be appended to the logger panel
     */
    public void addProgressSource(String name, JIPipeProgressInfo progressInfo, boolean populateExistingLog) {
        sourcesLock.lock();
        try {
            // Remove existing source with same name if present
            if (sources.containsKey(name)) {
                unsubscribeFromSource(sources.get(name));
            }

            ProgressSource source = new ProgressSource(name, progressInfo);
            sources.put(name, source);
            subscribeToSource(source);

            // Populate existing log entries if requested
            if (populateExistingLog) {
                CharSequence existingLog = progressInfo.getLog();
                if (existingLog != null && existingLog.length() > 0) {
                    String logText = existingLog.toString();
                    String[] lines = logText.split("\n");
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            loggerPanel.appendLine("[" + name + "] " + line);
                        }
                    }
                }
            }
        } finally {
            sourcesLock.unlock();
        }
    }
    /**
     * Removes a progress info source.
     *
     * @param name The identifier of the source to remove
     */
    public void removeProgressSource(String name) {
        sourcesLock.lock();
        try {
            ProgressSource source = sources.remove(name);
            if (source != null) {
                unsubscribeFromSource(source);
            }
        } finally {
            sourcesLock.unlock();
        }
    }

    /**
     * Removes all progress info sources.
     */
    public void clearProgressSources() {
        sourcesLock.lock();
        try {
            for (ProgressSource source : sources.values()) {
                unsubscribeFromSource(source);
            }
            sources.clear();
        } finally {
            sourcesLock.unlock();
        }
    }

    /**
     * Gets the names of all registered progress sources.
     *
     * @return Collection of source names
     */
    public Collection<String> getProgressSourceNames() {
        sourcesLock.lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(sources.keySet()));
        } finally {
            sourcesLock.unlock();
        }
    }

    /**
     * Gets the underlying logger panel for customization.
     *
     * @return The logger panel
     */
    public LoggerPanel getLoggerPanel() {
        return loggerPanel;
    }

    /**
     * Gets the current text content of the log.
     *
     * @return the current text content
     */
    public String getText() {
        return loggerPanel.getText();
    }

    /**
     * Gets the current title.
     *
     * @return The current title
     */
    public String getTitle() {
        return titleLabel.getText();
    }

    /**
     * Sets the title displayed in the header.
     *
     * @param title The new title
     */
    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    /**
     * Checks if auto-scroll is enabled.
     *
     * @return True if auto-scroll is enabled
     */
    public boolean isAutoScrollEnabled() {
        return autoScrollEnabled;
    }

    /**
     * Enables or disables auto-scrolling to the last line.
     *
     * @param enabled True to enable auto-scroll
     */
    public void setAutoScrollEnabled(boolean enabled) {
        this.autoScrollEnabled = enabled;
        loggerPanel.setAutoScrollEnabled(enabled);
        updateAutoScrollButton();
    }

    /**
     * Clears the log display.
     */
    public void clearLog() {
        loggerPanel.setLogText("");
    }

    /**
     * Gets the number of currently monitored sources.
     *
     * @return Number of sources
     */
    public int getSourceCount() {
        sourcesLock.lock();
        try {
            return sources.size();
        } finally {
            sourcesLock.unlock();
        }
    }

    /**
     * Adds a component to the header button panel.
     *
     * @param component The component to add
     */
    public void addHeaderPanelComponent(Component component) {
        headerButtonPanel.add(component);
    }

    private void subscribeToSource(ProgressSource source) {
        if (!source.subscribed && source.progressInfo != null) {
            source.subscription = source.progressInfo.subscribeToBatchEvents(event -> onProgressBatch(source, event));
            source.subscribed = true;
        }
    }

    private void unsubscribeFromSource(ProgressSource source) {
        if (source.subscribed && source.progressInfo != null) {
            if (source.subscription != null) {
                source.subscription.close();
                source.subscription = null;
            }
            source.subscribed = false;
        }
    }

    /**
     * Handles one coalesced batch of a source's progress store. Runs on the store's writer thread:
     * the batch's records are read and rendered here, and the UI update is hopped to the EDT as
     * ONE runnable per batch (the store's render shapes are used; the append-line cap of the
     * logger panel applies as before).
     */
    private void onProgressBatch(ProgressSource source, JIPipeProgressLog.ProgressBatchEvent event) {
        JIPipeProgressLog store = (JIPipeProgressLog) event.getSource();
        boolean emptyFinalBatch = event.fromLine() > event.toLine();
        List<String> lines = List.of();
        if (!emptyFinalBatch) {
            lines = renderBatch(store, event.fromLine(), event.toLine());
        }
        // Track the latest changed node for the aggregate progress bar
        int latestNodeId = JIPipeProgressNodeUtils.latestNodeId(store, event.changedNodes());
        if (latestNodeId > 0) {
            source.lastNodeId = latestNodeId;
        }
        String status = emptyFinalBatch ? "Finished." : lastLine(lines);
        List<String> batchLines = lines;
        SwingUtilities.invokeLater(() -> appendBatch(source, batchLines, status));
    }

    /**
     * Renders the records of a line range with the store's render shape, prefixing each line with
     * the source name (the same prefix the tail-populate path uses).
     */
    private static List<String> renderBatch(JIPipeProgressLog store, int fromLine, int toLine) {
        List<JIPipeProgressRecord> records = store.readRecords(fromLine, toLine - fromLine + 1);
        if (records.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        store.renderRecords(records, JIPipeLogLevel.INFO, line -> {
            if (!line.isBlank()) {
                lines.add(line);
            }
        });
        return lines;
    }

    private static String lastLine(List<String> lines) {
        return lines.isEmpty() ? null : lines.get(lines.size() - 1);
    }

    /**
     * EDT: appends the rendered batch lines (one append per batch) and updates the status label
     * and the aggregate progress bar.
     */
    private void appendBatch(ProgressSource source, List<String> lines, String status) {
        if (!lines.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String line : lines) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append('[').append(source.name).append("] ").append(line);
            }
            loggerPanel.appendLine(builder.toString());
        }
        if (status != null) {
            statusLabel.setText(status);
        }
        updateProgressBar();
    }

    /**
     * EDT: aggregates the tracked nodes of all sources (determinate states are summed; any
     * indeterminate source makes the bar indeterminate).
     */
    private void updateProgressBar() {
        int totalProgress = 0;
        int totalMaxProgress = 0;
        boolean anyIndeterminate = false;
        int sourceCount = 0;

        sourcesLock.lock();
        try {
            sourceCount = sources.size();
            for (ProgressSource source : sources.values()) {
                JIPipeProgressLog store = source.progressInfo != null ? source.progressInfo.getStore() : null;
                JIPipeProgressNode node = store != null && source.lastNodeId > 0 ? store.getNode(source.lastNodeId) : null;
                if (node == null || node.getMaxProgress() <= 0) {
                    anyIndeterminate = true;
                } else {
                    totalProgress += node.getProgress();
                    totalMaxProgress += node.getMaxProgress();
                }
            }
        } finally {
            sourcesLock.unlock();
        }

        if (anyIndeterminate || totalMaxProgress <= 0) {
            progressBar.setIndeterminate(sourceCount > 0);
        } else {
            progressBar.setIndeterminate(false);
            progressBar.setMaximum(totalMaxProgress);
            progressBar.setValue(totalProgress);
        }
    }

    /**
     * Inner class to track each progress source.
     */
    private class ProgressSource {
        final String name;
        final JIPipeProgressInfo progressInfo;
        JIPipeProgressInfo.BatchEventSubscription subscription;

        /**
         * The node whose state is displayed for this source (the latest node with a progress
         * update among the batches' changed nodes); written by batch listeners, read on the EDT.
         */
        volatile int lastNodeId;
        boolean subscribed;

        ProgressSource(String name, JIPipeProgressInfo progressInfo) {
            this.name = name;
            this.progressInfo = progressInfo;
            this.subscribed = false;
        }
    }
}
