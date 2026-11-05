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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNode;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeKind;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link JIPipeProgressInfo} facade over the progress store: the facade-to-record
 * mapping, the log view, the percentage throttle, and detached progress counters.
 */
class ProgressInfoFacadeTest {

    @TempDir
    Path tmp;

    @Test
    void facadeMapsToStoreRecords() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.log("hello");
        JIPipeProgressInfo child = info.resolve("Node A");
        child.log("sub");
        child.percentage("P").logPercentage(50, 100);
        child.log(new RuntimeException("boom"));

        JIPipeProgressLog store = info.getStore();
        // Node structure from the index (silent stores keep no n records in the tail)
        JIPipeProgressNode childNode = store.getNode(2);
        assertNotNull(childNode);
        assertEquals("Node A", childNode.getLabel());
        assertEquals(JIPipeProgressNodeKind.ITEM, childNode.getKind());
        assertEquals(1, childNode.getParentId());
        JIPipeProgressNode percentageNode = store.getNode(3);
        assertNotNull(percentageNode);
        assertEquals("P", percentageNode.getLabel());
        assertEquals(2, percentageNode.getParentId());

        // Records from the tail (renderable records only)
        List<JIPipeProgressRecord> records = store.readRecords(1, 16);
        JIPipeProgressRecord.Log hello = assertLog(records, "hello");
        assertEquals(1, hello.id());
        assertEquals(JIPipeLogLevel.INFO.value(), hello.lv());
        JIPipeProgressRecord.Log sub = assertLog(records, "sub");
        assertEquals(2, sub.id());
        JIPipeProgressRecord.Progress progress = records.stream()
                .filter(JIPipeProgressRecord.Progress.class::isInstance)
                .map(JIPipeProgressRecord.Progress.class::cast)
                .findFirst().orElse(null);
        assertNotNull(progress, "progress record expected");
        assertEquals(3, progress.id());
        assertEquals(50, progress.v());
        assertEquals(100, progress.mx());
        JIPipeProgressRecord.Error error = records.stream()
                .filter(JIPipeProgressRecord.Error.class::isInstance)
                .map(JIPipeProgressRecord.Error.class::cast)
                .findFirst().orElse(null);
        assertNotNull(error, "error record expected");
        assertEquals(2, error.id());
        assertEquals("java.lang.RuntimeException", error.c());
        assertEquals("boom", error.m());
    }

    private static JIPipeProgressRecord.Log assertLog(List<JIPipeProgressRecord> records, String message) {
        return records.stream()
                .filter(JIPipeProgressRecord.Log.class::isInstance)
                .map(JIPipeProgressRecord.Log.class::cast)
                .filter(record -> message.equals(record.m()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No log record with message: " + message));
    }

    @Test
    void getLogViewRendersTail() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.log("visible");
        assertTrue(info.getLog().toString().contains("visible"));
    }

    @Test
    void defaultConstructorCreatesSilentStoreWithoutFile() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.log("x");
        assertNull(info.getStore().getFile());
    }

    @Test
    void debugAndExplicitLevelMapToLevelRecords() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.debug("quiet");
        info.log(JIPipeLogLevel.WARN, "loud");
        List<JIPipeProgressRecord> records = info.getStore().readRecords(1, 16);
        assertEquals(JIPipeLogLevel.DEBUG.value(), assertLog(records, "quiet").lv());
        assertEquals(JIPipeLogLevel.WARN.value(), assertLog(records, "loud").lv());
    }

    @Test
    void resolveAndLogCreatesNodeWithoutExtraRecord() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        JIPipeProgressInfo child = info.resolveAndLog("Step");
        assertEquals("Step", info.getStore().getNode(2).getLabel());
        // The node row is the rendered line: no log record was emitted for the resolve
        List<JIPipeProgressRecord> records = info.getStore().readRecords(1, 16);
        assertEquals(0, records.stream().filter(JIPipeProgressRecord.Log.class::isInstance).count());
        assertNotNull(child);
    }

    @Test
    void percentageThrottleEmitsProgressRecords() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        JIPipePercentageProgressInfo percentage = info.percentage("P");
        // First report always emits (lastTime starts at 0); identical percentages never emit twice
        percentage.logPercentage(12.34, 100);
        percentage.logPercentage(12.34, 100);
        List<JIPipeProgressRecord.Progress> reported = info.getStore().readRecords(1, 64).stream()
                .filter(JIPipeProgressRecord.Progress.class::isInstance)
                .map(JIPipeProgressRecord.Progress.class::cast)
                .toList();
        assertEquals(1, reported.size());
        assertEquals(12, reported.get(0).v());
        // The percentage is reported as value with maximum 100
        assertEquals(100, reported.get(0).mx());
        assertEquals(100, info.getStore().getNode(2).getMaxProgress());
    }

    @Test
    void percentageDefaultIntervalOnlyReports50And100WithinInterval() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        JIPipePercentageProgressInfo percentage = info.percentage("P");
        percentage.logPercentage(10, 100);   // first report: always emitted
        percentage.logPercentage(30, 100);   // within the 1 s interval: suppressed
        percentage.logPercentage(50, 100);   // 50 % is always reported
        percentage.logPercentage(75, 100);   // suppressed
        percentage.logPercentage(100, 100);  // 100 % is always reported
        List<Integer> reported = info.getStore().readRecords(1, 64).stream()
                .filter(JIPipeProgressRecord.Progress.class::isInstance)
                .map(JIPipeProgressRecord.Progress.class::cast)
                .map(JIPipeProgressRecord.Progress::v)
                .toList();
        assertEquals(List.of(10, 50, 100), reported);
    }

    @Test
    void detachProgressCreatesChildNodeWithIndependentCounters() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.setProgress(5, 10);
        JIPipeProgressInfo detached = info.detachProgress();
        assertTrue(detached.isDetachedProgress());
        assertEquals(5, detached.getProgress());
        assertEquals(10, detached.getMaxProgress());
        // Independent counters: the detached handle's updates do not affect the parent
        detached.setProgress(1, 2);
        assertEquals(1, detached.getProgress());
        assertEquals(2, detached.getMaxProgress());
        assertEquals(5, info.getProgress());
        assertEquals(10, info.getMaxProgress());
        // Detached progress is a child node
        assertEquals(1, info.getStore().getNode(2).getParentId());
        // Cancellation is still shared (same contract as the pre-store facade)
        detached.cancel("stop");
        assertTrue(info.isCancelled());
        assertEquals("stop", detached.getCancelReason());
    }

    @Test
    void progressCountersEmitProgressRecords() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.setMaxProgress(4);
        info.incrementProgress();
        info.incrementProgress();
        info.addMaxProgress(2);
        info.setProgress(3, 8);
        List<JIPipeProgressRecord.Progress> progress = info.getStore().readRecords(1, 64).stream()
                .filter(JIPipeProgressRecord.Progress.class::isInstance)
                .map(JIPipeProgressRecord.Progress.class::cast)
                .toList();
        // setMaxProgress(4), increment, increment, addMaxProgress(2), setProgress(3, 8)
        assertEquals(5, progress.size());
        assertEquals(0, progress.get(0).v());
        assertEquals(4, progress.get(0).mx());
        assertEquals(1, progress.get(1).v());
        assertEquals(4, progress.get(1).mx());
        assertEquals(2, progress.get(2).v());
        assertEquals(4, progress.get(2).mx());
        assertEquals(2, progress.get(3).v());
        assertEquals(6, progress.get(3).mx());
        assertEquals(3, progress.get(4).v());
        assertEquals(8, progress.get(4).mx());
        assertEquals(3, info.getProgress());
        assertEquals(8, info.getMaxProgress());
    }

    @Test
    void clearLogClearsCachedViewOnly() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.log("before");
        assertNotNull(info.getLog().toString());
        info.clearLog();
        info.log("after");
        // The cached view was dropped; the next view is re-rendered from the tail
        assertTrue(info.getLog().toString().contains("after"));
        // The store itself is untouched (the record still exists)
        assertLog(info.getStore().readRecords(1, 16), "after");
    }

    @Test
    void setStorageBaseSwitchesToAFileBackedStore() throws Exception {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.setStorageBase(tmp);
        info.log("stored");
        Path file = info.getStore().getFile();
        assertNotNull(file, "file-backed store expected after setStorageBase");
        assertTrue(file.toString().endsWith(".ndjson"));
        assertTrue(file.startsWith(tmp.resolve("logs")));
        // The file is created lazily by the writer thread on the first flushed record
        long deadline = System.currentTimeMillis() + 10_000;
        while (!Files.exists(file) && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(Files.exists(file), "the store file should be created after the first record");
        info.getStore().close();
    }

    @Test
    void notificationsAreRecordedAsWRecords() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.getNotifications().push(new JIPipeNotification("Heading", "Description"));
        JIPipeProgressRecord.Notification notification = info.getStore().readRecords(1, 16).stream()
                .filter(JIPipeProgressRecord.Notification.class::isInstance)
                .map(JIPipeProgressRecord.Notification.class::cast)
                .findFirst().orElse(null);
        assertNotNull(notification, "notification record expected");
        assertEquals("Heading", notification.h());
        assertEquals("Description", notification.d());
    }

    @Test
    void silentAndStdoutUseTheStoreImplementations() {
        assertNull(JIPipeProgressInfo.SILENT.getStore().getFile());
        assertNull(JIPipeProgressInfo.STDOUT.getStore().getFile());
        assertEquals(1, JIPipeProgressInfo.SILENT.getNodeId());
    }

    @Test
    void deprecatedStatusUpdatedTrioIsUnfed() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        int[] events = {0};
        JIPipeProgressInfo.StatusUpdatedEventListener listener = event -> events[0]++;
        // The trio still exists as standalone types; the facade no longer exposes an emitter
        JIPipeProgressInfo.StatusUpdatedEventEmitter emitter = new JIPipeProgressInfo.StatusUpdatedEventEmitter();
        emitter.subscribe(listener);
        info.log("nothing emitted");
        info.setProgress(1, 2);
        assertEquals(0, events[0]);
        assertFalse(info.getLog().toString().isEmpty());
    }
}
