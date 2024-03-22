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

package org.hkijena.jipipe.desktop.app.project;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPWriteDataStorage;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JIPipeDesktopSaveProjectAndCacheToZipRun implements JIPipeRunnable {
    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeProject project;
    private final Path zipFile;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public JIPipeDesktopSaveProjectAndCacheToZipRun(JIPipeDesktopWorkbench workbench, JIPipeProject project, Path zipFile) {
        this.workbench = workbench;
        this.project = project;
        this.zipFile = zipFile;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Save project and cache (ZIP)";
    }

    @Override
    public void run() {
        if (Files.exists(zipFile)) {
            try {
                Files.delete(zipFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (JIPipeZIPWriteDataStorage storage = new JIPipeZIPWriteDataStorage(progressInfo, zipFile)) {
            Path outputPath = storage.getFileSystemPath();
            JIPipeDesktopSaveProjectAndCacheToDirectoryRun run = new JIPipeDesktopSaveProjectAndCacheToDirectoryRun(workbench, project, outputPath, false);
            run.setProgressInfo(progressInfo);
            run.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
