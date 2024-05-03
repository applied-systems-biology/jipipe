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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;

import java.util.ArrayList;
import java.util.List;

public class JIPipeArtifactRepositoryApplyInstallUninstallRun extends JIPipeArtifactRepositoryOperationRun {

    private final List<JIPipeRemoteArtifact> toInstall;
    private final List<JIPipeLocalArtifact> toUninstall;

    public JIPipeArtifactRepositoryApplyInstallUninstallRun(List<JIPipeRemoteArtifact> toInstall, List<JIPipeLocalArtifact> toUninstall) {
        this.toInstall = toInstall;
        this.toUninstall = toUninstall;
    }

    @Override
    protected void doOperation(JIPipeProgressInfo progressInfo) {
        for (int i = 0; i < toUninstall.size(); i++) {
            if(progressInfo.isCancelled())
                return;
            JIPipeLocalArtifact artifact = toUninstall.get(i);
            JIPipeProgressInfo uninstallingProgress = progressInfo.resolveAndLog("Uninstalling " + artifact.getFullId(), i, toUninstall.size());
            JIPipeArtifactRepositoryUninstallArtifactRun run = new JIPipeArtifactRepositoryUninstallArtifactRun(artifact);
            run.setProgressInfo(uninstallingProgress);
             try {
                 run.doOperation(uninstallingProgress);
             }
             catch (Throwable e) {
                 progressInfo.getNotifications().push(new JIPipeNotification("artifact-uninstall-error-" + artifact.getFullId(),
                         "Error while uninstalling " + artifact.getFullId(),
                         "The artifact " + artifact.getFullId() + " could not be fully uninstalled. " +
                                 "Please review the log."));
             }
        }
        for (int i = 0; i < toInstall.size(); i++) {
            if(progressInfo.isCancelled())
                return;
            JIPipeRemoteArtifact artifact = toInstall.get(i);
            JIPipeProgressInfo installingProgress = progressInfo.resolveAndLog("Installing " + artifact.getFullId(), i, toInstall.size());
            JIPipeArtifactRepositoryInstallArtifactRun run = new JIPipeArtifactRepositoryInstallArtifactRun(artifact);
            run.setProgressInfo(installingProgress);
            try {
                run.doOperation(installingProgress);
            }
            catch (Throwable e) {
                progressInfo.getNotifications().push(new JIPipeNotification("artifact-install-error-" + artifact.getFullId(),
                        "Error while installing " + artifact.getFullId(),
                        "The artifact " + artifact.getFullId() + " could not be fully installed. " +
                                "Please review the log."));
            }
        }

    }

    @Override
    public RepositoryLockType getLockType() {
        return RepositoryLockType.Write;
    }

    @Override
    public String getTaskLabel() {
        return "Apply artifact repository changes";
    }
}
