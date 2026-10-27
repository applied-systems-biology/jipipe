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
}
