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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsMigrationTest {
    @TempDir Path tempDir;

    private static JsonNode read(Path file) throws Exception {
        return JsonUtils.getObjectMapper().readTree(Files.readString(file));
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

    @Test void noOpRunDoesNotTouchTheBackupDirectory() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
        Path backup = p.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME);
        long backupCount = Files.list(backup).count();
        String backupBytes = Files.readString(backup.resolve("settings.json"));

        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);

        assertEquals(backupCount, Files.list(backup).count(), "a no-op run must not touch the backup directory");
        assertEquals(backupBytes, Files.readString(backup.resolve("settings.json")),
                "a no-op run must not overwrite backup content");
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

        // Backup: all legacy files moved, none left behind.
        Path backup = p.resolve("settings-migration-backup");
        assertTrue(Files.isRegularFile(backup.resolve("settings.json")));
        assertTrue(Files.isRegularFile(backup.resolve("settings-db-node-db.json")));
        assertTrue(Files.isRegularFile(backup.resolve("settings-db-other.json")));
        assertTrue(Files.isRegularFile(backup.resolve("recent-projects.txt")));
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
        assertEquals("{\"x\":{\"a\":1}}", Files.readString(backup.resolve("settings-1.json")),
                "a clashing name gets the first free -1, -2, … suffix");
        assertEquals("{\"k\":\"v\"}", Files.readString(backup.resolve("settings-db-node-db.json")),
                "the plain name was free, so no suffix is needed");
    }

    @Test void backupOfTheSameFileTwiceGetsDistinctNames() throws Exception {
        Path p = tempDir;
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        // An interrupted migration left the new-format settings.json plus a
        // fresh legacy settings.json (as the profile carry-over would).
        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);
        Files.writeString(p.resolve("settings.json"), "{\"x\":{\"a\":2}}");

        SettingsMigration.migrateLegacyProfile(p, b -> true, Map.of(), JIPipeProgressInfo.SILENT);

        Path backup = p.resolve("settings-migration-backup");
        assertEquals("{\"x\":{\"a\":1}}", Files.readString(backup.resolve("settings.json")));
        assertEquals("{\"x\":{\"a\":2}}", Files.readString(backup.resolve("settings-1.json")));
        assertEquals(2, read(p.resolve("settings.json")).get("sheet:x").get("a").asInt());
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
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> SettingsMigration.migrateLegacyProfile(tempDir, null, Map.of(), JIPipeProgressInfo.SILENT));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> SettingsMigration.migrateLegacyProfile(tempDir, b -> true, null, JIPipeProgressInfo.SILENT));
        assertFalse(SettingsMigration.needsMigration(tempDir.resolve("does-not-exist")));
    }
}