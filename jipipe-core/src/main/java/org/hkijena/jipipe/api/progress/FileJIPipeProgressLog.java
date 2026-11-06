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
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * A progress store that writes NDJSON records to a file via a single async writer thread.
 * <p>
 * Producers enqueue records and return immediately (no I/O on the producer path; bounded
 * backpressure kicks in only when the writer falls far behind). The writer thread drains the queue,
 * encodes records on the writer thread, appends them to the file, and flushes when 64 KB have been
 * buffered, after 250 ms, or on {@code finish()}/{@code close()}. The file is created lazily on the
 * first flushed record. Index and tail are updated on the writer thread (single writer — no
 * contention). Sequence numbers are assigned on submission, so submission order equals line order.
 * <p>
 * Read-back opens the file with a separate channel and tolerates a truncated final line
 * (writer killed mid-flush): parsing stops at the last parseable line without throwing.
 * <p>
 * Random-range reads ({@code readRecords(fromLine, ...)}) do not skip-scan from the file start:
 * the writer thread records a line→byte-offset checkpoint every {@link #CHECKPOINT_INTERVAL_LINES}
 * lines — the exact byte offset where the checkpointed line starts — and publishes it as an
 * immutable {@link CheckpointIndex} snapshot through a volatile field. Readers take one volatile
 * read and never observe a partially built index; the writer never blocks on readers. Lines
 * before the first checkpoint are scanned from the file start as before.
 * <p>
 * Batch notifications: the writer thread emits coalesced {@link JIPipeProgressLog.ProgressBatchEvent}s
 * — at most one per {@link #setCoalesceIntervalMillis(long) coalesce interval}, plus a guaranteed
 * final batch when the store is closed (via {@link #finish(JIPipeProgressNodeState)} or
 * {@link #close()}). Listeners run inline on the writer thread and must not block; they must hop
 * to another thread for heavy work (e.g. Swing). The coalescer state lives in the writer thread
 * only; events are always emitted outside {@code submitLock} and {@code tailLock}.
 * <p>
 * <b>Read-only mode</b> ({@link JIPipeProgressLog#openReadOnly(Path)}): a private constructor
 * creates a mode of this class with no writer thread and a born-closed store — producer calls are
 * dropped, {@code close()} is a no-op, the emitter never emits, and the file is opened for reading
 * only. A one-pass background scan at construction builds the checkpoint index and replays the
 * decoded records into the node index and tail ({@code applyScannedRecord}); the scan thread is
 * joined by the constructor, so the store is fully indexed when {@code openReadOnly} returns.
 * Read-back behaves exactly like a writer-fed store (checkpointed seeks, truncation tolerance).
 */
public class FileJIPipeProgressLog extends AbstractJIPipeProgressLog {

    private static final int FLUSH_BUFFER_BYTES = 64 * 1024;
    private static final long FLUSH_INTERVAL_NANOS = 250 * 1_000_000L;
    private static final int READ_BUFFER_CHARS = 64 * 1024;

    /**
     * Every this many lines, the writer thread records the byte offset where the checkpointed
     * line starts, so that random-range reads can seek there instead of skip-scanning from the
     * file start. The in-memory index is bounded by the line count divided by this interval
     * (6,000,000 lines → 600 entries).
     */
    private static final long CHECKPOINT_INTERVAL_LINES = 10_000;

    /**
     * Number of records read from the file per page while rendering or level-filtered reading
     * (bounded memory, independent of the total record count).
     */
    private static final int WALK_PAGE_SIZE = 10_000;

    /**
     * Upper bound for the number of queued records (backpressure threshold). Keeps the heap bounded
     * if a burst of producers outpaces the writer for a long time.
     */
    private static final int MAX_QUEUED_RECORDS = 262_144;
    private static final long BACKPRESSURE_BACKOFF_NANOS = 100_000;

    private final Path file;
    private final ConcurrentLinkedQueue<QueuedRecord> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queuedRecords = new AtomicInteger();
    private final Thread writerThread;
    private volatile boolean writerWaiting;

    // Checkpoint index — the writer thread appends and publishes immutable snapshots; readers
    // take one volatile read of the current snapshot (never blocked, never sees a partial index)
    private volatile CheckpointIndex checkpointIndex = CheckpointIndex.EMPTY;

    // Writer-thread state only
    private BufferedOutputStream sink;
    private long lastFlushNanos;
    private long recordsSinceFlush;
    private boolean sinkBroken;
    private long writtenBytes;
    private long lastCheckpointLine;

    // Coalescer state — writer-thread only, never touched by producers (no synchronization needed)
    private long lastEmitNanos;
    private long pendingFromLine;
    private long pendingToLine;
    private long lastWrittenLine;
    private HashSet<Integer> pendingNodes = new HashSet<>();

    /**
     * Creates the store and starts the writer thread.
     *
     * @param file     the NDJSON file
     * @param runUuid  the run UUID (written into the head record)
     * @param runLabel the run label (written into the head record)
     */
    public FileJIPipeProgressLog(Path file, String runUuid, String runLabel) {
        super(runUuid, runLabel);
        this.file = file;
        this.writerThread = new Thread(this::writeLoop, "JIPipe-Progress-Writer-" + file.getFileName());
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    /**
     * Creates the read-only reopen of a completed store file (see
     * {@link JIPipeProgressLog#openReadOnly(Path)}).
     *
     * @param file the completed NDJSON file (opened for reading only)
     * @return the read-only store view
     */
    static JIPipeProgressLog openReadOnly(Path file) {
        return new FileJIPipeProgressLog(file);
    }

    /**
     * Creates the read-only reopen of a completed store file: no writer thread, the store is born
     * closed (producer calls are dropped, {@code close()} is a no-op), and a background one-pass
     * scan builds the checkpoint index and replays the records into the node index and tail. The
     * run UUID and label come from the file's head record, so the reopened root node equals the
     * original's. The scan thread is joined here, so the store is fully indexed when the
     * constructor returns.
     *
     * @param file the completed NDJSON file (opened for reading only)
     */
    private FileJIPipeProgressLog(Path file) {
        super(headRunUuid(file), headRunLabel(file));
        this.file = file;
        this.writerThread = null;
        // Born closed: every producer entry point drops its record (the same semantic as a
        // closed writer-fed store), and a later close() is an idempotent no-op
        close();
        Thread scanThread = new Thread(this::scanReadOnly, "JIPipe-Progress-Reopen-" + file.getFileName());
        scanThread.setDaemon(true);
        scanThread.start();
        boolean interrupted = false;
        try {
            scanThread.join();
        } catch (InterruptedException e) {
            interrupted = true;
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Path getFile() {
        return file;
    }

    @Override
    protected void awaitCapacity() {
        while (!isClosed() && queuedRecords.get() >= MAX_QUEUED_RECORDS) {
            LockSupport.parkNanos(BACKPRESSURE_BACKOFF_NANOS);
        }
    }

    @Override
    protected void enqueue(long seq, JIPipeProgressRecord record) {
        queue.offer(new QueuedRecord(seq, record));
        queuedRecords.incrementAndGet();
        if (writerWaiting) {
            LockSupport.unpark(writerThread);
        }
    }

    @Override
    protected void onClose() {
        if (writerThread == null) {
            // Read-only reopen: no writer thread to stop (close() is a no-op)
            return;
        }
        LockSupport.unpark(writerThread);
        try {
            writerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the run UUID of a store file's head record (line 1) — the reopened root node's
     * metadata must equal the original's.
     *
     * @param file the completed NDJSON file
     * @return the run UUID, or {@code null} if the file has no readable head record
     */
    private static String headRunUuid(Path file) {
        return headRecordField(file, "run");
    }

    /**
     * Returns the run label of a store file's head record (line 1).
     *
     * @param file the completed NDJSON file
     * @return the run label, or {@code null} if the file has no readable head record
     */
    private static String headRunLabel(Path file) {
        return headRecordField(file, "label");
    }

    /**
     * Reads one field of a store file's head record (line 1). Never throws — an unreadable or
     * malformed file yields {@code null} (the scan tolerates it the same way).
     *
     * @param file  the completed NDJSON file
     * @param field the head record's field name ({@code run} or {@code label})
     * @return the field value, or {@code null}
     */
    private static String headRecordField(Path file, String field) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    JsonNode node = JsonUtils.getObjectMapper().readTree(line);
                    if ("head".equals(node.path("t").asText(null))) {
                        return node.path(field).asText(null);
                    }
                    // The first record is not a head record (not a store file)
                    return null;
                } catch (JsonProcessingException e) {
                    return null;
                }
            }
        } catch (IOException e) {
            // Unreadable file — the scan will tolerate it too
        }
        return null;
    }

    /**
     * The read-only reopen scan: one sequential pass over the file that tracks byte offsets
     * exactly, records a line→byte-offset checkpoint every {@link #CHECKPOINT_INTERVAL_LINES}
     * lines, and replays every decoded record into the node index and tail — the same updates
     * the writer thread performs for a live store, minus the file write. A truncated final line
     * is tolerated exactly like in {@link #readRecords(int, int)} (the scan stops serving records
     * there; earlier lines are unaffected). The scan never throws: an unreadable file leaves a
     * partial index, which the reads tolerate.
     */
    private void scanReadOnly() {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        long lineNo = 0;
        long lineStart = 0;
        long lastCheckpointLine = 0;
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(READ_BUFFER_CHARS);
        byte[] buffer = new byte[READ_BUFFER_CHARS];
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file, StandardOpenOption.READ), READ_BUFFER_CHARS)) {
            long pos = 0; // absolute byte offset where buffer[0] starts
            int read;
            while ((read = input.read(buffer)) > 0) {
                int lineBegin = 0;
                for (int i = 0; i < read; i++) {
                    if (buffer[i] != '\n') {
                        continue;
                    }
                    lineBuffer.write(buffer, lineBegin, i - lineBegin);
                    lineBegin = i + 1;
                    lineNo++;
                    // Checkpoint before applying the line: the entry's offset is where THIS line
                    // starts (the same bookkeeping the writer thread does before its write)
                    if (lineNo - lastCheckpointLine >= CHECKPOINT_INTERVAL_LINES) {
                        addCheckpoint(lineNo, lineStart);
                        lastCheckpointLine = lineNo;
                    }
                    applyScannedLine(mapper, lineBuffer, lineNo);
                    lineBuffer.reset();
                    lineStart = pos + i + 1;
                }
                lineBuffer.write(buffer, lineBegin, read - lineBegin);
                pos += read;
            }
            // A final line without a trailing newline (a truncated write): count and try to
            // parse it — the same tolerance readRecords applies
            if (lineBuffer.size() > 0) {
                lineNo++;
                if (lineNo - lastCheckpointLine >= CHECKPOINT_INTERVAL_LINES) {
                    addCheckpoint(lineNo, lineStart);
                    lastCheckpointLine = lineNo;
                }
                applyScannedLine(mapper, lineBuffer, lineNo);
            }
        } catch (IOException e) {
            System.err.println("Failed to scan progress log file " + file);
            e.printStackTrace();
        }
        // A file without a parseable run-status record (the writer was killed before the final
        // flush) must not reopen as a live run
        finishScannedRunWithoutStatus();
    }

    /**
     * Decodes one scanned line and replays the record into the index and tail. Blank, corrupt,
     * and unknown-typed lines are skipped (forward compatibility — the same tolerance
     * {@link #readRecords(int, int)} applies).
     *
     * @param mapper     the object mapper
     * @param lineBuffer the line's bytes (without the terminating newline)
     * @param lineNo     the line's 1-based number
     */
    private void applyScannedLine(ObjectMapper mapper, ByteArrayOutputStream lineBuffer, long lineNo) {
        if (lineBuffer.size() == 0) {
            return;
        }
        JsonNode node;
        try {
            node = mapper.readTree(lineBuffer.toByteArray());
        } catch (IOException e) {
            return; // truncated or corrupt line (typically the final line)
        }
        JIPipeProgressRecord record = parseRecord(mapper, node);
        if (record != null) {
            applyScannedRecord(lineNo, record);
        }
        // Unknown record types are skipped (forward compatibility)
    }

    @Override
    protected void writeRecord(long line, JIPipeProgressRecord record) {
        try {
            // The record is encoded to bytes here (instead of via a Writer) so that the running
            // byte offset — and with it the checkpoint index — stays byte-exact
            byte[] bytes = JsonUtils.toJsonString(record).getBytes(StandardCharsets.UTF_8);
            // Checkpoint before the write: writtenBytes is the exact byte offset where this line
            // starts. The checkpoint stays valid even if this write fails (the reader tolerates
            // a truncated final line).
            if (line - lastCheckpointLine >= CHECKPOINT_INTERVAL_LINES) {
                addCheckpoint(line, writtenBytes);
                lastCheckpointLine = line;
            }
            sink.write(bytes);
            sink.write('\n');
            writtenBytes += bytes.length + 1L;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Appends one checkpoint to the index and publishes a new immutable snapshot. Called on the
     * writer thread only; readers pick the snapshot up via the volatile field.
     *
     * @param line   the 1-based line number the checkpoint covers
     * @param offset the byte offset where this line starts
     */
    private void addCheckpoint(long line, long offset) {
        checkpointIndex = checkpointIndex.withEntry(line, offset);
    }

    /**
     * Returns the current checkpoint index snapshot (package-private for tests).
     */
    CheckpointIndex checkpointIndexForTesting() {
        return checkpointIndex;
    }

    @Override
    public List<JIPipeProgressRecord> readRecords(int fromLine, int limit) {
        if (limit <= 0 || !Files.exists(file)) {
            return List.of();
        }
        List<JIPipeProgressRecord> result = new ArrayList<>();
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        // Open a separate channel — never the writer's stream
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ);
             BufferedReader reader = new BufferedReader(Channels.newReader(channel, StandardCharsets.UTF_8), READ_BUFFER_CHARS)) {
            long startLine = Math.max(fromLine, 1);
            // Seek to the nearest checkpoint at or before the requested line and scan forward
            // from there; without a covering checkpoint, scan from the file start as before
            long toSkip = startLine - 1;
            CheckpointIndex checkpoints = checkpointIndex;
            int checkpoint = checkpoints.floor(startLine);
            if (checkpoint >= 0) {
                channel.position(checkpoints.offsetAt(checkpoint));
                toSkip = startLine - checkpoints.lineAt(checkpoint);
            }
            String line;
            while (toSkip > 0 && (line = reader.readLine()) != null) {
                toSkip--;
            }
            if (toSkip == 0) {
                while (result.size() < limit && (line = reader.readLine()) != null) {
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
                    JIPipeProgressRecord record = parseRecord(mapper, node);
                    if (record != null) {
                        result.add(record);
                    }
                    // Unknown record types are skipped (forward compatibility)
                }
            }
        } catch (IOException e) {
            // Read-back never throws; return what was read so far
            System.err.println("Failed to read progress records from " + file);
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void walkRecords(Consumer<JIPipeProgressRecord> recordConsumer) {
        // Stream the file in pages — bounded memory, independent of the total record count
        int fromLine = 1;
        while (true) {
            List<JIPipeProgressRecord> page = readRecords(fromLine, WALK_PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            for (JIPipeProgressRecord record : page) {
                recordConsumer.accept(record);
            }
            if (page.size() < WALK_PAGE_SIZE) {
                break;
            }
            fromLine += page.size();
        }
    }

    @Override
    public List<JIPipeProgressRecord> readRecords(int fromLine, int limit, JIPipeLogLevel minLevel) {
        if (limit <= 0) {
            return List.of();
        }
        List<JIPipeProgressRecord> result = new ArrayList<>();
        int pageStart = Math.max(fromLine, 1);
        while (result.size() < limit) {
            List<JIPipeProgressRecord> page = readRecords(pageStart, WALK_PAGE_SIZE);
            if (page.isEmpty()) {
                break;
            }
            for (JIPipeProgressRecord record : page) {
                if (passesLevelFilter(record, minLevel)) {
                    result.add(record);
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
            if (page.size() < WALK_PAGE_SIZE) {
                break;
            }
            pageStart += page.size();
        }
        return result;
    }

    /**
     * Maps one parsed record line to a record by its {@code t} discriminator. The field serializes
     * last in the line, so lines must be parsed with Jackson — never by string splitting.
     * <p>
     * Package-private: also serves {@link JIPipeProgressLog#renderFile(Path, JIPipeLogLevel, Consumer)},
     * which renders a store file that outlived its store instance.
     *
     * @param mapper the object mapper
     * @param node   the parsed line
     * @return the record, or {@code null} if the record type is unknown or the line is malformed
     */
    static JIPipeProgressRecord parseRecord(ObjectMapper mapper, JsonNode node) {
        String type = node.path("t").asText(null);
        if (type == null) {
            return null;
        }
        try {
            return switch (type) {
                case "head" -> mapper.treeToValue(node, JIPipeProgressRecord.Head.class);
                case "n" -> mapper.treeToValue(node, JIPipeProgressRecord.Node.class);
                case "N" -> mapper.treeToValue(node, JIPipeProgressRecord.NodeFinished.class);
                case "l" -> mapper.treeToValue(node, JIPipeProgressRecord.Log.class);
                case "p" -> mapper.treeToValue(node, JIPipeProgressRecord.Progress.class);
                case "e" -> mapper.treeToValue(node, JIPipeProgressRecord.Error.class);
                case "s" -> mapper.treeToValue(node, JIPipeProgressRecord.Status.class);
                case "w" -> mapper.treeToValue(node, JIPipeProgressRecord.Notification.class);
                default -> null;
            };
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void writeLoop() {
        lastFlushNanos = System.nanoTime();
        lastEmitNanos = lastFlushNanos;
        while (true) {
            drainQueue();
            if (isClosed() && queue.isEmpty()) {
                break;
            }
            emitPendingIfDue();
            if (recordsSinceFlush > 0 && System.nanoTime() - lastFlushNanos >= FLUSH_INTERVAL_NANOS) {
                flushSink();
            }
            writerWaiting = true;
            boolean workPending = !queue.isEmpty() || isClosed();
            if (!workPending) {
                boolean flushWait = recordsSinceFlush > 0;
                boolean emitWait = pendingFromLine > 0;
                if (flushWait || emitWait) {
                    // Wake for whichever deadline comes first: the 250 ms flush or the coalesce
                    // interval (a pending batch must not wait longer than its interval)
                    long remaining = Long.MAX_VALUE;
                    if (flushWait) {
                        remaining = Math.min(remaining, FLUSH_INTERVAL_NANOS - (System.nanoTime() - lastFlushNanos));
                    }
                    if (emitWait) {
                        remaining = Math.min(remaining, coalesceIntervalNanos() - (System.nanoTime() - lastEmitNanos));
                    }
                    LockSupport.parkNanos(Math.max(1_000_000L, remaining));
                } else {
                    LockSupport.park();
                }
            }
            writerWaiting = false;
        }
        emitFinalBatch();
        flushSink();
        closeSink();
    }

    private void drainQueue() {
        QueuedRecord entry;
        while ((entry = queue.poll()) != null) {
            queuedRecords.decrementAndGet();
            trackPending(entry.seq(), entry.record());
            try {
                applyRecord(entry.seq(), entry.record());
                if (!sinkBroken) {
                    ensureSink();
                    writeRecord(entry.seq(), entry.record());
                    recordsSinceFlush++;
                }
            } catch (Throwable e) {
                // The writer must survive a broken sink or a bad record
                System.err.println("Failed to write progress record; remaining records are kept in memory only");
                e.printStackTrace();
                sinkBroken = true;
            }
            // Mid-drain emission: a long burst must stream batches while it runs, not only after
            // the queue has drained (the burst test's more-than-one-batch contract)
            emitPendingIfDue();
        }
    }

    /**
     * Adds one drained record to the pending batch summary. The sequence numbers of queued records
     * are strictly increasing (assigned under the submission lock, queue is FIFO), so the pending
     * range is updated monotonically.
     *
     * @param seq    the record's line number
     * @param record the record
     */
    private void trackPending(long seq, JIPipeProgressRecord record) {
        if (pendingFromLine == 0) {
            pendingFromLine = seq;
        }
        pendingToLine = seq;
        lastWrittenLine = seq;
        int nodeId = nodeIdOf(record);
        if (nodeId > 0) {
            pendingNodes.add(nodeId);
        }
    }

    /**
     * Returns the node id carried by a record, or {@code 0} for run-level records
     * ({@code head}, {@code s}, {@code w}) that are not attributed to a node.
     *
     * @param record the record
     * @return the node id, or 0
     */
    private static int nodeIdOf(JIPipeProgressRecord record) {
        if (record instanceof JIPipeProgressRecord.Node nodeRecord) {
            return nodeRecord.id();
        } else if (record instanceof JIPipeProgressRecord.Log logRecord) {
            return logRecord.id();
        } else if (record instanceof JIPipeProgressRecord.Progress progressRecord) {
            return progressRecord.id();
        } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
            return errorRecord.id();
        } else if (record instanceof JIPipeProgressRecord.NodeFinished finishRecord) {
            return finishRecord.id();
        }
        return 0;
    }

    /**
     * Emits the pending batch if one is due: records are pending and at least one coalesce
     * interval has passed since the last emission.
     */
    private void emitPendingIfDue() {
        if (pendingFromLine == 0) {
            return;
        }
        if (System.nanoTime() - lastEmitNanos < coalesceIntervalNanos()) {
            return;
        }
        emitPendingBatch(false);
    }

    /**
     * Emits and clears the pending batch summary. Called on the writer thread while holding no
     * lock — the event is delivered synchronously to the listeners on this thread.
     *
     * @param finalBatch whether this is the guaranteed final batch
     */
    private void emitPendingBatch(boolean finalBatch) {
        ProgressBatchEvent event = new ProgressBatchEvent(this,
                (int) pendingFromLine, (int) pendingToLine, pendingNodes, finalBatch);
        pendingFromLine = 0;
        pendingToLine = 0;
        pendingNodes = new HashSet<>();
        lastEmitNanos = System.nanoTime();
        emitBatchEvent(event);
    }

    /**
     * Emits the guaranteed final batch, called once when the writer loop exits (the store was
     * closed and the queue is drained). Covers all lines not covered by earlier batches; if every
     * line was already covered, the final batch is emitted anyway as the terminal signal, as an
     * empty range ({@code fromLine > toLine}, {@code toLine} = the last written line). A store
     * that never recorded anything emits nothing.
     */
    private void emitFinalBatch() {
        if (pendingFromLine > 0) {
            emitPendingBatch(true);
        } else if (lastWrittenLine > 0) {
            emitBatchEvent(new ProgressBatchEvent(this,
                    (int) lastWrittenLine + 1, (int) lastWrittenLine, Set.of(), true));
        }
    }

    /**
     * Returns the coalesce interval in nanoseconds.
     *
     * @return the interval in nanoseconds
     */
    private long coalesceIntervalNanos() {
        return getCoalesceIntervalMillis() * 1_000_000L;
    }

    private void ensureSink() throws IOException {
        if (sink == null) {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            // Appending: start counting bytes at the file's current size so checkpoints stay
            // byte-exact even if a store writes into a pre-existing file
            writtenBytes = Files.exists(file) ? Files.size(file) : 0;
            sink = new BufferedOutputStream(
                    Files.newOutputStream(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    FLUSH_BUFFER_BYTES);
        }
    }

    private void flushSink() {
        lastFlushNanos = System.nanoTime();
        if (sink == null || sinkBroken || recordsSinceFlush == 0) {
            return;
        }
        try {
            sink.flush();
        } catch (IOException e) {
            System.err.println("Failed to flush progress records to " + file);
            e.printStackTrace();
            sinkBroken = true;
        }
        recordsSinceFlush = 0;
    }

    private void closeSink() {
        if (sink != null) {
            try {
                sink.close();
            } catch (IOException e) {
                System.err.println("Failed to close progress log file " + file);
                e.printStackTrace();
            }
            sink = null;
        }
    }

    private record QueuedRecord(long seq, JIPipeProgressRecord record) {
    }

    /**
     * An immutable snapshot of the line→byte-offset checkpoint index. Entries are strictly
     * increasing in both line number and byte offset. The writer thread replaces the published
     * snapshot on every checkpoint (once per {@link #CHECKPOINT_INTERVAL_LINES} lines); readers
     * take one volatile read and use the snapshot without further synchronization, so they never
     * observe a partially built index and never block the writer.
     */
    static final class CheckpointIndex {

        static final CheckpointIndex EMPTY = new CheckpointIndex(new long[0], new long[0]);

        private final long[] lines;
        private final long[] offsets;

        private CheckpointIndex(long[] lines, long[] offsets) {
            this.lines = lines;
            this.offsets = offsets;
        }

        /**
         * Returns the number of checkpoints.
         */
        int size() {
            return lines.length;
        }

        /**
         * Returns the line number of the checkpoint at {@code index}.
         */
        long lineAt(int index) {
            return lines[index];
        }

        /**
         * Returns the byte offset where the line of the checkpoint at {@code index} starts.
         */
        long offsetAt(int index) {
            return offsets[index];
        }

        /**
         * Returns the index of the greatest checkpoint whose line is {@code <= line}, or
         * {@code -1} if no checkpoint covers the line.
         */
        int floor(long line) {
            int low = 0;
            int high = lines.length - 1;
            int result = -1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                if (lines[mid] <= line) {
                    result = mid;
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
            return result;
        }

        /**
         * Returns a snapshot with one more checkpoint. The arrays are copied (append-only
         * immutable snapshots) — one copy per {@link #CHECKPOINT_INTERVAL_LINES} records is
         * negligible.
         *
         * @param line   the 1-based line number the new checkpoint covers
         * @param offset the byte offset where this line starts
         * @return the new snapshot
         */
        CheckpointIndex withEntry(long line, long offset) {
            long[] newLines = Arrays.copyOf(lines, lines.length + 1);
            long[] newOffsets = Arrays.copyOf(offsets, offsets.length + 1);
            newLines[lines.length] = line;
            newOffsets[offsets.length] = offset;
            return new CheckpointIndex(newLines, newOffsets);
        }
    }
}
