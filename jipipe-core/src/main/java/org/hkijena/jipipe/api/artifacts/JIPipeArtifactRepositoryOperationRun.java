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
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.io.IOException;

public abstract class JIPipeArtifactRepositoryOperationRun extends DefaultJIPipeRunnable {

    private JIPipeArtifactOperationContext externalContext;

    @Override
    public void run() {
        getProgressInfo().log("Requesting repository lock: " + getLockType());

        if (externalContext == null) {
            try (JIPipeArtifactOperationContext context = new JIPipeArtifactOperationContext(getProgressInfo())) {
                if (getLockType() == RepositoryLockType.Read) {
                    context.waitUntilRead();
                } else if (getLockType() == RepositoryLockType.Write) {
                    context.waitUntilWrite();
                }
                doOperation(context, getProgressInfo());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Just forward the context
            if (getLockType() == RepositoryLockType.Read) {
                externalContext.waitUntilRead();
            } else if (getLockType() == RepositoryLockType.Write) {
                externalContext.waitUntilWrite();
            }
            doOperation(externalContext, getProgressInfo());
        }

        if (getLockType() == RepositoryLockType.Write) {
            // Update local caches
            JIPipe.getArtifacts().updateCachedArtifacts(getProgressInfo().resolve("Update cache"));
        }
    }

    protected abstract void doOperation(JIPipeArtifactOperationContext context, JIPipeProgressInfo progressInfo);

    public abstract RepositoryLockType getLockType();

    public JIPipeArtifactOperationContext getExternalContext() {
        return externalContext;
    }

    public void setExternalContext(JIPipeArtifactOperationContext externalContext) {
        this.externalContext = externalContext;
    }

    public enum RepositoryLockType {
        Read,
        Write
    }
}
