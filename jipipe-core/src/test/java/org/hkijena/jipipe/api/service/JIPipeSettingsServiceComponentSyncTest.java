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

package org.hkijena.jipipe.api.service;

import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeSettingKey;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the multi-instance sync wiring of
 * {@link JIPipeSettingsServiceComponent}: watcher → merge → document,
 * change events with the {@code EXTERNAL_FILE} origin, the
 * {@code hasForeignConflict()} flag, the cheap-callback hand-off, and the
 * post-save read-back verification that closes the watcher's documented
 * own-fingerprint false negative.
 * <p>
 * The watcher started by {@link JIPipeSettingsServiceComponent#document} is
 * a real watcher (the test JVM is not headless), with a 2 s poll interval;
 * all assertions on watcher-driven foreign changes are therefore bounded
 * {@code waitFor} loops rather than fixed sleeps.
 */
class JIPipeSettingsServiceComponentSyncTest {

    @TempDir
    Path tempDir;

    private static boolean waitFor(long timeoutMillis, Supplier<Boolean> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condition.get();
    }

    private static JIPipeSettingsServiceComponent newComponent(Path userDir) {
        return JIPipeSettingsServiceComponent.forTesting(userDir);
    }

    private static String fileContent(Path file) throws Exception {
        return Files.readString(file);
    }

    private static boolean fileContains(Path file, String needle) {
        try {
            return Files.readString(file).contains(needle);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * A file change made by "another process" is detected by the watcher,
     * merged into the live document, re-emitted with the EXTERNAL_FILE
     * origin, and visible through the binding — without a conflict flag
     * (no unsaved local edit was lost).
     */
    @Test void foreignFileChangeIsAdoptedWithExternalFileOrigin() throws Exception {
        JIPipeSettingsServiceComponent c = newComponent(tempDir);
        JIPipeSettingKey<String> key = JIPipeSettingKey.string("registry:adp", "x", String.class).defaultValue("d");
        SettingsBinding<String> b = c.bind(key);
        b.set("one");
        c.save();

        List<JIPipeSettingsServiceComponent.SettingsChangedEvent> events = new CopyOnWriteArrayList<>();
        c.getChangedEventEmitter().subscribeLambda((emitter, event) -> events.add(event));

        // Simulate another JIPipe instance writing the file (a different
        // size guarantees a different fingerprint).
        Path file = c.fileFor(SettingsScope.APPLICATION);
        Files.writeString(file,
                "{ \"$schema\": 2, \"registry:adp\": { \"x\" : \"two\" }, \"$meta\": { \"writer-id\": \"other\" } }");

        // Reading the event list first in the condition establishes the
        // happens-before edge from the executor thread's document mutation
        // (which precedes the event emission) to this thread's b.get().
        assertTrue(waitFor(8000, () -> !events.isEmpty() && "two".equals(b.get())),
                "the foreign value must reach the document and the binding; document: "
                        + c.document(SettingsScope.APPLICATION).get("registry:adp", "x"));
        assertEquals("two", c.document(SettingsScope.APPLICATION).get("registry:adp", "x").asText());
        assertFalse(c.hasForeignConflict(), "an adoption (no unsaved local edit lost) is not a conflict");

        boolean sawExternalAdoption = false;
        for (JIPipeSettingsServiceComponent.SettingsChangedEvent e : events) {
            if ("registry:adp/x".equals(e.getPath())) {
                sawExternalAdoption = true;
                assertEquals("one", e.getOldValue().asText(), "old value of the adopted change");
                assertEquals("two", e.getNewValue().asText(), "new value of the adopted change");
                assertEquals(JIPipeSettingsServiceComponent.ChangeOrigin.EXTERNAL_FILE, e.getOrigin(),
                        "a watcher-detected foreign change is EXTERNAL_FILE, not a local change");
            }
        }
        assertTrue(sawExternalAdoption, "the adopted sub-path must fire a change event; events: " + events);
        c.dispose();
    }

    /**
     * Proves the carry-forward ruling: a foreign write interleaved with our
     * own save is not silently lost. The sequence is (1) this process saves
     * x="one", (2) a foreign process rewrites the file to x="foreign"
     * before the read-back, and (3) the post-save verification reads the
     * file back and finds it differs from what this process wrote. Without
     * the verification, step 3 would record the foreign fingerprint as our
     * own and the foreign change would never be reported — the watcher's
     * documented false negative. With it, the foreign content is fed
     * through the merge path. The assertions run <em>immediately after</em>
     * the verification returns, long before the watcher's 2 s poll could
     * have reconciled anything, so only the verification path can have
     * produced them.
     */
    @Test void interleavedForeignWriteIsNotSilentlyLost() throws Exception {
        JIPipeSettingsServiceComponent c = newComponent(tempDir);
        JIPipeSettingKey<String> key = JIPipeSettingKey.string("registry:sync", "x", String.class).defaultValue("d");
        SettingsBinding<String> b = c.bind(key);
        b.set("one");
        c.save();

        Path file = c.fileFor(SettingsScope.APPLICATION);
        // The foreign process rewrites the file between our save and the
        // (simulated) fingerprint read-back: same block, a different value,
        // a different size.
        String onDisk = fileContent(file);
        assertTrue(onDisk.contains("\"one\""), "precondition: our save was on disk; file: " + onDisk);
        Files.writeString(file, onDisk.replace(": \"one\"", ": \"foreign\""));

        List<JIPipeSettingsServiceComponent.SettingsChangedEvent> events = new CopyOnWriteArrayList<>();
        c.getChangedEventEmitter().subscribeLambda((emitter, event) -> events.add(event));

        // The self-healing read-back: compares the file against what this
        // process actually wrote (x="one") and finds the foreign content.
        SettingsDocument doc = c.document(SettingsScope.APPLICATION);
        c.verifyOwnWriteForTesting(SettingsScope.APPLICATION);

        // Immediate assertions: the merge ran synchronously inside the
        // verification, not via the watcher's poll (which would be ~2 s
        // away at the earliest).
        assertEquals("foreign", doc.get("registry:sync", "x").asText(),
                "the interleaved foreign write must be merged in immediately, not silently absorbed");
        assertFalse(c.hasForeignConflict(),
                "the foreign write replaced a SAVED value; no unsaved local edit was lost, so this is an adoption, not a conflict");
        boolean sawExternalAdoption = false;
        for (JIPipeSettingsServiceComponent.SettingsChangedEvent e : events) {
            if ("registry:sync/x".equals(e.getPath())) {
                sawExternalAdoption = true;
                assertEquals(JIPipeSettingsServiceComponent.ChangeOrigin.EXTERNAL_FILE, e.getOrigin());
            }
        }
        assertTrue(sawExternalAdoption, "the adopted block must fire an EXTERNAL_FILE event; events: " + events);

        // The foreign file already holds x="foreign" and the local document
        // now matches it, so the merge adopted it (no re-save needed).
        assertTrue(fileContent(file).contains("\"foreign\""),
                "the file still holds the foreign value we adopted");
        c.dispose();
    }

    /**
     * A true same-block collision: an unsaved local edit (x="two", not yet
     * persisted) meets a foreign write (x="three") of the same block. The
     * foreign value wins (it is the most recent on-disk truth), the
     * collision is reported via {@code hasForeignConflict()} for the UI
     * banner, and the file converges on the foreign value.
     */
    @Test void sameBlockCollisionIsReportedAndForeignWins() throws Exception {
        JIPipeSettingsServiceComponent c = newComponent(tempDir);
        JIPipeSettingKey<String> key = JIPipeSettingKey.string("registry:col", "x", String.class).defaultValue("d");
        SettingsBinding<String> b = c.bind(key);
        b.set("one");
        c.save();

        // An unsaved local edit: the file still holds "one".
        b.set("two");
        assertFalse(c.hasForeignConflict());

        List<JIPipeSettingsServiceComponent.SettingsChangedEvent> events = new CopyOnWriteArrayList<>();
        c.getChangedEventEmitter().subscribeLambda((emitter, event) -> events.add(event));

        Path file = c.fileFor(SettingsScope.APPLICATION);
        Files.writeString(file,
                "{ \"$schema\": 2, \"registry:col\": { \"x\" : \"three\" }, \"$meta\": { \"writer-id\": \"other\" } }");

        // Reading the event list first establishes the happens-before edge
        // from the executor thread's document mutation to this read.
        assertTrue(waitFor(8000, () -> !events.isEmpty() && "three".equals(b.get())),
                "on a same-block collision the foreign value must win; document: "
                        + c.document(SettingsScope.APPLICATION).get("registry:col", "x"));
        assertTrue(waitFor(8000, c::hasForeignConflict),
                "a collision must be flagged for the UI banner");
        // The local edit "two" must have been discarded in favor of the
        // foreign value (data-loss direction pinned).
        for (JIPipeSettingsServiceComponent.SettingsChangedEvent e : events) {
            if ("registry:col/x".equals(e.getPath()) && "two".equals(e.getOldValue() == null ? null : e.getOldValue().asText())) {
                assertEquals("three", e.getNewValue().asText(), "foreign wins on collision");
                assertEquals(JIPipeSettingsServiceComponent.ChangeOrigin.EXTERNAL_FILE, e.getOrigin());
            }
        }
        assertTrue(waitFor(8000, () -> fileContains(file, "\"three\"")),
                "the conflicted merge must persist the winning value so the file converges");
        c.dispose();
    }

    /**
     * Proves the watcher callback does not block on file I/O: the merge —
     * which reads the settings file from disk — must run on the
     * component's single-thread foreign-change executor, not on the
     * watcher's worker thread (which invokes the callback while holding
     * its instance monitor; a heavy callback there would block
     * {@code close()} and the other worker). The proof: the merge's
     * per-path change events are emitted by the thread performing the
     * file load + merge, so the event delivery thread is observed and
     * asserted to be the executor thread, not a
     * {@code SettingsFileWatcher-*} thread. If the merge were inlined
     * into the watcher callback, the events would be delivered on the
     * watcher thread and this test fails.
     */
    @Test void mergeRunsOnTheExecutorThreadNotInTheWatcherCallback() throws Exception {
        JIPipeSettingsServiceComponent c = newComponent(tempDir);
        JIPipeSettingKey<String> key = JIPipeSettingKey.string("registry:thd", "x", String.class).defaultValue("d");
        SettingsBinding<String> b = c.bind(key);
        b.set("one");
        c.save();

        List<String> eventThreads = new CopyOnWriteArrayList<>();
        c.getChangedEventEmitter().subscribeLambda((emitter, event) ->
                eventThreads.add(Thread.currentThread().getName()));

        Path file = c.fileFor(SettingsScope.APPLICATION);
        Files.writeString(file,
                "{ \"$schema\": 2, \"registry:thd\": { \"x\" : \"two\" }, \"$meta\": { \"writer-id\": \"other\" } }");

        assertTrue(waitFor(8000, () -> "two".equals(b.get())), "the foreign change must be merged");
        assertFalse(eventThreads.isEmpty(), "the merge must emit change events");
        for (String threadName : eventThreads) {
            assertTrue(threadName.startsWith("jipipe-settings-foreign-merge"),
                    "the file load + merge must run on the executor thread, not in the watcher callback; "
                            + "event was emitted on: " + threadName);
        }
        c.dispose();
    }
}
