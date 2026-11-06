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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Light metadata of one node inside the progress tree, as tracked by the progress store's node index.
 * <p>
 * Nodes correspond to {@code beginNode()}/resolve calls, not log lines, so the index stays small even for
 * runs with millions of records. Messages never live here — they are records in the NDJSON log. Record
 * attribution: a record with a node {@code id} ({@code n}, {@code N}, {@code l}, {@code p}, {@code e})
 * is attributed to that node's range ({@link #getFirstLine()}, {@link #getLastLine()},
 * {@link #getRecordCount()}).
 * <p>
 * All mutable state is atomically updated: the metadata is created on the thread that calls
 * {@code beginNode()}, while record ranges and progress/state are applied by the thread that owns the
 * store's sink (the writer thread for file-backed stores). The root node ({@code id} 1) is created
 * automatically and is implied by the file's head record.
 */
public class JIPipeProgressNode {

    private final int id;
    private final int parentId;
    private final String label;
    private final JIPipeProgressNodeKind kind;
    private final String location;
    private final AtomicReference<JIPipeProgressNodeState> state = new AtomicReference<>(JIPipeProgressNodeState.RUNNING);
    private final AtomicInteger progress = new AtomicInteger();
    private final AtomicInteger maxProgress = new AtomicInteger();
    private final AtomicInteger descendantNodeCount = new AtomicInteger();
    private final AtomicLong recordCount = new AtomicLong();
    private final AtomicInteger firstLine = new AtomicInteger();
    private final AtomicInteger lastLine = new AtomicInteger();

    JIPipeProgressNode(int id, int parentId, String label, JIPipeProgressNodeKind kind, String location) {
        this.id = id;
        this.parentId = parentId;
        this.label = label;
        this.kind = kind;
        this.location = location;
    }

    /**
     * Returns the numeric node id. The root node has id 1.
     *
     * @return the node id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the parent node id. The root node's parent id is 0.
     *
     * @return the parent node id
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * Returns the node label.
     *
     * @return the node label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the node kind.
     *
     * @return the node kind
     */
    public JIPipeProgressNodeKind getKind() {
        return kind;
    }

    /**
     * Returns the optional location URI of the node (e.g. {@code node://<uuid>} for a graph-run
     * algorithm node; free-form for future schemes).
     *
     * @return the location URI, or {@code null} if the node has no location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns the current node state.
     *
     * @return the node state
     */
    public JIPipeProgressNodeState getState() {
        return state.get();
    }

    /**
     * Returns the current progress value.
     *
     * @return the progress value
     */
    public int getProgress() {
        return progress.get();
    }

    /**
     * Returns the progress maximum. A negative maximum indicates an indeterminate progress.
     *
     * @return the progress maximum
     */
    public int getMaxProgress() {
        return maxProgress.get();
    }

    /**
     * Returns the number of descendants (transitive children).
     *
     * @return the number of descendant nodes
     */
    public int getDescendantNodeCount() {
        return descendantNodeCount.get();
    }

    /**
     * Returns the number of records attributed to this node (its {@code n}, {@code l}, {@code p},
     * {@code e}, and {@code N} records).
     *
     * @return the number of records
     */
    public long getRecordCount() {
        return recordCount.get();
    }

    /**
     * Returns the sequence number of the first record attributed to this node, or 0 if none.
     * The sequence number of a record is its line number in the NDJSON file.
     *
     * @return the first record's sequence number, or 0
     */
    public int getFirstLine() {
        return firstLine.get();
    }

    /**
     * Returns the sequence number of the last record attributed to this node, or 0 if none.
     *
     * @return the last record's sequence number, or 0
     */
    public int getLastLine() {
        return lastLine.get();
    }

    /**
     * Sets the node state. Called by the thread that owns the store's sink.
     *
     * @param state the new state
     */
    void setState(JIPipeProgressNodeState state) {
        this.state.set(state);
    }

    /**
     * Updates the progress counters. Called by the thread that owns the store's sink.
     *
     * @param value the progress value
     * @param max   the progress maximum (negative = indeterminate)
     */
    void setProgress(int value, int max) {
        progress.set(value);
        maxProgress.set(max);
    }

    /**
     * Increments the descendant count. Called on all ancestors when a child node is created.
     */
    void incrementDescendantNodeCount() {
        descendantNodeCount.incrementAndGet();
    }

    /**
     * Attributes a record to this node. Called by the thread that owns the store's sink.
     *
     * @param seq the record's sequence number (its line number)
     */
    void recordAt(long seq) {
        int line = (int) seq;
        firstLine.compareAndSet(0, line);
        lastLine.set(line);
        recordCount.incrementAndGet();
    }
}
