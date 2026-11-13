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

package org.hkijena.jipipe.utils;

import ij.IJ;
import ij.text.TextPanel;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Takes control of the log in {@link ij.IJ} and pumps its messages into a {@link JIPipeProgressInfo}.
 * <p>
 * Reading strategy (fix for #1361): the pump reads the log panel <b>incrementally</b>. One poll per
 * interval reads only the lines appended since the previous poll (a line cursor over
 * {@link TextPanel#getLineCount()} and {@link TextPanel#getLine(int)}) - never the whole text. Polls
 * run on a shared daemon executor thread, never on the EDT: the pump does no UI work, and the previous
 * Swing-timer implementation copied the ENTIRE log as one string every 250 ms on the EDT. ImageJ never
 * truncates that log, so the per-tick cost grew linearly with the run's total ImageJ log volume and
 * saturated the EDT for chatty ImageJ code (one pump per executing algorithm, multiplied by parallel
 * nodes).
 * <p>
 * Clear handling: ImageJ's {@code \Clear} command empties the panel, so line indices restart at zero,
 * and a clear can also race with an in-flight poll. A clear is detected when the line count shrinks
 * below the cursor, or - if the panel was already refilled up to the old cursor - when two anchor
 * lines (the last read line and the line at half the cursor) no longer match their remembered
 * content. A poll whose line read fails mid-pass (the clear race) is discarded entirely and retried
 * on the next poll, so lines are neither duplicated nor lost. Each poll reads at most
 * {@value #MAX_LINES_PER_PASS} lines; a larger backlog is delivered over multiple polls.
 * <p>
 * Lines modified in place after being read (ImageJ {@code \Update:} commands) are captured once, as
 * their first version. The previous implementation re-emitted every rewrite.
 */
public class IJLogToJIPipeProgressInfoPump implements Closeable, AutoCloseable {

    /**
     * One shared daemon thread serves all live pumps: each poll is O(new lines) and almost always
     * returns immediately, so a single thread never becomes a bottleneck.
     */
    private static final ScheduledExecutorService POLL_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "IJ-Log-Pump");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Maximum lines read per poll. A larger backlog (possible after the executor was stalled) is
     * delivered over subsequent polls instead of bursting.
     */
    static final int MAX_LINES_PER_PASS = 20_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    /**
     * Minimal read access to the log panel. Production binds this to the real ij.text.TextPanel;
     * tests bind an in-memory stand-in (the real panel cannot be constructed in headless JVMs).
     */
    interface LogView {

        /**
         * Returns the current number of lines.
         */
        int lineCount();

        /**
         * Returns the line at the given index. Throws a RuntimeException when the index is out of
         * range (which is what a concurrently executed {@code \Clear} produces).
         */
        String line(int index);
    }

    private final JIPipeProgressInfo target;
    private final LogView view;
    private final Window logWindow;
    private final ScheduledFuture<?> pollTask;
    private volatile boolean closed;

    /**
     * The line cursor: the index of the first line not yet pumped.
     */
    private int nextLine;
    /**
     * Clear-detection anchors: the content of line {@code nextLine-1} and line {@code nextLine/2}
     * as of the last poll. After a {@code \Clear} that was already refilled up to the old cursor,
     * both no longer match while the line count alone cannot detect the clear.
     */
    private String lastLineAnchor;
    private String midLineAnchor;

    /**
     * Creates a pump that forwards new {@code IJ.log()} output to the target until closed or until
     * the target is cancelled. Lines that already exist in the log are not forwarded.
     *
     * @param target the progress info that receives the log lines
     */
    public IJLogToJIPipeProgressInfoPump(JIPipeProgressInfo target) {
        this.target = target;
        IJ.log(""); // Inits the log
        TextPanel resolvedPanel;
        try {
            Field logPanelField = IJ.class.getDeclaredField("logPanel");
            logPanelField.setAccessible(true);
            resolvedPanel = (TextPanel) logPanelField.get(null);
            if (resolvedPanel == null) {
                target.log("Unable to hook into IJ.log(): Is null");
            }
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            target.log("Unable to hook into IJ.log(): " + e);
            resolvedPanel = null;
        }
        if (resolvedPanel == null) {
            // No panel to read: stay inert (same as the previous implementation)
            this.view = null;
            this.logWindow = null;
            this.pollTask = null;
            return;
        }
        final TextPanel panel = resolvedPanel;
        this.view = new LogView() {
            @Override
            public int lineCount() {
                return panel.getLineCount();
            }

            @Override
            public String line(int index) {
                return panel.getLine(index);
            }
        };
        this.logWindow = SwingUtilities.getWindowAncestor(panel);
        if (logWindow != null) {
            SwingUtilities.invokeLater(() -> logWindow.setVisible(false));
        }
        attachCursor();
        this.pollTask = POLL_EXECUTOR.scheduleWithFixedDelay(this::pollSafely,
                POLL_INTERVAL_MILLIS, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Testable core without ij.text.TextPanel (headless JVMs cannot construct one). Does not
     * schedule polling; tests drive {@link #pollOnce()} directly.
     */
    IJLogToJIPipeProgressInfoPump(JIPipeProgressInfo target, LogView view) {
        this.target = target;
        this.view = view;
        this.logWindow = null;
        attachCursor();
        this.pollTask = null;
    }

    /**
     * Initializes the line cursor at the panel's current end: lines that already exist are not
     * forwarded (same semantics as the previous implementation).
     */
    private void attachCursor() {
        nextLine = view.lineCount();
        refreshAnchors();
    }

    /**
     * The scheduled entry point: stops on close or cancellation, otherwise runs one poll.
     */
    private void pollSafely() {
        if (closed || target.isCancelled()) {
            cancelTask();
            return;
        }
        pollOnce();
    }

    /**
     * Runs one poll: reads the lines appended since the last poll and forwards them to the target.
     * Stops when the pump is closed or the target is cancelled. Package-private so tests can drive
     * polls deterministically.
     */
    void pollOnce() {
        if (closed || target.isCancelled() || view == null) {
            return;
        }
        List<String> lines = readPass();
        if (lines == null) {
            // The pass failed mid-read (a clear raced the poll): discard it entirely - the next
            // poll re-reads the same range, so nothing is duplicated or lost
            return;
        }
        for (String line : lines) {
            target.log(line);
        }
    }

    /**
     * Reads one bounded pass of new lines and advances the cursor. Returns {@code null} when the
     * pass must be discarded (the panel changed under the read).
     */
    private List<String> readPass() {
        int count;
        try {
            count = view.lineCount();
        } catch (RuntimeException e) {
            return null;
        }
        if (count < nextLine || (nextLine > 0 && clearDetected())) {
            // \Clear: the panel content was emptied and line indices restarted
            nextLine = 0;
            lastLineAnchor = null;
            midLineAnchor = null;
        }
        int end = Math.min(count, nextLine + MAX_LINES_PER_PASS);
        if (end <= nextLine) {
            refreshAnchors();
            return List.of();
        }
        List<String> lines = new ArrayList<>(end - nextLine);
        try {
            for (int i = nextLine; i < end; i++) {
                lines.add(view.line(i));
            }
        } catch (RuntimeException e) {
            return null;
        }
        nextLine = end;
        refreshAnchors();
        return lines;
    }

    /**
     * Detects a {@code \Clear} whose panel was already refilled up to the old cursor (the line
     * count alone cannot distinguish this from normal appends): both anchor lines no longer hold
     * their remembered content.
     */
    private boolean clearDetected() {
        if (lastLineAnchor == null || midLineAnchor == null) {
            return false;
        }
        return !Objects.equals(anchorAt(nextLine - 1), lastLineAnchor)
                && !Objects.equals(anchorAt(nextLine / 2), midLineAnchor);
    }

    private String anchorAt(int index) {
        try {
            return view.line(index);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void refreshAnchors() {
        lastLineAnchor = nextLine > 0 ? anchorAt(nextLine - 1) : null;
        midLineAnchor = nextLine > 0 ? anchorAt(nextLine / 2) : null;
    }

    /**
     * Stops polling and drains everything the polls have not read yet, synchronously on the caller
     * thread (the node's run thread). The drain loops over capped passes so a backlog larger than
     * one poll's cap - possible when the algorithm finished faster than the first poll - is
     * delivered completely. Safe to call multiple times; a closed pump stays inert. Keeps the
     * ImageJ log window hidden (it may have been re-opened by the user).
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        cancelTask();
        if (view != null) {
            // Null (a clear racing the read) and capped passes both retry; the run thread that
            // owned this pump is done producing, so the panel cannot grow faster than the drain
            int passes = 0;
            while (passes++ < 10_000) {
                List<String> lines = readPass();
                if (lines == null || lines.isEmpty()) {
                    if (lines == null) {
                        continue;
                    }
                    break;
                }
                for (String line : lines) {
                    target.log(line);
                }
            }
        }
        if (logWindow != null) {
            SwingUtilities.invokeLater(() -> logWindow.setVisible(false));
        }
    }

    private void cancelTask() {
        ScheduledFuture<?> task = pollTask;
        if (task != null) {
            task.cancel(false);
        }
    }
}
