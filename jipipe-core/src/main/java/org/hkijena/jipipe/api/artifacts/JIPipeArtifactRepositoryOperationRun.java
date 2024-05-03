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

package org.hkijena.jipipe.api.artifacts;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.FileLocker;

public abstract class JIPipeArtifactRepositoryOperationRun extends AbstractJIPipeRunnable {

    @Override
    public void run() {
        getProgressInfo().log("Requesting repository lock: " + getLockType());

        FileLocker locker = new FileLocker(getProgressInfo(), JIPipe.getArtifacts().getLocalUserRepositoryPath().resolve("lockfile"));
        try {
            if(getLockType() == RepositoryLockType.Write) {
                locker.acquireWriteLock();
            }
            else {
                locker.acquireReadLock();
            }
            doOperation(getProgressInfo());
        }
        finally {
            locker.releaseLock();
        }

        if(getLockType() == RepositoryLockType.Write) {
            // Update local caches
            JIPipe.getArtifacts().updateCachedArtifacts(getProgressInfo().resolve("Update cache"));
        }
    }

    protected abstract void doOperation(JIPipeProgressInfo progressInfo);

    public abstract RepositoryLockType getLockType();

    public enum RepositoryLockType {
        Read,
        Write
    }
}
