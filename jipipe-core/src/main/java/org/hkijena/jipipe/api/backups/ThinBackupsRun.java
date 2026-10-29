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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupObjectStore;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupSnapshot;
import org.hkijena.jipipe.api.backups.store.JIPipeLocalBackupObjectStore;
import org.hkijena.jipipe.api.service.components.JIPipeProjectBackupServiceComponent;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeBackupApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ThinBackupsRun extends DefaultJIPipeRunnable {

    /** Maximum time to wait for the store write lock before the GC is skipped */
    private static final long GC_LOCK_TIMEOUT_MILLIS = 60_000;

    private final boolean interactive;

    public ThinBackupsRun(boolean interactive) {
        this.interactive = interactive;
    }

    @Override
    public String getTaskLabel() {
        return "Thin backups";
    }

    @Override
    public void run() {
        JIPipeProjectBackupServiceComponent projectBackupServiceComponent = JIPipe.getInstance().getProjectBackup();
        JIPipeBackupApplicationSettings.CleanupSettings settings = JIPipeBackupApplicationSettings.getInstance().getCleanupSettings();


        Path backupsDir = projectBackupServiceComponent.getCurrentBackupPath();
        CollectBackupsRun subRun = new CollectBackupsRun();
        subRun.setProgressInfo(getProgressInfo().resolve("Collecting backups"));
        subRun.run();

        List<JIPipeProjectBackupItemCollection> backupItemCollections = subRun.getOutput();
        List<JIPipeProjectBackupItem> itemsToDelete = new ArrayList<>();


        LocalDateTime now = LocalDateTime.now();

        for (JIPipeProjectBackupItemCollection backupItemCollection : backupItemCollections) {
            applyCascadingCleanupRules(backupItemCollection, itemsToDelete, now, settings, getProgressInfo().resolve(backupItemCollection.renderName()));
        }

        getProgressInfo().log("-> Collected " + itemsToDelete.size() + " items to delete");

        if(!itemsToDelete.isEmpty()) {
            if(interactive) {
                for (int i = 5; i >= 0; i--) {
                    getProgressInfo().log("Will continue in " + i + " seconds before deleting backups");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (getProgressInfo().isCancelled())
                    return;

            }

            for (JIPipeProjectBackupItem backupItem : itemsToDelete) {
                try {
                    getProgressInfo().log("Delete: " + backupItem);
                    backupItem.delete(getProgressInfo());
                } catch (Exception e) {
                    getProgressInfo().log("-> Error: " + e);
                }
            }
        }

        // Garbage collection: frees store objects no longer referenced by any
        // snapshot (best-effort — never blocks the cleanup queue)
        if (!itemsToDelete.isEmpty() && settings.isEnableAutoGc()) {
            garbageCollectBestEffort(backupsDir, getProgressInfo().resolve("Garbage collection"));
        }

        // Store size limit: drop the OLDEST snapshots (across all sessions)
        // until the live backup data is under the limit, then GC again
        enforceStoreSizeLimit(backupsDir, settings, backupItemCollections,
                getProgressInfo().resolve("Store size limit"));

        PruneBackupsRun.pruneEmptySessions(backupsDir, getProgressInfo().resolve("Postprocessing"));
    }

    /**
     * Best-effort garbage collection of the backup object store: takes the
     * bounded store write lock and sweeps the unreferenced objects. On a
     * lock timeout (another JIPipe instance is writing) the GC is SKIPPED
     * with a warning — the cleanup queue is never blocked by it.
     */
    private void garbageCollectBestEffort(Path backupsDir, JIPipeProgressInfo progressInfo) {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupsDir);
        if (!store.acquireWriteLock(GC_LOCK_TIMEOUT_MILLIS)) {
            progressInfo.log("Skipping backup garbage collection: another JIPipe instance is writing to the backup store");
            return;
        }
        try {
            store.garbageCollect(progressInfo);
        } catch (Exception e) {
            progressInfo.log("Backup garbage collection failed: " + e);
        } finally {
            store.releaseWriteLock();
        }
    }

    /**
     * Enforces the "maximum backup storage" limit: while the live backup
     * data (the union of the objects referenced by the REMAINING snapshot
     * manifests, by their actual object size) exceeds the limit, the
     * oldest snapshot across ALL sessions is deleted, and after the loop a
     * final garbage collection frees the objects the dropped manifests
     * referenced. The cascade rule engine is untouched — this is a second
     * pass over the same collected items.
     */
    private void enforceStoreSizeLimit(Path backupsDir, JIPipeBackupApplicationSettings.CleanupSettings settings,
                                       List<JIPipeProjectBackupItemCollection> backupItemCollections, JIPipeProgressInfo progressInfo) {
        OptionalIntegerParameter limit = settings.getStoreSizeLimitMb();
        if (!limit.isEnabled()) {
            return;
        }
        long limitBytes = (long) limit.getContent() * 1024L * 1024L;
        JIPipeBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupsDir);

        // All snapshot items across all sessions, oldest first
        List<JIPipeProjectBackupItem> snapshotItems = new ArrayList<>();
        for (JIPipeProjectBackupItemCollection collection : backupItemCollections) {
            for (JIPipeProjectBackupItem item : collection.getBackupItemList()) {
                if (item.isSnapshot()) {
                    snapshotItems.add(item);
                }
            }
        }
        snapshotItems.sort(Comparator.comparing(JIPipeProjectBackupItem::getBackupTime));

        long before = liveSnapshotBytes(backupsDir, store, progressInfo);
        if (before <= limitBytes) {
            progressInfo.log("Store size limit: " + (before / (1024 * 1024)) + " MB live data, limit "
                    + limit.getContent() + " MB — nothing to delete");
            return;
        }

        int dropped = 0;
        for (JIPipeProjectBackupItem item : snapshotItems) {
            long current = liveSnapshotBytes(backupsDir, store, progressInfo);
            if (current <= limitBytes) {
                break;
            }
            try {
                progressInfo.log("Store size limit: deleting oldest backup " + item);
                item.delete(progressInfo);
                dropped++;
            } catch (Exception e) {
                progressInfo.log("-> Error: " + e);
            }
        }
        long after = liveSnapshotBytes(backupsDir, store, progressInfo);
        if (dropped > 0) {
            progressInfo.log("Store size limit: dropped " + dropped + " oldest backups ("
                    + (before / (1024 * 1024)) + " MB → " + (after / (1024 * 1024)) + " MB live data, limit "
                    + limit.getContent() + " MB)");
            // The dropped manifests' objects are now unreferenced — free them
            garbageCollectBestEffort(backupsDir, progressInfo.resolve("Garbage collection"));
        }
    }

    /**
     * Sums the actual object sizes of the union of all objects referenced by
     * the REMAINING snapshot manifests (the live backup data). Legacy file
     * items are not part of the object store and are not counted. A manifest
     * that is missing or unreadable contributes nothing.
     */
    private long liveSnapshotBytes(Path backupsDir, JIPipeBackupObjectStore store, JIPipeProgressInfo progressInfo) {
        Set<String> hashes = new HashSet<>();
        Path snapshotsDirectory = backupsDir.resolve("snapshots");
        if (!Files.isDirectory(snapshotsDirectory)) {
            return 0;
        }
        try (var stream = Files.list(snapshotsDirectory)) {
            for (Path manifest : stream.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                try {
                    JIPipeBackupSnapshot snapshot = JsonUtils.readFromFile(manifest, JIPipeBackupSnapshot.class);
                    for (JIPipeBackupSnapshot.SnapshotEntry entry : snapshot.getEntries())
                        hashes.add(entry.getHash());
                } catch (Exception e) {
                    progressInfo.log("Could not read snapshot manifest " + manifest.getFileName() + ", ignoring it in the size accounting: " + e);
                }
            }
        } catch (Exception e) {
            progressInfo.log("Could not list the snapshots directory: " + e);
            return 0;
        }
        long sum = 0;
        for (String hash : hashes)
            sum += store.sizeOf(hash);
        return sum;
    }


    private void applyCascadingCleanupRules(JIPipeProjectBackupItemCollection backupItemCollection,
                                            List<JIPipeProjectBackupItem> itemsToDelete,
                                            LocalDateTime now,
                                            JIPipeBackupApplicationSettings.CleanupSettings settings, JIPipeProgressInfo progressInfo) {
        
        List<JIPipeProjectBackupItem> backupItems = backupItemCollection.getBackupItemList();
        if (backupItems.isEmpty()) {
            return;
        }

        // Sort by backup time (newest first)
        backupItems.sort(Comparator.comparing(JIPipeProjectBackupItem::getBackupTime).reversed());

        // Keep track of items that should be preserved
        Set<JIPipeProjectBackupItem> itemsToKeep = new HashSet<>();

        // Always keep the newest backup overall
        itemsToKeep.add(backupItems.getFirst());

        Multimap<Long,JIPipeProjectBackupItem> bucketYears = HashMultimap.create();
        Multimap<Long,JIPipeProjectBackupItem> bucketMonths = HashMultimap.create();
        Multimap<Long,JIPipeProjectBackupItem> bucketWeeks = HashMultimap.create();
        Multimap<Long,JIPipeProjectBackupItem> bucketDays = HashMultimap.create();
        Multimap<Long,JIPipeProjectBackupItem> bucketHours = HashMultimap.create();

        for (JIPipeProjectBackupItem backupItem : backupItems) {
            LocalDateTime backupTime = backupItem.getBackupTime();
            long ageInYears = ChronoUnit.YEARS.between(backupTime, now);
            long ageInMonths = ChronoUnit.MONTHS.between(backupTime, now);
            long ageInWeeks = ChronoUnit.WEEKS.between(backupTime, now);
            long ageInDays = ChronoUnit.DAYS.between(backupTime, now);
            long ageInHours = ChronoUnit.HOURS.between(backupTime, now);
            boolean ruleMatched = false;

            ruleMatched = tryBucket(ageInYears, settings.getKeepBackupsPerYear(), bucketYears, backupItem, ruleMatched);
            ruleMatched = tryBucket(ageInMonths, settings.getKeepBackupsPerMonth(), bucketMonths, backupItem, ruleMatched);
            ruleMatched = tryBucket(ageInWeeks, settings.getKeepBackupsPerWeek(), bucketWeeks, backupItem, ruleMatched);
            ruleMatched = tryBucket(ageInDays, settings.getKeepBackupsPerDay(), bucketDays, backupItem, ruleMatched);
            ruleMatched = tryBucket(ageInHours, settings.getKeepBackupsPerHour(), bucketHours, backupItem, ruleMatched);

            // For very young backups (less than 1 h, we keep them)
            if(ageInHours == 0) {
                itemsToKeep.add(backupItem);
            }

            if(!ruleMatched) {
                if(settings.getMaxAgeDays().isEnabled()) {
                    if(ageInDays > settings.getMaxAgeDays().getContent()) {
                        itemsToKeep.add(backupItem);
                    }
                }
                else {
                    // No deletion
                    itemsToKeep.add(backupItem);
                }
            }
        }

        // Process buckets
        expandBucket(bucketYears, itemsToKeep);
        expandBucket(bucketMonths, itemsToKeep);
        expandBucket(bucketWeeks, itemsToKeep);
        expandBucket(bucketDays, itemsToKeep);
        expandBucket(bucketHours, itemsToKeep);

        progressInfo.log("Keeping " + itemsToKeep.size() + "/" + backupItems.size() + " [Y" + bucketYears.size() + "M" +  bucketMonths.size() + "W" + bucketWeeks.size() + "d" +  bucketDays.size() + "h" + bucketHours.size() + "]");

        // Mark all items not in itemsToKeep for deletion
        for (JIPipeProjectBackupItem backupItem : backupItems) {
            if (!itemsToKeep.contains(backupItem)) {
                itemsToDelete.add(backupItem);
            }
        }
    }

    private void expandBucket(Multimap<Long, JIPipeProjectBackupItem> bucket, Set<JIPipeProjectBackupItem> itemsToKeep) {
        for (Map.Entry<Long, JIPipeProjectBackupItem> entry : bucket.entries()) {
            itemsToKeep.add(entry.getValue());
        }
    }

    private boolean tryBucket(long age, OptionalIntegerParameter keep, Multimap<Long, JIPipeProjectBackupItem> bucket, JIPipeProjectBackupItem backupItem, boolean alreadyMatched) {
        if(alreadyMatched) {
            return alreadyMatched;
        }
        if(age >= 1 && keep.isEnabled()) {
            if(bucket.get(age).size() < keep.getContent()) {
                bucket.put(age, backupItem);
            }
            return true;
        }
        return false;
    }

}
