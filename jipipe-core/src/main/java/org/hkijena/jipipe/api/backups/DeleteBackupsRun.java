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

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Deletes the given backup items. Runs on the backup queue (never on the EDT,
 * because a snapshot deletion takes the cross-process store write lock with a
 * 60 s bound — a concurrent JIPipe instance holding it must not freeze the UI).
 * <p>
 * Each item is deleted independently: a failure (e.g. the write lock could not
 * be acquired because another instance is writing) is LOGGED and the run
 * CONTINUES with the remaining items, so one stuck deletion never aborts the
 * batch. At the end the run logs how many items were deleted and how many
 * failed.
 */
public class DeleteBackupsRun extends DefaultJIPipeRunnable {
    private final List<JIPipeProjectBackupItem> items;

    public DeleteBackupsRun(Collection<JIPipeProjectBackupItem> items) {
        this.items = new ArrayList<>(items);
    }

    @Override
    public String getTaskLabel() {
        return "Delete backups";
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.log("Deleting " + items.size() + " backup item(s) ...");

        int deleted = 0;
        int failed = 0;
        for (JIPipeProjectBackupItem item : items) {
            progressInfo.log("Delete: " + item);
            try {
                item.delete(progressInfo);
                deleted++;
            } catch (Exception e) {
                // A single item that cannot be deleted (e.g. the store write
                // lock is held by a concurrent instance) must not abort the
                // rest of the batch — log it and continue.
                progressInfo.log("-> Failed to delete " + item + ": " + e);
                failed++;
            }
        }

        progressInfo.log("Deleted " + deleted + " of " + items.size() + " backup item(s)"
                + (failed > 0 ? ", " + failed + " failed (see log)" : ""));
    }
}
