package org.hkijena.jipipe.api.backups;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.extensions.settings.BackupSettings;
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

public class CollectBackupsRun extends AbstractJIPipeRunnable {

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
        Path backupsDir = BackupSettings.getInstance().getCurrentBackupPath();
        getProgressInfo().log("Reading backups from " + backupsDir);

        // Collect all the backup items
        List<JIPipeProjectBackupItem> items = new ArrayList<>();

        try (Stream<Path> stream = Files.list(backupsDir)) {
            stream.forEach(sessionPath -> {
                if (Files.isDirectory(sessionPath)) {
                    Path infoFile = sessionPath.resolve("backup-info.json");
                    if(Files.isRegularFile(infoFile)) {
                        getProgressInfo().log("Reading backup info: " + infoFile);
                        try {
                            JIPipeProjectBackupSessionInfo sessionInfo = JsonUtils.readFromFile(infoFile, JIPipeProjectBackupSessionInfo.class);
                            try (Stream<Path> stream1 = Files.list(sessionPath)) {
                                stream1.forEach(backupFile -> {
                                    try {
                                        if (Files.isRegularFile(backupFile) && backupFile.getFileName().toString().endsWith(".jip")) {
                                            JIPipeProjectBackupItem item = new JIPipeProjectBackupItem();
                                            item.setProjectPath(backupFile);
                                            item.setBackupTime(LocalDateTime.ofInstant(Files.getLastModifiedTime(backupFile).toInstant(), ZoneId.systemDefault()));
                                            item.setSessionId(sessionInfo.getProjectSessionId());
                                            item.setOriginalProjectPath(sessionInfo.getProjectStoragePath());
                                            if (StringUtils.isNullOrEmpty(item.getSessionId())) {
                                                item.setSessionId(sessionPath.getFileName().toString());
                                            }
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
                } else if (Files.isRegularFile(sessionPath) && sessionPath.endsWith(".jip")) {
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
            if(StringUtils.isNullOrEmpty(item.getSessionId())) {
                item.setSessionId("unnamed-" + UUID.randomUUID());
            }
            else if(item.getSessionId().equals("legacy")) {
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


}
