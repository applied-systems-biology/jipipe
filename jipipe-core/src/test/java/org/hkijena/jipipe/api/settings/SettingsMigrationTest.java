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

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsMigrationTest {
    private static final Path BACKUP_NAME = Path.of(SettingsMigration.BACKUP_DIRECTORY_NAME);

    @TempDir Path tempDir;

    private static JsonNode read(Path file) throws Exception {
        return JsonUtils.getObjectMapper().readTree(Files.readString(file));
    }

    private static List<String> fileNames(Path directory) throws Exception {
        try (var s = Files.list(directory)) {
            return s.map(f -> f.getFileName().toString()).sorted().toList();
        }
    }

    @Test void migratesSheetRegistryAndRecentProjects() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"),
                "{\"org.hkijena.jipipe:general-ui\":{\"theme\":\"Dark\"}, \"unknown-sheet:1\":{\"a\":1}}");
        Files.writeString(p.resolve("settings-db-node-db.json"), "{\"pinned-node-ids\":[\"n1\"]}");
        Files.writeString(p.resolve("recent-projects.txt"), "/a/b.jip\n/c/d.jip\n");

        SettingsMigration.migrateLegacyProfile(p, block -> true,
                Map.of("registry:node-db", SettingsScope.APPLICATION), JIPipeProgressInfo.SILENT);

        JsonNode global = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json")));
        assertEquals(2, global.get("$schema").asInt());
        assertEquals("Dark", global.get("sheet:org.hkijena.jipipe:general-ui").get("theme").asText());
        assertEquals(1, global.get("sheet:unknown-sheet:1").get("a").asInt(), "unknown sheet ids survive");
        assertEquals("n1", global.get("registry:node-db").get("pinned-node-ids").get(0).asText());
        assertEquals(2, global.get("state:recent-projects").size());
        assertTrue(Files.isDirectory(p.resolve("settings-migration-backup")));
        assertFalse(Files.exists(p.resolve("recent-projects.txt")), "legacy file must be moved, not left behind");
    }

    @Test void unambiguousBlocksDefaultToApplication() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
        JsonNode global = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json")));
        assertEquals(1, global.get("sheet:x").get("a").asInt());
    }

    @Test void secondRunIsANoOp() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
        JsonNode first = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json")));
        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
        assertEquals(first, JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json"))));
        assertFalse(SettingsMigration.needsMigration(p));
    }

    @Test void runtimeBlocksGoToRuntimeFile() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"), "{\"org.hkijena.jipipe:runtime\":{\"num-threads\":4}}");
        SettingsMigration.migrateLegacyProfile(p, b -> true,
                Map.of("sheet:org.hkijena.jipipe:runtime", SettingsScope.RUNTIME), JIPipeProgressInfo.SILENT);
        JsonNode global = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("settings.json")));
        JsonNode runtime = JsonUtils.getObjectMapper().readTree(Files.readString(p.resolve("runtime-settings.json")));
        assertFalse(global.has("sheet:org.hkijena.jipipe:runtime"), "runtime block must leave settings.json");
        assertEquals(4, runtime.get("sheet:org.hkijena.jipipe:runtime").get("num-threads").asInt());
    }

    // --- Tests demanded by the task requirements beyond the brief's four ---

    @Test void freshProfileIsNotAMigrationCandidate() {
        assertFalse(SettingsMigration.needsMigration(tempDir), "an empty profile dir needs no migration");
    }

    @Test void migratedProfileNeedNoMigrationAndEveryBlockIsKept() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"),
                "{\"org.hkijena.jipipe:general-ui\":{\"theme\":\"Dark\"}, \"unknown-sheet:1\":{\"a\":1}, \"scalar-sheet\":7}");
        Files.writeString(p.resolve("settings-db-node-db.json"), "{\"a/b\":\"c\", \"x\":{\"y\":[1,2]}}");
        Files.writeString(p.resolve("settings-db-other.json"), "{\"q\":true}");
        Files.writeString(p.resolve("recent-projects.txt"), "/a/b.jip\n\n  /c/d.jip  \n");

        assertTrue(SettingsMigration.needsMigration(p));
        SettingsMigration.migrateLegacyProfile(p, b -> true,
                Map.of("registry:node-db", SettingsScope.APPLICATION,
                        "registry:other", SettingsScope.RUNTIME,
                        "sheet:unknown-sheet:1", SettingsScope.RUNTIME),
                JIPipeProgressInfo.SILENT);

        JsonNode global = read(p.resolve("settings.json"));
        JsonNode runtime = read(p.resolve("runtime-settings.json"));

        // Nothing is lost: every legacy block is present in exactly one document.
        assertTrue(global.has("sheet:org.hkijena.jipipe:general-ui"));
        assertTrue(global.has("registry:node-db"), "application-routed registry stays in settings.json");
        assertFalse(global.has("registry:other"), "runtime-routed registry leaves settings.json");
        assertFalse(global.has("sheet:unknown-sheet:1"), "runtime-routed sheet leaves settings.json");
        assertEquals("c", global.get("registry:node-db").get("a/b").asText());
        assertEquals(2, global.get("registry:node-db").get("x").get("y").size());
        assertEquals(1, runtime.get("registry:other").size());
        assertTrue(runtime.get("registry:other").get("q").asBoolean());
        assertEquals(1, runtime.get("sheet:unknown-sheet:1").get("a").asInt());
        assertEquals(7, global.get("sheet:scalar-sheet").asInt(), "non-object top-level values are kept verbatim");

        JsonNode recent = global.get("state:recent-projects");
        assertEquals(2, recent.size(), "blank lines are skipped and lines are trimmed");
        assertEquals("/a/b.jip", recent.get(0).asText());
        assertEquals("/c/d.jip", recent.get(1).asText());

        // Meta and merge invariants.
        assertEquals(2, global.get("$schema").asInt());
        assertEquals(2, runtime.get("$schema").asInt());
        assertTrue(global.get("$meta").hasNonNull("written-by-version"));
        assertTrue(global.get("$meta").hasNonNull("writer-id"));
        assertTrue(global.get("$meta").hasNonNull("written-at"));

        // Backup: the directory stays (it is visible to the user), but a
        // successful migration cleans out the legacy inputs it put there.
        Path backup = p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME);
        assertTrue(Files.isDirectory(backup));
        assertTrue(fileNames(backup).isEmpty(), "legacy inputs are cleaned out after success: " + fileNames(backup));
        try (var s = Files.list(p)) {
            List<String> leftovers = s.map(f -> f.getFileName().toString())
                    .filter(n -> n.equals("recent-projects.txt") || n.startsWith("settings-db-"))
                    .toList();
            assertTrue(leftovers.isEmpty(), "no legacy file may remain in the profile: " + leftovers);
        }

        assertFalse(SettingsMigration.needsMigration(p));
    }

    @Test void applicationOnlyMigrationCreatesNoRuntimeFile() throws Exception {
        Files.writeString(tempDir.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        SettingsMigration.migrateLegacyProfile(tempDir, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
        assertFalse(Files.exists(tempDir.resolve("runtime-settings.json")),
                "no empty runtime file may be created");
    }

    @Test void corruptLegacySettingsFileIsPreservedAndTheRestStillMigrates() throws Exception {
        Path p = tempDir;
        String corrupt = "{ this is not json";
        Files.writeString(p.resolve("settings.json"), corrupt);
        Files.writeString(p.resolve("settings-db-node-db.json"), "{\"k\":\"v\"}");
        Files.writeString(p.resolve("recent-projects.txt"), "/a/b.jip\n");

        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);

        Path backup = p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME);
        Path bad = Files.list(backup)
                .filter(f -> f.getFileName().toString().startsWith("settings.json.bad-"))
                .findFirst().orElseThrow(() -> new AssertionError("corrupt settings.json must be preserved as .bad-<timestamp>"));
        assertEquals(corrupt, Files.readString(bad), "the corrupt file's bytes must be preserved");
        assertEquals(List.of(bad.getFileName().toString()), fileNames(backup),
                ".bad-* files stay in the backup; the cleaned-out legacy inputs are gone");

        JsonNode global = read(p.resolve("settings.json"));
        assertEquals(2, global.get("$schema").asInt(), "a fresh new-format document replaces the corrupt one");
        assertEquals("v", global.get("registry:node-db").get("k").asText());
        assertEquals(1, global.get("state:recent-projects").size());
        assertFalse(Files.exists(p.resolve("recent-projects.txt")));
        assertFalse(SettingsMigration.needsMigration(p));
    }

    @Test void corruptRegistryFileIsPreservedAndDoesNotAbortTheMigration() throws Exception {
        Path p = tempDir;
        String corrupt = "[1, 2";
        Files.writeString(p.resolve("settings-db-broken.json"), corrupt);
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");

        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);

        Path backup = p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME);
        Path bad = Files.list(backup)
                .filter(f -> f.getFileName().toString().startsWith("settings-db-broken.json.bad-"))
                .findFirst().orElseThrow(() -> new AssertionError("corrupt registry file must be preserved as .bad-<timestamp>"));
        assertEquals(corrupt, Files.readString(bad));
        JsonNode global = read(p.resolve("settings.json"));
        assertEquals(1, global.get("sheet:x").get("a").asInt());
        assertFalse(global.has("registry:broken"));
        assertFalse(SettingsMigration.needsMigration(p), "a preserved corrupt file is not legacy input anymore");
    }

    @Test void existingBackupFilesAreNeverOverwritten() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        Files.writeString(p.resolve("settings-db-node-db.json"), "{\"k\":\"v\"}");
        Path backup = Files.createDirectory(p.resolve("settings-migration-backup"));
        Files.writeString(backup.resolve("settings.json"), "{\"older\":true}");
        Files.writeString(backup.resolve("settings-db-node-db-1.json"), "{\"older\":true}");

        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);

        assertEquals("{\"older\":true}", Files.readString(backup.resolve("settings.json")),
                "an existing backup file must never be overwritten");
        assertEquals("{\"older\":true}", Files.readString(backup.resolve("settings-db-node-db-1.json")),
                "an existing suffixed backup file must never be overwritten");
        assertEquals(List.of("settings-db-node-db-1.json", "settings.json"), fileNames(backup),
                "the freshly moved legacy files are cleaned out; pre-existing backups stay");
    }

    @Test void noOpRunDoesNotTouchTheBackupDirectory() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
        Path backup = p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME);
        Files.writeString(backup.resolve("keep-me.txt"), "user file");
        long backupCount = Files.list(backup).count();

        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);

        assertEquals(backupCount, Files.list(backup).count(), "a no-op run must not touch the backup directory");
        assertEquals("user file", Files.readString(backup.resolve("keep-me.txt")));
    }

    @Test void partialMigrationMergesTheRemainingLegacyFilesWithoutClobbering() throws Exception {
        Path p = tempDir;
        // A previous (interrupted) run already produced the new-format
        // document, and one registry file was left behind.
        Files.writeString(p.resolve("settings.json"), """
                {
                  "$schema": 2,
                  "$meta": { "written-by-version": "6.0.0" },
                  "sheet:org.hkijena.jipipe:general-ui": { "theme": "Dark" },
                  "registry:node-db": { "keep": "existing" }
                }
                """);
        Files.writeString(p.resolve("settings-db-node-db.json"), "{\"keep\":\"legacy\", \"add\":\"legacy\"}");
        Files.writeString(p.resolve("settings-db-new.json"), "{\"n\":1}");
        Files.writeString(p.resolve("recent-projects.txt"), "/a/b.jip\n");

        assertTrue(SettingsMigration.needsMigration(p), "a leftover registry file needs migration");
        SettingsMigration.migrateLegacyProfile(p, b -> true,
                Map.of("registry:new", SettingsScope.RUNTIME), JIPipeProgressInfo.SILENT);

        JsonNode global = read(p.resolve("settings.json"));
        JsonNode runtime = read(p.resolve("runtime-settings.json"));
        assertEquals("Dark", global.get("sheet:org.hkijena.jipipe:general-ui").get("theme").asText());
        assertEquals("existing", global.get("registry:node-db").get("keep").asText(),
                "an existing new-format block wins over legacy content");
        assertEquals(1, global.get("state:recent-projects").size());
        assertEquals(1, runtime.get("registry:new").get("n").asInt());
        assertFalse(Files.exists(p.resolve("settings-db-node-db.json")));
        assertFalse(Files.exists(p.resolve("recent-projects.txt")));
        assertFalse(SettingsMigration.needsMigration(p));
    }

    @Test void blockArgumentsAreRequiredToBeSane() {
        assertThrows(NullPointerException.class,
                () -> SettingsMigration.migrateLegacyProfile(tempDir, null, Map.of(), JIPipeProgressInfo.SILENT));
        assertThrows(NullPointerException.class,
                () -> SettingsMigration.migrateLegacyProfile(tempDir, b -> true, null, JIPipeProgressInfo.SILENT));
        assertThrows(NullPointerException.class,
                () -> SettingsMigration.recoverFromInterruptedMigration(tempDir, null, Map.of(), JIPipeProgressInfo.SILENT));
        assertThrows(NullPointerException.class,
                () -> SettingsMigration.recoverFromInterruptedMigration(tempDir, b -> true, null, JIPipeProgressInfo.SILENT));
        assertFalse(SettingsMigration.needsMigration(tempDir.resolve("does-not-exist")));
        assertFalse(SettingsMigration.recoverFromInterruptedMigration(tempDir.resolve("does-not-exist"),
                b -> true, Map.of(), JIPipeProgressInfo.SILENT));
    }

    // --- Review fixes: all-or-nothing backup moves, interrupted-migration recovery ---

    @Test void failedBackupMoveRollsBackAndWritesNothing() throws Exception {
        Path p = tempDir;
        String legacySettings = "{\"x\":{\"a\":1}}";
        Files.writeString(p.resolve("settings.json"), legacySettings);
        Files.writeString(p.resolve("settings-db-node-db.json"), "{\"k\":\"v\"}");
        // A regular file where the backup directory belongs: neither the
        // directory creation nor the moves into it can succeed.
        Files.writeString(p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME), "not a directory");

        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);

        assertEquals(legacySettings, Files.readString(p.resolve("settings.json")),
                "the legacy settings.json must be byte-identical after a failed backup");
        assertEquals("{\"k\":\"v\"}", Files.readString(p.resolve("settings-db-node-db.json")),
                "the legacy registry file must be untouched after a failed backup");
        assertEquals("not a directory", Files.readString(p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME)),
                "the blocking file must not be replaced");
        assertFalse(Files.exists(p.resolve("runtime-settings.json")));
        assertTrue(SettingsMigration.needsMigration(p), "the profile stays migratable, so the next start retries");
    }

    @Test void failedBackupMoveLeavesEveryLegacyFileIntact() throws Exception {
        Path p = tempDir;
        String legacySettings = "{\"x\":{\"a\":1}, \"$degenerate\":{\"b\":2}}";
        Files.writeString(p.resolve("settings.json"), legacySettings);
        Files.writeString(p.resolve("recent-projects.txt"), "/a/b.jip\n");
        Files.writeString(p.resolve("settings-db-node-db.json"), "{\"k\":\"v\"}");
        Files.writeString(p.resolve("settings-db-runtime.json"), "{\"threads\":4}");
        // A read-only backup directory: creating it succeeds, moving into it fails.
        Path backup = Files.createDirectory(p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME));
        assertTrue(backup.toFile().setWritable(false, false), "could not make the backup dir read-only");

        try {
            SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);

            // No file may be touched: not moved, not rewritten, not deleted.
            assertEquals(legacySettings, Files.readString(p.resolve("settings.json")),
                    "every legacy file (incl. $ fields) must be byte-identical after a failed backup");
            assertEquals("/a/b.jip\n", Files.readString(p.resolve("recent-projects.txt")));
            assertEquals("{\"k\":\"v\"}", Files.readString(p.resolve("settings-db-node-db.json")));
            assertEquals("{\"threads\":4}", Files.readString(p.resolve("settings-db-runtime.json")));
            assertFalse(Files.exists(p.resolve("runtime-settings.json")),
                    "an aborted migration must not write the runtime document either");
            assertTrue(SettingsMigration.needsMigration(p), "the profile stays migratable, so the next start retries");
        } finally {
            backup.toFile().setWritable(true, false);
        }
    }

    /**
     * The all-or-nothing guarantee also covers a failure in the middle of the
     * move sequence: files moved before the failure are restored. The failure
     * is injected through the progress callback, which runs between moves.
     */
    @Test void aFailedLaterMoveRestoresTheAlreadyMovedFiles() throws Exception {
        Path p = tempDir;
        String legacySettings = "{\"x\":{\"a\":1}}";
        String legacyRecent = "/a/b.jip\n";
        Files.writeString(p.resolve("settings.json"), legacySettings);
        Files.writeString(p.resolve("recent-projects.txt"), legacyRecent);
        Path backup = p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME);
        JIPipeProgressInfo progress = new JIPipeProgressInfo() {
            @Override
            public void log(String message) {
                if (message.startsWith("Moved legacy settings file recent-projects.txt")) {
                    // The first move succeeded; make every later one fail.
                    backup.toFile().setWritable(false, false);
                }
            }
        };

        try {
            SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), progress);

            assertEquals(legacyRecent, Files.readString(p.resolve("recent-projects.txt")),
                    "the already-moved file must be restored to its original name");
            assertEquals(legacySettings, Files.readString(p.resolve("settings.json")));
            assertFalse(read(p.resolve("settings.json")).has("$schema"), "no new-format document may be written");
            assertFalse(Files.exists(p.resolve("runtime-settings.json")));
            assertTrue(SettingsMigration.needsMigration(p), "the profile stays migratable");
        } finally {
            backup.toFile().setWritable(true, false);
        }
    }

    @Test void interruptedMigrationIsRecoveredFromTheBackup() throws Exception {
        Path p = tempDir;
        // Post-crash state: the legacy files were moved, the process died
        // before the new documents were written.
        Path backup = Files.createDirectory(p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME));
        Files.writeString(backup.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        Files.writeString(backup.resolve("settings-db-node-db.json"), "{\"k\":\"v\"}");
        Files.writeString(backup.resolve("recent-projects.txt"), "/a/b.jip\n");
        assertFalse(SettingsMigration.needsMigration(p), "nothing legacy is left in the profile");

        assertTrue(SettingsMigration.recoverFromInterruptedMigration(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT));

        JsonNode global = read(p.resolve("settings.json"));
        assertEquals(1, global.get("sheet:x").get("a").asInt());
        assertEquals("v", global.get("registry:node-db").get("k").asText());
        assertEquals("/a/b.jip", global.get("state:recent-projects").get(0).asText());
        assertEquals(2, global.get("$schema").asInt());
        assertFalse(Files.exists(p.resolve("runtime-settings.json")), "nothing routes to RUNTIME");

        // Recovery never moves files: the only copy stays in the backup.
        assertTrue(Files.isRegularFile(backup.resolve("settings.json")));
        assertTrue(Files.isRegularFile(backup.resolve("settings-db-node-db.json")));
        assertTrue(Files.isRegularFile(backup.resolve("recent-projects.txt")));
    }

    @Test void noRecoveryAfterASuccessfulMigration() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("recent-projects.txt"), "/a/b.jip\n");
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
        // The backup inputs were cleaned out on success, so a user who deletes
        // settings.json afterwards must NOT get the stale profile back.
        Files.delete(p.resolve("settings.json"));

        assertFalse(SettingsMigration.recoverFromInterruptedMigration(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT));

        assertFalse(Files.exists(p.resolve("settings.json")), "no document may be resurrected");
        assertFalse(Files.exists(p.resolve("runtime-settings.json")));
    }

    @Test void recoveryOnlyAddsTheMissingRuntimeFileAndKeepsTheApplicationDocument() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"), """
                {
                  "$schema": 2,
                  "$meta": { "written-by-version": "6.0.0" },
                  "registry:node-db": { "keep": "existing" }
                }
                """);
        // Crash window: the runtime document was never written.
        Path backup = Files.createDirectory(p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME));
        Files.writeString(backup.resolve("settings-db-node-db.json"), "{\"keep\":\"legacy\", \"add\":\"legacy\"}");
        Files.writeString(backup.resolve("settings-db-rt.json"), "{\"threads\":4}");

        assertTrue(SettingsMigration.recoverFromInterruptedMigration(p, b -> true,
                Map.of("registry:rt", SettingsScope.RUNTIME), JIPipeProgressInfo.SILENT));

        JsonNode global = read(p.resolve("settings.json"));
        JsonNode runtime = read(p.resolve("runtime-settings.json"));
        assertEquals("existing", global.get("registry:node-db").get("keep").asText(),
                "an application block written meanwhile must not be clobbered");
        assertEquals(4, runtime.get("registry:rt").get("threads").asInt());
        assertFalse(runtime.has("registry:node-db"), "the application block stays in the application document");
    }
}
