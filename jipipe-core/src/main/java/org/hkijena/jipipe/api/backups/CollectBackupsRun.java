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
import org.hkijena.jipipe.api.backups.store.JIPipeBackupSnapshot;
import org.hkijena.jipipe.plugins.settings.application.JIPipeBackupApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectBackupsRun extends DefaultJIPipeRunnable {

    private final List<JIPipeProjectBackupItemCollection> output = new ArrayList<>();

    @Override
    public String getTaskLabel() {
        return "Loading backups";
    }

    public List<JIPipeProjectBackupItemCollection> getOutput() {
        return output;
    }

    @Override
    public void run() {
        Path backupsDir = JIPipe.getInstance().getProjectBackup().getCurrentBackupPath();
        getProgressInfo().log("Reading backups from " + backupsDir);

        // Collect all the backup items
        List<JIPipeProjectBackupItem> items = new ArrayList<>();

        try (Stream<Path> stream = Files.list(backupsDir)) {
            stream.forEach(sessionPath -> {
                if (Files.isDirectory(sessionPath)) {
                    Path infoFile = sessionPath.resolve("backup-info.json");
                    if (Files.isRegularFile(infoFile)) {
                        getProgressInfo().log("Reading backup info: " + infoFile);
                        try {
                            JIPipeProjectBackupSessionInfo sessionInfo = JsonUtils.readFromFile(infoFile, JIPipeProjectBackupSessionInfo.class);
                            String sessionId;
                            if (StringUtils.isNullOrEmpty(sessionInfo.getProjectSessionId())) {
                                sessionId = sessionPath.getFileName().toString();
                            } else {
                                sessionId = sessionInfo.getProjectSessionId();
                            }
                            // Content-addressed snapshot items (snapshots.json index)
                            collectSnapshotItems(backupsDir, sessionPath, sessionInfo, sessionId, items);
                            // Legacy file items (.jip/.jipx files in the session dir)
                            try (Stream<Path> stream1 = Files.list(sessionPath)) {
                                stream1.forEach(backupFile -> {
                                    try {
                                        if (Files.isRegularFile(backupFile) && JIPipeProjectBackupUtils.isBackupFile(backupFile)) {
                                            JIPipeProjectBackupItem item = new JIPipeProjectBackupItem();
                                            item.setProjectPath(backupFile);
                                            item.setBackupTime(LocalDateTime.ofInstant(Files.getLastModifiedTime(backupFile).toInstant(), ZoneId.systemDefault()));
                                            item.setSessionId(sessionId);
                                            item.setOriginalProjectPath(sessionInfo.getProjectStoragePath());
                                            items.add(item);
                                        }
                                    } catch (Exception e) {
                                        getProgressInfo().log("Unable to read: " + backupFile);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            getProgressInfo().log("... Failed!");
                            getProgressInfo().log(e.toString());
                        }
                    }
                } else if (Files.isRegularFile(sessionPath) && JIPipeProjectBackupUtils.isBackupFile(sessionPath)) {
                    try {
                        // Legacy backup
                        getProgressInfo().log("Adding legacy backup " + sessionPath);
                        JIPipeProjectBackupItem item = new JIPipeProjectBackupItem();
                        item.setProjectPath(sessionPath);
                        item.setSessionId("legacy");
                        item.setBackupTime(LocalDateTime.ofInstant(Files.getLastModifiedTime(sessionPath).toInstant(), ZoneId.systemDefault()));
                        items.add(item);
                    } catch (Exception e) {
                        getProgressInfo().log("... Failed!");
                        getProgressInfo().log(e.toString());
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Fix legacy sessions
        for (JIPipeProjectBackupItem item : items) {
            if (StringUtils.isNullOrEmpty(item.getSessionId())) {
                item.setSessionId("unnamed-" + UUID.randomUUID());
            } else if (item.getSessionId().equals("legacy")) {
                item.setSessionId("legacy-" + UUID.randomUUID());
            }
        }

        // Organize items by session and create collections
        Map<String, List<JIPipeProjectBackupItem>> groupedBackups = items.stream().collect(Collectors.groupingBy(JIPipeProjectBackupItem::getSessionId));
        for (Map.Entry<String, List<JIPipeProjectBackupItem>> entry : groupedBackups.entrySet()) {
            JIPipeProjectBackupItemCollection collection = new JIPipeProjectBackupItemCollection();
            collection.setSessionId(entry.getKey());
            collection.getBackupItemList().addAll(entry.getValue());
            collection.getBackupItemList().sort(Comparator.comparing(JIPipeProjectBackupItem::getBackupTime).reversed());
            collection.setLegacy(entry.getKey().startsWith("legacy-"));
            output.add(collection);
        }

        // Sort collections by lowest date
        Map<String, LocalDateTime> groupedBackupModificationTimes = new HashMap<>();
        for (Map.Entry<String, List<JIPipeProjectBackupItem>> entry : groupedBackups.entrySet()) {
            JIPipeProjectBackupItem newestBackup = entry.getValue().stream().sorted(Comparator.comparing(JIPipeProjectBackupItem::getBackupTime).reversed()).findFirst().get();
            groupedBackupModificationTimes.put(entry.getKey(), newestBackup.getBackupTime());
        }
        output.sort(Comparator.comparing((JIPipeProjectBackupItemCollection item) -> groupedBackupModificationTimes.get(item.getSessionId())).reversed());

        // Pruning
        PruneBackupsRun.pruneEmptySessions(backupsDir, getProgressInfo().resolve("Prune"));
    }

    /**
     * Collects the content-addressed snapshot items of one session: for each
     * id in the session's {@code snapshots.json} index, the manifest
     * {@code <backupsDir>/snapshots/<id>.json} becomes an item (its
     * {@code created} time is the backup time; the object files stay in the
     * store, only the manifest is listed here). Unreadable or missing
     * manifests are skipped with a log entry — the session's file items are
     * unaffected.
     */
    private void collectSnapshotItems(Path backupsDir, Path sessionPath, JIPipeProjectBackupSessionInfo sessionInfo,
                                      String sessionId, List<JIPipeProjectBackupItem> items) {
        Path indexFile = sessionPath.resolve("snapshots.json");
        if (!Files.isRegularFile(indexFile)) {
            return;
        }
        List<String> snapshotIds;
        try {
            snapshotIds = JsonUtils.readListFromFile(indexFile, String.class);
        } catch (Exception e) {
            getProgressInfo().log("Could not read snapshot index " + indexFile + ": " + e);
            return;
        }
        Path snapshotsDirectory = backupsDir.resolve("snapshots");
        for (String snapshotId : snapshotIds) {
            Path manifest = snapshotsDirectory.resolve(snapshotId + ".json");
            try {
                if (!Files.isRegularFile(manifest)) {
                    getProgressInfo().log("Snapshot manifest is missing, skipping: " + manifest);
                    continue;
                }
                JIPipeBackupSnapshot snapshot = JsonUtils.readFromFile(manifest, JIPipeBackupSnapshot.class);
                JIPipeProjectBackupItem item = new JIPipeProjectBackupItem();
                item.setSnapshotId(snapshot.getSnapshotId());
                item.setSnapshotManifestPath(manifest);
                item.setProjectFormat(snapshot.getProjectFormat());
                item.setTotalSizeBytes(snapshot.getTotalSize());
                item.setBackupTime(LocalDateTime.parse(snapshot.getCreated()));
                item.setSessionId(sessionId);
                item.setOriginalProjectPath(sessionInfo.getProjectStoragePath());
                items.add(item);
            } catch (Exception e) {
                getProgressInfo().log("Unable to read snapshot " + manifest + ": " + e);
            }
        }
    }
}
