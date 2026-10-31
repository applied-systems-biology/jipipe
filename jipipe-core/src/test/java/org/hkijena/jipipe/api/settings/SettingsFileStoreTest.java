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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.databind.node.BooleanNode;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettingsFileStoreTest {
    @TempDir Path tempDir;

    @Test void corruptFileIsBackedUpAndDefaultsLoad() throws Exception {
        Path f = tempDir.resolve("settings.json");
        Files.writeString(f, "{ this is not json");
        SettingsFileStore store = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true);
        SettingsDocument doc = store.load();
        assertTrue(doc.blockPaths().isEmpty(), "corrupt file must load as empty document");
        try (var s = Files.list(tempDir)) {
            assertTrue(s.anyMatch(p -> p.getFileName().toString().startsWith("settings.json.bad-")),
                    "corrupt file must be preserved as .bad-<timestamp>");
        }
    }

    @Test void corruptBytesArePreservedInTheBackup() throws Exception {
        Path f = tempDir.resolve("settings.json");
        String corrupt = "{ this is not json";
        Files.writeString(f, corrupt);
        new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true).load();
        Path backup = Files.list(tempDir)
                .filter(p -> p.getFileName().toString().startsWith("settings.json.bad-"))
                .findFirst().orElseThrow();
        assertEquals(corrupt, Files.readString(backup), "the .bad- backup must preserve the original bytes");
    }

    @Test void corruptFileIsBackedUpExactlyOnceAcrossRepeatedLoads() throws Exception {
        Path f = tempDir.resolve("settings.json");
        Files.writeString(f, "{ this is not json");
        SettingsFileStore store = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true);
        store.load();
        store.load();
        store.load();
        try (var s = Files.list(tempDir)) {
            List<String> names = s.map(p -> p.getFileName().toString()).toList();
            assertEquals(1, names.stream().filter(n -> n.startsWith("settings.json.bad-")).count(),
                    "repeated loads of an already-recovered file must not create further backups");
            assertFalse(names.stream().anyMatch(n -> n.split("\\.bad-", -1).length > 2),
                    "a .bad- file must never itself be backed up (no .bad-.bad- files)");
        }
    }

    @Test void missingFileLoadsEmptyAndFirstSaveCreatesIt() {
        Path f = tempDir.resolve("sub/settings.json");
        SettingsFileStore store = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true);
        SettingsDocument doc = store.load();
        doc.set("registry:x", "a", BooleanNode.TRUE);
        store.save(doc);
        assertTrue(Files.isRegularFile(f));
    }

    @Test void saveIsAtomicAndLeavesNoTempFile() throws Exception {
        Path f = tempDir.resolve("settings.json");
        SettingsFileStore store = new SettingsFileStore(f, SettingsScope.RUNTIME, p -> true);
        SettingsDocument doc = store.load();
        doc.set("registry:x", "a", BooleanNode.TRUE);
        store.save(doc);
        try (var s = Files.list(tempDir)) {
            assertFalse(s.anyMatch(p -> p.getFileName().toString().contains(".tmp.")), "no tmp file may remain");
        }
    }

    @Test void savedDocumentSurvivesAReloadRoundTrip() throws Exception {
        Path f = tempDir.resolve("settings.json");
        SettingsFileStore store = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true);
        SettingsDocument doc = store.load();
        doc.set("registry:x", "a", BooleanNode.TRUE);
        doc.setBlock("state:recent-projects", JsonUtils.getObjectMapper().valueToTree(List.of("/a/b.jip")));
        store.save(doc);

        SettingsDocument reloaded = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true).load();
        assertEquals(BooleanNode.TRUE, reloaded.get("registry:x", "a"),
                "the block saved through the tmp+move path must be readable again, fully intact");
        assertEquals(doc.getBlock("state:recent-projects"), reloaded.getBlock("state:recent-projects"));
        assertEquals(doc.schemaVersion(), reloaded.schemaVersion());
    }

    @Test void fingerprintChangesAfterSave() throws Exception {
        Path f = tempDir.resolve("settings.json");
        SettingsFileStore store = new SettingsFileStore(f, SettingsScope.APPLICATION, p -> true);
        SettingsDocument doc = store.load();
        assertNull(store.fingerprint());
        doc.set("registry:x", "a", BooleanNode.TRUE);
        store.save(doc);
        assertNotNull(store.fingerprint());
        assertTrue(store.fingerprint().size() > 0);
    }

    @Test void orphanTempFilesAreCleanedButFreshOnesSurvive() throws Exception {
        Path fresh = Files.writeString(tempDir.resolve("settings.json.tmp.abc"), "x");
        Path old = Files.writeString(tempDir.resolve("settings.json.tmp.def"), "x");
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(48, ChronoUnit.HOURS)));
        SettingsFileStore.cleanUpOrphanTempFiles(tempDir);
        assertTrue(Files.exists(fresh));
        assertFalse(Files.exists(old));
    }

    /**
     * Item 3: {@code null} means "absent" and nothing else. A file that exists
     * but cannot be fingerprinted must not be reported as absent (the watcher
     * would then swallow the next foreign change). An unreadable *file* is
     * hard to fabricate portably, so this pins the contract at its other end:
     * an existing file never yields null, and a missing one always does.
     */
    @Test void fingerprintIsNullOnlyForAbsence() throws Exception {
        Path file = tempDir.resolve("settings.json");
        SettingsFileStore store = new SettingsFileStore(file, org.hkijena.jipipe.api.settings.SettingsScope.APPLICATION, p -> true);
        assertNull(store.fingerprint(), "an absent file fingerprints to null");
        Files.writeString(file, "{\"$schema\": 2}");
        assertNotNull(store.fingerprint(), "an existing file must never fingerprint to null");
        Files.delete(file);
        assertNull(store.fingerprint(), "a deleted file fingerprints to null again");
    }

    /**
     * Item 4: only the exceptions a genuinely corrupt file produces are
     * turned into a {@code .bad-} backup; a code bug must propagate instead of
     * hiding as "corruption".
     * <p>
     * The bug is injected through the classification predicate, which
     * {@code SettingsDocument.isPassThrough} consults lazily. It therefore
     * proves the narrower contract at the point it matters: a loaded document
     * whose classification throws is NOT backed up and NOT replaced by an
     * empty one, and the storage file is untouched.
     */
    @Test void codeBugsAreNotLaunderedIntoCorruption() throws Exception {
        Path file = tempDir.resolve("settings.json");
        Files.writeString(file, "{\"$schema\": 2, \"registry:x\": {\"a\": 1}}");
        SettingsFileStore store = new SettingsFileStore(file, org.hkijena.jipipe.api.settings.SettingsScope.APPLICATION,
                p -> {
                    throw new NullPointerException("a bug in the registration predicate");
                });
        SettingsDocument doc = store.load();
        assertThrows(NullPointerException.class, () -> doc.isPassThrough("registry:x"),
                "a predicate bug surfaces to the caller instead of being swallowed by load()");
        assertTrue(Files.isRegularFile(file), "the file must be untouched: this was not corruption");
        assertEquals(0, listBadBackups().size(), "no backup is made for a code bug");
    }

    /**
     * The counterpart: a parse failure is a load-time failure and still takes
     * the recovery path (pinned by the existing corrupt-file tests above).
     */
    @Test void parseFailuresStillTakeTheRecoveryPathWithoutThrowing() throws Exception {
        Path file = tempDir.resolve("settings.json");
        Files.writeString(file, "{ this is not json");
        SettingsFileStore store = new SettingsFileStore(file, org.hkijena.jipipe.api.settings.SettingsScope.APPLICATION, p -> true);
        assertNotNull(store.load(), "a corrupt file yields an empty document");
    }

    /**
     * ... while a genuinely corrupt file still takes the recovery path.
     */
    @Test void corruptFileStillTakesTheRecoveryPath() throws Exception {
        Path file = tempDir.resolve("settings.json");
        Files.writeString(file, "{ this is not json");
        SettingsFileStore store = new SettingsFileStore(file, org.hkijena.jipipe.api.settings.SettingsScope.APPLICATION, p -> true);
        assertNotNull(store.load(), "a corrupt file yields an empty document");
        assertEquals(1, listBadBackups().size(), "a corrupt file is preserved as one .bad- backup");
    }

    /**
     * Item 5: backups are pruned to the newest 5 per base file name.
     */
    @Test void oldBadBackupsArePrunedToTheNewestFive() throws Exception {
        Path file = tempDir.resolve("settings.json");
        Files.writeString(file, "{ not json");
        for (int i = 0; i < 7; i++) {
            Path backup = tempDir.resolve("settings.json.bad-2026-10-0" + (i + 1) + "T00-00-00Z");
            Files.writeString(backup, "{ not json " + i + " }");
            Files.setLastModifiedTime(backup, FileTime.from(Instant.parse("2026-10-0" + (i + 1) + "T00:00:00Z")));
        }
        // A backup of a DIFFERENT base name must be left alone.
        Path other = tempDir.resolve("runtime-settings.json.bad-2026-10-01T00-00-00Z");
        Files.writeString(other, "{}");

        SettingsFileStore store = new SettingsFileStore(file, org.hkijena.jipipe.api.settings.SettingsScope.APPLICATION, p -> true);
        store.pruneBadBackups();

        List<Path> kept = listBadBackups().stream()
                .filter(p -> p.getFileName().toString().startsWith("settings.json.bad-"))
                .sorted()
                .toList();
        assertEquals(SettingsFileStore.maxBadBackupsPerFile(), kept.size(), "only the newest 5 backups are kept");
        assertEquals("settings.json.bad-2026-10-03T00-00-00Z", kept.get(0).getFileName().toString(),
                "the oldest surviving backup is the 3rd of the 7");
        assertTrue(Files.exists(other), "backups of another base file name are not touched");
    }

    private List<Path> listBadBackups() throws java.io.IOException {
        try (var stream = Files.list(tempDir)) {
            return stream.filter(p -> p.getFileName().toString().contains(".bad-")).sorted().toList();
        }
    }
}
