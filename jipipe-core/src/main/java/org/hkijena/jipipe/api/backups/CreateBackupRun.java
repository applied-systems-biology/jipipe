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

import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupFilesCache;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupObjectStore;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupSnapshot;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupStoreOperations;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeProjectBackupServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A queued run that creates a content-addressed backup snapshot of one project
 * window through the {@link JIPipeBackupObjectStore}.
 * <p>
 * The run acquires the store's bounded cross-process write lock, so concurrent
 * JIPipe instances sharing the same backup root never write objects or
 * manifests at the same time. If the lock is still held after the timeout
 * (another instance is backing up), the cycle is skipped — it never blocks
 * forever.
 * <p>
 * The {@code backup-info.json} write happens inside
 * {@link JIPipeBackupStoreOperations#persistSnapshot}; the per-session files
 * cache is persisted by this run (it is the run's state, consulted on the next
 * cycle to skip re-hashing unchanged workspace files).
 */
public class CreateBackupRun extends DefaultJIPipeRunnable {
    /** Maximum time to wait for the store write lock before skipping the cycle */
    private static final long LOCK_TIMEOUT_MILLIS = 60_000;

    private final JIPipeDesktopProjectWindow window;
    private final String projectName;

    public CreateBackupRun(JIPipeDesktopProjectWindow window, String projectName) {
        this.window = window;
        this.projectName = projectName;
    }

    /**
     * @return the window this run backs up (used by the service component's cancel check)
     */
    public JIPipeDesktopProjectWindow getWindow() {
        return window;
    }

    @Override
    public String getTaskLabel() {
        return "Creating backup";
    }

    @Override
    public void run() {
        JIPipeProject project = window.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }
        JIPipeProjectBackupServiceComponent component = JIPipe.getInstance().getProjectBackup();
        Path backupRoot = component.getCurrentBackupPath();
        String sessionId = window.getSessionId().toString();
        Path cacheFile = backupRoot.resolve("files-cache").resolve(sessionId + ".json");

        // Lock-free: the cache is only a speed cache
        JIPipeBackupFilesCache cache = JIPipeBackupFilesCache.load(cacheFile);

        JIPipeBackupObjectStore store = component.getStore();
        if (!store.acquireWriteLock(LOCK_TIMEOUT_MILLIS)) {
            getProgressInfo().log("Another JIPipe instance is backing up — skipping this cycle (could not acquire the store write lock within "
                    + LOCK_TIMEOUT_MILLIS + " ms)");
            SwingUtilities.invokeLater(() -> window.getProjectWorkbench().sendStatusBarText("Backup skipped: another JIPipe instance is backing up"));
            return;
        }
        try {
            Path sessionDirectory = backupRoot.resolve(sessionId);
            Files.createDirectories(sessionDirectory);

            JIPipeProgressInfo pi = getProgressInfo();
            boolean projectIsJipx = PathUtils.isJipxPath(window.getProjectSavePath());
            JIPipeBackupSnapshot snapshot = projectIsJipx
                    ? JIPipeBackupStoreOperations.createJipxSnapshot(store, project, cache, projectName, pi)
                    : JIPipeBackupStoreOperations.createJipSnapshot(store, project, projectName, pi);

            JIPipeProjectBackupSessionInfo sessionInfo = new JIPipeProjectBackupSessionInfo();
            sessionInfo.setProjectSessionId(sessionId);
            sessionInfo.setProjectStoragePath(window.getProjectSavePath() != null ? window.getProjectSavePath().toString() : "");
            sessionInfo.setLastDateTimeInfo(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            JIPipeBackupStoreOperations.persistSnapshot(store, snapshot, sessionDirectory, sessionInfo);

            // Persist the updated cache (atomic; it is this run's state for the next cycle).
            // The cache is only a speed cache — the snapshot is already persisted, so a
            // failed cache save must not fail the run.
            try {
                cache.save(cacheFile);
            } catch (RuntimeException e) {
                getProgressInfo().log("Could not save files cache " + cacheFile + " (the backup itself was persisted): " + e);
            }

            SwingUtilities.invokeLater(() -> window.getProjectWorkbench().sendStatusBarText("Saved backup to " + sessionDirectory + " at " + snapshot.getCreated()));
        } catch (IOException e) {
            getProgressInfo().log("Failed to save backup: " + e);
            SwingUtilities.invokeLater(() -> window.getProjectWorkbench().sendStatusBarText("Failed to save backup: " + e.getMessage()));
            IJ.handleException(e);
        } finally {
            store.releaseWriteLock();
        }
    }
}
