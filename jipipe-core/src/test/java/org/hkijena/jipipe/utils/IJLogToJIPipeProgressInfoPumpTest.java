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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the incremental log-reading contract of {@link IJLogToJIPipeProgressInfoPump} (fix for
 * #1361): each poll reads only the lines appended since the previous poll (never the whole panel),
 * a cleared panel restarts the capture without duplicating or losing lines, a poll whose read
 * fails mid-pass is discarded entirely, and the per-poll cap delays but never loses lines.
 * <p>
 * The tests drive the pump through the {@link IJLogToJIPipeProgressInfoPump.LogView} seam because
 * the real ij.text.TextPanel cannot be constructed in headless test JVMs (its Scrollbar throws a
 * HeadlessException). The seam mirrors the panel's read contract: {@code line(i)} throws when the
 * index is out of range, which is what a concurrently executed {@code \Clear} produces.
 */
class IJLogToJIPipeProgressInfoPumpTest {

    /**
     * Records every pumped line without touching a store.
     */
    private static final class RecordingTarget extends JIPipeProgressInfo {
        private final List<String> lines = new ArrayList<>();
        private boolean cancelled;

        @Override
        public void log(String message) {
            lines.add(message);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * In-memory stand-in for the log panel: same read contract, including out-of-range throws.
     */
    private static final class FakeLogView implements IJLogToJIPipeProgressInfoPump.LogView {
        private final List<String> lines = new ArrayList<>();
        private int failNextReadOfIndex = -1;

        @Override
        public int lineCount() {
            return lines.size();
        }

        @Override
        public String line(int index) {
            if (index == failNextReadOfIndex) {
                failNextReadOfIndex = -1;
                throw new IllegalStateException("simulated clear racing the read");
            }
            return lines.get(index);
        }
    }

    @Test
    void capturesOnlyNewLinesAcrossPolls() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        append(view, 0, 3);
        pump.pollOnce();
        assertEquals(List.of("line-0", "line-1", "line-2"), target.lines);

        append(view, 3, 5);
        pump.pollOnce();
        assertEquals(List.of("line-0", "line-1", "line-2", "line-3", "line-4"), target.lines);
    }

    @Test
    void ignoresLinesThatExistedBeforeAttach() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        append(view, 0, 2);
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        pump.pollOnce();
        assertTrue(target.lines.isEmpty(), "pre-existing lines must not be re-captured");

        append(view, 2, 3);
        pump.pollOnce();
        assertEquals(List.of("line-2"), target.lines);
    }

    @Test
    void clearWithRefillRestartsCaptureWithoutDuplicationOrLoss() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        append(view, 0, 2);
        pump.pollOnce();
        assertEquals(List.of("line-0", "line-1"), target.lines);

        // \Clear + enough refill that the count reaches the old cursor again: only the anchor
        // lines can detect this (a pure count-shrink check would miss it)
        view.lines.clear();
        view.lines.add("cleared-0");
        view.lines.add("cleared-1");

        pump.pollOnce();
        assertEquals(List.of("line-0", "line-1", "cleared-0", "cleared-1"), target.lines);
    }

    @Test
    void clearWithoutRefillIsDetectedByCountShrink() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        append(view, 0, 2);
        pump.pollOnce();
        assertEquals(List.of("line-0", "line-1"), target.lines);

        view.lines.clear();
        pump.pollOnce();
        assertEquals(List.of("line-0", "line-1"), target.lines, "nothing new yet");

        view.lines.add("cleared-0");
        pump.pollOnce();
        assertEquals(List.of("line-0", "line-1", "cleared-0"), target.lines);
    }

    @Test
    void abortedPassEmitsNothingAndLosesNothing() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        append(view, 0, 2);
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        append(view, 2, 5);
        view.failNextReadOfIndex = 3;
        pump.pollOnce();
        assertTrue(target.lines.isEmpty(), "a pass whose read fails must not emit partially");

        pump.pollOnce();
        assertEquals(List.of("line-2", "line-3", "line-4"), target.lines);
    }

    @Test
    void perPollCapDelaysButNeverLoses() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        int total = 20_005;
        append(view, 0, total);

        pump.pollOnce();
        assertTrue(target.lines.size() < total, "a single poll must be capped");
        assertTrue(target.lines.size() > 0);

        pump.pollOnce();
        assertEquals(total, target.lines.size(), "the backlog must be delivered completely");
        assertEquals(total, new HashSet<>(target.lines).size(), "no duplicates");
        assertEquals("line-" + (total - 1), target.lines.get(total - 1));
    }

    @Test
    void closeDrainsRemainingLines() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        append(view, 0, 1);
        pump.pollOnce();
        assertEquals(List.of("line-0"), target.lines);

        append(view, 1, 3);
        pump.close();
        assertEquals(List.of("line-0", "line-1", "line-2"), target.lines,
                "close must drain what the polls have not read yet");
    }

    @Test
    void closeDrainsBacklogsLargerThanThePollCap() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        // A fast algorithm can finish (and close its pump) before the first poll fires, leaving a
        // backlog larger than one poll's cap on the panel
        int total = 45_000;
        append(view, 0, total);
        pump.close();

        assertEquals(total, target.lines.size(),
                "close must drain the whole backlog, not only one capped pass");
        assertEquals(total, new HashSet<>(target.lines).size(), "no duplicates");
        assertEquals("line-" + (total - 1), target.lines.get(total - 1));
    }

    @Test
    void closedPumpStopsCapturing() {
        RecordingTarget target = new RecordingTarget();
        FakeLogView view = new FakeLogView();
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        append(view, 0, 1);
        pump.pollOnce();
        pump.close();

        append(view, 1, 2);
        pump.pollOnce();
        assertEquals(List.of("line-0"), target.lines, "a closed pump must not capture further");
    }

    @Test
    void cancelledTargetStopsCapturing() {
        RecordingTarget target = new RecordingTarget();
        target.cancelled = true;
        FakeLogView view = new FakeLogView();
        IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(target, view);

        append(view, 0, 2);
        pump.pollOnce();
        assertTrue(target.lines.isEmpty(), "a cancelled target must stop the pump (old behavior)");
    }

    private static void append(FakeLogView view, int from, int to) {
        for (int i = from; i < to; i++) {
            view.lines.add("line-" + i);
        }
    }
}
