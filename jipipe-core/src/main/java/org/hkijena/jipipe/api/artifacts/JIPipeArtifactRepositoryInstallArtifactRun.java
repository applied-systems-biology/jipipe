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
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.plugins.artifacts.ArtifactSettings;
import org.hkijena.jipipe.plugins.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.ArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class JIPipeArtifactRepositoryInstallArtifactRun extends JIPipeArtifactRepositoryOperationRun {

    private final JIPipeRemoteArtifact artifact;

    public JIPipeArtifactRepositoryInstallArtifactRun(JIPipeRemoteArtifact artifact) {
        this.artifact = artifact;
    }

    @Override
    protected void doOperation(JIPipeProgressInfo progressInfo) {
        Path targetPath = artifact.getDefaultInstallationPath(JIPipe.getInstance().getArtifactsRegistry().getLocalUserRepositoryPath());
        progressInfo.log("Artifact to install: " + artifact.getFullId());
        progressInfo.log("Target path: " + targetPath);

        if(Files.exists(targetPath)) {
            // Delete existing installation
            PathUtils.deleteDirectoryRecursively(targetPath, progressInfo.resolve("Delete old artifact directory"));
        }

        try {
            Files.createDirectories(targetPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String suffix = artifact.getUrl().endsWith(".zip") ? ".zip" : ".tar.gz";
        Path tmpFile = RuntimeSettings.generateTempFile("artifact", suffix);
        try {
            //Download
            WebUtils.download(new URL(artifact.getUrl()), tmpFile, "Download", progressInfo);
            if(progressInfo.isCancelled())
                return;

            // Extract
            if(suffix.equals(".zip")) {
                ArchiveUtils.decompressZipFile(tmpFile, targetPath, progressInfo.resolve("Extracting"));
            }
            else {
                ArchiveUtils.decompressTarGZ(tmpFile, targetPath, progressInfo.resolve("Extracting"));
            }

            // Create metadata file
            if(progressInfo.isCancelled())
                return;
            JsonUtils.saveToFile(artifact, targetPath.resolve("artifact.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(Files.exists(tmpFile)) {
                try {
                    Files.delete(tmpFile);
                } catch (IOException e) {
                    progressInfo.log("Warning: could not delete " + tmpFile);
                }
            }
        }
    }

    @Override
    public RepositoryLockType getLockType() {
        return RepositoryLockType.Write;
    }

    @Override
    public String getTaskLabel() {
        return "Install artifact " + artifact.getFullId();
    }
}
