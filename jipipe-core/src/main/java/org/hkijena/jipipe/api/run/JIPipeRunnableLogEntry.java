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

package org.hkijena.jipipe.api.run;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.project.JIPipeProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * A finished run inside the Tools &gt; Logs collection. Entries hold a reference to the run's
 * NDJSON data file ({@code <run-uuid>.ndjson}; see {@link JIPipeProgressLog}) plus run metadata —
 * never a rendered copy of the log text. The log is rendered from the file on demand
 * ({@link #renderText(Consumer)}), so neither the heap nor the collection scales with the log
 * volume; deleting the file (eviction, clear, project close) is what bounds the disk usage.
 * <p>
 * Runs on non-persistent stores ({@link JIPipeProgressLog#silent()}/{@link JIPipeProgressLog#stdout()})
 * have no file — those entries hold the store itself and render its bounded in-memory tail while
 * the store is alive.
 */
public class JIPipeRunnableLogEntry {

    /**
     * The single line rendered when the log data is unavailable (no data file and no store, the
     * file was removed — e.g. evicted or cleaned up with the project's temporary directory — or
     * the render found no renderable records at any level).
     */
    public static final String DATA_UNAVAILABLE_LINE = "[log data unavailable — removed]";

    /**
     * Builds the single line rendered when the log data exists but renders nothing at the
     * requested level (e.g. a DEBUG-only run viewed at INFO): the hint names the probed record
     * count and the level.
     *
     * @param count    the probed count (rendered-line count of the lowest-level probe render)
     * @param minLevel the requested minimum level
     * @return the hint line
     */
    public static String belowLevelLine(int count, JIPipeLogLevel minLevel) {
        return "[" + count + " records below " + minLevel.name() + " level — enable debug in the progress panel]";
    }

    private final String name;
    private final LocalDateTime dateTime;
    private final Path dataFile;
    private final JIPipeProgressLog store;
    private final JIPipeNotificationInbox notifications;
    private final boolean success;
    private JIPipeProject project;
    private boolean read = false;

    /**
     * Creates an entry for a finished run. The run's store must already be finished and closed
     * (the worker does this in {@code done()}), so the NDJSON file is complete and renders of it
     * are stable.
     *
     * @param run     the finished run
     * @param success whether the run succeeded
     */
    public JIPipeRunnableLogEntry(JIPipeRunnable run, boolean success) {
        this(run.getProgressInfo(), run.getTaskLabel(), success);
    }

    /**
     * Creates an entry from a progress handle. Used for runs whose label differs from the task
     * label and for pushes that have no {@link JIPipeRunnable} (e.g. the service initialization).
     *
     * @param progressInfo the finished run's (root) progress handle
     * @param name         the displayed entry name
     * @param success      whether the run succeeded
     */
    public JIPipeRunnableLogEntry(JIPipeProgressInfo progressInfo, String name, boolean success) {
        this.name = name;
        this.dateTime = LocalDateTime.now();
        this.notifications = new JIPipeNotificationInbox(progressInfo.getNotifications());
        this.success = success;
        JIPipeProgressLog progressStore = progressInfo.getStore();
        this.dataFile = progressStore != null ? progressStore.getFile() : null;
        // File-backed stores are finished and closed before an entry is created — the NDJSON file
        // is the durable artifact, so only the file is referenced. Non-persistent stores
        // (silent/stdout) keep their records only in the bounded in-memory tail, so the entry
        // holds the store itself to stay renderable.
        this.store = progressStore != null && dataFile == null ? progressStore : null;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    /**
     * Returns the NDJSON data file holding this entry's log.
     *
     * @return the data file, or {@code null} if the run's store wrote no file (silent/stdout)
     */
    public Path getPath() {
        return dataFile;
    }

    /**
     * Returns the project that owns this entry's run, if any.
     *
     * @return the project, or {@code null} for global (non-graph) runs
     */
    public JIPipeProject getProject() {
        return project;
    }

    /**
     * Tags this entry with the project that owns its run. Set by the log collection at push time.
     *
     * @param project the project, or {@code null} for global runs
     */
    public void setProject(JIPipeProject project) {
        this.project = project;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getName() {
        return name;
    }

    public JIPipeNotificationInbox getNotifications() {
        return notifications;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    /**
     * Renders this entry's log as compatibility text lines at INFO level (the shape behind
     * {@code log.txt} and the former {@code getLog()} view), handing each line to the sink.
     * The data is read on demand — from the NDJSON file, or from the store's tail for
     * non-persistent stores.
     * <p>
     * This never throws and always produces at least one line: unavailable data (missing file,
     * empty store, ...) renders the single {@link #DATA_UNAVAILABLE_LINE} placeholder.
     *
     * @param lineSink the consumer of the rendered lines
     */
    public void renderText(Consumer<String> lineSink) {
        renderText(JIPipeLogLevel.INFO, lineSink);
    }

    /**
     * Renders this entry's log as compatibility text lines, handing each line to the sink.
     * See {@link #renderText(Consumer)} for the data sources and the placeholder contract.
     *
     * @param minLevel the minimum level of log records to render
     * @param lineSink the consumer of the rendered lines
     */
    public void renderText(JIPipeLogLevel minLevel, Consumer<String> lineSink) {
        int[] lines = {0};
        Consumer<String> countingSink = line -> {
            lines[0]++;
            lineSink.accept(line);
        };
        if (dataFile != null) {
            JIPipeProgressLog.renderFile(dataFile, minLevel, countingSink);
        } else if (store != null) {
            store.renderText(minLevel, countingSink);
        }
        if (lines[0] == 0) {
            // Distinguish "no data at all" from "data exists but renders nothing at the requested
            // level" (e.g. a DEBUG-only run viewed at INFO): probe once at the lowest level and
            // count what renders there
            int[] probe = {0};
            if (dataFile != null) {
                JIPipeProgressLog.renderFile(dataFile, JIPipeLogLevel.DEBUG, line -> probe[0]++);
            } else if (store != null) {
                store.renderText(JIPipeLogLevel.DEBUG, line -> probe[0]++);
            }
            if (probe[0] > 0) {
                lineSink.accept(belowLevelLine(probe[0], minLevel));
            } else {
                lineSink.accept(DATA_UNAVAILABLE_LINE);
            }
        }
    }

    /**
     * Deletes this entry's NDJSON data file (if any). Called by the log collection when the entry
     * is evicted, the collection is cleared, or the owning project closes — this is what bounds
     * the disk usage of kept logs. Never throws; a file that cannot be deleted (e.g. locked by a
     * viewer on Windows) is reported and left in place (temporary-directory cleanup removes it
     * later).
     */
    public void deleteDataFile() {
        if (dataFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(dataFile);
        } catch (IOException e) {
            System.err.println("Failed to delete progress log data file " + dataFile);
            e.printStackTrace();
        }
    }
}
