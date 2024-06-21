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

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.plugins.settings.JIPipeBackupApplicationSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DeleteOldBackupsRun extends AbstractJIPipeRunnable {
    private final Duration maxAge;

    public DeleteOldBackupsRun(Duration maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public String getTaskLabel() {
        return "Delete old backups";
    }

    @Override
    public void run() {
        Path backupsDir = JIPipeBackupApplicationSettings.getInstance().getCurrentBackupPath();
        CollectBackupsRun subRun = new CollectBackupsRun();
        subRun.setProgressInfo(getProgressInfo().resolve("Collecting backups"));
        subRun.run();

        List<JIPipeProjectBackupItemCollection> backupItemCollections = subRun.getOutput();
        List<JIPipeProjectBackupItem> itemsToDelete = new ArrayList<>();

        LocalDateTime targetDateTime = LocalDateTime.now().minus(maxAge);

        for (JIPipeProjectBackupItemCollection backupItemCollection : backupItemCollections) {
            for (JIPipeProjectBackupItem backupItem : backupItemCollection.getBackupItemList()) {

                if (backupItem.getBackupTime().isBefore(targetDateTime)) {
                    itemsToDelete.add(backupItem);
                }
            }
        }

        getProgressInfo().log("-> Collected " + itemsToDelete.size() + " items to delete");

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
}
