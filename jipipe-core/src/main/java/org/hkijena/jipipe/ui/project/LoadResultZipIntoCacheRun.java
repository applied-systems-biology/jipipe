/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.project;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.data.storage.JIPipeZIPReadDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.importers.JIPipeImportCachedSlotOutputRun;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class LoadResultZipIntoCacheRun implements JIPipeRunnable {
    private final JIPipeWorkbench workbench;
    private final JIPipeProject project;
    private final Path resultPath;
    private final boolean clearBefore;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public LoadResultZipIntoCacheRun(JIPipeWorkbench workbench, JIPipeProject project, Path resultPath, boolean clearBefore) {
        this.workbench = workbench;
        this.project = project;
        this.resultPath = resultPath;
        this.clearBefore = clearBefore;
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
        return "Load exported data into cache";
    }

    @Override
    public void run() {
        progressInfo.log("Extracting ZIP file ...");
        try(JIPipeZIPReadDataStorage storage = new JIPipeZIPReadDataStorage(progressInfo, resultPath)) {
            Path fileSystemPath = storage.getFileSystemPath();
            LoadResultDirectoryIntoCacheRun run = new LoadResultDirectoryIntoCacheRun(workbench, project, fileSystemPath, clearBefore);
            run.setProgressInfo(progressInfo);
            run.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
