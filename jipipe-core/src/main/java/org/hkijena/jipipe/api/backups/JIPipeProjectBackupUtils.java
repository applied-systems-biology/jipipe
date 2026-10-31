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

import org.hkijena.jipipe.utils.PathUtils;

import java.nio.file.Path;

/**
 * Shared file recognition for the project backup machinery (JIPX spec §7:
 * "New backups of .jipx projects are .jipx").
 * <p>
 * Backups are project files, and since 6.0.0 they exist in both formats:
 * legacy {@code .jip} and JIPipe 6 {@code .jipx}. Every place that scans
 * a backup directory for backup files must accept BOTH - otherwise
 * {@code .jipx} backups are invisible in the backup manager, and a session
 * directory whose backups are all {@code .jipx} is considered empty and
 * deleted by {@link PruneBackupsRun#pruneEmptySessions} (which the
 * auto-cleanup runs at startup by default).
 */
public final class JIPipeProjectBackupUtils {

    private JIPipeProjectBackupUtils() {
    }

    /**
     * Checks if the given path is a project backup file: a project file in
     * either format ({@code .jip} or {@code .jipx}, case-insensitive).
     *
     * @param path the path to check (null-safe)
     * @return true if the file name is a project file in either format
     */
    public static boolean isBackupFile(Path path) {
        return PathUtils.isJipPath(path) || PathUtils.isJipxPath(path);
    }
}
