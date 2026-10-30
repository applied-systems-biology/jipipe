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

package org.hkijena.jipipe.api.service.init;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.JIPipeInitializationException;
import org.hkijena.jipipe.api.settings.SettingsBlockPath;
import org.hkijena.jipipe.api.settings.SettingsMigration;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The startup preparation of the settings file (Task 13 review, Finding 2):
 * a failure of the 5.x migration or of the interrupted-migration recovery must
 * abort the startup loudly, because continuing would let a later save write a
 * new-format {@code settings.json} from the (empty or partial) document — after
 * which {@code needsMigration} is false forever and the user's values would
 * exist only in {@code settings-migration-backup/}.
 * <p>
 * Exercises the package-visible seam
 * {@link JIPipeServiceDefaultInitializer#prepareApplicationSettings} directly, so
 * the contract is testable without booting the service.
 */
class SettingsStartupPreparationTest {

    private static final Predicate<String> REGISTERED = path -> new SettingsBlockPath(path).kind() != null;

    @TempDir
    Path tempDir;

    /**
     * A legacy profile whose backup directory is a regular FILE: the migration
     * cannot create the backup directory, so moving the legacy files fails and
     * the migration aborts (all-or-nothing, nothing written). The preparation
     * must surface that as a fatal initialization error, not log and continue.
     */
    @Test
    void failedMigrationAbortsStartupLoudly() throws Exception {
        Path profile = tempDir.resolve("profile");
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("recent-projects.txt"), "/a/b.jip\n");
        // The migration's target document is a DIRECTORY, so writing the new
        // settings.json fails after the legacy files were moved into the backup.
        Files.createDirectories(profile.resolve("settings.json"));

        JIPipeInitializationException failure = assertThrows(JIPipeInitializationException.class,
                () -> JIPipeServiceDefaultInitializer.prepareApplicationSettings(profile, REGISTERED,
                        java.util.Map.of(), JIPipeProgressInfo.SILENT, () -> {
                        }),
                "a failed migration must abort the startup instead of being swallowed");

        assertNotNull(failure.getCause(), "the underlying error must be preserved for the log");
        assertTrue(failure.getMessage().contains("migrate"),
                "the message must name the failed step: " + failure.getMessage());
        assertTrue(failure.getMessage().contains(SettingsMigration.BACKUP_DIRECTORY_NAME),
                "the message must tell where the settings are preserved: " + failure.getMessage());
    }

    /**
     * A failed recovery (the backup directory holds legacy input but is not
     * readable as such) is fatal too: continuing could overwrite the only copy.
     */
    @Test
    void failedRecoveryAbortsStartupLoudly() throws Exception {
        Path profile = tempDir.resolve("profile2");
        Files.createDirectories(profile);
        // Interrupted-migration shape: legacy input in the backup, and the
        // profile's settings.json is a DIRECTORY, so the recovery cannot write
        // the recovered document (an I/O failure inside the recovery).
        Path backup = profile.resolve(SettingsMigration.BACKUP_DIRECTORY_NAME);
        Files.createDirectories(backup);
        Files.writeString(backup.resolve("settings.json"), "{\"x\":{\"a\":1}}");
        Files.createDirectories(profile.resolve("settings.json"));

        assertThrows(JIPipeInitializationException.class,
                () -> JIPipeServiceDefaultInitializer.prepareApplicationSettings(profile, REGISTERED,
                        java.util.Map.of(), JIPipeProgressInfo.SILENT, () -> {
                        }),
                "a failed recovery must abort the startup instead of being swallowed");
    }

    /**
     * The happy path: a legacy profile is migrated, and the preparation does
     * not throw.
     */
    @Test
    void successfulMigrationMigratesAndDoesNotThrow() throws Exception {
        Path profile = tempDir.resolve("profile3");
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("settings.json"),
                "{\"org.hkijena.jipipe:general-ui\":{\"theme\":\"Dark\"}}");
        Files.writeString(profile.resolve("recent-projects.txt"), "/a/b.jip\n");

        AtomicInteger carryOverCalls = new AtomicInteger();
        assertDoesNotThrow(() -> JIPipeServiceDefaultInitializer.prepareApplicationSettings(profile, REGISTERED,
                java.util.Map.of(), JIPipeProgressInfo.SILENT, carryOverCalls::incrementAndGet));
        assertEquals(1, carryOverCalls.get(), "the carry-over runs once, after the migration");

        JsonNode migrated = JsonUtils.getObjectMapper().readTree(Files.readString(profile.resolve("settings.json")));
        assertEquals("Dark", migrated.get("sheet:org.hkijena.jipipe:general-ui").get("theme").asText(),
                "the legacy settings were migrated into the new shape");
        assertTrue(migrated.has("$schema"), "the migrated file is a new-format document");
        assertEquals("/a/b.jip", migrated.get("state:recent-projects").get(0).asText());
        assertTrue(Files.notExists(profile.resolve("recent-projects.txt")));
    }

    /**
     * A failing carry-over is NOT fatal (it only recovers unknown blocks from
     * sibling profiles).
     */
    @Test
    void failedCarryOverIsNotFatal() throws Exception {
        Path profile = tempDir.resolve("profile4");
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("settings.json"),
                "{\"$schema\":2,\"sheet:x\":{}}");

        assertDoesNotThrow(() -> JIPipeServiceDefaultInitializer.prepareApplicationSettings(profile, REGISTERED,
                java.util.Map.of(), JIPipeProgressInfo.SILENT, () -> {
                    throw new IllegalStateException("sibling scan failed");
                }),
                "the carry-over is non-load-bearing: its failure must not abort the startup");
    }

    /**
     * An already-migrated profile is a no-op: nothing throws, nothing is
     * rewritten, and the carry-over still runs.
     */
    @Test
    void alreadyMigratedProfileIsANoOp() throws Exception {
        Path profile = tempDir.resolve("profile5");
        Files.createDirectories(profile);
        Files.writeString(profile.resolve("settings.json"), "{\"$schema\":2,\"sheet:x\":{\"a\":1}}");
        byte[] before = Files.readAllBytes(profile.resolve("settings.json"));

        assertDoesNotThrow(() -> JIPipeServiceDefaultInitializer.prepareApplicationSettings(profile, REGISTERED,
                java.util.Map.of(), JIPipeProgressInfo.SILENT, () -> {
                }));
        org.junit.jupiter.api.Assertions.assertArrayEquals(before,
                Files.readAllBytes(profile.resolve("settings.json")),
                "a migrated profile must not be rewritten");
    }
}