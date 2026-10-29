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

package org.hkijena.jipipe.api.backups;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The backup file recognition contract (JIPX spec §7: "New backups of
 * .jipx projects are .jipx").
 * <p>
 * Backups exist in both formats since 6.0.0. Every place that scans a
 * backup directory must accept BOTH - otherwise .jipx backups are
 * invisible in the backup manager, and a session directory whose
 * backups are ALL .jipx is considered empty and DELETED by
 * {@link PruneBackupsRun#pruneEmptySessions} (the ThinBackupsRun
 * auto-cleanup runs at startup by default).
 */
class JIPipeProjectBackupUtilsTest {

    @Test
    void recognizesBothBackupFormats() {
        // .jip recognized (legacy backups keep working)
        assertTrue(JIPipeProjectBackupUtils.isBackupFile(Path.of("/backups/session_1/backup_2026.jip")));
        // .jipx recognized (new backups)
        assertTrue(JIPipeProjectBackupUtils.isBackupFile(Path.of("/backups/session_1/backup_2026.jipx")));
        // Case-insensitive
        assertTrue(JIPipeProjectBackupUtils.isBackupFile(Path.of("/backups/session_1/backup_2026.JIPX")));
        assertTrue(JIPipeProjectBackupUtils.isBackupFile(Path.of("/backups/session_1/backup_2026.JIP")));
        // Null-safe
        assertFalse(JIPipeProjectBackupUtils.isBackupFile(null));
        // Non-project files are not backups
        assertFalse(JIPipeProjectBackupUtils.isBackupFile(Path.of("/backups/session_1/backup-info.json")));
        assertFalse(JIPipeProjectBackupUtils.isBackupFile(Path.of("/backups/session_1/backup_2026.jipc")));
        assertFalse(JIPipeProjectBackupUtils.isBackupFile(Path.of("/backups/session_1/backup_2026.zip")));
    }

    @Test
    void jipxOnlySessionIsNotEmpty(@TempDir Path sessionDir) throws IOException {
        // A session directory whose only backup is a .jipx file must be
        // treated as NON-EMPTY by the prune logic (otherwise
        // pruneEmptySessions would delete it - the startup auto-cleanup
        // runs by default).
        Files.createDirectories(sessionDir);
        Files.writeString(sessionDir.resolve("backup_2026-10-23T10-00-00.jipx"), "fake");
        assertTrue(PruneBackupsRun.sessionHasBackups(sessionDir, JIPipeProgressInfo.STDOUT),
                "A .jipx-only session must not be considered empty");
    }

    @Test
    void mixedSessionIsNotEmpty(@TempDir Path sessionDir) throws IOException {
        Files.createDirectories(sessionDir);
        Files.writeString(sessionDir.resolve("backup_a.jip"), "fake");
        Files.writeString(sessionDir.resolve("backup_b.jipx"), "fake");
        assertTrue(PruneBackupsRun.sessionHasBackups(sessionDir, JIPipeProgressInfo.STDOUT));
    }

    @Test
    void trulyEmptySessionIsEmpty(@TempDir Path sessionDir) throws IOException {
        Files.createDirectories(sessionDir);
        Files.writeString(sessionDir.resolve("backup-info.json"), "{}");
        assertFalse(PruneBackupsRun.sessionHasBackups(sessionDir, JIPipeProgressInfo.STDOUT),
                "A session with only backup-info.json (no project files) is empty");
    }

    @Test
    void pruneKeepsJipxOnlySession(@TempDir Path backupsDir) throws IOException {
        // End-to-end: a session dir with ONLY a .jipx backup must survive
        // pruneEmptySessions (the regression this contract guards).
        Path session = backupsDir.resolve("session-jipx-only");
        Files.createDirectories(session);
        Files.writeString(session.resolve("backup-info.json"), "{}");
        Files.writeString(session.resolve("backup_2026-10-23T10-00-00.jipx"), "fake");
        Path emptySession = backupsDir.resolve("session-empty");
        Files.createDirectories(emptySession);
        Files.writeString(emptySession.resolve("backup-info.json"), "{}");

        PruneBackupsRun.pruneEmptySessions(backupsDir, JIPipeProgressInfo.STDOUT);

        try (Stream<Path> remaining = Files.list(backupsDir)) {
            assertTrue(remaining.anyMatch(p -> p.getFileName().toString().equals("session-jipx-only")),
                    "The .jipx-only session must survive pruning; remaining: " + backupsDir);
        }
        assertFalse(Files.exists(emptySession),
                "The truly empty session must be pruned");
    }
}
