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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.backups.store.JIPipeBackupStoreOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * A single backup item — either a legacy backup FILE (a full {@code .jip}/{@code .jipx}
 * project file in the session directory or at the backup root, with
 * {@link #projectPath} set) or a content-addressed SNAPSHOT (a manifest in
 * {@code <backupRoot>/snapshots/} with its objects in the store, with
 * {@link #snapshotId} set and {@link #projectPath} null).
 */
public class JIPipeProjectBackupItem {
    private Path projectPath;

    private String originalProjectPath;
    private LocalDateTime backupTime;
    private String sessionId;

    // Snapshot fields (Task 6): null for legacy file items
    private String snapshotId;
    private Path snapshotManifestPath;
    private String projectFormat;
    private long totalSizeBytes;

    public String getOriginalProjectPath() {
        return originalProjectPath;
    }

    public void setOriginalProjectPath(String originalProjectPath) {
        this.originalProjectPath = originalProjectPath;
    }

    public Path getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(Path projectPath) {
        this.projectPath = projectPath;
    }

    public LocalDateTime getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(LocalDateTime backupTime) {
        this.backupTime = backupTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * @return the id of the snapshot this item points at, or null for legacy file items
     */
    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    /**
     * @return the path of the snapshot manifest ({@code <backupRoot>/snapshots/<id>.json}),
     * or null for legacy file items
     */
    public Path getSnapshotManifestPath() {
        return snapshotManifestPath;
    }

    public void setSnapshotManifestPath(Path snapshotManifestPath) {
        this.snapshotManifestPath = snapshotManifestPath;
    }

    /**
     * @return the project format this snapshot was created from
     * ({@code "jip"} or {@code "jipx"}), or null for legacy file items
     */
    public String getProjectFormat() {
        return projectFormat;
    }

    public void setProjectFormat(String projectFormat) {
        this.projectFormat = projectFormat;
    }

    /**
     * @return the total size of the snapshot in bytes, or 0 for legacy file items
     */
    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    /**
     * @return true if this item is a content-addressed snapshot (not a legacy file item)
     */
    public boolean isSnapshot() {
        return snapshotId != null;
    }

    /**
     * Deletes this backup item.
     * <ul>
     *     <li>a legacy file item deletes its project file;</li>
     *     <li>a snapshot item removes its manifest and its session-index entry via
     *     {@link JIPipeBackupStoreOperations#deleteSnapshot} — the objects are only
     *     reclaimed later by a garbage collection run (the deletion itself is
     *     lock-free: the manifest deletion and the index rewrite are each atomic).</li>
     * </ul>
     *
     * @param progressInfo progress logging
     * @throws IOException if the file or manifest/index cannot be deleted
     */
    public void delete(JIPipeProgressInfo progressInfo) throws IOException {
        if (isSnapshot()) {
            // <backupRoot>/snapshots/<id>.json → parent = snapshots/, parent.parent = backupRoot
            Path backupRoot = snapshotManifestPath.getParent().getParent();
            JIPipeBackupStoreOperations.deleteSnapshot(backupRoot, snapshotId,
                    backupRoot.resolve(sessionId), progressInfo);
        } else {
            if (!Files.isRegularFile(projectPath)) {
                progressInfo.log("Backup file is already gone, skipping: " + projectPath);
                return;
            }
            Files.delete(projectPath);
        }
    }

    @Override
    public String toString() {
        return isSnapshot()
                ? "snapshot " + snapshotId + (projectFormat != null ? " (" + projectFormat + ", " + totalSizeBytes + " bytes)" : "")
                : String.valueOf(projectPath);
    }
}
