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

package org.hkijena.jipipe.api.progress;

import org.hkijena.jipipe.JIPipe;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for {@link JIPipeProgressLog} implementations. Owns all tree logic — the node index,
 * node id allocation, record sequence numbers, the bounded tail of renderable records, and the
 * coalescer plumbing — exactly once. Implementations differ only in the sink
 * ({@link #writeRecord(JIPipeProgressRecord)}) and in how records are handed to it
 * ({@link #enqueue(long, JIPipeProgressRecord)}).
 * <p>
 * Record flow: producer methods build a record and call {@link #submit(JIPipeProgressRecord)}, which
 * atomically assigns the next sequence number and hands the record to {@link #enqueue(long,
 * JIPipeProgressRecord)}. The sequence number of a record is its line number (1-based; head = 1), so
 * sequence assignment and hand-off must stay in submission order. The thread that owns the sink then
 * applies the record to the index and tail ({@link #applyRecord(long, JIPipeProgressRecord)}) and
 * writes it ({@link #writeRecord(JIPipeProgressRecord)}): synchronously on the caller thread for the
 * default implementation of {@link #enqueue(long, JIPipeProgressRecord)}, or on the writer thread for
 * file-backed stores.
 * <p>
 * The tail holds the renderable records ({@code l}, {@code p}, {@code e}, {@code w}) only — structural
 * records ({@code head}, {@code n}, {@code N}, {@code s}) render nothing and are excluded. It is bounded
 * by the record-count limit (default 8,192) or by ~8 MB of message text, whichever binds first;
 * eviction removes the oldest entries. Records with
 * a node id ({@code n}, {@code l}, {@code p}, {@code e}, {@code N}) are attributed to that node's range
 * in the index.
 */
public abstract class AbstractJIPipeProgressLog implements JIPipeProgressLog {

    /**
     * The id of the automatically created root node.
     */
    protected static final int ROOT_NODE_ID = 1;

    private static final int DEFAULT_TAIL_LIMIT = 8192;

    /**
     * Approximate upper bound for the message text retained by the tail, in characters (~8 MB;
     * one char counts as one byte — the bound is approximate by design). The tail is bounded by
     * {@link #DEFAULT_TAIL_LIMIT} or by this cap, whichever binds first.
     */
    private static final long TAIL_TEXT_LIMIT_CHARS = 8L * 1024 * 1024;

    private static final long DEFAULT_COALESCE_INTERVAL_MILLIS = 100;

    private final String runUuid;
    private final String runLabel;
    private final JIPipeProgressNode rootNode;
    private final ConcurrentMap<Integer, JIPipeProgressNode> nodes = new ConcurrentHashMap<>();
    private final AtomicLong nodeIdCounter = new AtomicLong(ROOT_NODE_ID);
    private final AtomicLong sequenceCounter = new AtomicLong();
    private final ArrayDeque<TailEntry> tail = new ArrayDeque<>();
    private final ReentrantLock tailLock = new ReentrantLock();
    private final ReentrantLock submitLock = new ReentrantLock();
    private final ProgressBatchEventEmitter batchEventEmitter = new ProgressBatchEventEmitter();
    private volatile int tailLimit = DEFAULT_TAIL_LIMIT;
    private volatile long coalesceIntervalMillis = DEFAULT_COALESCE_INTERVAL_MILLIS;
    private long tailChars;
    private volatile boolean started;
    private volatile boolean finished;
    private volatile boolean closed;

    /**
     * Creates the store and its automatically created root node.
     *
     * @param runUuid  the run UUID ({@code null} for stores that write no head record file)
     * @param runLabel the run label ({@code null} for stores that write no head record file)
     */
    protected AbstractJIPipeProgressLog(String runUuid, String runLabel) {
        this.runUuid = runUuid;
        this.runLabel = runLabel;
        this.rootNode = new JIPipeProgressNode(ROOT_NODE_ID, 0, runLabel, JIPipeProgressNodeKind.RUN);
        this.nodes.put(ROOT_NODE_ID, rootNode);
    }

    // ---------------------------------------------------------------------
    // Producer API
    // ---------------------------------------------------------------------

    @Override
    public final int getRootNodeId() {
        return ROOT_NODE_ID;
    }

    @Override
    public final int nextNodeId() {
        return (int) Math.min(nodeIdCounter.get() + 1, Integer.MAX_VALUE);
    }

    @Override
    public final JIPipeProgressNode getNode(int id) {
        if (id <= 0) {
            return null;
        }
        return nodes.get(id);
    }

    @Override
    public final int beginNode(int parentId, String label, JIPipeProgressNodeKind kind) {
        Objects.requireNonNull(kind, "Node kind must not be null");
        submitLock.lock();
        try {
            if (!startRunLocked()) {
                // The store is closed: return a distinct, unused id (records are dropped anyway)
                return (int) Math.min(nodeIdCounter.incrementAndGet(), Integer.MAX_VALUE);
            }
            JIPipeProgressNode parent = resolveParentLocked(parentId);
            int id = (int) nodeIdCounter.incrementAndGet();
            JIPipeProgressNode node = new JIPipeProgressNode(id, parent.getId(), label, kind);
            nodes.put(id, node);
            for (JIPipeProgressNode ancestor = parent; ancestor != null; ancestor = nodes.get(ancestor.getParentId())) {
                ancestor.incrementDescendantNodeCount();
            }
            // Children of the root reference it via p=0 (the root is implied by the head record)
            int wireParent = parent.getId() == ROOT_NODE_ID ? 0 : parent.getId();
            submitLocked(JIPipeProgressRecord.node(id, wireParent, label, kind.name(), now()));
            return id;
        } finally {
            submitLock.unlock();
        }
    }

    @Override
    public final void log(int nodeId, JIPipeLogLevel level, String message) {
        Objects.requireNonNull(level, "Log level must not be null");
        submitLock.lock();
        try {
            if (!startRunLocked()) {
                return;
            }
            submitLocked(JIPipeProgressRecord.log(nodeId, level.value(), message, now()));
        } finally {
            submitLock.unlock();
        }
    }

    @Override
    public final void progress(int nodeId, int value, int max) {
        submitLock.lock();
        try {
            if (!startRunLocked()) {
                return;
            }
            submitLocked(JIPipeProgressRecord.progress(nodeId, value, max, now()));
        } finally {
            submitLock.unlock();
        }
    }

    @Override
    public final void error(int nodeId, Throwable throwable) {
        Objects.requireNonNull(throwable, "Throwable must not be null");
        submitLock.lock();
        try {
            if (!startRunLocked()) {
                return;
            }
            submitLocked(JIPipeProgressRecord.error(nodeId,
                    throwable.getClass().getName(),
                    throwable.getMessage() == null ? "" : throwable.getMessage(),
                    renderStackTrace(throwable),
                    now()));
        } finally {
            submitLock.unlock();
        }
    }

    @Override
    public final void nodeFinished(int nodeId, JIPipeProgressNodeState state) {
        Objects.requireNonNull(state, "Node state must not be null");
        submitLock.lock();
        try {
            if (!startRunLocked()) {
                return;
            }
            submitLocked(JIPipeProgressRecord.nodeFinished(nodeId, mapNodeStateToWire(state), now()));
        } finally {
            submitLock.unlock();
        }
    }

    @Override
    public final void finish(JIPipeProgressNodeState runState) {
        Objects.requireNonNull(runState, "Run state must not be null");
        submitLock.lock();
        try {
            if (closed || finished) {
                return;
            }
            if (!started) {
                enqueueHeadLocked();
            }
            rootNode.setState(runState);
            long seq = sequenceCounter.incrementAndGet();
            enqueue(seq, JIPipeProgressRecord.status(mapRunStateToWire(runState), now()));
            finished = true;
        } finally {
            submitLock.unlock();
        }
        close();
    }

    @Override
    public final void close() {
        submitLock.lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
        } finally {
            submitLock.unlock();
        }
        onClose();
    }

    // ---------------------------------------------------------------------
    // Tree logic — applied by the thread that owns the sink
    // ---------------------------------------------------------------------

    /**
     * Applies a record to the node index and the tail. Called by the thread that owns the sink:
     * the writer thread for file-backed stores, the calling thread otherwise.
     *
     * @param seq    the record's sequence number (its line number)
     * @param record the record
     */
    protected final void applyRecord(long seq, JIPipeProgressRecord record) {
        if (record instanceof JIPipeProgressRecord.Node nodeRecord) {
            attribute(seq, nodes.get(nodeRecord.id()));
        } else if (record instanceof JIPipeProgressRecord.Log logRecord) {
            attribute(seq, nodes.get(logRecord.id()));
        } else if (record instanceof JIPipeProgressRecord.Progress progressRecord) {
            JIPipeProgressNode node = nodes.get(progressRecord.id());
            if (node != null) {
                node.setProgress(progressRecord.v(), progressRecord.mx());
                attribute(seq, node);
            }
        } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
            attribute(seq, nodes.get(errorRecord.id()));
        } else if (record instanceof JIPipeProgressRecord.NodeFinished finishRecord) {
            JIPipeProgressNode node = nodes.get(finishRecord.id());
            if (node != null) {
                node.setState(mapNodeStateFromWire(finishRecord.s()));
                attribute(seq, node);
            }
        }
        // Run-level records (head, s) and notifications (w) are not attributed to a node
        if (isRenderable(record)) {
            appendTail(seq, record);
        }
    }

    private static void attribute(long seq, JIPipeProgressNode node) {
        if (node != null) {
            node.recordAt(seq);
        }
    }

    private void appendTail(long seq, JIPipeProgressRecord record) {
        tailLock.lock();
        try {
            TailEntry entry = new TailEntry(seq, record, estimateTextSize(record));
            tail.addLast(entry);
            tailChars += entry.chars();
            evictTailLocked();
        } finally {
            tailLock.unlock();
        }
    }

    /**
     * Evicts entries from the head while the tail is over either bound: the record-count limit or
     * the message-text cap. Called while holding {@link #tailLock}.
     */
    private void evictTailLocked() {
        while (!tail.isEmpty() && (tail.size() > tailLimit || tailChars > TAIL_TEXT_LIMIT_CHARS)) {
            tailChars -= tail.pollFirst().chars();
        }
    }

    /**
     * Approximates the text size retained by one tail entry: the characters of its message text
     * (log message, stack trace, heading/description) plus the label of a progress record.
     *
     * @param record the record
     * @return the approximate retained character count
     */
    private static long estimateTextSize(JIPipeProgressRecord record) {
        if (record instanceof JIPipeProgressRecord.Log logRecord) {
            return length(logRecord.m());
        } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
            return length(errorRecord.c()) + length(errorRecord.m()) + length(errorRecord.st());
        } else if (record instanceof JIPipeProgressRecord.Notification notificationRecord) {
            return length(notificationRecord.h()) + length(notificationRecord.d());
        }
        return 0;
    }

    private static long length(String text) {
        return text == null ? 0 : text.length();
    }

    private static boolean isRenderable(JIPipeProgressRecord record) {
        return record instanceof JIPipeProgressRecord.Log
                || record instanceof JIPipeProgressRecord.Progress
                || record instanceof JIPipeProgressRecord.Error
                || record instanceof JIPipeProgressRecord.Notification;
    }

    // ---------------------------------------------------------------------
    // Read-back
    // ---------------------------------------------------------------------

    @Override
    public List<JIPipeProgressRecord> readRecords(int fromLine, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        long start = Math.max(fromLine, 1);
        List<JIPipeProgressRecord> result = new ArrayList<>();
        tailLock.lock();
        try {
            for (TailEntry entry : tail) {
                if (entry.seq() >= start) {
                    result.add(entry.record());
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
        } finally {
            tailLock.unlock();
        }
        return result;
    }

    @Override
    public List<JIPipeProgressRecord> readRecent(int count) {
        if (count <= 0) {
            return List.of();
        }
        tailLock.lock();
        try {
            int skip = Math.max(0, tail.size() - count);
            List<JIPipeProgressRecord> result = new ArrayList<>(tail.size() - skip);
            for (TailEntry entry : tail) {
                if (skip > 0) {
                    skip--;
                } else {
                    result.add(entry.record());
                }
            }
            return result;
        } finally {
            tailLock.unlock();
        }
    }

    // ---------------------------------------------------------------------
    // Settings and plumbing
    // ---------------------------------------------------------------------

    @Override
    public void setCoalesceIntervalMillis(long ms) {
        coalesceIntervalMillis = ms;
    }

    /**
     * Returns the coalesce interval in milliseconds (used by the coalescer).
     *
     * @return the interval in milliseconds
     */
    protected final long getCoalesceIntervalMillis() {
        return coalesceIntervalMillis;
    }

    @Override
    public void setTailLimit(int records) {
        int limit = Math.max(0, records);
        tailLimit = limit;
        tailLock.lock();
        try {
            evictTailLocked();
        } finally {
            tailLock.unlock();
        }
    }

    /**
     * Returns the current tail limit.
     *
     * @return the tail limit
     */
    protected final int getTailLimit() {
        return tailLimit;
    }

    @Override
    public ProgressBatchEventEmitter getBatchEventEmitter() {
        return batchEventEmitter;
    }

    /**
     * Returns the run UUID written into the head record.
     *
     * @return the run UUID (may be null)
     */
    protected final String getRunUuid() {
        return runUuid;
    }

    /**
     * Returns the run label written into the head record.
     *
     * @return the run label (may be null)
     */
    protected final String getRunLabel() {
        return runLabel;
    }

    /**
     * Returns whether the store is closed. Records submitted after closing are dropped.
     *
     * @return true if closed
     */
    protected final boolean isClosed() {
        return closed;
    }

    /**
     * Returns whether the run was finished ({@link #finish(JIPipeProgressNodeState)} was called).
     * The run-status record was the last accepted record.
     *
     * @return true if finished
     */
    protected final boolean isFinished() {
        return finished;
    }

    /**
     * Called once when the store is closed, after no further records will be submitted.
     * The default implementation does nothing.
     */
    protected void onClose() {
    }

    /**
     * Writes one record to the physical sink. Called by the thread that owns the sink, after the
     * record was applied to the index and tail.
     *
     * @param record the record
     */
    protected abstract void writeRecord(JIPipeProgressRecord record);

    /**
     * Hands one record to the sink. The default delivers synchronously on the calling thread;
     * implementations may queue the record for delivery on another thread. Called while holding the
     * submission lock, in submission order (the sequence number was already assigned).
     *
     * @param seq    the record's sequence number (its line number)
     * @param record the record
     */
    protected void enqueue(long seq, JIPipeProgressRecord record) {
        applyRecord(seq, record);
        writeRecord(record);
    }

    /**
     * Blocks while the sink's queue is full (backpressure). The default does nothing. Called while
     * holding the submission lock and before the sequence number is assigned; the sink's consumer
     * must never wait for this lock.
     */
    protected void awaitCapacity() {
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private JIPipeProgressRecord headRecord(long ts) {
        return JIPipeProgressRecord.head(1, runUuid, runLabel, JIPipe.getJIPipeVersion(), ts);
    }

    /**
     * Enqueues the head record. Must be called before the first record of a run so that the head is
     * line 1. Called while holding the submission lock.
     */
    private void enqueueHeadLocked() {
        started = true;
        long seq = sequenceCounter.incrementAndGet();
        enqueue(seq, headRecord(now()));
    }

    /**
     * Ensures the head record is enqueued before the first real record.
     * Called while holding the submission lock.
     *
     * @return false if the store is closed (the caller must drop the record)
     */
    private boolean startRunLocked() {
        if (closed) {
            return false;
        }
        if (!started) {
            enqueueHeadLocked();
        }
        return true;
    }

    /**
     * Assigns the next sequence number, waits for queue capacity, and hands the record to the sink.
     * Called while holding the submission lock.
     *
     * @param record the record
     */
    private void submitLocked(JIPipeProgressRecord record) {
        if (closed || finished) {
            return;
        }
        awaitCapacity();
        long seq = sequenceCounter.incrementAndGet();
        enqueue(seq, record);
    }

    private JIPipeProgressNode resolveParentLocked(int parentId) {
        JIPipeProgressNode parent;
        if (parentId <= 0 || parentId == ROOT_NODE_ID) {
            parent = rootNode;
        } else {
            parent = nodes.get(parentId);
        }
        if (parent == null) {
            throw new IllegalArgumentException("Unknown parent node id: " + parentId);
        }
        return parent;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static String renderStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    /**
     * Maps a node finish state to its wire literal ({@code s} field of {@code N} records).
     * The literals are part of the NDJSON contract and must not change.
     *
     * @param state the state
     * @return the wire literal
     */
    static String mapNodeStateToWire(JIPipeProgressNodeState state) {
        return switch (state) {
            case OK -> "ok";
            case FAILED -> "fail";
            case CANCELLED -> "cancel";
            case RUNNING -> throw new IllegalArgumentException("A node cannot finish into state RUNNING");
        };
    }

    /**
     * Maps a run state to its wire literal ({@code s} field of {@code s} records).
     * The literals are part of the NDJSON contract and must not change.
     *
     * @param state the state
     * @return the wire literal
     */
    static String mapRunStateToWire(JIPipeProgressNodeState state) {
        return switch (state) {
            case RUNNING -> "running";
            case OK -> "finished";
            case FAILED -> "failed";
            case CANCELLED -> "cancelled";
        };
    }

    /**
     * Maps the wire literal of an {@code N} record back to a node state. Unknown literals are mapped
     * to {@code null} (the state is left unchanged).
     *
     * @param wire the wire literal
     * @return the state, or {@code null}
     */
    static JIPipeProgressNodeState mapNodeStateFromWire(String wire) {
        return switch (wire) {
            case "ok" -> JIPipeProgressNodeState.OK;
            case "fail" -> JIPipeProgressNodeState.FAILED;
            case "cancel" -> JIPipeProgressNodeState.CANCELLED;
            default -> null;
        };
    }

    private record TailEntry(long seq, JIPipeProgressRecord record, long chars) {
    }
}
