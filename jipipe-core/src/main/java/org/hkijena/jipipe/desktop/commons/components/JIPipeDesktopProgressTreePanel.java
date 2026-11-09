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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The v2 progress tree panel - the Swing implementation of the approved design contract
 * ({@code docs/superpowers/specs/2026-11-04-progress-system-overhaul/ui/v2-progress-panel.html}).
 * Replaces the flat log in the run window; the AI monitor keeps the old
 * {@link JIPipeDesktopProgressLoggerPanel}.
 * <p>
 * <b>Update driver:</b> batch events ({@link #onProgressBatch}) arrive on the store's writer thread
 * and only mark the model dirty (they read the batch's records once to maintain the per-node
 * timestamp/debug counters - the same bounded batch reads the previous flat panels did, but no
 * rendering). A 250 ms {@link Timer} rebuilds the visible rows from the store's node index
 * (in-memory {@code getNode} reads) and schedules bounded record reads for expanded rows on a
 * background executor; results are applied on the EDT. No per-record work happens on the EDT.
 * <p>
 * <b>Row model:</b> a flat, virtualized {@link JList} of {@link Row} snapshots - node rows and
 * phase rows sit directly in the list; {@code PARTITION}-kind nodes are squashed into the dim
 * {@code P0} chip of their descendants (never rendered as rows). Expanded nodes show their records
 * in one of two detail modes (R30):
 * <ul>
 * <li><b>Virtual mode</b> (all of the node's own records pass the category toggles): the detail
 * block maps the node's renderable records ({@code l}/{@code e}) contiguously - detail slot k is
 * the k-th renderable record of the node, ordered by line (R36; the list extent equals the
 * renderable count, so the scrollbar is proportional to the real content and structural records
 * never occupy rows). Chunks of {@value #VIRTUAL_CHUNK_SLOTS} slots load around the viewport off
 * the EDT: a chunk is filled by a forward scan from the nearest cached slot-to-line boundary (or
 * the node's first line), and the newest slots load from the node's last line window with a
 * growing margin; unloaded slots render as {@code …} placeholder rows; the chunk and boundary
 * caches are pruned beyond the viewport window, so memory stays bounded regardless of the record
 * count. The tail grows live: the view follows it while the detail autoscroll is engaged
 * (scroll-up disengages, the go-to-end button re-engages).</li>
 * <li><b>Paged mode</b> (the node has records hidden by the toggles): the last
 * {@value #DETAIL_PAGE_SIZE} records plus the {@code Show N older records from run data…} row,
 * re-read on demand.</li>
 * </ul>
 */
public class JIPipeDesktopProgressTreePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(JIPipeDesktopProgressTreePanel.class);

    public static final String ICON_STATUS_OK = "actions/circle-check.png";
    public static final String ICON_STATUS_ERROR = "emblems/vcs-conflicting.png";
    public static final String ICON_STATUS_CANCELLED = "emblems/emblem-unavailable.png";

    private static final int REFRESH_INTERVAL_MILLIS = 250;
    private static final int DETAIL_PAGE_SIZE = 1000;
    private static final int COPY_LINE_LIMIT = 8192;
    private static final int CATCH_UP_PAGE_SIZE = 10_000;

    /**
     * R36 chunked virtual detail loading over slot space. A chunk covers
     * {@value #VIRTUAL_CHUNK_SLOTS} renderable records (detail slots; structural records occupy
     * no slots). Around the viewport's slot span, {@value #VIRTUAL_PREFETCH_CHUNKS} chunks toward
     * each side are loaded, and {@value #VIRTUAL_CACHED_CHUNKS} chunks toward each side are kept
     * - everything outside is pruned (chunks and slot-to-line boundaries alike). The cache bound
     * is therefore about {@code (2 * VIRTUAL_CACHED_CHUNKS + 2) * VIRTUAL_CHUNK_SLOTS ≈ 13 × 512
     * ≈ 6,700} records plus the boundaries per expanded node (the +2 covers the chunks the
     * viewport span itself straddles), independent of the node's record count.
     */
    private static final int VIRTUAL_CHUNK_SLOTS = 512;
    private static final int VIRTUAL_PREFETCH_CHUNKS = 2;
    private static final int VIRTUAL_CACHED_CHUNKS = 2 * VIRTUAL_PREFETCH_CHUNKS + 1;
    /**
     * Chunks whose slot range reaches the newest slots load from the node's last line window
     * (a growing margin) instead of a forward scan: a forward scan from the node's start would
     * traverse the whole node once it is under a few thousand renderable records.
     */
    private static final int VIRTUAL_TAIL_WINDOW_SLOTS = 4 * VIRTUAL_CHUNK_SLOTS;

    /**
     * The record categories of the toggle-button filter (R22): debug = {@code l}@DEBUG, info =
     * {@code l}@INFO, warning = {@code l}@WARN + notification ({@code w}) records, error =
     * {@code l}@ERROR + error ({@code e}) records.
     */
    private enum RecordCategory {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    private final JIPipeDesktopWorkbench workbench;

    // --- Header ---
    private final JCheckBox showLogCheckBox = new JCheckBox("Show log");
    private final JCheckBox autoShowProgressCheckBox = new JCheckBox("Auto show progress");
    private final JCheckBox autoViewResultsCheckBox = new JCheckBox("Auto view results");
    private final JToggleButton debugToggleButton = new JToggleButton("debug");
    private final JToggleButton infoToggleButton = new JToggleButton("info");
    private final JToggleButton warningToggleButton = new JToggleButton("warning");
    private final JToggleButton errorToggleButton = new JToggleButton("error");
    private final JButton copyButton = new JButton("Copy", JIPipe.RESOURCES.getIcon16("actions/copy.png"));
    private final JButton exportButton = new JButton("Export", JIPipe.RESOURCES.getIcon16("actions/document-export.png"));
    private final JPanel headerComponentsPanel = UIUtils.boxHorizontal();

    // --- Center (the flat row list) ---
    private final ProgressListModel listModel = new ProgressListModel();
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
    private final JToggleButton detailAutoscrollButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/arrow-down-double.png"));
    private final JButton detailGoToEndButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/go-bottom.png"));

    // --- Store state (batch listeners run on the store's writer thread; the EDT consumes) ---
    private final Object storeLock = new Object();
    private volatile JIPipeProgressLog store;
    private final AtomicInteger generation = new AtomicInteger();
    private final Map<Integer, NodeUiState> nodeStates = new ConcurrentHashMap<>();
    private final Map<Integer, String> partitionChips = new ConcurrentHashMap<>();
    private final Set<String> openStacks = ConcurrentHashMap.newKeySet();
    private final Set<Integer> pendingNodeIds = ConcurrentHashMap.newKeySet();
    /**
     * The currently expanded nodes (mirrors {@link NodeUiState#expanded} for the writer thread,
     * which re-reads running expanded nodes on every batch so that run-level notification records
     * appear in their detail windows).
     */
    private final Set<Integer> expandedNodeIds = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean catchUpScheduled = new AtomicBoolean();
    private volatile boolean dirty;
    private volatile int latestNodeId;
    /**
     * The most recent node that reported determinate progress ({@code mx > 0}); the running
     * footer prefers it over the newest node, because the newest node during an iteration is the
     * freshly resolved slice/item row whose own counters stay at zero (R41).
     */
    private volatile int footerProgressNodeId;
    private volatile long latestNodeTs;
    private volatile long lastKnownLine;
    /**
     * True while one scheduler pass is queued behind a chunk apply (EDT-only): a chained scan's
     * discovered boundary can make a successor chunk schedulable that the previous pass could
     * not fill - the passes coalesce so a burst of applies queues one pass, not one per chunk.
     */
    private boolean chunkApplyPassQueued;
    private volatile long fileSize;

    /**
     * The visible record categories of the toggle-button filter (R22). Replaced - never mutated -
     * on the EDT (button clicks); read on background threads (detail page filtering).
     */
    private volatile EnumSet<RecordCategory> visibleCategories = EnumSet.of(RecordCategory.INFO, RecordCategory.WARNING, RecordCategory.ERROR);

    /**
     * Live per-category record counts (the toggle buttons' glanceable volumes). Incremented by the
     * batch/catch-up record accumulation (off the EDT); read on the EDT (button labels).
     */
    private final AtomicLong[] categoryCounts = new AtomicLong[RecordCategory.values().length];

    // EDT-only
    private final Map<Integer, Integer> rowPositions = new HashMap<>();
    /**
     * The virtual detail blocks emitted by the current rebuild pass (R21) - passed to the row
     * model's reset. EDT-only, valid only during {@code updateTreeModel}.
     */
    private final List<VirtualBlock> rebuildBlocks = new ArrayList<>();
    private int rebuildBlockShift;

    // --- Virtual detail scroll state (R30; all EDT-only) ---
    /**
     * The node whose detail tail the view follows while {@link #detailAutoscroll} is engaged
     * (the most recently expanded node, re-targeted by the go-to-end button).
     */
    private int autoscrollNodeId;
    private boolean detailAutoscroll;
    /**
     * Pending scroll-to-detail-end request (expansion and the go-to-end button set it; the next
     * refresh tick applies it once the row positions are known).
     */
    private int scrollToDetailEndNodeId;
    /**
     * The most recently expanded node (the go-to-end/autoscroll target when the viewport is
     * outside any detail block).
     */
    private int lastExpandedNodeId;
    /**
     * True while a programmatic scroll runs - the adjustment listener must not interpret it as a
     * manual scroll-up (which disengages the detail autoscroll).
     */
    private boolean scrollProgrammatic;
    private int lastScrollValue;
    /**
     * The model size at the last adjustment event: a model shrink (a closed stack trace, an
     * eroded parent window over a growing descendant) clamps the scrollbar value while the
     * viewport stays pinned - that decrease is not a user scroll-up.
     */
    private int lastScrollModelSize;

    private final Timer refreshTimer;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "JIPipe-Progress-Tree-Read");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Creates a new progress tree panel.
     *
     * @param workbench the desktop workbench (used by the Export action; may be null - Export stays disabled)
     */
    public JIPipeDesktopProgressTreePanel(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        for (RecordCategory category : RecordCategory.values()) {
            categoryCounts[category.ordinal()] = new AtomicLong();
        }
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

        // Flat chrome on the panel background: the panel fills its host edge-to-edge, with the
        // header/footer as flat strips separated by hairlines (mockup: no rounded island, no rim)
        var style = ThemeUtils.getCurrentStyle();
        setOpaque(true);
        setBackground(style.getPanelBackground());

        // Header: Show log / Auto show progress / Auto view results | category toggles | Copy | Export
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 12, 8, 12),
                BorderFactory.createMatteBorder(0, 0, 1, 0, style.getBorderColor())));
        JPanel headerLeft = UIUtils.boxHorizontal(showLogCheckBox, autoShowProgressCheckBox, autoViewResultsCheckBox);
        JPanel headerRight = UIUtils.boxHorizontal(debugToggleButton, infoToggleButton, warningToggleButton, errorToggleButton, copyButton, exportButton, headerComponentsPanel);
        headerLeft.setOpaque(false);
        headerRight.setOpaque(false);
        headerComponentsPanel.setOpaque(false);
        for (JComponent component : List.of(showLogCheckBox, autoShowProgressCheckBox, autoViewResultsCheckBox,
                debugToggleButton, infoToggleButton, warningToggleButton, errorToggleButton, copyButton, exportButton, headerComponentsPanel)) {
            component.setAlignmentY(0.5f);
        }
        headerPanel.add(headerLeft, BorderLayout.WEST);
        headerPanel.add(headerRight, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center: the flat, virtualized row list
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

        // Scroll tracking (R30): a manual scroll-up pauses the detail tail follow; every scroll
        // position change loads the chunks of the surrounding viewport
        listScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            int value = e.getAdjustable().getValue();
            int modelSize = listModel.getSize();
            boolean scrolledUp = value < lastScrollValue;
            // A model shrink clamps the scrollbar value while the viewport stays pinned at the
            // bottom - that decrease is not a user scroll-up (the follow re-asserts the tail
            // position right after the rebuild)
            boolean modelShrank = modelSize < lastScrollModelSize;
            lastScrollValue = value;
            lastScrollModelSize = modelSize;
            if (scrolledUp && !modelShrank && detailAutoscroll && !scrollProgrammatic) {
                detailAutoscroll = false;
                autoscrollNodeId = 0;
                detailAutoscrollButton.setSelected(false);
            }
            JIPipeProgressLog s = store;
            if (s != null) {
                scheduleVirtualChunkLoads(s);
            }
        });

        // Footer: state icon | breadcrumb + live progress | N records · size | data file | buttons
        JPanel footerPanel = new JPanel(new BorderLayout(8, 0));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, style.getBorderColor()),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        footerStateLabel.setIcon(footerSpinner);
        footerStateLabel.setFont(footerStateLabel.getFont().deriveFont(12.0f));
        footerStatusLabel.setFont(footerStatusLabel.getFont().deriveFont(12.0f));
        footerStatusLabel.setForeground(style.getTextMuted());
        footerStateLabel.setAlignmentY(0.5f);
        footerStatusLabel.setAlignmentY(0.5f);
        footerRecordsLabel.setFont(footerRecordsLabel.getFont().deriveFont(12.0f));
        footerRecordsLabel.setForeground(style.getTextMuted());
        footerRecordsLabel.setAlignmentY(0.5f);
        footerFileLabel.setFont(footerFileLabel.getFont().deriveFont(12.0f));
        footerFileLabel.setForeground(style.getTextMuted());
        footerFileLabel.setAlignmentY(0.5f);

        // Detail scroll controls (R30): tail follow + go-to-end. Independent of the header's
        // "Auto show progress" (which governs the row list, not the expanded details).
        detailAutoscrollButton.setEnabled(false);
        detailAutoscrollButton.setToolTipText("Follow the tail of the expanded records (scrolling up pauses it)");
        detailAutoscrollButton.addActionListener(e -> {
            if (detailAutoscrollButton.isSelected()) {
                int nodeId = currentDetailNodeId();
                if (nodeId > 0) {
                    engageDetailAutoscroll(nodeId);
                } else {
                    detailAutoscrollButton.setSelected(false);
                }
            } else {
                detailAutoscroll = false;
                autoscrollNodeId = 0;
            }
        });
        detailGoToEndButton.setEnabled(false);
        detailGoToEndButton.setToolTipText("Jump to the newest record of the expanded node");
        detailGoToEndButton.addActionListener(e -> goToNewestRecords());
        JPanel footerLeft = UIUtils.boxHorizontal(footerStateLabel, footerStatusLabel, Box.createHorizontalStrut(10), detailAutoscrollButton, detailGoToEndButton);
        footerLeft.setOpaque(false);

        // Struts separate the information segments and the button group
        JPanel footerRight = UIUtils.boxHorizontal(footerRecordsLabel, Box.createHorizontalStrut(12), footerFileLabel, Box.createHorizontalStrut(12), footerButtons);
        footerRight.setOpaque(false);
        footerButtons.setOpaque(false);
        footerPanel.add(footerLeft, BorderLayout.WEST);
        footerPanel.add(footerRight, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);

        showLogCheckBox.setSelected(true);
        showLogCheckBox.addActionListener(e -> updateShowLogVisibility());
        autoShowProgressCheckBox.setSelected(true);
        autoShowProgressCheckBox.setToolTipText("Automatically scroll to the currently running node");
        // Toggle-button filter (R22): each button toggles its category's visibility and carries a
        // live per-category record count. Default: debug off, info/warning/error on.
        debugToggleButton.setSelected(false);
        debugToggleButton.setToolTipText("Toggle DEBUG records (per-item/per-pair logging)");
        infoToggleButton.setSelected(true);
        infoToggleButton.setToolTipText("Toggle INFO records");
        warningToggleButton.setSelected(true);
        warningToggleButton.setToolTipText("Toggle warnings (including run-level notifications)");
        errorToggleButton.setSelected(true);
        errorToggleButton.setToolTipText("Toggle errors");
        debugToggleButton.addActionListener(e -> toggleCategory(RecordCategory.DEBUG, debugToggleButton.isSelected()));
        infoToggleButton.addActionListener(e -> toggleCategory(RecordCategory.INFO, infoToggleButton.isSelected()));
        warningToggleButton.addActionListener(e -> toggleCategory(RecordCategory.WARNING, warningToggleButton.isSelected()));
        errorToggleButton.addActionListener(e -> toggleCategory(RecordCategory.ERROR, errorToggleButton.isSelected()));
        updateCategoryButtons();
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
     * Shows the progress of the given store (called with the run's current store - a graph run may
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
        // lines - batches cover the rest without double-counting).
        JIPipeProgressNode root = newStore != null ? newStore.getNode(newStore.getRootNodeId()) : null;
        if (newStore != null && (root == null || root.getState() != JIPipeProgressNodeState.RUNNING || newStore.getFile() == null)
                && catchUpScheduled.compareAndSet(false, true)) {
            scheduleCatchUp(1, Integer.MAX_VALUE);
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
     * The empty final batch (fromLine &gt; toLine) only triggers a refresh - the run's
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
            // The accumulated prefix as this batch sees it: the gap check below must judge
            // contiguity against the value before this batch, not after its own advance
            long prevLastKnown = lastKnownLine;
            // Read the batch's records once (the same bounded reads the previous flat panels did)
            // to maintain the timestamps and hidden-debug counters; no rendering, no EDT work
            accumulate(batchStore, batchStore.readRecords(batch.fromLine(), batch.toLine() - batch.fromLine() + 1));
            if (batch.toLine() > lastKnownLine) {
                lastKnownLine = batch.toLine();
            }
            // A catch-up is needed only for lines that no batch fed yet: fromLine beyond the
            // already-accumulated prefix. The first batch of a live run starts at line 1 and
            // every later batch continues contiguously, so a fromLine > 1 on its own is normal,
            // not a gap. Triggering the catch-up on fromLine > 1 re-accumulated the covered
            // prefix and counted its records twice: the inflated extents made every tail read
            // come up short, so the expanded nodes' placeholders never resolved (V6-1).
            if (batch.fromLine() > prevLastKnown + 1) {
                scheduleCatchUp((int) Math.min(prevLastKnown + 1, batch.fromLine() - 1L), batch.fromLine() - 1);
            }
            // Run-level records (notifications) change no node: re-read expanded RUNNING nodes so
            // new notifications surface in their detail windows
            for (Integer expandedId : expandedNodeIds) {
                JIPipeProgressNode expandedNode = batchStore.getNode(expandedId);
                if (expandedNode != null && expandedNode.getState() == JIPipeProgressNodeState.RUNNING) {
                    pendingNodeIds.add(expandedId);
                }
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
        // A programmatic setSelected fires no ActionEvent - the host restores the stored
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
            expandedNodeIds.clear();
            latestNodeId = 0;
            footerProgressNodeId = 0;
            latestNodeTs = 0;
            lastKnownLine = 0;
            fileSize = 0;
            for (AtomicLong count : categoryCounts) {
                count.set(0);
            }
            catchUpScheduled.set(false);
        }
        dirty = true;
        SwingUtilities.invokeLater(() -> {
            listModel.reset(List.of(), List.of());
            rowPositions.clear();
            updateFooter();
        });
    }

    /**
     * Accumulates the per-node counters (first/last timestamp, hidden-debug count, renderable
     * record count) and the per-category record counts (the toggle buttons' volumes) from records.
     * Called on the writer thread (per batch) and on the background executor (catch-up scan); all
     * state is atomic. Batch deltas plus the catch-up scan partition the record lines exactly once,
     * so the counts are exact for the observed range.
     */
    private void accumulate(JIPipeProgressLog s, List<JIPipeProgressRecord> records) {
        int rootNodeId = s.getRootNodeId();
        for (JIPipeProgressRecord record : records) {
            // R41: remember the newest determinate-progress node for the running footer (the
            // catch-up scan feeds the same records, so a mid-run attach heals it too). The footer
            // read filters root and phase nodes out (fix round 1, F6).
            if (record instanceof JIPipeProgressRecord.Progress progressRecord && progressRecord.mx() > 0) {
                footerProgressNodeId = progressRecord.id();
            }
            RecordCategory category = categoryOf(record);
            if (category != null) {
                categoryCounts[category.ordinal()].incrementAndGet();
            }
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
                RecordCategory logCategory = categoryOfLevel(logRecord.lv());
                switch (logCategory) {
                    case DEBUG -> state.debugCount.incrementAndGet();
                    case INFO -> state.infoCount.incrementAndGet();
                    case WARNING -> state.warnCount.incrementAndGet();
                    case ERROR -> state.errorCount.incrementAndGet();
                }
            } else if (record instanceof JIPipeProgressRecord.Error) {
                state.errorCount.incrementAndGet();
            }
            // R38 machinery progress lands on the run root; a root p record is run-level noise,
            // not a step - it must not capture the footer's latest-node narration (fix round 1, F6)
            if (nodeId == rootNodeId && record instanceof JIPipeProgressRecord.Progress) {
                continue;
            }
            if (ts >= latestNodeTs) {
                latestNodeTs = ts;
                latestNodeId = nodeId;
            }
        }
    }

    /**
     * Returns the record's filter category ({@code null} for records that render nothing:
     * progress, node, node-finished, head, and run-status records).
     */
    private static RecordCategory categoryOf(JIPipeProgressRecord record) {
        if (record instanceof JIPipeProgressRecord.Log logRecord) {
            return categoryOfLevel(logRecord.lv());
        } else if (record instanceof JIPipeProgressRecord.Error) {
            return RecordCategory.ERROR;
        } else if (record instanceof JIPipeProgressRecord.Notification) {
            return RecordCategory.WARNING;
        }
        return null;
    }

    private static RecordCategory categoryOfLevel(int levelValue) {
        if (levelValue <= JIPipeLogLevel.DEBUG.value()) {
            return RecordCategory.DEBUG;
        } else if (levelValue <= JIPipeLogLevel.INFO.value()) {
            return RecordCategory.INFO;
        } else if (levelValue <= JIPipeLogLevel.WARN.value()) {
            return RecordCategory.WARNING;
        } else {
            return RecordCategory.ERROR;
        }
    }

    private NodeUiState nodeState(int nodeId) {
        return nodeStates.computeIfAbsent(nodeId, NodeUiState::new);
    }

    /**
     * Reads all records in the given line range once - covers the records that were written
     * before the panel attached to the store (full scan at attach time) or that sit in a gap
     * between the accumulated prefix and a later batch (bounded scan) - and applies them to the
     * counters. Ranges scheduled from the batch handler never overlap: each starts after the
     * accumulated prefix of its triggering batch, which is monotonically ahead of every earlier
     * scheduling point, so every covered line is counted exactly once.
     *
     * @param fromLine          the first line to read
     * @param upToLineExclusive the last line to stop before ({@link Integer#MAX_VALUE} = until the end)
     */
    private void scheduleCatchUp(int fromLine, int upToLineExclusive) {
        int gen = generation.get();
        JIPipeProgressLog storeSnapshot = store;
        backgroundExecutor.submit(() -> {
            int from = fromLine;
            while (from < upToLineExclusive && gen == generation.get() && !Thread.currentThread().isInterrupted()) {
                int limit = Math.min(CATCH_UP_PAGE_SIZE, upToLineExclusive - from);
                List<JIPipeProgressRecord> page = storeSnapshot.readRecords(from, limit);
                if (page.isEmpty()) {
                    break;
                }
                accumulate(storeSnapshot, page);
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
        if (!SwingUtilities.isEventDispatchThread()) {
            return;
        }
        JIPipeProgressLog s = store;
        // The chunk scheduler runs on EVERY tick (R37), not only on dirty ones: after the run's
        // last batch nothing sets dirty, and expansion's single pass starves whenever its reads
        // come up short or fail - in a finished run there are no batches and (without scrolling)
        // no further scheduling events, so placeholders would persist forever. The scheduler is
        // an async-load-completion concern; the model rebuild below stays batch-driven (R21).
        if (s != null) {
            scheduleVirtualChunkLoads(s);
        }
        if (!dirty) {
            return;
        }
        dirty = false;
        if (s == null) {
            updateFooter();
            return;
        }
        updateTreeModel(s);
        scheduleDetailReloads(s);
        updateFooter();
        updateCategoryButtons();
        scheduleFileSizeUpdate(s);
        updateSpinners(s);
        followDetailTail();
        updateDetailControls();
        autoScroll();
    }

    /**
     * Rebuilds the flat row list from the store's node index (in-memory {@code getNode} reads only).
     * The root node renders as a row only when it holds visible records (R33): non-graph runs
     * keep their narration root row, while graph runs (V3-1: a recordless root, narration in the
     * "Pipeline setup"/"Pipeline finished" phase rows) show their node and phase rows directly at
     * the top level with no "Pipeline run" grouping row.
     */
    private void updateTreeModel(JIPipeProgressLog s) {
        List<JIPipeDesktopProgressTreeRowRenderer.Row> rows = new ArrayList<>();
        rowPositions.clear();
        rebuildBlocks.clear();
        rebuildBlockShift = 0;
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
        nodeState(s.getRootNodeId());
        int rootNodeId = s.getRootNodeId();
        NodeUiState rootState = nodeStates.get(rootNodeId);
        if (rootState != null && visibleRecordCount(rootState) > 0) {
            // Non-graph run: the root's own records render under the root row, as before (R26)
            appendNodeRow(s, childMap, s.getNode(rootNodeId), 0, null, rows);
        } else {
            // Graph run (or a root whose records are all hidden by the toggles): no grouping row;
            // the top-level children start at depth 0. A recordless root also has no detail area.
            walkChildren(s, childMap, rootNodeId, 0, null, rows);
        }
        listModel.reset(rows, new ArrayList<>(rebuildBlocks));
    }

    /**
     * Emits one node row (with its detail rows when expanded) and recurses into its visible
     * children. All visibility/expandability/badge numbers are computed under the CURRENT filter
     * state (R27/R29) - the full-row-snapshot rebuild (R21) recomputes them on every dirty tick
     * and on toggle flips without any accounting changes.
     */
    private void appendNodeRow(JIPipeProgressLog s, Map<Integer, List<Integer>> childMap,
                               JIPipeProgressNode node, int depth, String inheritedChip,
                               List<JIPipeDesktopProgressTreeRowRenderer.Row> rows) {
        int id = node.getId();
        NodeUiState state = nodeStates.get(id);
        // R29: the +N badge is the hidden CHILD NODE count only (visible under the current
        // filters, never records); the triangle needs visible children or visible records
        int visibleDescendantNodes = countVisibleDescendantNodes(s, childMap, id);
        long visibleRecords = state != null ? visibleRecordCount(state) : 0;
        boolean expandable = visibleDescendantNodes > 0 || visibleRecords > 0;
        String label = node.getLabel();
        if (label == null || label.isBlank()) {
            label = "(unnamed)";
        }
        // The faint [uuid] suffix is derived from a node:// location URI (R23); other/absent
        // locations render no suffix
        String uuidSuffix = null;
        String location = node.getLocation();
        if (location != null && location.startsWith("node://")) {
            String uuid = location.substring("node://".length());
            if (!uuid.isBlank()) {
                uuidSuffix = "[" + uuid + "]";
            }
        }
        int debugChipCount = 0;
        long firstTs = 0;
        long lastTs = 0;
        if (state != null) {
            debugChipCount = visibleCategories.contains(RecordCategory.DEBUG)
                    ? 0
                    : (int) Math.min(Integer.MAX_VALUE, state.debugCount.get());
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
                visibleDescendantNodes,
                debugChipCount,
                node.getMaxProgress() > 0 ? node.getProgress() : -1,
                node.getMaxProgress() > 0 ? node.getMaxProgress() : -1,
                // R38: phase rows render no bar (machinery progress lives on the run root)
                node.getKind() != JIPipeProgressNodeKind.PHASE,
                phaseText(node, firstTs, lastTs)));
        rowPositions.put(id, rows.size() - 1 + rebuildBlockShift);
        if (state != null && state.expanded) {
            if (state.virtual != null) {
                // R30 virtual mode: the node's own records all pass the current toggles - the
                // detail area becomes a contiguous virtual block (row ↔ own record line) whose
                // rows are served from the chunk cache; a late batch that brings records hidden
                // by the toggles demotes the node back to the paged mode
                if (ownHiddenRecordCount(state) > 0) {
                    state.virtual = null;
                    clearOpenStacks(id);
                    if (!state.loading) {
                        loadDetailPage(s, id, false);
                    }
                    appendDetailRows(rows, id, state, node);
                } else {
                    VirtualBlock block = buildVirtualBlock(id, rows.size(), state);
                    if (block != null) {
                        rebuildBlocks.add(block);
                        rebuildBlockShift += block.extent;
                    }
                }
            } else {
                appendDetailRows(rows, id, state, node);
            }
            walkChildren(s, childMap, id, depth + 1, inheritedChip, rows);
        }
    }

    private void walkChildren(JIPipeProgressLog s, Map<Integer, List<Integer>> childMap, int parentId, int depth,
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
                // v2 decision: partitions squash into the dim chip of their descendants - no row,
                // same depth (the data model still records them; the UI spends no tree level)
                walkChildren(s, childMap, id, depth, partitionChipFor(id), rows);
                continue;
            }
            if (node.getLevel() == JIPipeLogLevel.DEBUG && !visibleCategories.contains(RecordCategory.DEBUG)) {
                // R27: DEBUG-level nodes hide with their whole subtree while the debug toggle is
                // off (graph-run debug content: GC nodes and their records)
                continue;
            }
            appendNodeRow(s, childMap, node, depth, inheritedChip, rows);
        }
    }

    /**
     * The phase tag of a node row: duration, {@code running Ns}, {@code failed}, or {@code cancelled}.
     * A finished row with records always shows its duration - also {@code 0.0s} when all records
     * share one millisecond (an empty tag reads as "no duration measured", which was the
     * inconsistency the user flagged on same-millisecond rows like the Storage rows, R37).
     */
    private static String phaseText(JIPipeProgressNode node, long firstTs, long lastTs) {
        return switch (node.getState()) {
            case FAILED -> "failed";
            case CANCELLED -> "cancelled";
            case RUNNING -> firstTs > 0 ? "running " + Math.max(1, (System.currentTimeMillis() - firstTs) / 1000) + "s" : "running";
            case OK -> firstTs > 0 ? formatDuration(Math.max(0, lastTs - firstTs)) : "";
        };
    }

    /**
     * Counts the descendant NODES of a node that are visible under the current filters (the R29
     * badge number). Squashed partition nodes contribute no row and are not counted, but their
     * children are; DEBUG-level nodes count as hidden - together with their whole subtree - while
     * the debug toggle is off (R27). Walks the child map built in this cycle.
     */
    private int countVisibleDescendantNodes(JIPipeProgressLog s, Map<Integer, List<Integer>> childMap, int nodeId) {
        int total = 0;
        Deque<Integer> stack = new ArrayDeque<>(childMap.getOrDefault(nodeId, List.of()));
        while (!stack.isEmpty()) {
            int id = stack.pop();
            JIPipeProgressNode node = s.getNode(id);
            if (node == null) {
                continue;
            }
            if (node.getKind() == JIPipeProgressNodeKind.PARTITION) {
                // Squashed: not a row - but its children are rows, so recurse without counting it
                List<Integer> partitionChildren = childMap.get(id);
                if (partitionChildren != null) {
                    for (int child : partitionChildren) {
                        stack.push(child);
                    }
                }
                continue;
            }
            if (node.getLevel() == JIPipeLogLevel.DEBUG && !visibleCategories.contains(RecordCategory.DEBUG)) {
                // Hidden with its whole subtree (R27)
                continue;
            }
            total++;
            List<Integer> children = childMap.get(id);
            if (children != null) {
                for (int child : children) {
                    stack.push(child);
                }
            }
        }
        return total;
    }

    /**
     * The node's own record count under the current category filter (the R29 triangle check:
     * visible records &gt; 0 makes a records-only node expandable).
     */
    private long visibleRecordCount(NodeUiState state) {
        EnumSet<RecordCategory> visible = visibleCategories;
        long total = 0;
        if (visible.contains(RecordCategory.DEBUG)) {
            total += state.debugCount.get();
        }
        if (visible.contains(RecordCategory.INFO)) {
            total += state.infoCount.get();
        }
        if (visible.contains(RecordCategory.WARNING)) {
            total += state.warnCount.get();
        }
        if (visible.contains(RecordCategory.ERROR)) {
            total += state.errorCount.get();
        }
        return total;
    }

    private String partitionChipFor(int partitionNodeId) {
        return partitionChips.computeIfAbsent(partitionNodeId, k -> "P" + partitionChips.size());
    }

    /**
     * Appends the detail rows of an expanded node: its records (last page; run-level notification
     * records render where they fall), and the "show older" action.
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
            boolean isNotification = record instanceof JIPipeProgressRecord.Notification;
            if (!isLog && !isError && !isNotification) {
                // Progress (p), node (n), and node-finished (N) records render nothing
                continue;
            }
            boolean hasStack = isError && ((JIPipeProgressRecord.Error) record).st() != null
                    && !((JIPipeProgressRecord.Error) record).st().isBlank();
            boolean stackOpen = hasStack && openStacks.contains(errorKey(nodeId, (JIPipeProgressRecord.Error) record));
            rows.add(new JIPipeDesktopProgressTreeRowRenderer.DetailRow(nodeId, record, hasStack, stackOpen, -1));
            if (stackOpen) {
                for (String line : ((JIPipeProgressRecord.Error) record).st().split("\n")) {
                    rows.add(new JIPipeDesktopProgressTreeRowRenderer.StackRow(nodeId, line));
                }
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
            // Remove per id (not clear()) - a batch concurrently adding ids must not lose them
            pendingNodeIds.remove(nodeId);
            if (state == null || !state.expanded || state.loading) {
                continue;
            }
            if (state.virtual != null) {
                // Virtual mode: the block extent follows the node's line range on the next
                // rebuild; only the paged mode re-reads pages
                continue;
            }
            loadDetailPage(s, nodeId, false);
        }
    }

    /**
     * Reads a page of a node's records from the store (off the EDT - checkpointed seek + bounded
     * parse) and applies it on the EDT.
     * <ul>
     * <li>Default (refresh/expand): the node's last page (up to {@value #DETAIL_PAGE_SIZE} records),
     * extended upwards by any older pages the user loaded before.</li>
     * <li>{@code older}: the page before the currently loaded history, prepended to it
     * ("Show N older records from run data…" - the shown records accumulate).</li>
     * </ul>
     *
     * @param nodeId the node
     * @param older  true to load the page before the currently shown history
     */
    private void loadDetailPage(JIPipeProgressLog s, int nodeId, boolean older) {
        NodeUiState state = nodeStates.get(nodeId);
        if (state == null || state.virtual != null) {
            // Virtual nodes render from the chunk cache, not from pages
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
                    if (mergeOlder) {
                        // The window before the loaded history (exactly one page wide)
                        from = Math.max(firstLine, fromHint);
                        List<JIPipeProgressRecord> raw = s.readRecords(from, Math.max(1, prevFrom - from));
                        page = filterPage(raw, nodeId);
                        hasOlder = from > firstLine;
                    } else {
                        int tailStart = Math.max(firstLine, lastLine - DETAIL_PAGE_SIZE + 1);
                        // Keep the history pages the user already loaded
                        from = state.loaded ? Math.min(state.pageFromLine, tailStart) : tailStart;
                        List<JIPipeProgressRecord> raw = s.readRecords(from, Math.max(1, lastLine - from + 1));
                        page = filterPage(raw, nodeId);
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

    /**
     * Filters one read window down to the detail rows of a node: renderable records of the node
     * itself that pass the category filter (R22), plus run-level notification records ({@code w},
     * warning category) that fall into the window - they render inside the node whose range covers
     * them. Progress/node/node-finished records render nothing and are dropped here.
     */
    private List<JIPipeProgressRecord> filterPage(List<JIPipeProgressRecord> raw, int nodeId) {
        EnumSet<RecordCategory> visible = visibleCategories;
        List<JIPipeProgressRecord> filtered = new ArrayList<>();
        for (JIPipeProgressRecord record : raw) {
            RecordCategory category = categoryOf(record);
            if (category == null || !visible.contains(category)) {
                continue;
            }
            int id = recordNodeId(record);
            if (id != nodeId && !(record instanceof JIPipeProgressRecord.Notification)) {
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
                // An older-page merge happened since this read was scheduled - it stays visible;
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

    /**
     * Applies a category toggle (R22): updates the visible set, re-reads the detail pages of the
     * expanded nodes (the filter changes what they show), and refreshes the row list (the debug
     * chips depend on the visibility).
     *
     * @param category the category
     * @param visible  whether the category's records are shown
     */
    private void toggleCategory(RecordCategory category, boolean visible) {
        EnumSet<RecordCategory> next = EnumSet.copyOf(visibleCategories);
        if (visible) {
            next.add(category);
        } else {
            next.remove(category);
        }
        visibleCategories = next;
        JIPipeProgressLog s = store;
        if (s != null) {
            for (NodeUiState state : nodeStates.values()) {
                if (!state.expanded) {
                    continue;
                }
                // Mode re-evaluation (R30): a node whose own records are now (or no longer)
                // hidden by the toggles switches between the virtual and the paged detail mode
                if (state.virtual != null) {
                    if (ownHiddenRecordCount(state) > 0) {
                        // Demote to paged
                        state.virtual = null;
                        clearOpenStacks(state.nodeId);
                        state.loaded = false;
                        state.loading = false;
                        loadDetailPage(s, state.nodeId, false);
                    }
                } else if (!state.loading && state.loaded) {
                    if (ownHiddenRecordCount(state) == 0) {
                        // Promote to virtual; a previous chunk cache is still valid (it holds
                        // unfiltered records - the category filter applies per rendered row)
                        state.loaded = false;
                        state.pageFromLine = 0;
                        state.detailPage = List.of();
                        state.hasOlder = false;
                        clearOpenStacks(state.nodeId);
                        state.virtual = new VirtualDetailState();
                    } else {
                        // Stay paged: re-read so the new filter applies
                        loadDetailPage(s, state.nodeId, false);
                    }
                }
            }
        }
        refreshNow();
    }

    // -------------------------------------------------------------------------
    // Virtual detail mode (R30)
    // -------------------------------------------------------------------------

    /**
     * The node's own record count that the current category toggles hide (Log records per level
     * plus Error records - all ERROR-category). Notifications are not attributed to nodes and
     * are filtered per rendered row instead.
     */
    private long ownHiddenRecordCount(NodeUiState state) {
        EnumSet<RecordCategory> visible = visibleCategories;
        long hidden = 0;
        if (!visible.contains(RecordCategory.DEBUG)) {
            hidden += state.debugCount.get();
        }
        if (!visible.contains(RecordCategory.INFO)) {
            hidden += state.infoCount.get();
        }
        if (!visible.contains(RecordCategory.WARNING)) {
            hidden += state.warnCount.get();
        }
        if (!visible.contains(RecordCategory.ERROR)) {
            hidden += state.errorCount.get();
        }
        return hidden;
    }

    /**
     * The node's renderable record count: the sum of its per-category counters (the exact number
     * of own {@code l}/{@code e} records - the same source the R29 visibility checks use). This
     * is the virtual detail's slot count (R36).
     */
    private static int renderableRecordCount(NodeUiState state) {
        long total = state.debugCount.get() + state.infoCount.get()
                + state.warnCount.get() + state.errorCount.get();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /**
     * Builds the virtual block of an expanded unfiltered node: one row per renderable record
     * (detail slot; R36) plus its open stack anchors. Structural records (child {@code n}/{@code
     * N}, own {@code p}) and other nodes' records occupy no slots and never render. Returns
     * {@code null} when the node has nothing to show (an empty block is not added).
     */
    private VirtualBlock buildVirtualBlock(int nodeId, int rowsBefore, NodeUiState state) {
        if (renderableRecordCount(state) == 0 && state.virtual.stackAnchors.isEmpty()) {
            return null;
        }
        return new VirtualBlock(nodeId, rowsBefore, renderableRecordCount(state), state.virtual.stackAnchors);
    }

    /**
     * The current viewport's slot span within the block ({@code {minSlot, maxSlot}}), or
     * {@code null} when the block is not visible (or the layout cannot measure it yet). Stack
     * rows carry no slots and are skipped.
     */
    private long[] visibleSlotSpan(VirtualBlock block) {
        int first = list.getFirstVisibleIndex();
        int last = list.getLastVisibleIndex();
        if (first < 0 || last < 0 || block.startIndex + block.extent <= first || block.startIndex > last) {
            return null;
        }
        int j0 = Math.max(0, first - block.startIndex);
        int j1 = Math.min(block.extent - 1, last - block.startIndex);
        long slot0 = Long.MAX_VALUE;
        long slot1 = Long.MIN_VALUE;
        for (int j = j0; j <= j1; j++) {
            int slot = block.flatOfRow(j);
            if (slot >= 0) {
                slot0 = Math.min(slot0, slot);
                slot1 = Math.max(slot1, slot);
            }
        }
        return slot1 < slot0 ? null : new long[]{slot0, slot1};
    }

    /**
     * Prunes the node's chunk and boundary caches to the currently visible viewport's keep
     * window. Called on the EDT apply path: a queued read can land long after its scheduling
     * event (a fast fling across a huge block queues a read per traversed chunk), so the bound
     * is re-enforced when each read lands. A block that is currently not visible keeps nothing -
     * its chunks are re-read on the next viewport change.
     */
    private void pruneChunkCache(int nodeId, VirtualDetailState virtual) {
        VirtualBlock block = blockOfNode(nodeId);
        long[] span = block != null ? visibleSlotSpan(block) : null;
        if (span == null) {
            virtual.chunks.clear();
            virtual.boundaries.clear();
            return;
        }
        long keepLo = span[0] - (long) VIRTUAL_CACHED_CHUNKS * VIRTUAL_CHUNK_SLOTS;
        long keepHi = span[1] + (long) VIRTUAL_CACHED_CHUNKS * VIRTUAL_CHUNK_SLOTS;
        virtual.chunks.keySet().removeIf(base -> base + VIRTUAL_CHUNK_SLOTS - 1 < keepLo || base > keepHi);
        virtual.boundaries.keySet().removeIf(base -> base + VIRTUAL_CHUNK_SLOTS - 1 < keepLo || base > keepHi);
    }

    /**
     * Ensures the chunks around the viewport of every visible virtual block are loaded (R30/R36)
     * and prunes the caches outside the viewport window (≈13 chunks per expanded node - see the
     * {@code VIRTUAL_*} constants).
     */
    private void scheduleVirtualChunkLoads(JIPipeProgressLog s) {
        List<VirtualBlock> blocks = listModel.blocks();
        if (blocks.isEmpty()) {
            return;
        }
        int gen = generation.get();
        for (VirtualBlock block : blocks) {
            NodeUiState state = nodeStates.get(block.nodeId);
            VirtualDetailState virtual = state != null ? state.virtual : null;
            if (virtual == null) {
                continue;
            }
            long[] visibleSpan = visibleSlotSpan(block);
            if (visibleSpan == null) {
                continue;
            }
            JIPipeProgressNode node = s.getNode(block.nodeId);
            if (node == null || node.getFirstLine() <= 0) {
                continue;
            }
            long slot0 = visibleSpan[0];
            long slot1 = visibleSpan[1];
            long keepLo = slot0 - (long) VIRTUAL_CACHED_CHUNKS * VIRTUAL_CHUNK_SLOTS;
            long keepHi = slot1 + (long) VIRTUAL_CACHED_CHUNKS * VIRTUAL_CHUNK_SLOTS;
            long loadLo = Math.max(0, slot0 - (long) VIRTUAL_PREFETCH_CHUNKS * VIRTUAL_CHUNK_SLOTS);
            long loadHi = Math.min(block.slotCount - 1L, slot1 + (long) VIRTUAL_PREFETCH_CHUNKS * VIRTUAL_CHUNK_SLOTS);
            for (long base = loadLo - loadLo % VIRTUAL_CHUNK_SLOTS; base <= loadHi; base += VIRTUAL_CHUNK_SLOTS) {
                int key = (int) base;
                // A cached chunk may be STALE: its span was computed from the extent when it
                // loaded, and the last chunk's span grows with the tail (slotCount - base).
                // Serve-from-cache only when the chunk covers the slots the current extent
                // expects; a stale chunk re-loads like a missing one (its size is always the
                // complete span of an earlier extent, so the re-read converges).
                List<JIPipeProgressRecord> cached = virtual.chunks.get(key);
                int expected = (int) Math.min(VIRTUAL_CHUNK_SLOTS, block.slotCount - base);
                if ((cached == null || cached.size() < expected) && virtual.loadingChunks.add(key)) {
                    int span = (int) Math.min(VIRTUAL_CHUNK_SLOTS, block.slotCount - base);
                    // Chain the scan from the nearest cached slot-to-line boundary at or before
                    // the chunk; the implied slot 0 starts at the node's first record line
                    int chainSlot = 0;
                    int chainLine = node.getFirstLine();
                    for (Map.Entry<Integer, Integer> boundary : virtual.boundaries.entrySet()) {
                        if (boundary.getKey() <= key && boundary.getKey() > chainSlot) {
                            chainSlot = boundary.getKey();
                            chainLine = boundary.getValue();
                        }
                    }
                    submitChunkRead(s, gen, block.nodeId, key, span, chainSlot, chainLine);
                }
            }
            virtual.chunks.keySet().removeIf(base -> base + VIRTUAL_CHUNK_SLOTS - 1 < keepLo || base > keepHi);
            virtual.boundaries.keySet().removeIf(base -> base + VIRTUAL_CHUNK_SLOTS - 1 < keepLo || base > keepHi);
        }
    }

    /**
     * Queues ONE scheduler pass after a chunk apply (R37): the fresh chunk may have crossed
     * chunk boundaries whose discovery makes a successor chunk schedulable that the previous
     * pass could not fill (and in a finished run no tick-triggering event may follow). The
     * pass runs on the EDT after the apply settles; concurrent applies coalesce into one pass.
     */
    private void scheduleVirtualChunkLoadsAfterApply() {
        if (chunkApplyPassQueued) {
            return;
        }
        chunkApplyPassQueued = true;
        SwingUtilities.invokeLater(() -> {
            chunkApplyPassQueued = false;
            JIPipeProgressLog s = store;
            if (s != null) {
                scheduleVirtualChunkLoads(s);
            }
        });
    }

    /**
     * Whether the record is a renderable record of the node (own {@code l}/{@code e}; R36 slots).
     * Run-level notifications ({@code w}) are root-scoped and never belong to a node's slots -
     * post-V3-1 they land in the phase rows' ranges.
     */
    private static boolean isOwnRenderable(JIPipeProgressRecord record, int nodeId) {
        if (record instanceof JIPipeProgressRecord.Log logRecord) {
            return logRecord.id() == nodeId;
        }
        return record instanceof JIPipeProgressRecord.Error errorRecord && errorRecord.id() == nodeId;
    }

    /**
     * Forward slot scan (R36): reads the node's record lines from the chained boundary, counting
     * own renderable records until the chunk's slots are collected. Structural records and other
     * nodes' records are skipped, never cached, never rendered. Bounded by the node's record
     * range; every crossed chunk boundary is discovered so later loads chain closer.
     */
    private List<JIPipeProgressRecord> scanForwardChunks(JIPipeProgressLog s, JIPipeProgressNode node, int nodeId,
                                                         int slotBase, int span, int chainSlot, int chainLine,
                                                         Map<Integer, Integer> discovered) {
        List<JIPipeProgressRecord> slice = new ArrayList<>(span);
        int slot = chainSlot;
        int line = chainLine;
        int lastLine = node.getLastLine();
        while (slice.size() < span && line <= lastLine) {
            List<JIPipeProgressRecord> page = s.readRecords(line, 512);
            if (page.isEmpty()) {
                break;
            }
            for (int i = 0; i < page.size() && slice.size() < span; i++) {
                JIPipeProgressRecord record = page.get(i);
                if (!isOwnRenderable(record, nodeId)) {
                    continue;
                }
                if (slot % VIRTUAL_CHUNK_SLOTS == 0) {
                    discovered.put(slot, line + i);
                }
                if (slot >= slotBase) {
                    slice.add(record);
                }
                slot++;
            }
            line += page.size();
        }
        return slice.size() == span ? slice : null;
    }

    /**
     * Tail window scan (R36): reads the node's last line window with a growing margin, collects
     * own renderable records, and keeps the last {@code span} of the requested range. Used for
     * the newest slots, where a forward scan would have to traverse the whole node; a short
     * collect (the writer still lags the counters) is retried with a doubled margin, bounded by
     * the node's record range.
     * <p>
     * The window ends at the panel's own accumulated line (the counter-consistent prefix), not
     * at the drain-time node index: the index can be ahead of the counters, and anchoring at its
     * end would label not-yet-counted records with the top slots - producing a chunk that fills
     * exactly, passes the stale check, and keeps shifted content until viewport pruning (fix
     * round 1, finding 1).
     */
    private List<JIPipeProgressRecord> scanTailWindow(JIPipeProgressLog s, JIPipeProgressNode node, int nodeId,
                                                      int slotBase, int span, int extent,
                                                      Map<Integer, Integer> discovered) {
        int needed = extent - slotBase;
        if (needed <= 0) {
            // The extent moved past this chunk between schedule and read: nothing to fill
            return null;
        }
        int firstLine = node.getFirstLine();
        int countedLast = (int) Math.min(node.getLastLine(), lastKnownLine);
        if (countedLast < firstLine) {
            // No counted record of the node yet - retry on a later scheduling event
            return null;
        }
        int rangeLines = countedLast - firstLine + 1;
        int margin = Math.min(rangeLines, Math.max(1024, needed * 4));
        List<JIPipeProgressRecord> collected = new ArrayList<>();
        List<Integer> lines = new ArrayList<>();
        while (true) {
            int from = Math.max(firstLine, countedLast - margin + 1);
            collected.clear();
            lines.clear();
            List<JIPipeProgressRecord> window = s.readRecords(from, countedLast - from + 1);
            for (int i = 0; i < window.size(); i++) {
                JIPipeProgressRecord record = window.get(i);
                if (isOwnRenderable(record, nodeId)) {
                    collected.add(record);
                    lines.add(from + i);
                }
            }
            if (collected.size() >= needed || from <= firstLine) {
                break;
            }
            margin = Math.min(rangeLines, margin * 2);
        }
        if (collected.size() < needed) {
            return null;
        }
        int offset = collected.size() - needed;
        List<JIPipeProgressRecord> slice = new ArrayList<>(span);
        for (int j = 0; j < span; j++) {
            int index = offset + j;
            slice.add(collected.get(index));
            int slot = extent - collected.size() + index;
            if (slot % VIRTUAL_CHUNK_SLOTS == 0) {
                discovered.put(slot, lines.get(index));
            }
        }
        return slice;
    }

    /**
     * Loads one detail chunk off the EDT (R36 slot space) and applies it on the EDT in O(chunk).
     * The newest slots load from the node's last line window; everything else loads by a forward
     * scan chained from the nearest cached slot-to-line boundary. The chunk applies only when it
     * filled completely: a short fill means the buffered writer still lags the counters at the
     * live tail, and caching it would strand the remaining slots as permanent placeholders - it
     * stays uncached and retries on the next scheduling event (bounded by the event rate). The
     * keep-window bound is re-enforced when the read lands: a fling can have moved the viewport
     * far away while the read was in flight.
     */
    private void submitChunkRead(JIPipeProgressLog s, int gen, int nodeId, int slotBase, int span, int chainSlot, int chainLine) {
        backgroundExecutor.submit(() -> {
            List<JIPipeProgressRecord> slice = null;
            Map<Integer, Integer> discovered = new HashMap<>();
            try {
                if (gen != generation.get() || store != s) {
                    return;
                }
                JIPipeProgressNode node = s.getNode(nodeId);
                NodeUiState readState = nodeStates.get(nodeId);
                if (node != null && node.getFirstLine() > 0 && node.getLastLine() >= node.getFirstLine() && readState != null) {
                    int extent = renderableRecordCount(readState);
                    if (extent - slotBase <= VIRTUAL_TAIL_WINDOW_SLOTS) {
                        slice = scanTailWindow(s, node, nodeId, slotBase, span, extent, discovered);
                    } else {
                        slice = scanForwardChunks(s, node, nodeId, slotBase, span, chainSlot, chainLine, discovered);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to read progress records of node {} at slot {}", nodeId, slotBase, e);
                slice = null;
            }
            List<JIPipeProgressRecord> read = slice;
            Map<Integer, Integer> foundBoundaries = discovered;
            SwingUtilities.invokeLater(() -> {
                NodeUiState state = nodeStates.get(nodeId);
                VirtualDetailState virtual = state != null ? state.virtual : null;
                if (virtual != null) {
                    // The in-flight marker is ALWAYS cleaned while the state exists (also when
                    // the apply is stale): a key left behind would make every future scheduler
                    // pass skip this chunk - placeholders forever (R37)
                    virtual.loadingChunks.remove(slotBase);
                    if (read != null && read.size() == span && gen == generation.get()) {
                        virtual.chunks.put(slotBase, read);
                        virtual.boundaries.putAll(foundBoundaries);
                        pruneChunkCache(nodeId, virtual);
                    }
                }
                list.repaint();
                scheduleVirtualChunkLoadsAfterApply();
            });
        });
    }

    /**
     * Resolves a block-local row to its content: a stack-trace line of an open anchor, the
     * detail record of its slot from the chunk cache, or a placeholder for an unloaded chunk.
     */
    private JIPipeDesktopProgressTreeRowRenderer.Row virtualRowOf(VirtualBlock block, int row) {
        NodeUiState state = nodeStates.get(block.nodeId);
        VirtualDetailState virtual = state != null ? state.virtual : null;
        if (virtual == null) {
            // The block is stale (the mode changed since this rebuild) - replaced on the next tick
            return new JIPipeDesktopProgressTreeRowRenderer.PlaceholderRow(block.nodeId);
        }
        // Open stack traces insert their lines after their error row (anchored by slot)
        int adjusted = row;
        for (int i = 0; i < block.anchors.size(); i++) {
            int anchorRow = block.anchorRows[i];
            if (adjusted <= anchorRow) {
                break;
            }
            if (adjusted <= anchorRow + block.anchorLengths[i]) {
                return new JIPipeDesktopProgressTreeRowRenderer.StackRow(block.nodeId,
                        block.anchors.get(i).lines()[adjusted - anchorRow - 1]);
            }
            adjusted -= block.anchorLengths[i];
        }
        int slot = adjusted;
        int chunkBase = slot - slot % VIRTUAL_CHUNK_SLOTS;
        List<JIPipeProgressRecord> chunk = virtual.chunks.get(chunkBase);
        if (chunk == null || slot - chunkBase >= chunk.size()) {
            // Unloaded chunk, or the file has not caught up with the counters yet
            return new JIPipeDesktopProgressTreeRowRenderer.PlaceholderRow(block.nodeId);
        }
        JIPipeProgressRecord record = chunk.get(slot - chunkBase);
        if (record instanceof JIPipeProgressRecord.Error errorRecord) {
            boolean hasStack = errorRecord.st() != null && !errorRecord.st().isBlank();
            boolean stackOpen = hasStack && openStacks.contains(errorKey(block.nodeId, errorRecord));
            return new JIPipeDesktopProgressTreeRowRenderer.DetailRow(block.nodeId, record, hasStack, stackOpen, slot);
        }
        // Own log record (the chunk holds only the node's own renderable records; virtual mode
        // implies all of them pass the toggles)
        return new JIPipeDesktopProgressTreeRowRenderer.DetailRow(block.nodeId, record, false, false, slot);
    }

    /**
     * Applies the pending scroll-to-detail-end request and, while the detail autoscroll is
     * engaged, keeps the followed node's detail tail visible as it grows.
     */
    private void followDetailTail() {
        if (scrollToDetailEndNodeId > 0) {
            int nodeId = scrollToDetailEndNodeId;
            NodeUiState state = nodeStates.get(nodeId);
            if (state == null || !state.expanded) {
                scrollToDetailEndNodeId = 0;
            } else if (state.virtual != null) {
                VirtualBlock block = blockOfNode(nodeId);
                if (block != null) {
                    scrollToDetailEndNodeId = 0;
                    ensureVisibleRow(block.startIndex + block.extent - 1);
                }
            } else if (state.loaded && !state.loading) {
                // Paged go-to-end: land on the LAST row of the detail block (the newest record -
                // the page's rows have replaced the old ones here). While a page load is in
                // flight the request stays pending; applyDetailPage's refresh re-issues it.
                scrollToDetailEndNodeId = 0;
                ensureVisibleRow(lastDetailRowIndex(nodeId));
            }
            // else: a paged page load is in flight - the request is consumed once its rows apply
        }
        if (!detailAutoscroll || autoscrollNodeId <= 0) {
            return;
        }
        NodeUiState state = nodeStates.get(autoscrollNodeId);
        if (state == null || !state.expanded || state.virtual == null) {
            detailAutoscroll = false;
            autoscrollNodeId = 0;
            detailAutoscrollButton.setSelected(false);
            return;
        }
        VirtualBlock block = blockOfNode(autoscrollNodeId);
        if (block != null && block.extent > 0) {
            ensureVisibleRow(block.startIndex + block.extent - 1);
        }
    }

    /**
     * The row index of the last row of a node's detail block: the rows between the node's row
     * and the next node row (its first visible child, or the next sibling - whichever follows).
     * Returns -1 when the node's row is not in the current model (an ancestor is collapsed).
     */
    private int lastDetailRowIndex(int nodeId) {
        Integer nodeIndex = rowPositions.get(nodeId);
        if (nodeIndex == null) {
            return -1;
        }
        int next = listModel.getSize();
        for (int other : rowPositions.values()) {
            if (other > nodeIndex && other < next) {
                next = other;
            }
        }
        return next - 1;
    }

    /**
     * Engages the detail tail follow for the node (the expansion and go-to-end paths): the next
     * ticks scroll to its detail end and keep it visible while records arrive.
     */
    private void engageDetailAutoscroll(int nodeId) {
        detailAutoscroll = true;
        autoscrollNodeId = nodeId;
        scrollToDetailEndNodeId = nodeId;
        detailAutoscrollButton.setSelected(true);
        refreshNow();
    }

    /**
     * The node whose detail the scroll controls act on: the block at the viewport's center, else
     * the most recently expanded node, else any expanded node.
     */
    private int currentDetailNodeId() {
        int center = (list.getFirstVisibleIndex() + list.getLastVisibleIndex()) / 2;
        if (center >= 0) {
            VirtualBlock block = listModel.blockAt(center);
            if (block != null) {
                return block.nodeId;
            }
        }
        NodeUiState state = nodeStates.get(lastExpandedNodeId);
        if (state != null && state.expanded) {
            return lastExpandedNodeId;
        }
        for (int id : expandedNodeIds) {
            return id;
        }
        return -1;
    }

    /**
     * The go-to-end action: virtual nodes jump to their detail tail (re-engaging the follow);
     * paged nodes drop any loaded older history and show the newest page.
     */
    private void goToNewestRecords() {
        int nodeId = currentDetailNodeId();
        if (nodeId <= 0) {
            return;
        }
        NodeUiState state = nodeStates.get(nodeId);
        JIPipeProgressLog s = store;
        if (state == null || !state.expanded || s == null) {
            return;
        }
        if (state.virtual != null) {
            engageDetailAutoscroll(nodeId);
        } else {
            state.pageFromLine = 0;
            if (!state.loading) {
                loadDetailPage(s, nodeId, false);
            }
            scrollToDetailEndNodeId = nodeId;
            refreshNow();
        }
    }

    private void updateDetailControls() {
        boolean anyExpanded = store != null && !expandedNodeIds.isEmpty();
        detailAutoscrollButton.setEnabled(anyExpanded);
        detailGoToEndButton.setEnabled(anyExpanded);
        if (detailAutoscrollButton.isSelected() != detailAutoscroll) {
            detailAutoscrollButton.setSelected(detailAutoscroll);
        }
    }

    private VirtualBlock blockOfNode(int nodeId) {
        for (VirtualBlock block : listModel.blocks()) {
            if (block.nodeId == nodeId) {
                return block;
            }
        }
        return null;
    }

    private void ensureVisibleRow(int index) {
        if (index < 0 || index >= listModel.getSize()) {
            return;
        }
        scrollProgrammatic = true;
        try {
            list.ensureIndexIsVisible(index);
        } finally {
            scrollProgrammatic = false;
        }
    }

    // -------------------------------------------------------------------------
    // Footer, filter buttons, spinners, auto-scroll
    // -------------------------------------------------------------------------

    /**
     * EDT: updates the warning/error toggle buttons' labels with the live record counts (the
     * glanceable volumes; counts are accumulated off the EDT). The debug/info buttons carry plain
     * labels per the literal R22 design.
     */
    private void updateCategoryButtons() {
        warningToggleButton.setText("warning (" + formatCount(categoryCounts[RecordCategory.WARNING.ordinal()].get()) + ")");
        errorToggleButton.setText("error (" + formatCount(categoryCounts[RecordCategory.ERROR.ordinal()].get()) + ")");
    }

    private static String formatCount(long count) {
        return NumberFormat.getIntegerInstance().format(count);
    }

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
        footerStateLabel.setText("");
        if (running) {
            footerStateLabel.setIcon(footerSpinner);
        } else {
            // Finished states show a resource icon (16px, the same icons the rest of the run UI uses) -
            // no UTF-8 glyphs, which rendered inconsistently across fonts
            switch (rootState) {
                case OK -> footerStateLabel.setIcon(JIPipe.RESOURCES.getIcon16(ICON_STATUS_OK));
                case FAILED -> footerStateLabel.setIcon(JIPipe.RESOURCES.getIcon16(ICON_STATUS_ERROR));
                case CANCELLED -> footerStateLabel.setIcon(JIPipe.RESOURCES.getIcon16(ICON_STATUS_CANCELLED));
                default -> footerStateLabel.setIcon(null);
            }
        }

        String status;
        // R41: while the run is running, the footer prefers the most recent node with determinate
        // progress over the newest node - during an iteration the newest node is the freshly
        // resolved slice/item row (no counters), which would hide the "5/200 (2%)" narration.
        // Fix round 1 (F6): the preference excludes root and phase nodes - root machinery
        // progress (R38) narrates the run level, not the step the run is in, so during a
        // counterless node the newest node's label chain is the better narration
        JIPipeProgressNode footerNode = null;
        if (running) {
            int progressNodeId = footerProgressNodeId;
            JIPipeProgressNode progressNode = progressNodeId > 0 ? s.getNode(progressNodeId) : null;
            if (progressNode != null && progressNodeId != s.getRootNodeId()
                    && progressNode.getKind() != JIPipeProgressNodeKind.PHASE
                    && progressNode.getMaxProgress() > 0) {
                footerNode = progressNode;
            } else {
                footerNode = latestNodeId > 0 ? s.getNode(latestNodeId) : null;
            }
        }
        if (running && footerNode != null) {
            String chain = labelPath(s, footerNode);
            String progress = progressText(footerNode);
            // No chain for a root-level progress record (V5-2): use the progress text alone
            // instead of a leading separator (would render as " - 3/7 (43%)")
            status = chain + (progress.isEmpty() ? "" : (chain.isEmpty() ? "" : " - ") + progress);
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
                // The file may not exist yet (nothing flushed) - keep the current footer
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
        if (detailAutoscroll) {
            // The engaged detail tail follow owns the viewport while it runs (the two scrolls
            // would fight); it disengages on scroll-up, handing control back to this row follow
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
        if (state.expanded) {
            expandedNodeIds.add(nodeId);
            lastExpandedNodeId = nodeId;
        } else {
            expandedNodeIds.remove(nodeId);
            // Collapse frees the chunk cache (the virtual state is rebuilt on the next expansion)
            state.virtual = null;
            clearOpenStacks(nodeId);
            if (autoscrollNodeId == nodeId) {
                detailAutoscroll = false;
                autoscrollNodeId = 0;
                detailAutoscrollButton.setSelected(false);
            }
        }
        JIPipeProgressLog s = store;
        if (state.expanded && s != null) {
            if (ownHiddenRecordCount(state) == 0) {
                // R30 virtual mode: expansion opens at the tail with the follow engaged
                if (state.virtual == null) {
                    state.virtual = new VirtualDetailState();
                }
                engageDetailAutoscroll(nodeId);
            } else if (!state.loaded && !state.loading) {
                loadDetailPage(s, nodeId, false);
            }
        }
        refreshNow();
    }

    private void toggleStack(int nodeId, JIPipeProgressRecord.Error error, int slot) {
        String key = errorKey(nodeId, error);
        NodeUiState state = nodeStates.get(nodeId);
        if (!openStacks.remove(key)) {
            openStacks.add(key);
            if (state != null && state.virtual != null && slot >= 0 && error.st() != null) {
                // Virtual mode: the open stack inserts its lines after the error row - the block
                // snapshot carries the anchor (rebuilt by the refresh below)
                state.virtual.stackAnchors.add(new StackAnchor(slot, key, error.st().split("\n")));
            }
        } else if (state != null && state.virtual != null) {
            state.virtual.stackAnchors.removeIf(anchor -> anchor.key().equals(key));
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
        // Records carry no sequence number - the stable identity of an error across page re-reads
        // is its node, timestamp, and class (identical errors at the same millisecond merge)
        return nodeId + "@" + error.ts() + "@" + error.c();
    }

    /**
     * Drops the node's open-stack flags when its detail state is rebuilt (collapse, paged↔virtual
     * mode switch): a stale flag would render an error row as "stack open" with no stack rows
     * behind it until it is clicked twice.
     */
    private void clearOpenStacks(int nodeId) {
        openStacks.removeIf(key -> key.startsWith(nodeId + "@"));
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
        backgroundExecutor.submit(() -> {
            try {
                // Bounded render from the store's tail (a full render of a huge log must not be
                // copied into the clipboard); the compat text render keeps its INFO-minLevel
                // behavior regardless of the category filter (R22)
                List<JIPipeProgressRecord> recent = s.readRecent(COPY_LINE_LIMIT);
                StringBuilder builder = new StringBuilder();
                s.renderRecords(recent, JIPipeLogLevel.INFO, line -> builder.append(line).append('\n'));
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
        // Truncated like the mockup ("5/200 (2%)" for 2.5%), never rounded
        int percent = Math.max(0, Math.min(100, (int) (100.0 * node.getProgress() / node.getMaxProgress())));
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
     * detail pages) is only written on the EDT. The per-category counters feed the R29
     * visibility checks (expandability, visible records) and the debug chip.
     */
    private static final class NodeUiState {
        final int nodeId;
        final AtomicLong firstTs = new AtomicLong();
        final AtomicLong lastTs = new AtomicLong();
        final AtomicLong debugCount = new AtomicLong();
        final AtomicLong infoCount = new AtomicLong();
        final AtomicLong warnCount = new AtomicLong();
        final AtomicLong errorCount = new AtomicLong();
        volatile boolean expanded;
        volatile boolean loading;
        volatile boolean loaded;
        volatile boolean hasOlder;
        volatile int pageFromLine;
        volatile List<JIPipeProgressRecord> detailPage = List.of();
        /**
         * The virtual detail state (R30 unfiltered mode); {@code null} while the node is
         * collapsed or in the paged mode. Mutated on the EDT only.
         */
        volatile VirtualDetailState virtual;

        private NodeUiState(int nodeId) {
            this.nodeId = nodeId;
        }
    }

    /**
     * The virtual detail state of one expanded node (R30 unfiltered mode; created on expansion,
     * freed on collapse and on mode switches). The chunk cache is pruned to the viewport window
     * on every tick and on every chunk apply - the bound is about 13 chunks × 512 ≈ 6,700
     * records per node (see the {@code VIRTUAL_*} constants), independent of the node's record
     * count. Mutated on the EDT only.
     */
    private static final class VirtualDetailState {
        /** Loaded chunks keyed by their first detail slot. */
        final Map<Integer, List<JIPipeProgressRecord>> chunks = new HashMap<>();
        /**
         * Discovered slot-to-line boundaries keyed by chunk-base slots: the line that holds the
         * chunk's first record. Lets a chunk's forward scan chain from the nearest cached
         * boundary instead of rescanning from the node's first record.
         */
        final Map<Integer, Integer> boundaries = new HashMap<>();
        /** Chunk reads in flight (deduplication between viewport changes). */
        final Set<Integer> loadingChunks = new HashSet<>();
        /** Open stack traces, anchored at their error record's slot (drives the block mapping). */
        final List<StackAnchor> stackAnchors = new ArrayList<>();
    }

    /**
     * An opened stack trace of a virtual detail: its lines render as extra rows after the error
     * row; the anchor keeps them alive even when the error's chunk is pruned from the cache.
     */
    private record StackAnchor(int errorSlot, String key, String[] lines) {
    }

    /**
     * One virtual detail block in the row space (rebuilt per dirty tick with the rest of the row
     * snapshot, R21): the node's renderable records ({@code l}/{@code e}, R36) mapped flat and
     * contiguous as detail slots. Row {@code startIndex + j} resolves to a detail slot, or to a
     * line of an open stack trace (which inserts extra rows after its error row). Structural
     * records (child {@code n}/{@code N}, own {@code p}) and run-level notifications occupy no
     * slots and never render here. Chunk loads change row content, never indices: the extent is
     * stable while loading.
     */
    private final class VirtualBlock {
        final int nodeId;
        final int rowsBefore;
        final int slotCount;
        final List<StackAnchor> anchors;
        final int[] anchorSlots;
        final int[] anchorRows;
        final int[] anchorLengths;
        final int extent;
        int startIndex;

        VirtualBlock(int nodeId, int rowsBefore, int slotCount, List<StackAnchor> stackAnchors) {
            this.nodeId = nodeId;
            this.rowsBefore = rowsBefore;
            this.slotCount = slotCount;
            // Open stacks sorted by their error slot; each anchor's row is its error slot
            // shifted by the lengths of the anchors before it. Anchors outside the current slot
            // range (a stale remnant) cannot be mapped and are dropped.
            List<StackAnchor> sorted = new ArrayList<>(stackAnchors);
            sorted.removeIf(anchor -> anchor.errorSlot() < 0 || anchor.errorSlot() >= slotCount);
            sorted.sort(Comparator.comparingInt(StackAnchor::errorSlot));
            this.anchors = sorted;
            this.anchorSlots = new int[sorted.size()];
            this.anchorRows = new int[sorted.size()];
            this.anchorLengths = new int[sorted.size()];
            int shift = 0;
            for (int i = 0; i < sorted.size(); i++) {
                anchorSlots[i] = sorted.get(i).errorSlot();
                anchorLengths[i] = sorted.get(i).lines().length;
                // anchorRows are the anchor positions in UNSHIFTED slot space: the row walk
                // (flatOfRow / virtualRowOf) strips the PREVIOUS anchors' stack rows from the
                // block-local row before comparing, so comparing against a fully-shifted
                // position would misplace every anchor after the first (fix round 1, finding 2)
                anchorRows[i] = anchorSlots[i];
                shift += anchorLengths[i];
            }
            this.extent = slotCount + shift;
        }

        /**
         * De-adjusts a block-local row into a detail slot, or -1 when the row is a line of an
         * open stack trace.
         */
        int flatOfRow(int row) {
            int adjusted = row;
            for (int i = 0; i < anchorRows.length; i++) {
                if (adjusted <= anchorRows[i]) {
                    break;
                }
                if (adjusted <= anchorRows[i] + anchorLengths[i]) {
                    return -1;
                }
                adjusted -= anchorLengths[i];
            }
            return adjusted;
        }
    }

    /**
     * The flat row list's model: the materialized row snapshot (rebuilt per dirty tick, R21)
     * plus the virtual detail blocks (R30). A block occupies a contiguous index range; its rows
     * are computed on demand from the chunk cache - the {@link JList} asks only for the visible
     * indices, so a node with millions of records costs O(viewport) per paint while the
     * scrollbar is proportional to the real content (the virtual scrollbar).
     */
    private class ProgressListModel extends AbstractListModel<JIPipeDesktopProgressTreeRowRenderer.Row> {
        private List<JIPipeDesktopProgressTreeRowRenderer.Row> materialized = List.of();
        private List<VirtualBlock> blocks = List.of();
        private int size;

        /**
         * Replaces the whole row space (the R21 full-row rebuild). Block start indices are
         * assigned here: a block starts at its materialized row position plus the extents of all
         * earlier blocks.
         */
        void reset(List<JIPipeDesktopProgressTreeRowRenderer.Row> materialized, List<VirtualBlock> blocks) {
            int oldSize = size;
            int shifted = 0;
            for (VirtualBlock block : blocks) {
                block.startIndex = block.rowsBefore + shifted;
                shifted += block.extent;
            }
            this.materialized = materialized;
            this.blocks = blocks;
            this.size = materialized.size() + shifted;
            if (oldSize > 0) {
                fireIntervalRemoved(this, 0, oldSize - 1);
            }
            if (size > 0) {
                fireIntervalAdded(this, 0, size - 1);
            }
        }

        List<VirtualBlock> blocks() {
            return blocks;
        }

        VirtualBlock blockAt(int index) {
            int lo = 0;
            int hi = blocks.size() - 1;
            int found = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (blocks.get(mid).startIndex <= index) {
                    found = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            if (found < 0) {
                return null;
            }
            VirtualBlock block = blocks.get(found);
            return index < block.startIndex + block.extent ? block : null;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public JIPipeDesktopProgressTreeRowRenderer.Row getElementAt(int index) {
            if (index < 0 || index >= size) {
                return new JIPipeDesktopProgressTreeRowRenderer.PlaceholderRow(-1);
            }
            VirtualBlock block = blockAt(index);
            if (block == null) {
                // A materialized row: the flat index includes the extents of the virtual blocks
                // emitted before it - subtract them (blocks are few; the walk is trivial)
                int shift = 0;
                for (VirtualBlock earlier : blocks) {
                    if (earlier.startIndex >= index) {
                        break;
                    }
                    shift += earlier.extent;
                }
                int materializedIndex = index - shift;
                // Clamped defensively: the Swing UI may query indices from a stale layout state
                // while the model is being reset
                return materializedIndex >= 0 && materializedIndex < materialized.size()
                        ? materialized.get(materializedIndex)
                        : new JIPipeDesktopProgressTreeRowRenderer.PlaceholderRow(-1);
            }
            return virtualRowOf(block, index - block.startIndex);
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
                            || row instanceof JIPipeDesktopProgressTreeRowRenderer.MoreRow;
                    setCursor(Cursor.getPredefinedCursor(interactive ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        private JIPipeDesktopProgressTreeRowRenderer.Row rowAtPoint(MouseEvent e) {
            int index = locationToIndex(e.getPoint());
            if (index < 0 || index >= listModel.getSize()) {
                return null;
            }
            Rectangle bounds = getCellBounds(index, index);
            if (bounds == null || !bounds.contains(e.getPoint())) {
                return null;
            }
            return listModel.getElementAt(index);
        }

        private void handleClick(JIPipeDesktopProgressTreeRowRenderer.Row row) {
            if (row instanceof JIPipeDesktopProgressTreeRowRenderer.NodeRow nodeRow) {
                if (nodeRow.isExpandable()) {
                    toggleNode(nodeRow.getNodeId());
                }
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.DetailRow detailRow) {
                if (detailRow.hasStack() && detailRow.record() instanceof JIPipeProgressRecord.Error error) {
                    toggleStack(detailRow.nodeId(), error, detailRow.slot());
                }
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.StackRow stackRow) {
                // Clicking anywhere in an opened stack block closes it
                NodeUiState state = nodeStates.get(stackRow.nodeId());
                if (state != null && state.virtual != null) {
                    // Virtual mode: find the open anchor that carries this line
                    StackAnchor matched = null;
                    for (StackAnchor anchor : state.virtual.stackAnchors) {
                        if (Arrays.asList(anchor.lines()).contains(stackRow.line())) {
                            matched = anchor;
                            break;
                        }
                    }
                    if (matched != null) {
                        openStacks.remove(matched.key());
                        state.virtual.stackAnchors.remove(matched);
                    }
                } else if (state != null) {
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
            }
        }

        /**
         * The rows fill the viewport width (no horizontal scrolling: long detail messages clip,
         * the tooltip carries the full text). This also keeps the Swing list's layout state
         * O(1): without a fixed cell width, the UI walks EVERY model index to find the widest
         * row - catastrophic with virtual detail extents of millions of rows.
         */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public void doLayout() {
            super.doLayout();
            Integer current = getFixedCellWidth();
            if (getWidth() > 0 && (current == null || current != getWidth())) {
                setFixedCellWidth(getWidth());
            }
        }

        @Override
        public void addNotify() {
            super.addNotify();
            Integer current = getFixedCellWidth();
            if (getWidth() > 0 && (current == null || current != getWidth())) {
                setFixedCellWidth(getWidth());
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
                            builder.append(" - ").append(progress);
                        }
                        builder.append(" [").append(node.getState()).append("]");
                        if (node.getLocation() != null) {
                            builder.append("\n").append(node.getLocation());
                        }
                    }
                }
                return builder.toString();
            } else if (row instanceof JIPipeDesktopProgressTreeRowRenderer.DetailRow detailRow) {
                if (detailRow.record() instanceof JIPipeProgressRecord.Log logRecord) {
                    return logRecord.m();
                } else if (detailRow.record() instanceof JIPipeProgressRecord.Error errorRecord) {
                    return errorRecord.c() + (errorRecord.m() == null || errorRecord.m().isEmpty() ? "" : ": " + errorRecord.m());
                } else if (detailRow.record() instanceof JIPipeProgressRecord.Notification notificationRecord) {
                    return notificationRecord.h() + (notificationRecord.d() == null || notificationRecord.d().isEmpty() ? "" : ": " + notificationRecord.d());
                }
            }
            return null;
        }
    }
}
