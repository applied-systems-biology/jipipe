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

package org.hkijena.jipipe.ui;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ArchiveProjectToDirectoryRun extends ArchiveProjectRun {

    private final Path outputDirectory;

    protected ArchiveProjectToDirectoryRun(JIPipeProject project, Path outputDirectory) {
        super(project);
        this.outputDirectory = outputDirectory;
    }

    @Override
    public String getTaskLabel() {
        return "Archive project to directory";
    }

    @Override
    public void run() {
        getProgressInfo().log("The archive will be written to " + outputDirectory);
        if (Files.exists(outputDirectory)) {
            PathUtils.deleteDirectoryRecursively(outputDirectory, getProgressInfo().resolve("Removing existing directory"));
        }
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (JIPipeFileSystemWriteDataStorage projectStorage = new JIPipeFileSystemWriteDataStorage(getProgressInfo(), outputDirectory)) {
            archive(projectStorage, projectStorage.resolve(UUID.randomUUID().toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }
}
