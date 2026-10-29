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
import org.hkijena.jipipe.api.backups.store.JIPipeBackupObjectStore;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupStoreOperations;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupVerificationReport;

import java.io.IOException;

/**
 * A queued run that verifies all content-addressed backup snapshots:
 * every snapshot manifest is enumerated and every unique backup file it
 * references is re-hashed in full (a dry run — nothing is written).
 * <p>
 * The verification is lock-free (it only reads the immutable store files),
 * so it can run in parallel with an ongoing backup. The report is stored in
 * a field for the caller to inspect after the run finishes.
 */
public class VerifyBackupsRun extends DefaultJIPipeRunnable {

    private volatile JIPipeBackupVerificationReport report;

    @Override
    public String getTaskLabel() {
        return "Verify backups";
    }

    @Override
    public boolean isLogSilent() {
        // The result is reported via the status bar (and a notification on
        // problems); the run-log entry is auto-marked as read when it is
        // pushed manually (the #753 pattern for runs on non-global queues)
        return true;
    }

    @Override
    public void run() {
        JIPipeBackupObjectStore store = JIPipe.getInstance().getProjectBackup().getStore();
        try {
            report = JIPipeBackupStoreOperations.verifyAll(store, getProgressInfo());
        } catch (IOException e) {
            // The run cannot declare the checked exception (JIPipeRunnable
            // extends Runnable); the worker appends the full stack trace to
            // the progress log, which the run-log entry carries
            throw new RuntimeException("Backup verification failed", e);
        }
    }

    /**
     * @return the verification report, or null if the run has not finished
     * (or failed before producing one)
     */
    public JIPipeBackupVerificationReport getReport() {
        return report;
    }
}
