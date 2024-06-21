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

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.project.JIPipeProject;

import java.io.IOException;
import java.nio.file.Path;

public class JIPipeDesktopLoadResultZipIntoCacheRun extends AbstractJIPipeRunnable {
    private final JIPipeWorkbench workbench;
    private final JIPipeProject project;
    private final Path resultPath;
    private final boolean clearBefore;

    public JIPipeDesktopLoadResultZipIntoCacheRun(JIPipeWorkbench workbench, JIPipeProject project, Path resultPath, boolean clearBefore) {
        this.workbench = workbench;
        this.project = project;
        this.resultPath = resultPath;
        this.clearBefore = clearBefore;
    }

    @Override
    public String getTaskLabel() {
        return "Load exported data into cache";
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.log("Extracting ZIP file ...");
        try (JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, resultPath)) {
            Path fileSystemPath = storage.getFileSystemPath();
            JIPipeDesktopLoadResultDirectoryIntoCacheRun run = new JIPipeDesktopLoadResultDirectoryIntoCacheRun(workbench, project, fileSystemPath, clearBefore);
            run.setProgressInfo(progressInfo);
            run.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
