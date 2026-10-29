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

package org.hkijena.jipipe.api.backups.store;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of {@link JIPipeBackupStoreOperations#verifyAll}: how many
 * snapshot manifests were checked, how many UNIQUE objects were re-hashed
 * (an object referenced by several snapshots is checked and counted once),
 * how many object bytes were read, and one human-readable string per
 * detected problem (corrupted or missing object, unreadable manifest).
 * <p>
 * Plain value holder — no Jackson (the report is never persisted).
 */
public class JIPipeBackupVerificationReport {
    /**
     * The number of snapshot manifests enumerated under
     * {@code <backupRoot>/snapshots/} that were read and checked.
     */
    public int snapshotsChecked;

    /**
     * The number of UNIQUE objects re-hashed (deduplicated across
     * snapshots by hash; a missing object is not counted).
     */
    public int objectsChecked;

    /**
     * The total number of object bytes read and re-hashed
     * (summed over the unique checked objects only).
     */
    public long bytesChecked;

    /**
     * One human-readable string per detected problem
     * (corrupted object, missing object, unreadable manifest).
     * Empty for a healthy store.
     */
    public final List<String> problems = new ArrayList<>();

    /**
     * @return true if no problems were detected
     */
    public boolean isHealthy() {
        return problems.isEmpty();
    }
}
