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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A progress store: one instance per top-level run. Records are written as NDJSON (one JSON object
 * per line; spec §Record types) into {@code <logs-base>/<run-uuid>.ndjson} and can be read back;
 * the node index and a bounded tail of recent renderable records are kept in memory.
 * <p>
 * Static factories select the implementation: {@link #silent()} discards records (index + tail still
 * maintained), {@link #stdout()} renders records to {@code System.out} on the calling thread
 * (non-persistent), and {@link #create(Path, String, String)} writes to a file via a single async
 * writer thread. Tree logic (node index, id allocation, tail) lives in
 * {@link AbstractJIPipeProgressLog}; implementations differ only in where records go.
 * <p>
 * Wire contract: the sequence number of a record is its line number (1-based; the head record is
 * line 1). The run's root node ({@code id} 1) is created automatically and is implied by the head
 * record — it has no {@code n} record; children of the root reference it via {@code p=0}. Readers
 * tolerate a truncated final line (writer killed mid-flush) and unknown record types/fields.
 */
public interface JIPipeProgressLog extends AutoCloseable {

    /**
     * Creates a silent store that discards records (no file, no output).
     * The node index and the tail are still maintained.
     *
     * @return the store
     */
    static JIPipeProgressLog silent() {
        return new SilentJIPipeProgressLog();
    }

    /**
     * Creates a store that renders each record as a compat line to {@code System.out} on the calling
     * thread. Non-persistent: no file is written, read-back serves the tail only. Intended for
     * low-volume use.
     *
     * @return the store
     */
    static JIPipeProgressLog stdout() {
        return new StdoutJIPipeProgressLog();
    }

    /**
     * Creates a file-backed store that writes NDJSON records via a single async writer thread.
     * The root node is created automatically. The file is created lazily by the writer thread on the
     * first flushed record — a run that never records anything creates no file.
     *
     * @param file     the NDJSON file (typically named {@code <run-uuid>.ndjson} inside a logs base)
     * @param runUuid  the run UUID (written into the head record)
     * @param runLabel the run label (written into the head record)
     * @return the store
     */
    static JIPipeProgressLog create(Path file, String runUuid, String runLabel) {
        return new FileJIPipeProgressLog(file, runUuid, runLabel);
    }

    /**
     * Opens the NDJSON file of a completed run as a <b>read-only</b> store view: no writer thread,
     * no file mutation (the file is only ever opened for reading), an emitter that never emits.
     * <p>
     * At construction the file is scanned once, in one sequential pass, to build the same
     * structures a writer-fed store keeps: the line→byte-offset checkpoint index and the node
     * index (decoded {@code n}, {@code p}, {@code N} records, plus the per-node record ranges
     * from the attributed records). Together with
     * the file's decode, the scan costs about one read of the file: opening a ~GB log takes seconds,
     * once, not per interaction. The initial catch-up scan of the progress tree panel issues a
     * second full read (that one decodes nothing — it only counts categories), so a very large
     * log opens with about two file reads' worth of I/O; the panel's paged/virtual detail reads
     * after that are checkpointed seeks.
     * <p>
     * After construction the view behaves exactly like the writer-fed store:
     * {@link #readRecords(int, int)}, {@link #readRecent(int)}, {@link #getNode(int)}, and
     * {@link #renderText(JIPipeLogLevel, Consumer)} serve the complete file, tolerating a truncated
     * final line. The store is born closed, so producer calls ({@code beginNode}, {@code log},
     * ...) are dropped exactly like on a closed writer-fed store, and {@link #close()} only
     * releases the background scan (idempotent; reads keep working afterwards).
     * <p>
     * A missing or unreadable file yields an empty view rather than an exception — the caller
     * surfaces the missing data (see {@code JIPipeRunnableLogEntry#DATA_UNAVAILABLE_LINE}).
     * A file without a parseable run-status record (a writer killed before the final flush)
     * reopens with the root node's state CANCELLED — a dead run must never render as live.
     *
     * @param file the NDJSON file of a {@link #create(Path, String, String)} store
     * @return the read-only store view
     */
    static JIPipeProgressLog openReadOnly(Path file) {
        return FileJIPipeProgressLog.openReadOnly(file);
    }

    /**
     * Resolves the logs base directory for a temporary base.
     *
     * @param projectTempBaseOrNull the temporary base to append {@code logs} to, or {@code null} to
     *                              resolve the global temporary base
     * @return {@code base.resolve("logs")}; the global base's {@code logs} directory if the argument
     * is {@code null} (may initialize JIPipe services and creates the directory)
     */
    static Path resolveStorageBase(Path projectTempBaseOrNull) {
        if (projectTempBaseOrNull == null) {
            return resolveStorageBase();
        }
        return projectTempBaseOrNull.resolve("logs");
    }

    /**
     * Resolves the global logs base directory from JIPipe's temporary base
     * ({@link JIPipe#getTemporaryBaseDirectory()}) and creates it if needed.
     *
     * @return the global {@code logs} directory
     */
    static Path resolveStorageBase() {
        Path base = JIPipe.getTemporaryBaseDirectory().resolve("logs");
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create progress logs base directory " + base, e);
        }
        return base;
    }

    /**
     * Returns the NDJSON file of this store.
     *
     * @return the file, or {@code null} for stores that write no file (silent/stdout)
     */
    Path getFile();

    /**
     * Creates a child node and enqueues its {@code n} record.
     *
     * @param parentId the parent node id ({@link #getRootNodeId()} for top-level nodes)
     * @param label    the node label
     * @param kind     the node kind
     * @return the new node's id
     */
    default int beginNode(int parentId, String label, JIPipeProgressNodeKind kind) {
        return beginNode(parentId, label, kind, null);
    }

    /**
     * Creates a child node and enqueues its {@code n} record, carrying an optional location URI.
     * The node level is {@link JIPipeLogLevel#INFO}.
     *
     * @param parentId the parent node id ({@link #getRootNodeId()} for top-level nodes)
     * @param label    the node label
     * @param kind     the node kind
     * @param location the free-form location URI (e.g. {@code node://<uuid>}), or {@code null} for no location
     * @return the new node's id
     */
    default int beginNode(int parentId, String label, JIPipeProgressNodeKind kind, String location) {
        return beginNode(parentId, label, kind, location, JIPipeLogLevel.INFO);
    }

    /**
     * Creates a child node and enqueues its {@code n} record, carrying an optional location URI and
     * an optional visibility level. The level is node metadata for consumers (e.g. the progress
     * panel hides DEBUG nodes by default); it does not affect the text render.
     *
     * @param parentId the parent node id ({@link #getRootNodeId()} for top-level nodes)
     * @param label    the node label
     * @param kind     the node kind
     * @param location the free-form location URI (e.g. {@code node://<uuid>}), or {@code null} for no location
     * @param level    the node level, or {@code null} for {@link JIPipeLogLevel#INFO}
     * @return the new node's id
     */
    int beginNode(int parentId, String label, JIPipeProgressNodeKind kind, String location, JIPipeLogLevel level);

    /**
     * Enqueues a log record ({@code l}) attached to a node.
     *
     * @param nodeId  the node id
     * @param level   the log level
     * @param message the message
     */
    void log(int nodeId, JIPipeLogLevel level, String message);

    /**
     * Enqueues a progress record ({@code p}) attached to a node.
     *
     * @param nodeId the node id
     * @param value  the progress value
     * @param max    the progress maximum ({@code max < 0} = indeterminate)
     */
    void progress(int nodeId, int value, int max);

    /**
     * Enqueues an error record ({@code e}) attached to a node.
     *
     * @param nodeId   the node id
     * @param throwable the throwable (class, message, and stack trace are recorded)
     */
    void error(int nodeId, Throwable throwable);

    /**
     * Enqueues a run-level user-facing notification record ({@code w}). The record is not
     * attributed to a node.
     *
     * @param heading     the notification heading
     * @param description the notification description
     */
    void notification(String heading, String description);

    /**
     * Enqueues a node-finished record ({@code N}) and updates the node's state.
     *
     * @param nodeId the node id
     * @param state  the finish state ({@code OK}, {@code FAILED}, or {@code CANCELLED})
     */
    void nodeFinished(int nodeId, JIPipeProgressNodeState state);

    /**
     * Finishes the run: sets the root node's state, enqueues the run-status record ({@code s}),
     * performs the final flush, and closes the store.
     *
     * @param runState the final run state
     */
    void finish(JIPipeProgressNodeState runState);

    /**
     * Closes the store after flushing all pending records. Records submitted after closing are
     * silently dropped. Calling this multiple times has no additional effect.
     */
    @Override
    void close();

    /**
     * Returns the metadata of a node from the index.
     *
     * @param id the node id
     * @return the node, or {@code null} if no such node exists
     */
    JIPipeProgressNode getNode(int id);

    /**
     * Returns the id of the automatically created root node.
     *
     * @return the root node id (always 1)
     */
    int getRootNodeId();

    /**
     * Returns the id that the next {@link #beginNode(int, String, JIPipeProgressNodeKind)} call will
     * return, without creating a node.
     *
     * @return the next node id
     */
    int nextNodeId();

    /**
     * Reads records starting at a line number. Line numbers are 1-based and line 1 is the head
     * record. On file-backed stores this parses the file (tolerating a truncated final line);
     * on non-persistent stores this serves what the tail still holds and never throws.
     *
     * @param fromLine the first line number to read (1-based)
     * @param limit    the maximum number of records to return
     * @return the records, in file order (oldest to newest); empty if none are available
     */
    List<JIPipeProgressRecord> readRecords(int fromLine, int limit);

    /**
     * Reads the most recent renderable records from the tail. Works on all implementations.
     *
     * @param count the maximum number of records to return
     * @return the records, oldest to newest; empty if none are available
     */
    List<JIPipeProgressRecord> readRecent(int count);

    /**
     * Reads records starting at a line number, filtered by log level: log records ({@code l})
     * below the minimum level are skipped; all other records have no level and are kept. The limit
     * bounds the number of returned (already filtered) records. On file-backed stores this parses
     * the file (tolerating a truncated final line); on non-persistent stores this serves what the
     * tail still holds and never throws.
     *
     * @param fromLine the first line number to read (1-based)
     * @param limit    the maximum number of records to return (after filtering)
     * @param minLevel the minimum level of log records to return
     * @return the records, in file order (oldest to newest); empty if none are available
     */
    List<JIPipeProgressRecord> readRecords(int fromLine, int limit, JIPipeLogLevel minLevel);

    /**
     * Renders the records of this store as compatibility text lines, handing each line to the
     * sink. This is the render behind {@code log.txt} and log views; it is a convenience render,
     * not a parseable contract. The line shapes:
     * <ul>
     * <li>Log records ({@code l}) at a level below {@code minLevel} are omitted entirely; all
     *     other records are unaffected by the level. A log record renders as
     *     {@code [v/mx] <chain> | message}, where {@code v/mx} is the node's running progress
     *     (the last {@code p} record of the node, initially {@code 1/1}) and {@code <chain>} is
     *     the node's path labels joined with {@code |}, skipping empty labels (a detached
     *     progress node contributes no segment). The root's label is not part of the chain;
     *     a record of the root itself renders without the chain part. The facade reports its
     *     percentage points as {@code l} records (e.g. {@code 50%}).</li>
     * <li>A progress record ({@code p}) renders nothing; it only updates the running progress
     *     of the following lines (indeterminate progress does not affect it).</li>
     * <li>An error record ({@code e}) renders as {@code [v/mx] <chain> | <class>: <message>}
     *     followed by one line per stack trace line, indented two spaces.</li>
     * <li>A notification record ({@code w}) renders as {@code @ <heading>: <description>}
     *     (run-level — no prefix, no chain).</li>
     * <li>Head, node ({@code n}), node-finished ({@code N}), and run-status ({@code s}) records
     *     render nothing.</li>
     * <li>Multi-line messages hand each continuation line to the sink as its own line.</li>
     * </ul>
     * <p>
     * File-backed stores stream the file in ~10,000-record pages (bounded memory; a store can be
     * rendered while the run is active, reflecting the flushed records, and after the run).
     * Non-persistent stores render what the tail holds — tail-bounded, never throws; chain labels
     * are resolved from the node index when the tail holds no {@code n} records.
     *
     * @param minLevel the minimum level of log records to render
     * @param lineSink the consumer of the rendered lines
     */
    void renderText(JIPipeLogLevel minLevel, Consumer<String> lineSink);

    /**
     * Renders the given records as compatibility text lines, handing each line to the sink, using
     * the exact shapes documented for {@link #renderText(JIPipeLogLevel, Consumer)}. This is the
     * per-batch render for batch-event consumers: a {@link ProgressBatchEvent} receiver reads the
     * batch's records via {@link #readRecords(int, int)} and renders them with this method.
     * <p>
     * Unlike {@link #renderText(JIPipeLogLevel, Consumer)}, which derives the {@code [v/mx]}
     * prefixes strictly from the walked records, this method falls back to the node index's
     * <b>current</b> progress state for nodes whose latest {@code p} record lies before the batch
     * (a per-batch render cannot see earlier records). This mirrors what a live consumer saw at
     * the time of the record.
     *
     * @param records  the records to render (e.g. one batch's {@link #readRecords(int, int)} result)
     * @param minLevel the minimum level of log records to render
     * @param lineSink the consumer of the rendered lines
     */
    default void renderRecords(List<JIPipeProgressRecord> records, JIPipeLogLevel minLevel, Consumer<String> lineSink) {
    }

    /**
     * Renders the records of a store's NDJSON file as compatibility text lines, handing each line
     * to the sink, using the exact shapes documented for
     * {@link #renderText(JIPipeLogLevel, Consumer)}. This renders a file that outlived its store
     * instance (e.g. the data file referenced by a log entry; the store is finished and closed by
     * the worker before the entry is created, so renders of the complete file are stable).
     * <p>
     * The file is streamed in a single pass (bounded memory, independent of the record count).
     * A truncated final line stops the render at the last parseable record. A missing or
     * unreadable file renders nothing and never throws.
     *
     * @param file     the NDJSON file of a {@link #create(Path, String, String)} store
     * @param minLevel the minimum level of log records to render
     * @param lineSink the consumer of the rendered lines
     */
    static void renderFile(Path file, JIPipeLogLevel minLevel, Consumer<String> lineSink) {
        Objects.requireNonNull(minLevel, "Minimum log level must not be null");
        Objects.requireNonNull(lineSink, "Line sink must not be null");
        if (file == null) {
            return;
        }
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        // A full-file walk has no node index: the walk state is derived from the records alone
        // (the null-store walk renders unknown nodes without a chain)
        AbstractJIPipeProgressLog.RenderWalk walk = new AbstractJIPipeProgressLog.RenderWalk(null, false);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ);
             BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8), 64 * 1024)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node;
                try {
                    node = mapper.readTree(line);
                } catch (JsonProcessingException e) {
                    // Truncated or corrupt line (typically the final line) — stop at the last parseable line
                    break;
                }
                JIPipeProgressRecord record = FileJIPipeProgressLog.parseRecord(mapper, node);
                if (record != null) {
                    walk.render(record, minLevel, lineSink);
                }
                // Unknown record types are skipped (forward compatibility)
            }
        } catch (IOException e) {
            // Rendering a file never throws (e.g. the file vanished or is locked); callers
            // surface the missing data (see JIPipeRunnableLogEntry)
            System.err.println("Failed to render progress log file " + file);
            e.printStackTrace();
        }
    }

    /**
     * Sets the coalesce interval for batch notifications: the minimum time between two batch
     * events of the writer thread of a file-backed store. Default 100. The final batch is exempt
     * (always emitted at run end). Has no effect on stores without a writer thread.
     *
     * @param ms the interval in milliseconds
     */
    void setCoalesceIntervalMillis(long ms);

    /**
     * Sets the maximum number of renderable records kept in the tail. Default 8192. Independently of
     * this limit, the tail retains at most ~8 MB of message text — whichever bound binds first.
     *
     * @param records the tail limit
     */
    void setTailLimit(int records);

    /**
     * Returns the emitter of {@link ProgressBatchEvent}s.
     * <p>
     * File-backed stores emit coalesced batches on their writer thread: at most one event per
     * {@link #setCoalesceIntervalMillis(long) coalesce interval} plus a guaranteed final batch
     * (flagged {@code finalBatch()}) when the store is closed — also on
     * {@link #finish(JIPipeProgressNodeState)} arriving inside the interval window. Batches
     * partition the written lines contiguously (no gaps). Listeners run inline on the writer
     * thread and must not block; heavy work (e.g. UI updates) must hop to another thread.
     * Silent and stdout stores have no writer thread; their emitters never emit.
     *
     * @return the batch event emitter
     */
    ProgressBatchEventEmitter getBatchEventEmitter();

    /**
     * A batch of new/changed records, emitted at most once per coalesce interval plus a guaranteed
     * final batch at run end. Consumers never receive per-record events on the hot path.
     * <p>
     * Empty-final-batch convention: {@code fromLine() > toLine()} marks the guaranteed final batch
     * of a run whose records were all covered by earlier batches — the run is complete and no new
     * records are attached (consumers must not render the range).
     */
    class ProgressBatchEvent extends AbstractJIPipeEvent {

        private final int fromLine;
        private final int toLine;
        private final Set<Integer> changedNodes;
        private final boolean finalBatch;

        public ProgressBatchEvent(Object source, int fromLine, int toLine, Set<Integer> changedNodes, boolean finalBatch) {
            super(source);
            this.fromLine = fromLine;
            this.toLine = toLine;
            this.changedNodes = changedNodes;
            this.finalBatch = finalBatch;
        }

        /**
         * Returns the first line number covered by this batch.
         *
         * @return the first line number
         */
        public int fromLine() {
            return fromLine;
        }

        /**
         * Returns the last line number covered by this batch.
         *
         * @return the last line number
         */
        public int toLine() {
            return toLine;
        }

        /**
         * Returns the ids of nodes changed within this batch.
         *
         * @return the changed node ids
         */
        public Set<Integer> changedNodes() {
            return changedNodes;
        }

        /**
         * Returns whether this is the guaranteed final batch of the run.
         *
         * @return true if this is the final batch
         */
        public boolean finalBatch() {
            return finalBatch;
        }
    }

    /**
     * Listener for {@link ProgressBatchEvent}s.
     */
    interface ProgressBatchEventListener {
        void onProgressBatch(ProgressBatchEvent event);
    }

    /**
     * Emitter for {@link ProgressBatchEvent}s.
     */
    class ProgressBatchEventEmitter extends JIPipeEventEmitter<ProgressBatchEvent, ProgressBatchEventListener> {

        @Override
        protected void call(ProgressBatchEventListener progressBatchEventListener, ProgressBatchEvent event) {
            progressBatchEventListener.onProgressBatch(event);
        }
    }
}
