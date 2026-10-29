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

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
     * Waits until {@code events} contains an event for {@code path} whose
     * origin is {@code EXTERNAL_FILE}. Change events are delivered on the
     * Swing EDT when a display is present, so they may arrive slightly after
     * the document mutation the test observed.
     */
    private static JIPipeSettingsServiceComponent.SettingsChangedEvent awaitExternalEvent(
            List<JIPipeSettingsServiceComponent.SettingsChangedEvent> events, String path) throws InterruptedException {
        boolean found = waitFor(5000, () -> events.stream().anyMatch(e ->
                path.equals(e.getPath())
                        && e.getOrigin() == JIPipeSettingsServiceComponent.ChangeOrigin.EXTERNAL_FILE));
        assertTrue(found, "an EXTERNAL_FILE event for " + path + " must be delivered; events: " + events);
        return events.stream()
                .filter(e -> path.equals(e.getPath()))
                .filter(e -> e.getOrigin() == JIPipeSettingsServiceComponent.ChangeOrigin.EXTERNAL_FILE)
                .findFirst()
                .orElseThrow();
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

        JIPipeSettingsServiceComponent.SettingsChangedEvent adopted = awaitExternalEvent(events, "registry:adp/x");
        assertEquals("one", adopted.getOldValue().asText(), "old value of the adopted change");
        assertEquals("two", adopted.getNewValue().asText(), "new value of the adopted change");
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
        awaitExternalEvent(events, "registry:sync/x");

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
        JIPipeSettingsServiceComponent.SettingsChangedEvent conflict = awaitExternalEvent(events, "registry:col/x");
        assertEquals("three", conflict.getNewValue().asText(), "foreign wins on collision");
        assertTrue(waitFor(8000, () -> fileContains(file, "\"three\"")),
                "the conflicted merge must persist the winning value so the file converges");
        c.dispose();
    }

    /**
     * <b>Critical regression pin: a foreign write is never clobbered by our
     * own save.</b> Sequence: this process saves {@code x="one"}; another
     * instance then writes {@code x="foreign"} to the file; our debounced
     * {@code save()} runs with that foreign content already on disk. A blind
     * whole-document replace would overwrite (and lose) the foreign write,
     * and the post-save verification could never notice it (it only sees
     * writes that land <em>after</em> our write).
     * <p>
     * The read-merge-write in {@code save()} must instead merge the foreign
     * content into the live document first, so the foreign block is present
     * both in the saved document <em>and</em> on disk after {@code save()}.
     * This test is written entirely without the watcher (no wait, no sleep):
     * the foreign file is written directly and {@code save()} is called
     * synchronously, so it fails deterministically against the blind-replace
     * implementation.
     */
    @Test void saveDoesNotClobberAForeignWriteThatLandedFirst() throws Exception {
        JIPipeSettingsServiceComponent c = newComponent(tempDir);
        JIPipeSettingKey<String> shared = JIPipeSettingKey.string("registry:cas", "x", String.class).defaultValue("d");
        JIPipeSettingKey<String> localOnly = JIPipeSettingKey.string("registry:cas-local", "y", String.class).defaultValue("d");
        c.bind(shared).set("one");
        c.save();

        Path file = c.fileFor(SettingsScope.APPLICATION);
        // Another instance writes the file (same block, different value,
        // different size) after our save and before our next save.
        String onDisk = fileContent(file);
        assertTrue(onDisk.contains("\"one\""), "precondition: our first save was on disk");
        Files.writeString(file,
                "{ \"$schema\": 2, \"registry:cas\": { \"x\" : \"foreign\" }, \"$meta\": { \"writer-id\": \"other\" } }");

        // A local edit in a *different* block makes this process save again.
        c.bind(localOnly).set("local-value");
        c.save();

        SettingsDocument doc = c.document(SettingsScope.APPLICATION);
        assertEquals("foreign", doc.get("registry:cas", "x").asText(),
                "the foreign block must be merged into the document, not overwritten");
        assertEquals("local-value", doc.get("registry:cas-local", "y").asText(),
                "the local edit is still saved");
        String saved = fileContent(file);
        assertTrue(saved.contains("\"foreign\""),
                "the foreign write must survive on disk after our save; file: " + saved);
        assertTrue(saved.contains("\"local-value\""),
                "the local edit must be written too; file: " + saved);
        c.dispose();
    }

    /**
     * A corrupt (partially written) foreign settings file must never abort a
     * save nor wipe the local document: the merge path treats it as "nothing
     * to merge", keeps the local blocks, preserves the broken file as
     * {@code .bad-*} (the store's existing recovery), and the save still
     * happens for the remaining/this scope.
     */
    @Test void corruptForeignFileDoesNotAbortOrWipeTheLocalSave() throws Exception {
        JIPipeSettingsServiceComponent c = newComponent(tempDir);
        JIPipeSettingKey<String> key = JIPipeSettingKey.string("registry:corrupt", "x", String.class).defaultValue("d");
        c.bind(key).set("one");
        c.save();

        Path file = c.fileFor(SettingsScope.APPLICATION);
        Files.writeString(file, "{ this is not json");

        // A local edit forces a save while the file on disk is corrupt.
        c.bind(JIPipeSettingKey.string("registry:corrupt", "z", String.class).defaultValue("d")).set("two");
        assertDoesNotThrow(c::save, "a corrupt foreign file must never throw out of save()");

        SettingsDocument doc = c.document(SettingsScope.APPLICATION);
        assertEquals("one", doc.get("registry:corrupt", "x").asText(),
                "the local block must not be deleted by merging an empty/corrupt foreign document");
        assertEquals("two", doc.get("registry:corrupt", "z").asText(),
                "the local edit must still be written");
        try (var s = Files.list(tempDir)) {
            assertTrue(s.anyMatch(p -> p.getFileName().toString().startsWith("settings.json.bad-")),
                    "the corrupt foreign file must be preserved as .bad-<timestamp>");
        }
        assertTrue(fileContent(file).contains("\"one\""),
                "the save must still have produced a valid file");
        c.dispose();
    }

    /**
     * Proves the watcher callback does not block on file I/O: the merge —
     * which reads the settings file from disk — must run on the
     * component's single-thread foreign-change executor, not on the
     * watcher's worker thread (which invokes the callback while holding
     * its instance monitor; a heavy callback there would block
     * {@code close()} and the other worker).
     * <p>
     * Two observations:
     * <ol>
     * <li>the thread that performed the merge (recorded by the component)
     * is {@code jipipe-settings-foreign-merge}, never a
     * {@code SettingsFileWatcher-*} thread — if the merge were inlined into
     * the callback this fails;</li>
     * <li>the change event reaches subscribers on the Swing EDT when a
     * display is present, i.e. never on the merge executor (mutation of UI
     * state off the EDT is what the event marshalling exists to prevent).</li>
     * </ol>
     * This test JVM is not headless, so the EDT branch is the one exercised;
     * headless runs deliver inline and the assertion below is skipped.
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
        assertTrue(c.getLastMergeThreadNameForTesting() != null
                        && c.getLastMergeThreadNameForTesting().startsWith("jipipe-settings-foreign-merge"),
                "the file load + merge must run on the executor thread, not in the watcher callback; was: "
                        + c.getLastMergeThreadNameForTesting());

        assertTrue(waitFor(3000, () -> !eventThreads.isEmpty()), "the merge must emit change events");
        if (!GraphicsEnvironment.isHeadless()) {
            for (String threadName : eventThreads) {
                assertTrue(threadName.startsWith("AWT-EventQueue"),
                        "with a display the change event must be delivered on the Swing EDT; was: " + threadName);
            }
        }
        c.dispose();
    }
}
