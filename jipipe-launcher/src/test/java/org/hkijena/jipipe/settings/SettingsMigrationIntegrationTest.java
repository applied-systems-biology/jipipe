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

package org.hkijena.jipipe.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.SettingsDocument;
import org.hkijena.jipipe.api.settings.SettingsFileStore;
import org.hkijena.jipipe.api.settings.SettingsMigration;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.plugins.settings.SettingsScopes;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 17 of the modern settings system: the golden-profile integration gate.
 * <p>
 * Three end-to-end scenarios against <b>committed</b> fixture profiles
 * ({@code src/test/resources/settings-fixtures/}, a single-source ship set: the
 * plan owns every byte). Each scenario runs in a temp profile directory only;
 * the developer's real profile is never read or written.
 * <ol>
 * <li>{@link #legacyProfileMigratesWithoutLoss()} — a 5.x-shaped profile
 * (flat {@code settings.json} + two {@code settings-db-*.json} +
 * {@code recent-projects.txt}) goes through the real
 * {@link SettingsMigration} code path with the static scope routing ({@link
 * SettingsScopes#scopeByBlock()}) and the real {@link SettingsFileStore}
 * writer, and comes out as the new-shape {@code settings.json} +
 * {@code runtime-settings.json}: every legacy block is in the document of its
 * scope, the legacy files are gone from the profile root, the backup directory
 * is clean, and {@code needsMigration} is false afterwards.</li>
 * <li>{@link #unknownBlocksSurviveASaveCycle()} — a 6.x profile containing an
 * unknown block kind, an unknown sheet block and an unknown sub-path inside a
 * known block round-trips through a {@code load → save} cycle with no loss
 * (the anti-regression property of the pass-through design).</li>
 * <li>{@link #leftoverTempFileAndPreviousFileLoadCleanly()} — a profile whose
 * process was killed mid-write (an orphan {@code settings.json.tmp.<id>} next
 * to the last good {@code settings.json}) loads the good document, and the
 * orphan cleanup removes the stale tmp file without touching the settings.</li>
 * </ol>
 * <p>
 * <b>Scope routing.</b> The migration in {@link #legacyProfileMigratesWithoutLoss()}
 * is pinned against the <em>static</em> routing map
 * ({@link SettingsScopes#scopeByBlock()}). That is exactly the routing the
 * production startup uses <em>at migration time</em>: the initializer
 * ({@code JIPipeServiceDefaultInitializer#prepareApplicationSettings}) passes
 * the component's effective map ({@code effectiveScopeByBlock()}, which layers
 * RUNTIME-scoped <em>registered keys</em> over the static map), but the
 * migration runs before any plugin — and therefore before any setting key —
 * is registered, so the effective map then equals the static one. What this
 * test does <em>not</em> prove: a block that is RUNTIME-scoped only via a
 * registered key still routes correctly during migration (that layer is
 * covered by {@code SettingsScopesTest}).
 * <p>
 * <b>Why not the fully booted service.</b> The startup path
 * ({@code JIPipeServiceDefaultInitializer#prepareApplicationSettingsDocument})
 * is a thin call into {@link SettingsMigration} followed by the first document
 * load; a fully booted service then attaches every sheet bridge, which
 * legitimately replaces a registered sheet's migrated fallback values with
 * that sheet's own defaults. This class therefore asserts the migration's
 * output (the documents on disk and their load back through
 * {@link SettingsFileStore}), which is what "nothing is lost" means, and stays
 * independent of the JVM-wide {@code JIPipe} singleton and of test ordering
 * (it needs the ImageJ1 legacy patcher only as a side effect of the surrounding
 * suite's listener).
 */
class SettingsMigrationIntegrationTest {

    /**
     * The committed fixture profiles, resolved from the test classpath.
     */
    private static final String FIXTURE_ROOT = "settings-fixtures";

    private static Path fixture(String name) throws URISyntaxException {
        URL url = SettingsMigrationIntegrationTest.class.getClassLoader().getResource(FIXTURE_ROOT + "/" + name);
        assertNotNull(url, "fixture profile '" + name + "' must be committed under src/test/resources/" + FIXTURE_ROOT);
        return Path.of(url.toURI());
    }

    // ------------------------------------------------------------------
    // 1. The 5.x profile migrates without loss
    // ------------------------------------------------------------------

    @Test
    void legacyProfileMigratesWithoutLoss(@TempDir Path tempBase) throws Exception {
        // The profile dir is <base>/<version>; the version is RESOLVED, never
        // hardcoded, so a version bump cannot break this test.
        String version = VersionUtils.getJIPipeVersion();
        Path profileDir = tempBase.resolve(version);
        copyDirectory(fixture("legacy-5x-profile"), profileDir);
        // The fixture's recent-projects.txt names two projects; the recent
        // projects registry prunes entries whose files do not exist, so the
        // test creates them and pins the fixture to that (temp) location.
        Path recentProjectA = tempBase.resolve("recent-project-a.jip");
        Path recentProjectB = tempBase.resolve("recent-project-b.jip");
        Files.writeString(recentProjectA, "{}");
        Files.writeString(recentProjectB, "{}");
        Files.writeString(profileDir.resolve("recent-projects.txt"),
                recentProjectA + System.lineSeparator() + recentProjectB + System.lineSeparator());

        // Preconditions: a genuinely 5.x-shaped profile.
        assertTrue(SettingsMigration.needsMigration(profileDir), "the fixture must be a 5.x-shaped profile");
        assertTrue(Files.isRegularFile(profileDir.resolve("settings.json")));
        assertTrue(Files.isRegularFile(profileDir.resolve("settings-db-ui-general.json")));
        assertTrue(Files.isRegularFile(profileDir.resolve("settings-db-node-db.json")));
        assertTrue(Files.isRegularFile(profileDir.resolve("recent-projects.txt")));
        assertFalse(Files.exists(profileDir.resolve("runtime-settings.json")));

        // The real migration, with the static scope routing. This is the
        // initializer's effective routing at migration time: no setting keys
        // are registered before the migration runs, so
        // effectiveScopeByBlock() equals the static map then (see the class
        // Javadoc). A block that is RUNTIME-scoped only via a registered key
        // is not covered by this test (SettingsScopesTest covers that layer).
        SettingsMigration.migrateLegacyProfile(profileDir, path -> true,
                SettingsScopes.scopeByBlock(), JIPipeProgressInfo.SILENT);

        // --- the migration state itself ---
        assertFalse(SettingsMigration.needsMigration(profileDir),
                "after a successful migration no legacy input is left");
        for (String legacyName : List.of("settings-db-ui-general.json",
                "settings-db-node-db.json", "recent-projects.txt")) {
            assertFalse(Files.exists(profileDir.resolve(legacyName)),
                    "legacy file " + legacyName + " must be gone from the profile root");
        }
        // settings.json is reused by the new document, so it must be the
        // NEW-shape file now (rewritten), not the old flat one.
        JsonNode applicationOnDisk = JsonUtils.getObjectMapper()
                .readTree(Files.readString(profileDir.resolve("settings.json")));
        assertTrue(applicationOnDisk.has("$schema"),
                "settings.json at the profile root must be the migrated (new-shape) document");
        Path backupDir = profileDir.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME);
        assertTrue(Files.isDirectory(backupDir), "the backup directory must exist after the migration");
        // A SUCCESSFUL migration moves every legacy file into the backup and
        // then deletes those copies again (see
        // SettingsMigration#removeMigratedBackups): what stays in the backup
        // is only the corruption backups (.bad-*) and unrelated user files, so
        // a stale backup can never be mistaken for an interrupted migration
        // (see recoverFromInterruptedMigration). The absence of the legacy
        // copies is therefore the proof that the migration completed.
        for (String legacyName : List.of("settings.json", "settings-db-ui-general.json",
                "settings-db-node-db.json", "recent-projects.txt")) {
            assertFalse(Files.exists(backupDir.resolve(legacyName)),
                    "a successful migration removes the migrated backup copy of " + legacyName);
        }
        assertTrue(isEmptyDirectory(backupDir),
                "the fixture has no corrupt legacy file, so the backup directory must be empty afterwards");

        // --- the runtime document was created and is new-shape ---
        Path runtimeFile = profileDir.resolve("runtime-settings.json");
        assertTrue(Files.isRegularFile(runtimeFile),
                "a runtime sheet block in the legacy profile must create the runtime document");
        JsonNode runtimeOnDisk = JsonUtils.getObjectMapper().readTree(Files.readString(runtimeFile));
        assertTrue(runtimeOnDisk.has("$schema"), "the new runtime-settings.json carries $schema");

        // --- every legacy block landed in the document of its scope ---
        // Application block A: a registered application sheet.
        assertEquals("from-5x-general-ui",
                applicationOnDisk.get("sheet:org.hkijena.jipipe:general-ui").get("legacy-marker-a").asText());
        assertTrue(applicationOnDisk.get("sheet:org.hkijena.jipipe:general-ui").get("legacy-flag").asBoolean());
        // Application block B: an invented sheet ID this version does not know
        // (5.x pass-through: it must still be migrated, not dropped).
        assertEquals("from-5x-unknown-sheet",
                applicationOnDisk.get("sheet:org.hkijena.jipipe:totally-unknown-sheet").get("legacy-marker-b").asText());
        // Application registries: two registry databases.
        assertEquals("from-5x-ui-general-db",
                applicationOnDisk.get("registry:ui-general").get("legacy-marker-c").asText());
        assertEquals(7, applicationOnDisk.get("registry:ui-general").get("legacy-counter").asInt());
        // A registry database with a multi-segment path entry (the shape the
        // legacy JIPipePathMetadataStore stores for key(a, b)) and a flat one.
        assertEquals("from-5x-node-db",
                applicationOnDisk.get("registry:node-db").get("legacy-pinned").get("id").asText());
        assertEquals(2, applicationOnDisk.get("registry:node-db").get("legacy-pinned").get("ids").size());
        assertEquals(11, applicationOnDisk.get("registry:node-db").get("legacy-counter").asInt());
        // Application state: the recent projects file.
        JsonNode recentProjects = applicationOnDisk.get("state:recent-projects");
        assertEquals(2, recentProjects.size());
        assertEquals(recentProjectA.toString(), recentProjects.get(0).asText());
        assertEquals(recentProjectB.toString(), recentProjects.get(1).asText());

        // Runtime block: routed per SettingsScopes.scopeByBlock().
        assertEquals(SettingsScope.RUNTIME,
                SettingsScopes.scopeByBlock().get("sheet:org.hkijena.jipipe:runtime"),
                "the runtime sheet must be routed to the RUNTIME scope");
        assertEquals("from-5x-runtime", runtimeOnDisk.get("sheet:org.hkijena.jipipe:runtime").get("legacy-marker-e").asText());
        assertEquals(3, runtimeOnDisk.get("sheet:org.hkijena.jipipe:runtime").get("legacy-threads").asInt());
        assertFalse(applicationOnDisk.has("sheet:org.hkijena.jipipe:runtime"),
                "the runtime block must not be duplicated into settings.json");
    }

    // ------------------------------------------------------------------
    // 2. Unknown blocks survive a load → save cycle
    // ------------------------------------------------------------------

    @Test
    void unknownBlocksSurviveASaveCycle(@TempDir Path profileDir) throws Exception {
        copyDirectory(fixture("unknown-sheet-profile"), profileDir);
        Path settingsFile = profileDir.resolve("settings.json");
        byte[] before = Files.readAllBytes(settingsFile);

        JIPipeSettingsServiceComponent component = JIPipeSettingsServiceComponent.forTesting(profileDir);
        try {
            // Loading through the service must not fail and must keep the
            // unknown blocks (the store's predicate only classifies; it never
            // filters).
            SettingsDocument doc = component.document(SettingsScope.APPLICATION);
            assertTrue(doc.isPassThrough("future-thing:abc"), "an unknown kind prefix is a pass-through block");
            // "Known kind, unknown id": the registry kind is known, the block is
            // not registered by this version, so it is pass-through content that
            // must round-trip verbatim.
            assertEquals("written-by-a-newer-version",
                    doc.get("registry:fixture-registry", "future-sub/nested").asText());
            assertEquals(2, doc.get("sheet:org.hkijena.jipipe:totally-unknown-sheet", "nested/one").size());
            assertFalse(doc.isPassThrough("sheet:org.hkijena.jipipe:totally-unknown-sheet"),
                    "an unknown sheet ID still has a known kind");
            assertTrue(doc.hasBlock("future-thing:abc"));
            assertTrue(doc.hasBlock("sheet:org.hkijena.jipipe:totally-unknown-sheet"));

            // A save cycle (flush + atomic write of the loaded document).
            component.save();

            JsonNode after = JsonUtils.getObjectMapper().readTree(Files.readString(settingsFile));
            JsonNode beforeTree = JsonUtils.getObjectMapper().readTree(new String(before, StandardCharsets.UTF_8));

            // (a) Unknown block KIND.
            assertTrue(after.has("future-thing:abc"), "an unknown block kind must survive a save cycle");
            assertEquals(beforeTree.get("future-thing:abc"), after.get("future-thing:abc"));

            // (b) Unknown SHEET block (not registered by any sheet).
            assertTrue(after.has("sheet:org.hkijena.jipipe:totally-unknown-sheet"),
                    "an unknown sheet block must survive a save cycle");
            assertEquals(beforeTree.get("sheet:org.hkijena.jipipe:totally-unknown-sheet"),
                    after.get("sheet:org.hkijena.jipipe:totally-unknown-sheet"));
            assertEquals("from-6x-unknown-sheet",
                    after.get("sheet:org.hkijena.jipipe:totally-unknown-sheet").get("legacy-marker-b").asText());

            // (c) Unknown SUB-PATH inside a known block — the specific value,
            // not just the block.
            assertEquals("written-by-a-newer-version",
                    after.get("registry:fixture-registry").get("future-sub").get("nested").asText());
            assertTrue(after.get("registry:fixture-registry").get("future-sub").get("flag").asBoolean());

            // And the known sub-paths of the known block are still intact.
            assertEquals("kept", after.get("registry:fixture-registry").get("known-value").asText());

            // Whole-document equality for every block: everything survives
            // the cycle. ($meta/$schema are the writer's bookkeeping — the
            // component stamps written-by/written-at/writer-id on every save
            // — so the content comparison covers the blocks.)
            ObjectNode blocksBefore = beforeTree.deepCopy();
            ObjectNode blocksAfter = after.deepCopy();
            blocksBefore.remove(List.of("$schema", "$meta"));
            blocksAfter.remove(List.of("$schema", "$meta"));
            assertEquals(blocksBefore, blocksAfter, "every block must survive the save cycle");
        } finally {
            component.dispose();
        }
    }

    // ------------------------------------------------------------------
    // 3. Killed mid-write: orphan tmp + previous good file
    // ------------------------------------------------------------------

    @Test
    void leftoverTempFileAndPreviousFileLoadCleanly(@TempDir Path profileDir) throws Exception {
        copyDirectory(fixture("unknown-sheet-profile"), profileDir);
        Path settingsFile = profileDir.resolve("settings.json");
        byte[] good = Files.readAllBytes(settingsFile);

        // Simulate a writer that was killed after writing its temp file but
        // before the atomic rename. Backdate it past the 24 h orphan window.
        Path orphan = profileDir.resolve("settings.json.tmp.dead-writer-4711");
        Files.writeString(orphan, "{\n  \"$schema\": 2,\n  \"registry:fixture-registry\": { \"trunc");
        Files.setLastModifiedTime(orphan, FileTime.from(Instant.now().minus(48, ChronoUnit.HOURS)));

        // 1) The load uses the good file, not the orphan.
        JIPipeSettingsServiceComponent component = JIPipeSettingsServiceComponent.forTesting(profileDir);
        try {
            SettingsDocument doc = component.document(SettingsScope.APPLICATION);
            assertEquals("kept", doc.get("registry:fixture-registry", "known-value").asText(),
                    "the last good settings file must be loaded");
            assertTrue(doc.hasBlock("future-thing:abc"));

            // 2) The orphan cleanup removes the stale tmp file...
            SettingsFileStore.cleanUpOrphanTempFiles(profileDir);
            assertFalse(Files.exists(orphan), "the stale orphan temp file must be removed");

            // ... and nothing else changed on disk.
            assertTrue(Files.isRegularFile(settingsFile));
            assertArrayEquals(good, Files.readAllBytes(settingsFile),
                    "the orphan cleanup must not touch the good settings file");
            assertTrue(listBadBackups(profileDir).isEmpty(),
                    "a load with a good settings file must not create a .bad- backup");

            // 3) A following save keeps every setting (only the $meta
            // bookkeeping changes), and leaves no tmp file behind.
            component.save();
            JsonNode goodTree = JsonUtils.getObjectMapper().readTree(new String(good, StandardCharsets.UTF_8));
            JsonNode savedTree = JsonUtils.getObjectMapper().readTree(Files.readString(settingsFile));
            assertEquals(goodTree.get("registry:fixture-registry"), savedTree.get("registry:fixture-registry"));
            assertEquals(goodTree.get("future-thing:abc"), savedTree.get("future-thing:abc"));
            assertTrue(hasNoTmpFiles(profileDir), "an atomic save must not leave a tmp file behind");
        } finally {
            component.dispose();
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static boolean isEmptyDirectory(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.findAny().isEmpty();
        }
    }

    private static boolean hasNoTmpFiles(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.noneMatch(p -> p.getFileName().toString().contains(".tmp."));
        }
    }

    private static List<Path> listBadBackups(Path dir) throws IOException {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().contains(".bad-")).forEach(result::add);
        }
        return result;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}