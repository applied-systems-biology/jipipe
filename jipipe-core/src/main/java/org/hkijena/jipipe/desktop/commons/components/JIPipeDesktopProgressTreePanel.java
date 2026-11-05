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
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNode;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeKind;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeState;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The v2 progress tree panel — the Swing implementation of the approved design contract
 * ({@code docs/superpowers/specs/2026-11-04-progress-system-overhaul/ui/v2-progress-panel.html}).
 * Replaces the flat log in the run window; the AI monitor keeps the old
 * {@link JIPipeDesktopProgressLoggerPanel}.
 * <p>
 * <b>Update driver:</b> batch events ({@link #onProgressBatch}) arrive on the store's writer thread
 * and only mark the model dirty (they read the batch's records once to maintain the per-node
 * timestamp/debug counters — the same bounded batch reads the previous flat panels did, but no
 * rendering). A 250 ms {@link Timer} rebuilds the visible rows from the store's node index
 * (in-memory {@code getNode} reads) and schedules bounded record reads for expanded rows on a
 * background executor; results are applied on the EDT. No per-record work happens on the EDT.
 * <p>
 * <b>Row model:</b> a flat, virtualized {@link JList} of {@link Row} snapshots — node rows and
 * phase rows sit directly in the list; {@code PARTITION}-kind nodes are squashed into the dim
 * {@code P0} chip of their descendants (never rendered as rows). Expanding a node shows its records
 * (paged — the last {@value #DETAIL_PAGE_SIZE} records by default, older pages via
 * {@code Show N older records from run data…}), read from the checkpointed store off the EDT.
 */
public class JIPipeDesktopProgressTreePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(JIPipeDesktopProgressTreePanel.class);

    private static final int REFRESH_INTERVAL_MILLIS = 250;
    private static final int DETAIL_PAGE_SIZE = 1000;
    private static final int COPY_LINE_LIMIT = 8192;
    private static final int CATCH_UP_PAGE_SIZE = 10_000;

    /**
     * Matches a trailing {@code [uuid]} suffix of a node label — rendered faintly (mockup element).
     */
    private static final Pattern TRAILING_UUID = Pattern.compile("^(.*) \\[([0-9a-fA-F][0-9a-zA-Z-]{7,})]$");

    private final JIPipeDesktopWorkbench workbench;

    // --- Header ---
    private final JCheckBox showLogCheckBox = new JCheckBox("Show log");
    private final JCheckBox autoShowProgressCheckBox = new JCheckBox("Auto show progress");
    private final JCheckBox autoViewResultsCheckBox = new JCheckBox("Auto view results");
    private final JComboBox<String> levelComboBox = new JComboBox<>(new String[]{"Level: INFO", "Level: DEBUG"});
    private final JButton copyButton = new JButton("Copy", JIPipe.RESOURCES.getIcon16("actions/copy.png"));
    private final JButton exportButton = new JButton("Export", JIPipe.RESOURCES.getIcon16("actions/document-export.png"));
    private final JPanel headerComponentsPanel = UIUtils.boxHorizontal();

    // --- Center (the flat row list) ---
    private final DefaultListModel<JIPipeDesktopProgressTreeRowRenderer.Row> listModel = new DefaultListModel<>();
    private final ProgressTreeList list;
    private final JScrollPane listScrollPane;
    private final JIPipeDesktopProgressTreeRowRenderer renderer;
    private final SpinnerIcon rowSpinner;

    // --- Footer ---
    private final SpinnerIcon footerSpinner;
    private final JLabel footerStateLabel = new JLabel();
    private final JLabel footerStatusLabel = new JLabel("Ready ...");
    private final JLabel footerRecordsLabel = new JLabel();
    private final JLabel footerFileLabel = new JLabel();
    private final JPanel footerButtons = UIUtils.boxHorizontal();

    // --- Store state (batch listeners run on the store's writer thread; the EDT consumes) ---
    private final Object storeLock = new Object();
    private volatile JIPipeProgressLog store;
    private final AtomicInteger generation = new AtomicInteger();
    private final Map<Integer, NodeUiState> nodeStates = new ConcurrentHashMap<>();
    private final Map<Integer, String> partitionChips = new ConcurrentHashMap<>();
    private final Set<String> openStacks = ConcurrentHashMap.newKeySet();
    private final Set<Integer> pendingNodeIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean catchUpScheduled = new AtomicBoolean();
    private volatile boolean dirty;
    private volatile int latestNodeId;
    private volatile long latestNodeTs;
    private volatile long lastKnownLine;
    private volatile long fileSize;
    private volatile JIPipeLogLevel minLevel = JIPipeLogLevel.INFO;

    // EDT-only
    private final Map<Integer, Integer> rowPositions = new HashMap<>();

    private final Timer refreshTimer;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "JIPipe-Progress-Tree-Read");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Creates a new progress tree panel.
     *
     * @param workbench the desktop workbench (used by the Export action; may be null — Export stays disabled)
     */
    public JIPipeDesktopProgressTreePanel(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        this.list = new ProgressTreeList();
        this.rowSpinner = new SpinnerIcon(list, 14);
        this.renderer = new JIPipeDesktopProgressTreeRowRenderer(rowSpinner);
        this.footerSpinner = new SpinnerIcon(footerStateLabel, 14);
        this.listScrollPane = new JScrollPane(list);
        initialize();
        this.refreshTimer = new Timer(REFRESH_INTERVAL_MILLIS, e -> refreshTick());
        refreshTimer.start();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Header: Show log / Auto show progress / Auto view results | Level select | Copy | Export
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 8, 6, 8),
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeUtils.getCurrentStyle().getBorderColor())));
        JPanel headerLeft = UIUtils.boxHorizontal(showLogCheckBox, autoShowProgressCheckBox, autoViewResultsCheckBox);
        JPanel headerRight = UIUtils.boxHorizontal(levelComboBox, copyButton, exportButton, headerComponentsPanel);
        headerPanel.add(headerLeft, BorderLayout.WEST);
        headerPanel.add(headerRight, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center: the flat, virtualized row list
        var style = ThemeUtils.getCurrentStyle();
        list.setBackground(style.getPanelBackground());
        list.setCellRenderer(renderer);
        list.setFont(list.getFont().deriveFont(12.0f));
        list.setFixedCellHeight(Math.max(22, getFontMetrics(list.getFont()).getHeight() + 9));
        list.setLayoutOrientation(JList.VERTICAL);
        ToolTipManager.sharedInstance().registerComponent(list);
        listScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        listScrollPane.getViewport().setBackground(style.getPanelBackground());
        listScrollPane.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        add(listScrollPane, BorderLayout.CENTER);

        // Footer: state icon | breadcrumb + live progress | N records · size | data file | buttons
        JPanel footerPanel = new JPanel(new BorderLayout(8, 0));
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeUtils.getCurrentStyle().getBorderColor()),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        footerStateLabel.setIcon(footerSpinner);
        footerStateLabel.setFont(footerStateLabel.getFont().deriveFont(12.0f));
        footerStatusLabel.setFont(footerStatusLabel.getFont().deriveFont(12.0f));
        footerStatusLabel.setForeground(style.getTextMuted());
        JPanel footerLeft = UIUtils.boxHorizontal(footerStateLabel, footerStatusLabel);
        footerRecordsLabel.setFont(footerRecordsLabel.getFont().deriveFont(12.0f));
        footerRecordsLabel.setForeground(style.getTextMuted());
        footerFileLabel.setFont(footerFileLabel.getFont().deriveFont(12.0f));
        footerFileLabel.setForeground(style.getTextMuted());
        JPanel footerRight = UIUtils.boxHorizontal(footerRecordsLabel, footerFileLabel, footerButtons);
        footerPanel.add(footerLeft, BorderLayout.WEST);
        footerPanel.add(footerRight, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);

        showLogCheckBox.setSelected(true);
        showLogCheckBox.addActionListener(e -> updateShowLogVisibility());
        autoShowProgressCheckBox.setSelected(true);
        autoShowProgressCheckBox.setToolTipText("Automatically scroll to the currently running node");
        levelComboBox.setToolTipText("Detail level of the record views");
        levelComboBox.addActionListener(e -> {
            setMinLevel(levelComboBox.getSelectedIndex() == 1 ? JIPipeLogLevel.DEBUG : JIPipeLogLevel.INFO);
        });
        copyButton.setToolTipText("Copy the rendered log (last " + NumberFormat.getIntegerInstance().format(COPY_LINE_LIMIT) + " lines)");
        copyButton.addActionListener(e -> copyToClipboard());
        exportButton.setToolTipText("Export the underlying run data file");
        exportButton.addActionListener(e -> exportDataFile());
        exportButton.setEnabled(false);

        // Apply the initial visibility: the host applies the restored RUN_QUEUE_SHOW_LOG setting
        // via setShowLogSelected BEFORE this line (a programmatic setSelected fires no ActionEvent,
        // so the visibility must not rely on the action listener alone)
        updateShowLogVisibility();
    }

    /**
     * Applies the Show log checkbox's state to the list's visibility (both the constructor-time
     * programmatic restore and user clicks go through this path).
     */
    private void updateShowLogVisibility() {
        listScrollPane.setVisible(showLogCheckBox.isSelected());
        revalidate();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Shows the progress of the given store (called with the run's current store — a graph run may
     * adopt a new store mid-flight; the first batch of the new store switches the view).
     *
     * @param newStore the store (may be null to clear)
     */
    public void setProgressStore(JIPipeProgressLog newStore) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setProgressStore(newStore));
            return;
        }
        if (newStore == store) {
            return;
        }
        switchStore(newStore);
        // A store attached while its run already finished will not emit batches anymore, and
        // silent/stdout stores never emit: read their existing records once. Active file-backed
        // stores schedule the same catch-up from their first batch (covering only the pre-attach
        // lines — batches cover the rest without double-counting).
        JIPipeProgressNode root = newStore != null ? newStore.getNode(newStore.getRootNodeId()) : null;
        if (newStore != null && (root == null || root.getState() != JIPipeProgressNodeState.RUNNING || newStore.getFile() == null)
                && catchUpScheduled.compareAndSet(false, true)) {
            scheduleCatchUp(Integer.MAX_VALUE);
        }
        refreshNow();
    }

    /**
     * Clears the view (a new run starts).
     */
    public void reset() {
        setProgressStore(null);
    }

    /**
     * Handles ONE coalesced batch of the monitored store. Runs on the store's writer thread: reads
     * the batch's records once to maintain the per-node timestamp/debug counters (bounded by the
     * coalesce interval), marks the model dirty, and hops everything else to the EDT.
     * The empty final batch (fromLine &gt; toLine) only triggers a refresh — the run's
     * Finished/Interrupted events own the terminal state.
     *
     * @param batch the coalesced batch
     */
    public void onProgressBatch(JIPipeProgressLog.ProgressBatchEvent batch) {
        JIPipeProgressLog batchStore = (JIPipeProgressLog) batch.getSource();
        if (batchStore != store) {
            switchStore(batchStore);
        }
        if (batch.fromLine() <= batch.toLine()) {
            // Read the batch's records once (the same bounded reads the previous flat panels did)
            // to maintain the timestamps and hidden-debug counters; no rendering, no EDT work
            accumulate(batchStore.readRecords(batch.fromLine(), batch.toLine() - batch.fromLine() + 1));
            if (batch.toLine() > lastKnownLine) {
                lastKnownLine = batch.toLine();
            }
            if (batch.fromLine() > 1 && catchUpScheduled.compareAndSet(false, true)) {
                scheduleCatchUp(batch.fromLine() - 1);
            }
        }
        pendingNodeIds.addAll(batch.changedNodes());
        dirty = true;
    }

    public boolean isShowLogSelected() {
        return showLogCheckBox.isSelected();
    }

    public void setShowLogSelected(boolean selected) {
        showLogCheckBox.setSelected(selected);
        // A programmatic setSelected fires no ActionEvent — the host restores the stored
        // RUN_QUEUE_SHOW_LOG setting through this setter, so the visibility must be applied here
        updateShowLogVisibility();
    }

    public JCheckBox getShowLogCheckBox() {
        return showLogCheckBox;
    }

    /**
     * Whether to automatically scroll to the running row. In the graph editor the same toggle also
     * drives the dock behavior of the run manager (exposed via the hosting panels).
     */
    public boolean isAutoShowProgress() {
        return autoShowProgressCheckBox.isSelected();
    }

    public void setAutoShowProgressSelected(boolean selected) {
        autoShowProgressCheckBox.setSelected(selected);
    }

    /**
     * Semantic passthrough for run managers (auto-open results). The tree panel itself does not
     * act on this toggle.
     */
    public boolean isAutoViewResults() {
        return autoViewResultsCheckBox.isSelected();
    }

    public void setAutoViewResultsSelected(boolean selected) {
        autoViewResultsCheckBox.setSelected(selected);
    }

    /**
     * Adds a component to the header (right of the Export button).
     *
     * @param component the component
     */
    public void addHeaderComponent(Component component) {
        headerComponentsPanel.add(component);
        headerComponentsPanel.revalidate();
        headerComponentsPanel.repaint();
    }

    /**
     * Adds a component to the footer (typically the Cancel/Close buttons of the hosting window).
     *
     * @param component the component
     */
    public void addFooterComponent(Component component) {
        footerButtons.add(component);
        footerButtons.revalidate();
        footerButtons.repaint();
    }

    /**
     * Stops the refresh timer and the background reads. Called by the hosting panel when disposed.
     */
    public void dispose() {
        refreshTimer.stop();
        backgroundExecutor.shutdownNow();
        rowSpinner.stop();
        footerSpinner.stop();
    }

    // -------------------------------------------------------------------------
    // Store switching and batch accumulation (writer thread + EDT)
    // -------------------------------------------------------------------------

    private void switchStore(JIPipeProgressLog newStore) {
        synchronized (storeLock) {
            store = newStore;
            generation.incrementAndGet();
            nodeStates.clear();
            partitionChips.clear();
            openStacks.clear();
            pendingNodeIds.clear();
            latestNodeId = 0;
            latestNodeTs = 0;
            lastKnownLine = 0;
            fileSize = 0;
            catchUpScheduled.set(false);
        }
        dirty = true;
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            rowPositions.clear();
            updateFooter();
        });
    }

    /**
     * Accumulates the per-node counters (first/last timestamp, hidden-debug count, renderable
     * record count) from records. Called on the writer thread (per batch) and on the background
     * executor (catch-up scan); all state is atomic.
     */
    private void accumulate(List<JIPipeProgressRecord> records) {
        for (JIPipeProgressRecord record : records) {
            int nodeId = recordNodeId(record);
            if (nodeId <= 0) {
                continue;
            }
            NodeUiState state = nodeState(nodeId);
            long ts = recordTs(record);
            if (ts > 0) {
                state.firstTs.compareAndExchange(0, ts);
                state.lastTs.accumulateAndGet(ts, Math::max);
            }
            if (record instanceof JIPipeProgressRecord.Log logRecord) {
                if (logRecord.lv() < JIPipeLogLevel.INFO.value()) {
                    state.debugCount.incrementAndGet();
                }
                state.renderableCount.incrementAndGet();
            } else if (record instanceof JIPipeProgressRecord.Error) {
                state.renderableCount.incrementAndGet();
            }
            if (ts >= latestNodeTs) {
                latestNodeTs = ts;
                latestNodeId = nodeId;
            }
        }
    }

    private NodeUiState nodeState(int nodeId) {
        return nodeStates.computeIfAbsent(nodeId, NodeUiState::new);
    }

    /**
     * Reads all records before the given line (exclusive) once — covers the records that were
     * written before the panel attached to the store — and applies them to the counters.
     *
     * @param upToLineExclusive the last line to stop before ({@link Integer#MAX_VALUE} = until the end)
     */
    private void scheduleCatchUp(int upToLineExclusive) {
        int gen = generation.get();
        JIPipeProgressLog storeSnapshot = store;
        backgroundExecutor.submit(() -> {
            int from = 1;
            while (from < upToLineExclusive && gen == generation.get() && !Thread.currentThread().isInterrupted()) {
                int limit = Math.min(CATCH_UP_PAGE_SIZE, upToLineExclusive - from);
                List<JIPipeProgressRecord> page = storeSnapshot.readRecords(from, limit);
                if (page.isEmpty()) {
                    break;
                }
                accumulate(page);
                if (upToLineExclusive == Integer.MAX_VALUE || page.size() < limit) {
                    // Reached the end (or the requested bound): the scanned lines are the full record count
                    lastKnownLine = Math.max(lastKnownLine, from + page.size() - 1);
                }
                if (page.size() < limit) {
                    break;
                }
                from += page.size();
            }
            if (upToLineExclusive != Integer.MAX_VALUE) {
                lastKnownLine = Math.max(lastKnownLine, upToLineExclusive - 1);
            }
            SwingUtilities.invokeLater(() -> {
                if (gen == generation.get()) {
                    refreshNow();
                }
            });
        });
    }

    // -------------------------------------------------------------------------
    // Refresh cycle (EDT)
    // -------------------------------------------------------------------------

    private void refreshNow() {
        dirty = true;
        refreshTick();
    }

    private void refreshTick() {
        if (!dirty || !SwingUtilities.isEventDispatchThread()) {
            return;
        }
        dirty = false;
        JIPipeProgressLog s = store;
        if (s == null) {
            updateFooter();
            return;
        }
        updateTreeModel(s);
        scheduleDetailReloads(s);
        updateFooter();
        scheduleFileSizeUpdate(s);
        updateSpinners(s);
        autoScroll();
    }

    /**
     * Rebuilds the flat row list from the store's node index (in-memory {@code getNode} reads only).
     */
    private void updateTreeModel(JIPipeProgressLog s) {
        List<JIPipeDesktopProgressTreeRowRenderer.Row> rows = new ArrayList<>();
        rowPositions.clear();
        Map<Integer, List<Integer>> childMap = new HashMap<>();
        int nextId = s.nextNodeId();
        for (int id = 2; id < nextId; id++) {
            JIPipeProgressNode node = s.getNode(id);
            if (node == null) {
                continue;
            }
            nodeState(id);
            int parentId = node.getParentId() <= 0 ? s.getRootNodeId() : node.getParentId();
            childMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(id);
        }
        walkRows(s, childMap, s.getRootNodeId(), 0, null, rows);
        listModel.removeAllElements();
        listModel.addAll(rows);
    }

    private void walkRows(JIPipeProgressLog s, Map<Integer, List<Integer>> childMap, int parentId, int depth,
                          String inheritedChip, List<JIPipeDesktopProgressTreeRowRenderer.Row> rows) {
        List<Integer> children = childMap.get(parentId);
        if (children == null) {
            return;
        }
        for (int id : children) {
            JIPipeProgressNode node = s.getNode(id);
            if (node == null) {
                continue;
            }
            if (node.getKind() == JIPipeProgressNodeKind.PARTITION) {
                // v2 decision: partitions squash into the dim chip of their descendants — no row,
                // same depth (the data model still records them; the UI spends no tree level)
                walkRows(s, childMap, id, depth, partitionChipFor(id), rows);
                continue;
            }
            NodeUiState state = nodeStates.get(id);
            boolean expandable = node.getDescendantNodeCount() > 0
                    || (state != null && state.renderableCount.get() > 0)
                    || node.getRecordCount() > 0;
            String label = node.getLabel();
            String uuidSuffix = null;
            if (label == null || label.isBlank()) {
                label = "(unnamed)";
            } else {
                Matcher matcher = TRAILING_UUID.matcher(label);
                if (matcher.matches()) {
                    label = matcher.group(1);
                    uuidSuffix = "[" + matcher.group(2) + "]";
                }
            }
            int badgeCount = (int) Math.min(Integer.MAX_VALUE,
                    node.getRecordCount() + node.getDescendantNodeCount() + descendantRecordCount(s, childMap, id));
            int debugCount = 0;
            long firstTs = 0;
            long lastTs = 0;
            if (state != null) {
                debugCount = minLevel == JIPipeLogLevel.INFO
                        ? (int) Math.min(Integer.MAX_VALUE, state.debugCount.get())
                        : 0;
                firstTs = state.firstTs.get();
                lastTs = state.lastTs.get();
            }
            rows.add(new JIPipeDesktopProgressTreeRowRenderer.NodeRow(
                    id,
                    depth,
                    inheritedChip,
                    label,
                    uuidSuffix,
                    node.getState(),
                    expandable,
                    state != null && state.expanded,
                    badgeCount,
                    debugCount,
                    node.getMaxProgress() > 0 ? node.getProgress() : -1,
                    node.getMaxProgress() > 0 ? node.getMaxProgress() : -1,
                    phaseText(node, firstTs, lastTs)));
            rowPositions.put(id, rows.size() - 1);
            if (state != null && state.expanded) {
                appendDetailRows(rows, id, state, node);
                walkRows(s, childMap, id, depth + 1, inheritedChip, rows);
            }
        }
    }

    /**
     * The phase tag of a node row: duration, {@code running Ns}, {@code failed}, or {@code cancelled}.
     */
    private static String phaseText(JIPipeProgressNode node, long firstTs, long lastTs) {
        return switch (node.getState()) {
            case FAILED -> "failed";
            case CANCELLED -> "cancelled";
            case RUNNING -> firstTs > 0 ? "running " + Math.max(1, (System.currentTimeMillis() - firstTs) / 1000) + "s" : "running";
            case OK -> firstTs > 0 && lastTs > firstTs ? formatDuration(lastTs - firstTs) : "";
        };
    }

    /**
     * Sums the record counts of all descendants of a node (walks the child map built in this cycle).
     */
    private long descendantRecordCount(JIPipeProgressLog s, Map<Integer, List<Integer>> childMap, int nodeId) {
        long total = 0;
        Deque<Integer> stack = new ArrayDeque<>(childMap.getOrDefault(nodeId, List.of()));
        while (!stack.isEmpty()) {
            int id = stack.pop();
            JIPipeProgressNode node = s.getNode(id);
            if (node == null) {
                continue;
            }
            total += node.getRecordCount();
            List<Integer> children = childMap.get(id);
            if (children != null) {
                for (int child : children) {
                    stack.push(child);
                }
            }
        }
        return total;
    }

    private String partitionChipFor(int partitionNodeId) {
        return partitionChips.computeIfAbsent(partitionNodeId, k -> "P" + partitionChips.size());
    }

    /**
     * Appends the detail rows of an expanded node: its records (last page), the hidden-debug line,
     * and the "show older" action.
     */
    private void appendDetailRows(List<JIPipeDesktopProgressTreeRowRenderer.Row> rows, int nodeId,
                                  NodeUiState state, JIPipeProgressNode node) {
        if (!state.loaded) {
            if (state.loading) {
                rows.add(new JIPipeDesktopProgressTreeRowRenderer.LoadingRow(nodeId));
            }
            return;
        }
        for (JIPipeProgressRecord record : state.detailPage) {
            boolean isLog = record instanceof JIPipeProgressRecord.Log;
            boolean isError = record instanceof JIPipeProgressRecord.Error;
            if (!isLog && !isError) {
                // Progress (p), node (n), and node-finished (N) records render nothing
                continue;
            }
            boolean hasStack = isError && ((JIPipeProgressRecord.Error) record).st() != null
                    && !((JIPipeProgressRecord.Error) record).st().isBlank();
            boolean stackOpen = hasStack && openStacks.contains(errorKey(nodeId, (JIPipeProgressRecord.Error) record));
            rows.add(new JIPipeDesktopProgressTreeRowRenderer.DetailRow(nodeId, record, hasStack, stackOpen));
            if (stackOpen) {
                for (String line : ((JIPipeProgressRecord.Error) record).st().split("\n")) {
                    rows.add(new JIPipeDesktopProgressTreeRowRenderer.StackRow(nodeId, line));
                }
            }
        }
        if (minLevel == JIPipeLogLevel.INFO) {
            long debugHidden = state.debugCount.get();
            if (debugHidden > 0) {
                rows.add(new JIPipeDesktopProgressTreeRowRenderer.DebugHiddenRow(nodeId,
                        (int) Math.min(Integer.MAX_VALUE, debugHidden)));
            }
        }
        if (state.hasOlder) {
            rows.add(new JIPipeDesktopProgressTreeRowRenderer.MoreRow(nodeId,
                    Math.max(1, Math.min(DETAIL_PAGE_SIZE, state.pageFromLine - node.getFirstLine()))));
        }
    }

    /**
     * Schedules page re-reads for expanded nodes that received new records.
     */
    private void scheduleDetailReloads(JIPipeProgressLog s) {
        for (Integer nodeId : pendingNodeIds) {
            NodeUiState state = nodeStates.get(nodeId);
            // Remove per id (not clear()) — a batch concurrently adding ids must not lose them
            pendingNodeIds.remove(nodeId);
            if (state == null || !state.expanded || state.loading) {
                continue;
            }
            loadDetailPage(s, nodeId, false);
        }
    }

    /**
     * Reads a page of a node's records from the store (off the EDT — checkpointed seek + bounded
     * parse) and applies it on the EDT.
     * <ul>
     * <li>Default (refresh/expand): the node's last page (up to {@value #DETAIL_PAGE_SIZE} records),
     * extended upwards by any older pages the user loaded before.</li>
     * <li>{@code older}: the page before the currently loaded history, prepended to it
     * ("Show N older records from run data…" — the shown records accumulate).</li>
     * </ul>
     *
     * @param nodeId the node
     * @param older  true to load the page before the currently shown history
     */
    private void loadDetailPage(JIPipeProgressLog s, int nodeId, boolean older) {
        NodeUiState state = nodeStates.get(nodeId);
        if (state == null) {
            return;
        }
        state.loading = true;
        int gen = generation.get();
        boolean mergeOlder = older;
        int prevFrom = state.pageFromLine;
        int fromHint = older ? prevFrom - DETAIL_PAGE_SIZE : -1;
        backgroundExecutor.submit(() -> {
            try {
                if (gen != generation.get() || store != s) {
                    return;
                }
                JIPipeProgressNode node = s.getNode(nodeId);
                List<JIPipeProgressRecord> page = List.of();
                int from = 0;
                boolean hasOlder = false;
                if (node != null && node.getLastLine() > 0) {
                    int firstLine = node.getFirstLine();
                    int lastLine = node.getLastLine();
                    JIPipeLogLevel level = minLevel;
                    if (mergeOlder) {
                        // The window before the loaded history (exactly one page wide)
                        from = Math.max(firstLine, fromHint);
                        List<JIPipeProgressRecord> raw = s.readRecords(from, Math.max(1, prevFrom - from));
                        page = filterPage(raw, nodeId, level);
                        hasOlder = from > firstLine;
                    } else {
                        int tailStart = Math.max(firstLine, lastLine - DETAIL_PAGE_SIZE + 1);
                        // Keep the history pages the user already loaded
                        from = state.loaded ? Math.min(state.pageFromLine, tailStart) : tailStart;
                        List<JIPipeProgressRecord> raw = s.readRecords(from, Math.max(1, lastLine - from + 1));
                        page = filterPage(raw, nodeId, level);
                        hasOlder = from > firstLine;
                    }
                }
                List<JIPipeProgressRecord> finalPage = page;
                int finalFrom = from;
                boolean finalHasOlder = hasOlder;
                SwingUtilities.invokeLater(() -> applyDetailPage(gen, s, nodeId, finalPage, finalFrom, finalHasOlder, mergeOlder));
            } catch (Exception e) {
                log.error("Failed to read progress records of node {}", nodeId, e);
                SwingUtilities.invokeLater(() -> {
                    NodeUiState failed = nodeStates.get(nodeId);
                    if (failed != null) {
                        failed.loading = false;
                    }
                });
            }
        });
    }

    private static List<JIPipeProgressRecord> filterPage(List<JIPipeProgressRecord> raw, int nodeId, JIPipeLogLevel level) {
        List<JIPipeProgressRecord> filtered = new ArrayList<>();
        for (JIPipeProgressRecord record : raw) {
            if (recordNodeId(record) != nodeId || !passesLevel(record, level)) {
                continue;
            }
            filtered.add(record);
        }
        return filtered;
    }

    private void applyDetailPage(int gen, JIPipeProgressLog s, int nodeId,
                                 List<JIPipeProgressRecord> page, int from, boolean hasOlder, boolean mergeOlder) {
        NodeUiState state = nodeStates.get(nodeId);
        if (state == null) {
            return;
        }
        state.loading = false;
        if (gen != generation.get() || store != s || !state.expanded) {
            return;
        }
        if (mergeOlder) {
            // Prepend the older records to the shown history
            List<JIPipeProgressRecord> merged = new ArrayList<>(page.size() + state.detailPage.size());
            merged.addAll(page);
            merged.addAll(state.detailPage);
            state.detailPage = merged;
            state.pageFromLine = from;
            state.hasOlder = hasOlder;
        } else {
            if (state.pageFromLine > 0 && state.pageFromLine < from) {
                // An older-page merge happened since this read was scheduled — it stays visible;
                // the next batch refresh re-reads from its (earlier) start
                return;
            }
            state.detailPage = page;
            state.pageFromLine = from;
            state.hasOlder = hasOlder;
        }
        state.loaded = true;
        refreshNow();
    }

    private void setMinLevel(JIPipeLogLevel level) {
        if (minLevel == level) {
            return;
        }
        minLevel = level;
        // The level filter changes what the detail pages (and the debug chips) show
        JIPipeProgressLog s = store;
        if (s != null) {
            for (NodeUiState state : nodeStates.values()) {
                if (state.expanded && state.loaded && !state.loading) {
                    loadDetailPage(s, state.nodeId, false);
                }
            }
        }
        refreshNow();
    }

    // -------------------------------------------------------------------------
    // Footer, spinners, auto-scroll
    // -------------------------------------------------------------------------

    private void updateFooter() {
        JIPipeProgressLog s = store;
        if (s == null) {
            footerStateLabel.setIcon(footerSpinner);
            footerSpinner.stop();
            footerStatusLabel.setText("Ready ...");
            footerRecordsLabel.setText("");
            footerFileLabel.setVisible(false);
            exportButton.setEnabled(false);
            return;
        }
        JIPipeProgressNode root = s.getNode(s.getRootNodeId());
        JIPipeProgressNodeState rootState = root != null ? root.getState() : JIPipeProgressNodeState.RUNNING;
        boolean running = rootState == JIPipeProgressNodeState.RUNNING;
        footerStateLabel.setIcon(running ? footerSpinner : null);
        if (!running) {
            footerStateLabel.setText(rootState == JIPipeProgressNodeState.OK ? "✔" : "✖");
            footerStateLabel.setForeground(rootState == JIPipeProgressNodeState.OK
                    ? ThemeUtils.getCurrentStyle().getSuccessColor()
                    : ThemeUtils.getCurrentStyle().getDangerColor());
        } else {
            footerStateLabel.setText("");
        }

        String status;
        JIPipeProgressNode latest = latestNodeId > 0 ? s.getNode(latestNodeId) : null;
        if (running && latest != null) {
            String chain = labelPath(s, latest);
            String progress = progressText(latest);
            status = chain + (progress.isEmpty() ? "" : " — " + progress);
            if (status.isBlank()) {
                status = "Running ...";
            }
        } else {
            NodeUiState rootState2 = nodeStates.get(s.getRootNodeId());
            String duration = rootState2 != null && rootState2.firstTs.get() > 0 && rootState2.lastTs.get() > rootState2.firstTs.get()
                    ? " · required " + formatDuration(rootState2.lastTs.get() - rootState2.firstTs.get())
                    : "";
            status = switch (rootState) {
                case OK -> "Run finished" + duration;
                case FAILED -> "Run ended with errors" + duration;
                case CANCELLED -> "Run cancelled" + duration;
                default -> "Running ...";
            };
        }
        footerStatusLabel.setText(status);

        if (lastKnownLine > 0) {
            String records = NumberFormat.getIntegerInstance().format(lastKnownLine) + " records";
            footerRecordsLabel.setText(fileSize > 0 ? records + " · " + StringUtils.formatBytes(fileSize) : records);
        } else {
            footerRecordsLabel.setText("");
        }
        Path file = s.getFile();
        footerFileLabel.setVisible(file != null);
        if (file != null) {
            footerFileLabel.setText("data file: " + abbreviatePath(file, 64));
        }
        exportButton.setEnabled(workbench != null && file != null);
    }

    /**
     * Stats the data file size in the background and updates the footer's {@code N records · size}.
     */
    private void scheduleFileSizeUpdate(JIPipeProgressLog s) {
        Path file = s.getFile();
        if (file == null) {
            return;
        }
        int gen = generation.get();
        backgroundExecutor.submit(() -> {
            try {
                long size = Files.size(file);
                SwingUtilities.invokeLater(() -> {
                    if (gen != generation.get()) {
                        return;
                    }
                    fileSize = size;
                    if (lastKnownLine > 0) {
                        footerRecordsLabel.setText(NumberFormat.getIntegerInstance().format(lastKnownLine)
                                + " records · " + StringUtils.formatBytes(size));
                    }
                });
            } catch (IOException ignored) {
                // The file may not exist yet (nothing flushed) — keep the current footer
            }
        });
    }

    private void updateSpinners(JIPipeProgressLog s) {
        JIPipeProgressNode root = s.getNode(s.getRootNodeId());
        boolean running = root != null && root.getState() == JIPipeProgressNodeState.RUNNING;
        if (running) {
            rowSpinner.start();
            footerSpinner.start();
        } else {
            rowSpinner.stop();
            footerSpinner.stop();
        }
    }

    private void autoScroll() {
        if (!autoShowProgressCheckBox.isSelected() || latestNodeId <= 0) {
            return;
        }
        JIPipeProgressLog s = store;
        JIPipeProgressNode root = s != null ? s.getNode(s.getRootNodeId()) : null;
        if (root == null || root.getState() != JIPipeProgressNodeState.RUNNING) {
            return;
        }
        int index = rowPositions.getOrDefault(latestNodeId, -1);
        if (index < 0) {
            index = nearestVisibleAncestorRow(s, latestNodeId);
        }
        if (index >= 0) {
            list.ensureIndexIsVisible(index);
        }
    }

    /**
     * Finds the nearest visible ancestor row of a node (its own row may be hidden under a collapsed parent).
     */
    private int nearestVisibleAncestorRow(JIPipeProgressLog s, int nodeId) {
        JIPipeProgressNode node = s.getNode(nodeId);
        while (node != null && node.getId() != s.getRootNodeId()) {
            int index = rowPositions.getOrDefault(node.getId(), -1);
            if (index >= 0) {
                return index;
            }
            int parentId = node.getParentId();
            if (parentId <= 0) {
                break;
            }
            node = s.getNode(parentId);
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Row interaction
    // -------------------------------------------------------------------------

    private void toggleNode(int nodeId) {
        NodeUiState state = nodeState(nodeId);
        state.expanded = !state.expanded;
        JIPipeProgressLog s = store;
        if (state.expanded && s != null && !state.loaded && !state.loading) {
            loadDetailPage(s, nodeId, false);
        }
        refreshNow();
    }

    private void toggleStack(int nodeId, JIPipeProgressRecord.Error error) {
        String key = errorKey(nodeId, error);
        if (!openStacks.remove(key)) {
            openStacks.add(key);
        }
        refreshNow();
    }

    private void showOlderRecords(int nodeId) {
        JIPipeProgressLog s = store;
        NodeUiState state = nodeStates.get(nodeId);
        if (s != null && state != null && !state.loading) {
            loadDetailPage(s, nodeId, true);
        }
    }

    private static String errorKey(int nodeId, JIPipeProgressRecord.Error error) {
        // Records carry no sequence number — the stable identity of an error across page re-reads
        // is its node, timestamp, and class (identical errors at the same millisecond merge)
        return nodeId + "@" + error.ts() + "@" + error.c();
    }

    // -------------------------------------------------------------------------
    // Copy / Export
    // -------------------------------------------------------------------------

    private void copyToClipboard() {
        JIPipeProgressLog s = store;
        if (s == null) {
            return;
        }
        int gen = generation.get();
        JIPipeLogLevel level = minLevel;
        backgroundExecutor.submit(() -> {
            try {
                // Bounded render from the store's tail (a full render of a huge log must not be
                // copied into the clipboard)
                List<JIPipeProgressRecord> recent = s.readRecent(COPY_LINE_LIMIT);
                StringBuilder builder = new StringBuilder();
                s.renderRecords(recent, level, line -> builder.append(line).append('\n'));
                String text = builder.toString();
                SwingUtilities.invokeLater(() -> {
                    if (gen == generation.get()) {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                    }
                });
            } catch (Exception e) {
                log.error("Failed to copy the progress log", e);
            }
        });
    }

    private void exportDataFile() {
        JIPipeProgressLog s = store;
        if (s == null || s.getFile() == null || workbench == null) {
            return;
        }
        Path target = JIPipeDesktop.saveFile(this, workbench,
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data,
                "Export progress data file", HTMLText.EMPTY, PathUtils.EXTENSION_FILTER_NDJSON);
        if (target != null) {
            try {
                Files.copy(s.getFile(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static int recordNodeId(JIPipeProgressRecord record) {
        if (record instanceof JIPipeProgressRecord.Node nodeRecord) {
            return nodeRecord.id();
        } else if (record instanceof JIPipeProgressRecord.NodeFinished finishRecord) {
            return finishRecord.id();
        } else if (record instanceof JIPipeProgressRecord.Log logRecord) {
            return logRecord.id();
        } else if (record instanceof JIPipeProgressRecord.Progress progressRecord) {
            return progressRecord.id();
        } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
            return errorRecord.id();
        }
        return 0;
    }

    private static long recordTs(JIPipeProgressRecord record) {
        if (record instanceof JIPipeProgressRecord.Node nodeRecord) {
            return nodeRecord.ts();
        } else if (record instanceof JIPipeProgressRecord.NodeFinished finishRecord) {
            return finishRecord.ts();
        } else if (record instanceof JIPipeProgressRecord.Log logRecord) {
            return logRecord.ts();
        } else if (record instanceof JIPipeProgressRecord.Progress progressRecord) {
            return progressRecord.ts();
        } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
            return errorRecord.ts();
        } else if (record instanceof JIPipeProgressRecord.Notification notificationRecord) {
            return notificationRecord.ts();
        } else if (record instanceof JIPipeProgressRecord.Status statusRecord) {
            return statusRecord.ts();
        } else if (record instanceof JIPipeProgressRecord.Head headRecord) {
            return headRecord.ts();
        }
        return 0;
    }

    /**
     * Whether a record passes the level filter (log records must be at or above the minimum level;
     * all other records have no level and pass — same rule as the store's renderer).
     */
    private static boolean passesLevel(JIPipeProgressRecord record, JIPipeLogLevel level) {
        return !(record instanceof JIPipeProgressRecord.Log logRecord) || logRecord.lv() >= level.value();
    }

    /**
     * Joins the labels of all ancestors of the node up to the root with {@code ›} (the mockup's
     * footer breadcrumb).
     */
    private static String labelPath(JIPipeProgressLog store, JIPipeProgressNode node) {
        Deque<String> labels = new ArrayDeque<>();
        JIPipeProgressNode current = node;
        while (current != null && current.getId() != store.getRootNodeId()) {
            if (current.getLabel() != null && !current.getLabel().isBlank()) {
                labels.addFirst(current.getLabel());
            }
            int parentId = current.getParentId();
            if (parentId <= 0 || parentId == store.getRootNodeId()) {
                break;
            }
            current = store.getNode(parentId);
        }
        return String.join(" › ", labels);
    }

    /**
     * The {@code v/mx (pct%)} progress text, or "" for indeterminate progress.
     */
    private static String progressText(JIPipeProgressNode node) {
        if (node.getMaxProgress() <= 0) {
            return "";
        }
        int percent = Math.max(0, Math.min(100, (int) Math.round(100.0 * node.getProgress() / node.getMaxProgress())));
        return node.getProgress() + "/" + node.getMaxProgress() + " (" + percent + "%)";
    }

    private static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return String.format("%.1fs", millis / 1000.0);
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static String abbreviatePath(Path path, int maxLength) {
        String text = path.toString();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength / 2 - 1) + "…" + text.substring(text.length() - maxLength / 2);
    }

    /**
     * Per-node UI state. Created lazily by batch listeners (writer thread), the catch-up task
     * (background), and the EDT refresh cycle; counters are atomic. Display state (expansion,
     * detail pages) is only written on the EDT.
     */
    private static final class NodeUiState {
        final int nodeId;
        final AtomicLong firstTs = new AtomicLong();
        final AtomicLong lastTs = new AtomicLong();
        final AtomicLong debugCount = new AtomicLong();
        final AtomicLong renderableCount = new AtomicLong();
        volatile boolean expanded;
        volatile boolean loading;
        volatile boolean loaded;
        volatile boolean hasOlder;
        volatile int pageFromLine;
        volatile List<JIPipeProgressRecord> detailPage = List.of();

        private NodeUiState(int nodeId) {
            this.nodeId = nodeId;
        }
    }

    /**
     * The row list: row-specific tooltips, hand cursor over interactive rows, and click routing
     * (expander toggles, stack trace toggles, paging, and the level-filter shortcut).
     */
    private class ProgressTreeList extends JList<JIPipeDesktopProgressTreeRowRenderer.Row> {

        private ProgressTreeList() {
            super(listModel);
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleClick(rowAtPoint(e));
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    JIPipeDesktopProgressTreeRowRenderer.Row row = rowAtPoint(e);
                    boolean interactive = row instanceof JIPipeDesktopProgressTreeRowRenderer.NodeRow nodeRow && nodeRow.isExpandable()
                            || row instanceof JIPipeDesktopProgressTreeRowRenderer.DetailRow detailRow && detailRow.hasStack()
                            || row instanceof JIPipeDesktopProgressTreeRowRenderer.StackRow
                            || row instanceof JIPipeDesktopProgressTreeRowRenderer.MoreRow
                            || row instanceof JIPipeDesktopProgressTreeRowRenderer.DebugHiddenRow;
                    setCursor(Cursor.getPredefinedCursor(interactive ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        private JIPipeDesktopProgressTreeRowRenderer.Row rowAtPoint(MouseEvent e) {
            int index = locationToIndex(e.getPoint());
            if (index < 0 || index >= listModel.size()) {
                return null;
            }
            Rectangle bounds = getCellBounds(index, index);
            if (bounds == null || !bounds.contains(e.getPoint())) {
                return null;
            }
            return listModel.get(index);
        }

        private void handleClick(JIPipeDesktopProgressTreeRowRenderer.Row row) {
            if (row instanceof JIPipeDesktopProgressTreeRowRenderer.NodeRow nodeRow) {
                if (nodeRow.isExpandable()) {
                    toggleNode(nodeRow.getNodeId());
                }
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.DetailRow detailRow) {
                if (detailRow.hasStack() && detailRow.record() instanceof JIPipeProgressRecord.Error error) {
                    toggleStack(detailRow.nodeId(), error);
                }
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.StackRow stackRow) {
                // Clicking anywhere in an opened stack block closes it
                NodeUiState state = nodeStates.get(stackRow.nodeId());
                if (state != null) {
                    for (JIPipeProgressRecord record : state.detailPage) {
                        if (record instanceof JIPipeProgressRecord.Error error
                                && error.st() != null && error.st().contains(stackRow.line())) {
                            openStacks.remove(errorKey(stackRow.nodeId(), error));
                            break;
                        }
                    }
                }
                refreshNow();
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.MoreRow moreRow) {
                showOlderRecords(moreRow.nodeId());
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.DebugHiddenRow) {
                levelComboBox.setSelectedIndex(1);
            }
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            JIPipeDesktopProgressTreeRowRenderer.Row row = rowAtPoint(event);
            if (row instanceof JIPipeDesktopProgressTreeRowRenderer.NodeRow nodeRow) {
                StringBuilder builder = new StringBuilder(nodeRow.getLabel());
                if (nodeRow.getPartitionChip() != null) {
                    builder.append(" (").append(nodeRow.getPartitionChip()).append(")");
                }
                JIPipeProgressLog s = store;
                if (s != null) {
                    JIPipeProgressNode node = s.getNode(nodeRow.getNodeId());
                    if (node != null) {
                        String progress = progressText(node);
                        if (!progress.isEmpty()) {
                            builder.append(" — ").append(progress);
                        }
                        builder.append(" [").append(node.getState()).append("]");
                    }
                }
                return builder.toString();
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.DetailRow detailRow) {
                if (detailRow.record() instanceof JIPipeProgressRecord.Log logRecord) {
                    return logRecord.m();
                } else if (detailRow.record() instanceof JIPipeProgressRecord.Error errorRecord) {
                    return errorRecord.c() + (errorRecord.m() == null || errorRecord.m().isEmpty() ? "" : ": " + errorRecord.m());
                }
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.DebugHiddenRow debugRow) {
                return "DEBUG records are hidden by the current level filter";
            }
            return null;
        }
    }
}
