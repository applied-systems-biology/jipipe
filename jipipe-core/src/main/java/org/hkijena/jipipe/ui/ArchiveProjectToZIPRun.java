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
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ArchiveProjectToZIPRun extends ArchiveProjectRun {

    private final Path outputFile;

    protected ArchiveProjectToZIPRun(JIPipeProject project, Path outputFile) {
        super(project);
        this.outputFile = outputFile;
    }

    @Override
    public String getTaskLabel() {
        return "Archive project to ZIP";
    }

    @Override
    public void run() {
        if (Files.exists(outputFile)) {
            try {
                Files.delete(outputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try (JIPipeZIPWriteDataStorage projectStorage = new JIPipeZIPWriteDataStorage(getProgressInfo(), outputFile)) {
            archive(projectStorage, projectStorage.resolve(UUID.randomUUID().toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
