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
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeState;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    @Test
    void resolveCarriesLocationIntoNodeRecord(@TempDir Path dir) {
        Path file = dir.resolve("facade-loc.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "facade-loc", "Facade locations");
        JIPipeProgressInfo info = new JIPipeProgressInfo(store);
        JIPipeProgressInfo tagged = info.resolve("Node A", "node://abc");
        JIPipeProgressInfo plain = tagged.resolve("Plain child");
        store.finish(JIPipeProgressNodeState.OK);

        // The index carries the location; resolve() without a location leaves it null
        assertEquals("node://abc", store.getNode(tagged.getNodeId()).getLocation());
        assertNull(store.getNode(plain.getNodeId()).getLocation());

        // The child node's n record carries the loc field (file-backed stores read it back)
        JIPipeProgressRecord.Node nodeRecord = store.readRecords(1, 16).stream()
                .filter(JIPipeProgressRecord.Node.class::isInstance)
                .map(JIPipeProgressRecord.Node.class::cast)
                .filter(record -> "Node A".equals(record.l()))
                .findFirst().orElse(null);
        assertNotNull(nodeRecord, "n record of the tagged node expected");
        assertEquals("node://abc", nodeRecord.loc());
        JIPipeProgressRecord.Node plainRecord = store.readRecords(1, 16).stream()
                .filter(JIPipeProgressRecord.Node.class::isInstance)
                .map(JIPipeProgressRecord.Node.class::cast)
                .filter(record -> "Plain child".equals(record.l()))
                .findFirst().orElse(null);
        assertNotNull(plainRecord, "n record of the untagged node expected");
        assertNull(plainRecord.loc());
    }

    private static JIPipeProgressRecord.Log assertLog(List<JIPipeProgressRecord> records, String message) {
        return records.stream()
                .filter(JIPipeProgressRecord.Log.class::isInstance)
                .map(JIPipeProgressRecord.Log.class::cast)
                .filter(record -> message.equals(record.m()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No log record with message: " + message));    }

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

    /**
     * Each percentage report emits the compatibility percentage line (an INFO log record with the
     * percentage as message, R15) and the progress record — exactly one of each per threshold
     * pass, the log record before the progress record (its rendered prefix shows the progress as
     * of before the report). The throttle is respected: a repeated report re-emits nothing.
     */
    @Test
    void logPercentageEmitsCompatLogLineAndProgressRecord() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        JIPipePercentageProgressInfo percentage = info.percentage("P");
        percentage.logPercentage(50, 100);

        JIPipeProgressLog store = info.getStore();
        List<JIPipeProgressRecord> records = store.readRecords(1, 64);
        List<JIPipeProgressRecord.Progress> progress = records.stream()
                .filter(JIPipeProgressRecord.Progress.class::isInstance)
                .map(JIPipeProgressRecord.Progress.class::cast)
                .toList();
        List<JIPipeProgressRecord.Log> logs = records.stream()
                .filter(JIPipeProgressRecord.Log.class::isInstance)
                .map(JIPipeProgressRecord.Log.class::cast)
                .toList();
        assertEquals(1, progress.size());
        assertEquals(50, progress.get(0).v());
        assertEquals(100, progress.get(0).mx());
        assertEquals(1, logs.size());
        assertEquals("50%", logs.get(0).m());
        assertEquals(JIPipeLogLevel.INFO.value(), logs.get(0).lv());
        // Both records belong to the percentage child node; the log record comes first
        assertEquals(percentage.getNodeId(), logs.get(0).id());
        assertEquals(percentage.getNodeId(), progress.get(0).id());
        assertTrue(records.indexOf(logs.get(0)) < records.indexOf(progress.get(0)),
                "the compat line must be emitted before the progress record");

        // Rendered: the percentage line comes from the log record (the progress record renders nothing)
        assertEquals("[1/1] P | 50%\n", info.getLog().toString());

        // Throttle: a repeated 50 % report re-emits nothing
        percentage.logPercentage(50, 100);
        records = store.readRecords(1, 64);
        assertEquals(1, records.stream().filter(JIPipeProgressRecord.Progress.class::isInstance).count());
        assertEquals(1, records.stream().filter(JIPipeProgressRecord.Log.class::isInstance).count());
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

    /**
     * A detached progress node has an empty label and must contribute no chain segment: its
     * records render without a leading or doubled {@code |} separator (store renderer path).
     */
    @Test
    void detachedChildWithEmptyLabelSkipsEmptyChainSegment() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        JIPipeProgressInfo child = info.resolve("Node A");
        child.setProgress(3, 10);
        JIPipeProgressInfo detached = child.detachProgress();
        detached.log("from detached");
        // Non-persistent store: getLog renders via the store's text renderer
        assertEquals("[3/10] Node A | from detached\n", info.getLog().toString());
    }

    /**
     * Same chain contract on the facade mirror: a file-backed store's log view renders the
     * detached (empty-label) node's records without an empty chain segment.
     */
    @Test
    void detachedChildWithEmptyLabelSkipsEmptyChainSegmentInFileStoreView() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.setStorageBase(tmp, "chain-skip-run", "Pipeline run");
        JIPipeProgressInfo child = info.resolve("Node A");
        child.setProgress(3, 10);
        JIPipeProgressInfo detached = child.detachProgress();
        detached.log("from detached");
        info.getStore().finish(JIPipeProgressNodeState.OK);
        // File-backed store: getLog renders from the tail via the facade mirror
        assertEquals("[3/10] Node A | from detached\n", info.getLog().toString());
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
    void setStorageBaseWithExplicitIdsAdoptsStoreAndRerootsHandle() {
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.log("before");
        info.setStorageBase(tmp, "0f75f2a6-6c05-4a10-a9ef-2f7e21a3f5de", "Pipeline run");

        // The handle is re-rooted onto the new store's root node
        assertEquals(info.getStore().getRootNodeId(), info.getNodeId(), "handle must be re-rooted");

        // Records submitted through the handle go to the new store; the re-root makes
        // resolve() work (a foreign node id would make beginNode throw)
        JIPipeProgressInfo child = info.resolve("Child");
        child.log("after");
        JIPipeProgressLog store = info.getStore();
        store.finish(JIPipeProgressNodeState.OK);
        List<JIPipeProgressRecord> records = store.readRecords(1, 64);
        assertFalse(records.stream().anyMatch(r -> r instanceof JIPipeProgressRecord.Log log && log.m().equals("before")),
                "records submitted before the swap stay in the previous store");
        assertTrue(records.stream().anyMatch(r -> r instanceof JIPipeProgressRecord.Log log && log.m().equals("after")),
                "records submitted after the swap go to the new store");

        // The explicit run UUID names the file and lands in the head record together with the label
        Path file = store.getFile();
        assertNotNull(file, "file-backed store expected");
        assertEquals("0f75f2a6-6c05-4a10-a9ef-2f7e21a3f5de.ndjson", file.getFileName().toString());
        assertTrue(file.startsWith(tmp.resolve("logs")));
        JIPipeProgressRecord.Head head = (JIPipeProgressRecord.Head) store.readRecords(1, 1).getFirst();
        assertEquals("0f75f2a6-6c05-4a10-a9ef-2f7e21a3f5de", head.run());
        assertEquals("Pipeline run", head.label());
        store.close();
    }

    @Test
    void setStorageBaseReusesAFileBackedStoreForTheSameRun() throws Exception {
        // Ruling R17: a caller (e.g. the queue UI) may create the run's store before the run starts
        // and inject a handle on it — run()'s adoption path must reuse it (never a second file)
        Path logsBase = tmp.resolve("logs");
        JIPipeProgressLog runStore = JIPipeProgressLog.create(logsBase.resolve("f81d4fae-7dec-11d0-a765-00a0c91e6bf6.ndjson"),
                "f81d4fae-7dec-11d0-a765-00a0c91e6bf6", "Pipeline run");
        JIPipeProgressInfo info = new JIPipeProgressInfo(runStore);
        info.log("before run()");

        info.setStorageBase(tmp, "f81d4fae-7dec-11d0-a765-00a0c91e6bf6", "Pipeline run");

        assertSame(runStore, info.getStore(), "the run's store must be reused, not replaced");
        assertEquals(logsBase.resolve("f81d4fae-7dec-11d0-a765-00a0c91e6bf6.ndjson"), info.getStore().getFile());

        // The reused store is the active one: records still land, finish() still emits the status record
        info.log("during run()");
        info.getStore().finish(JIPipeProgressNodeState.OK);
        List<JIPipeProgressRecord> records = info.getStore().readRecords(1, 64);
        assertTrue(records.stream().anyMatch(r -> r instanceof JIPipeProgressRecord.Log log && log.m().equals("before run()")),
                "records submitted before the adoption must be kept when the store is reused");
        assertTrue(records.stream().anyMatch(r -> r instanceof JIPipeProgressRecord.Log log && log.m().equals("during run()")));
        assertEquals("finished", ((JIPipeProgressRecord.Status) records.get(records.size() - 1)).s());

        // Exactly one store file exists for the run
        try (Stream<Path> files = Files.list(logsBase)) {
            assertEquals(1, files.count(), "no second store file may be created for one run");
        }
    }

    @Test
    void setStorageBaseLeavesTheDisplacedStoreOpen() {
        // Documented semantics: a displaced store is not closed by the facade — it may still be
        // referenced by other handles (derived handles keep their store; an injected handle's store
        // is owned by the caller context that created it). Its owner finishes/closes it.
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        info.setStorageBase(tmp, "displaced-a", "First");
        JIPipeProgressLog displaced = info.getStore();
        info.log("on a");

        info.setStorageBase(tmp, "displaced-b", "Second");
        assertNotSame(displaced, info.getStore());

        // The displaced store is still open: finish() writes its status record (a closed store
        // would make finish() a no-op and the file would get no s record)
        displaced.finish(JIPipeProgressNodeState.OK);
        List<JIPipeProgressRecord> displacedRecords = displaced.readRecords(1, 64);
        assertTrue(displacedRecords.stream().anyMatch(r -> r instanceof JIPipeProgressRecord.Log log && log.m().equals("on a")));
        assertEquals("finished", ((JIPipeProgressRecord.Status) displacedRecords.get(displacedRecords.size() - 1)).s());

        info.getStore().finish(JIPipeProgressNodeState.OK);
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
    void resolveWithKindAndLevelEmitsKindAndLevel() {
        Path logsBase = tmp.resolve("logs");
        JIPipeProgressLog runStore = JIPipeProgressLog.create(logsBase.resolve("kinds.ndjson"), "kinds", "Kinds");
        JIPipeProgressInfo info = new JIPipeProgressInfo(runStore);
        JIPipeProgressInfo gc = info.resolve("GC", JIPipeProgressNodeKind.ITEM, JIPipeLogLevel.DEBUG);
        gc.log("collecting");
        JIPipeProgressInfo partition = info.resolve("Partition 0", JIPipeProgressNodeKind.PARTITION);

        JIPipeProgressNode gcNode = runStore.getNode(gc.getNodeId());
        assertEquals(JIPipeProgressNodeKind.ITEM, gcNode.getKind());
        assertEquals(JIPipeLogLevel.DEBUG, gcNode.getLevel());
        JIPipeProgressNode partitionNode = runStore.getNode(partition.getNodeId());
        assertEquals(JIPipeProgressNodeKind.PARTITION, partitionNode.getKind());
        assertEquals(JIPipeLogLevel.INFO, partitionNode.getLevel(), "resolve(kind) defaults to INFO");

        runStore.finish(JIPipeProgressNodeState.OK);
        List<JIPipeProgressRecord.Node> nodes = runStore.readRecords(1, 64).stream()
                .filter(JIPipeProgressRecord.Node.class::isInstance)
                .map(JIPipeProgressRecord.Node.class::cast)
                .collect(java.util.stream.Collectors.toList());
        JIPipeProgressRecord.Node gcRecord = nodes.stream().filter(r -> r.l().equals("GC")).findFirst().orElseThrow();
        assertEquals("ITEM", gcRecord.k());
        assertEquals(10, gcRecord.lv());
        JIPipeProgressRecord.Node partitionRecord = nodes.stream().filter(r -> r.l().equals("Partition 0")).findFirst().orElseThrow();
        assertEquals("PARTITION", partitionRecord.k());
        assertNull(partitionRecord.lv(), "resolve(kind) without a level emits no lv");
    }

    @Test
    void silentAndStdoutUseTheStoreImplementations() {
        assertNull(JIPipeProgressInfo.SILENT.getStore().getFile());
        assertNull(JIPipeProgressInfo.STDOUT.getStore().getFile());
        assertEquals(1, JIPipeProgressInfo.SILENT.getNodeId());
    }

    @Test
    void batchSubscriptionSurvivesStoreAdoption() throws InterruptedException {
        // The facade starts on a silent store and adopts a file-backed store (the root-run
        // creation point): the subscription must migrate to the adopted store's emitter.
        JIPipeProgressInfo info = new JIPipeProgressInfo();
        Path file = tmp.resolve("adopted.ndjson");
        java.util.concurrent.CopyOnWriteArrayList<JIPipeProgressLog.ProgressBatchEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        java.util.concurrent.CountDownLatch firstBatch = new java.util.concurrent.CountDownLatch(1);
        JIPipeProgressInfo.BatchEventSubscription subscription = info.subscribeToBatchEvents(event -> {
            events.add(event);
            firstBatch.countDown();
        });
        try {
            assertNull(subscription.getStore().getFile(), "the subscription starts on the handle's store");
            info.setStorageBase(tmp, "adopted-run", "Adopted run");
            assertSame(info.getStore(), subscription.getStore(), "the subscription migrated to the adopted store");
            info.log("after adoption");
            // The writer emits within one coalesce interval; a generous deadline keeps the test
            // robust on a cold JVM (the writer's first JSON serialization may warm up Jackson)
            assertTrue(firstBatch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                    "batches of the adopted store must reach the subscriber");
            JIPipeProgressLog.ProgressBatchEvent batch = events.get(0);
            assertFalse(batch.finalBatch(), "the idle batch is not the final one");
            assertTrue(batch.toLine() >= 2, "the batch covers the record written after the adoption");
        } finally {
            subscription.close();
            info.getStore().close();
        }
        assertNull(subscription.getStore(), "closing the subscription unsubscribes from the store");
    }
}
