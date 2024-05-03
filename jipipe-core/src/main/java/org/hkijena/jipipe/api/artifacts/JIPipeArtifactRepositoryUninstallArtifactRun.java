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
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JIPipeArtifactRepositoryUninstallArtifactRun extends JIPipeArtifactRepositoryOperationRun {

    private final JIPipeLocalArtifact artifact;

    public JIPipeArtifactRepositoryUninstallArtifactRun(JIPipeLocalArtifact artifact) {
        this.artifact = artifact;
    }

    @Override
    protected void doOperation(JIPipeProgressInfo progressInfo) {
        Path localPath = artifact.getLocalPath();
        progressInfo.log("Uninstalling: " + localPath);
        if(Files.isDirectory(localPath)) {
            Path artifactInfoFile = localPath.resolve("artifact.json");
            try {
                Files.deleteIfExists(artifactInfoFile);
                PathUtils.deleteDirectoryRecursively(localPath, progressInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            progressInfo.log(localPath + " is not a directory! Unable to continue!");
        }
    }

    @Override
    public RepositoryLockType getLockType() {
        return RepositoryLockType.Write;
    }

    @Override
    public String getTaskLabel() {
        return "Uninstall " + artifact.getFullId();
    }
}
