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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.settings.application.JIPipeBackupApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PruneBackupsRun extends DefaultJIPipeRunnable {
    private final boolean deleteAllUnnamed;
    private final boolean deleteAllNamed;

    public PruneBackupsRun(boolean deleteAllUnnamed, boolean deleteAllNamed) {
        this.deleteAllUnnamed = deleteAllUnnamed;
        this.deleteAllNamed = deleteAllNamed;
    }

    public static void pruneEmptySessions(Path backupsDir, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Pruning empty sessions ...");

        List<Path> sessionsToDelete = new ArrayList<>();
        try (Stream<Path> stream = Files.list(backupsDir)) {
            stream.forEach(sessionPath -> {
                if (Files.isDirectory(sessionPath)) {
                    Path infoFile = sessionPath.resolve("backup-info.json");
                    if (Files.isRegularFile(infoFile)) {
                        if (!sessionHasBackups(sessionPath, progressInfo)) {
                            sessionsToDelete.add(sessionPath);
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Path path : sessionsToDelete) {
            progressInfo.log("Deleting: " + path);
            // Delete the session's snapshot manifests BEFORE the directory goes,
            // so the objects they reference become unreferenced and a later
            // garbage collection run can reclaim them
            deleteSessionSnapshotManifests(backupsDir, path, progressInfo);
            PathUtils.deleteDirectoryRecursively(path, progressInfo.resolve("Delete session"));
        }
    }

    /**
     * Checks whether the given backup session directory contains at least
     * one backup — a legacy project backup FILE in either format,
     * {@code .jip} or {@code .jipx} (see
     * {@link JIPipeProjectBackupUtils#isBackupFile}), or a NON-EMPTY
     * {@code snapshots.json} index of content-addressed snapshot manifests
     * (a snapshot-only session is NOT empty).
     * <p>
     * A session with only .jipx backups is NOT empty (JIPX spec §7).
     * A {@code snapshots.json} that cannot be parsed counts as EMPTY
     * (a corrupt index must not keep an otherwise empty session alive).
     *
     * @param sessionPath  the session directory
     * @param progressInfo the progress info
     * @return true if at least one backup is present
     */
    public static boolean sessionHasBackups(Path sessionPath, JIPipeProgressInfo progressInfo) {
        boolean found = true;
        try (Stream<Path> stream1 = Files.list(sessionPath)) {
            found = stream1.anyMatch(backupFile -> Files.isRegularFile(backupFile) && JIPipeProjectBackupUtils.isBackupFile(backupFile));
        } catch (Exception e) {
            progressInfo.log("-> Error: " + e);
        }
        if (!found) {
            // No legacy files — check the snapshot index
            found = hasSnapshotIndex(sessionPath, progressInfo);
        }
        return found;
    }

    /**
     * @return true if the session has a {@code snapshots.json} index that
     * parses and is non-empty
     */
    private static boolean hasSnapshotIndex(Path sessionPath, JIPipeProgressInfo progressInfo) {
        Path indexFile = sessionPath.resolve("snapshots.json");
        if (!Files.isRegularFile(indexFile)) {
            return false;
        }
        try {
            List<String> ids = JsonUtils.readListFromFile(indexFile, String.class);
            return ids != null && !ids.isEmpty();
        } catch (Exception e) {
            // A corrupt index counts as empty
            progressInfo.log("Could not read snapshot index " + indexFile + ", treating the session as empty: " + e);
            return false;
        }
    }

    /**
     * Deletes every snapshot manifest the given session's
     * {@code snapshots.json} index references
     * ({@code <backupsDir>/snapshots/<id>.json}). Best-effort: each failure
     * is logged and does not stop the sweep, so the session directory is
     * removed afterwards regardless (a leftover manifest is an orphan that
     * the next garbage collection run would not see, but the index — its
     * only reference point — is gone with the session).
     */
    private static void deleteSessionSnapshotManifests(Path backupsDir, Path sessionPath, JIPipeProgressInfo progressInfo) {
        Path indexFile = sessionPath.resolve("snapshots.json");
        if (!Files.isRegularFile(indexFile)) {
            return;
        }
        List<String> ids;
        try {
            ids = JsonUtils.readListFromFile(indexFile, String.class);
        } catch (Exception e) {
            progressInfo.log("Could not read snapshot index " + indexFile + ", skipping manifest cleanup: " + e);
            return;
        }
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Path snapshotsDirectory = backupsDir.resolve("snapshots");
        for (String id : ids) {
            Path manifest = snapshotsDirectory.resolve(id + ".json");
            try {
                if (Files.deleteIfExists(manifest)) {
                    progressInfo.log("Deleted snapshot manifest " + manifest + " (session is being pruned)");
                }
            } catch (IOException e) {
                progressInfo.log("Could not delete snapshot manifest " + manifest + ": " + e);
            }
        }
    }

    @Override
    public String getTaskLabel() {
        return "Prune backups";
    }

    @Override
    public void run() {
        Path backupsDir = JIPipe.getInstance().getProjectBackup().getCurrentBackupPath();
        CollectBackupsRun subRun = new CollectBackupsRun();
        subRun.setProgressInfo(getProgressInfo().resolve("Collecting backups"));
        subRun.run();

        List<JIPipeProjectBackupItemCollection> backupItemCollections = subRun.getOutput();
        List<JIPipeProjectBackupItem> itemsToDelete = new ArrayList<>();

        for (JIPipeProjectBackupItemCollection backupItemCollection : backupItemCollections) {
            for (JIPipeProjectBackupItem backupItem : backupItemCollection.getBackupItemList()) {
                boolean delete = false;
                if (!StringUtils.isNullOrEmpty(backupItem.getOriginalProjectPath())) {
                    if (deleteAllNamed) {
                        delete = true;
                    }
                } else {
                    if (deleteAllUnnamed) {
                        delete = true;
                    }
                }

                if (delete) {
                    itemsToDelete.add(backupItem);
                }
            }
        }

        getProgressInfo().log("-> Collected " + itemsToDelete.size() + " items to delete");

        for (JIPipeProjectBackupItem backupItem : itemsToDelete) {
            try {
                getProgressInfo().log("Delete: " + backupItem);
                backupItem.delete(getProgressInfo());
            } catch (Exception e) {
                getProgressInfo().log("-> Error: " + e);
            }
        }

        pruneEmptySessions(backupsDir, getProgressInfo().resolve("Prune"));


    }
}
