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
import org.hkijena.jipipe.api.service.components.JIPipeProjectBackupServiceComponent;
import org.hkijena.jipipe.plugins.settings.application.JIPipeBackupApplicationSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ThinBackupsRun extends DefaultJIPipeRunnable {

    public ThinBackupsRun() {
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

        // Get retention settings - check if they are disabled (null or empty)
        int hourlyRetention = settings.getMaxAgeHourly().orElse(Integer.MAX_VALUE);
        int dailyRetention = settings.getMaxAgeDaily().orElse(Integer.MAX_VALUE);
        int weeklyRetention = settings.getMaxAgeWeekly().orElse(Integer.MAX_VALUE);
        int monthlyRetention = settings.getMaxAgeMonthly().orElse(Integer.MAX_VALUE);

        boolean retentionEnabled = hourlyRetention != Integer.MAX_VALUE || dailyRetention != Integer.MAX_VALUE ||
                                 weeklyRetention != Integer.MAX_VALUE || monthlyRetention != Integer.MAX_VALUE;

        getProgressInfo().log("Applying graduated retention strategy:");
        if (retentionEnabled) {
            getProgressInfo().log("- Hourly retention: " + hourlyRetention + " hours");
            getProgressInfo().log("- Daily retention: " + dailyRetention + " days");
            getProgressInfo().log("- Weekly retention: " + weeklyRetention + " weeks");
            getProgressInfo().log("- Monthly retention: " + monthlyRetention + " months");
        } else {
            getProgressInfo().log("- Retention limits disabled - keeping all backups");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (JIPipeProjectBackupItemCollection backupItemCollection : backupItemCollections) {
            List<JIPipeProjectBackupItem> backupItems = backupItemCollection.getBackupItemList();
            
            // Sort by backup time (oldest first) for proper tiered retention
            backupItems.sort(Comparator.comparing(JIPipeProjectBackupItem::getBackupTime));

            // Apply graduated retention strategy
            applyGraduatedRetention(backupItemCollection, backupItems, itemsToDelete, now, hourlyRetention, dailyRetention, weeklyRetention, monthlyRetention);
        }

        getProgressInfo().log("-> Collected " + itemsToDelete.size() + " items to delete");

        if(itemsToDelete.isEmpty()) {
            return;
        }

        for (int i = 5; i >= 0; i--) {
            getProgressInfo().log("Will continue in " + i + " seconds before deleting backups");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if(true) {
            return;
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

    /**
     * Applies graduated retention strategy to a list of backup items.
     *
     * @param backupItemCollection the item collection
     * @param backupItems          List of backup items sorted by time (oldest first)
     * @param itemsToDelete        List to add items to be deleted to
     * @param now                  Current time
     * @param hourlyRetention      Hours to keep hourly backups
     * @param dailyRetention       Days to keep daily backups
     * @param weeklyRetention      Weeks to keep weekly backups
     * @param monthlyRetention     Months to keep monthly backups
     */
    private void applyGraduatedRetention(JIPipeProjectBackupItemCollection backupItemCollection, List<JIPipeProjectBackupItem> backupItems,
                                         List<JIPipeProjectBackupItem> itemsToDelete,
                                         LocalDateTime now,
                                         int hourlyRetention, int dailyRetention,
                                         int weeklyRetention, int monthlyRetention) {
        
        if (backupItems.isEmpty()) {
            return;
        }

        // Calculate cutoff times for each tier
        LocalDateTime hourlyCutoff = now.minusHours(hourlyRetention);
        LocalDateTime dailyCutoff = now.minusDays(dailyRetention);
        LocalDateTime weeklyCutoff = now.minusWeeks(weeklyRetention);
        LocalDateTime monthlyCutoff = now.minusMonths(monthlyRetention);

        // Process backups in tiers
        int hourlyCount = 0;
        int dailyCount = 0;
        int weeklyCount = 0;
        int monthlyCount = 0;
        int numDeleted = 0;

        for (JIPipeProjectBackupItem backupItem : backupItems) {
            LocalDateTime backupTime = backupItem.getBackupTime();

            // Delete everything older than monthly cutoff
            if (backupTime.isBefore(monthlyCutoff)) {
                itemsToDelete.add(backupItem);
                ++numDeleted;
                continue;
            }

            // For backups within monthly window, apply graduated retention
            if (backupTime.isBefore(weeklyCutoff)) {
                // Keep one backup per week in this tier
                if (weeklyCount == 0) {
                    // Keep the first (oldest) backup in this weekly tier
                    weeklyCount++;
                } else {
                    ++numDeleted;
                    itemsToDelete.add(backupItem);
                }
            } else if (backupTime.isBefore(dailyCutoff)) {
                // Keep one backup per day in this tier
                if (dailyCount == 0) {
                    // Keep the first (oldest) backup in this daily tier
                    dailyCount++;
                } else {
                    ++numDeleted;
                    itemsToDelete.add(backupItem);
                }
            } else if (backupTime.isBefore(hourlyCutoff)) {
                // Keep one backup per hour in this tier
                if (hourlyCount == 0) {
                    // Keep the first (oldest) backup in this hourly tier
                    hourlyCount++;
                } else {
                    ++numDeleted;
                    itemsToDelete.add(backupItem);
                }
            }
            // Keep all backups within the hourly window (most recent)
        }

        getProgressInfo().log(backupItemCollection.renderName() + ": " + backupItems.size() + " => ( h" + hourlyCount + " d" +  dailyCount + " w" + weeklyCount + " m" + monthlyCount + " D" + numDeleted + ")");
    }
}
