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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

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
 */
public class FileJIPipeProgressLog extends AbstractJIPipeProgressLog {

    private static final int FLUSH_BUFFER_CHARS = 64 * 1024;
    private static final long FLUSH_INTERVAL_NANOS = 250 * 1_000_000L;
    private static final int READ_BUFFER_CHARS = 64 * 1024;

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

    // Writer-thread state only
    private BufferedWriter sink;
    private long lastFlushNanos;
    private long recordsSinceFlush;
    private boolean sinkBroken;

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
        LockSupport.unpark(writerThread);
        try {
            writerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

        @Override
    protected void writeRecord(JIPipeProgressRecord record) {
        try {
            sink.write(JsonUtils.toJsonString(record));
            sink.write('\n');
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
            int toSkip = Math.max(fromLine, 1) - 1;
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

    /**
     * Maps one parsed record line to a record by its {@code t} discriminator. The field serializes
     * last in the line, so lines must be parsed with Jackson — never by string splitting.
     *
     * @param mapper the object mapper
     * @param node   the parsed line
     * @return the record, or {@code null} if the record type is unknown or the line is malformed
     */
    private static JIPipeProgressRecord parseRecord(ObjectMapper mapper, JsonNode node) {
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
        while (true) {
            drainQueue();
            if (isClosed() && queue.isEmpty()) {
                break;
            }
            if (recordsSinceFlush > 0 && System.nanoTime() - lastFlushNanos >= FLUSH_INTERVAL_NANOS) {
                flushSink();
            }
            writerWaiting = true;
            boolean workPending = !queue.isEmpty() || isClosed();
            if (!workPending) {
                if (recordsSinceFlush > 0) {
                    // Wake in time for the 250 ms flush
                    long remaining = FLUSH_INTERVAL_NANOS - (System.nanoTime() - lastFlushNanos);
                    LockSupport.parkNanos(Math.max(1_000_000L, remaining));
                } else {
                    LockSupport.park();
                }
            }
            writerWaiting = false;
        }
        flushSink();
        closeSink();
    }

    private void drainQueue() {
        QueuedRecord entry;
        while ((entry = queue.poll()) != null) {
            queuedRecords.decrementAndGet();
            try {
                applyRecord(entry.seq(), entry.record());
                if (!sinkBroken) {
                    ensureSink();
                    writeRecord(entry.record());
                    recordsSinceFlush++;
                }
            } catch (Throwable e) {
                // The writer must survive a broken sink or a bad record
                System.err.println("Failed to write progress record; remaining records are kept in memory only");
                e.printStackTrace();
                sinkBroken = true;
            }
        }
    }

    private void ensureSink() throws IOException {
        if (sink == null) {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            sink = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                    StandardCharsets.UTF_8), FLUSH_BUFFER_CHARS);
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
}
