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
import org.hkijena.jipipe.api.service.components.JIPipeProjectBackupServiceComponent;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeBackupApplicationSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ThinBackupsRun extends DefaultJIPipeRunnable {

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

        if(itemsToDelete.isEmpty()) {
            return;
        }

        if(interactive) {
            for (int i = 5; i >= 0; i--) {
                getProgressInfo().log("Will continue in " + i + " seconds before deleting backups");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (getProgressInfo().isCancelled())
            return;

        for (JIPipeProjectBackupItem backupItem : itemsToDelete) {
            try {
                getProgressInfo().log("Delete: " + backupItem);
                Files.delete(backupItem.getProjectPath());
            } catch (Exception e) {
                getProgressInfo().log("-> Error: " + e);
            }
        }

        PruneBackupsRun.pruneEmptySessions(backupsDir, getProgressInfo().resolve("Postprocessing"));
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
