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
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;

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
                    if (Files.isDirectory(sessionPath)) {
                        Path infoFile = sessionPath.resolve("backup-info.json");
                        if (Files.isRegularFile(infoFile)) {
                            if (!sessionHasBackups(sessionPath, progressInfo)) {
                                sessionsToDelete.add(sessionPath);
                            }
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Path path : sessionsToDelete) {
            progressInfo.log("Deleting: " + path);
            PathUtils.deleteDirectoryRecursively(path, progressInfo.resolve("Delete session"));
        }
    }

    /**
     * Checks whether the given backup session directory contains at least
     * one project backup file - in either format, {@code .jip} or
     * {@code .jipx} (see {@link JIPipeProjectBackupUtils#isBackupFile}).
     * A session with only .jipx backups is NOT empty (JIPX spec §7).
     *
     * @param sessionPath  the session directory
     * @param progressInfo the progress info
     * @return true if at least one backup file is present
     */
    public static boolean sessionHasBackups(Path sessionPath, JIPipeProgressInfo progressInfo) {
        boolean found = true;
        try (Stream<Path> stream1 = Files.list(sessionPath)) {
            found = stream1.anyMatch(backupFile -> Files.isRegularFile(backupFile) && JIPipeProjectBackupUtils.isBackupFile(backupFile));
        } catch (Exception e) {
            progressInfo.log("-> Error: " + e);
        }
        return found;
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
                Files.delete(backupItem.getProjectPath());
            } catch (Exception e) {
                getProgressInfo().log("-> Error: " + e);
            }
        }

        pruneEmptySessions(backupsDir, getProgressInfo().resolve("Prune"));


    }
}
