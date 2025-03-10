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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JIPipeArtifactRepositoryInstallManuallyRun extends JIPipeArtifactRepositoryOperationRun {

    private final Path archivePath;
    private final JIPipeArtifact artifactInfo;

    public JIPipeArtifactRepositoryInstallManuallyRun(Path archivePath, JIPipeArtifact artifactInfo) {
        this.archivePath = archivePath;
        this.artifactInfo = artifactInfo;
    }

    @Override
    protected void doOperation(JIPipeProgressInfo progressInfo) {
        Path targetPath = artifactInfo.getDefaultInstallationPath(JIPipe.getArtifacts().getLocalUserRepositoryPath());
        progressInfo.log("Artifact to install: " + artifactInfo.getFullId());
        progressInfo.log("Target path: " + targetPath);

        if (Files.exists(targetPath)) {
            // Delete existing installation
            PathUtils.deleteDirectoryRecursively(targetPath, progressInfo.resolve("Delete old artifact directory"));
        }

        try {
            Files.createDirectories(targetPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            // Extract
            if (archivePath.getFileName().toString().endsWith(".zip")) {
                ArchiveUtils.decompressZipFile(archivePath, targetPath, progressInfo.resolve("Extracting"));
            } else {
                ArchiveUtils.decompressTarGZ(archivePath, targetPath, progressInfo.resolve("Extracting"));
            }

            // Create metadata file
            if (progressInfo.isCancelled())
                return;
            JsonUtils.saveToFile(artifactInfo, targetPath.resolve("artifact.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RepositoryLockType getLockType() {
        return RepositoryLockType.Write;
    }

    @Override
    public String getTaskLabel() {
        return "Install artifact manually";
    }
}
