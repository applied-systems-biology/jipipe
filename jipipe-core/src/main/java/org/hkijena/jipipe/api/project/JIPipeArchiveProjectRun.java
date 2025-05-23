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

package org.hkijena.jipipe.api.project;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class JIPipeArchiveProjectRun extends AbstractJIPipeRunnable {

    private final JIPipeProject project;

    protected JIPipeArchiveProjectRun(JIPipeProject project) {
        this.project = project;
    }

    public JIPipeProject getProject() {
        return project;
    }

    protected void archive(JIPipeWriteDataStorage projectStorage, JIPipeWriteDataStorage wrappedExternalStorage) throws IOException {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.setProgress(0, 3);
        progressInfo.log("Copying project ...");
        // Store the project into a temporary file
        Path tempFile = getProject().newTemporaryFilePath("archive-project", ".jip");
        project.saveProject(tempFile);

        // Load the project again
        JIPipeProject copyProject = JIPipeProject.loadProject(tempFile, new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
        ImmutableList<JIPipeGraphNode> graphNodes = ImmutableList.copyOf(copyProject.getGraph().getGraphNodes());
        progressInfo.setProgress(0, graphNodes.size());
        JIPipeProgressInfo archivingProgress = progressInfo.resolve("Archiving data");
        for (int i = 0; i < graphNodes.size(); i++) {
            JIPipeGraphNode graphNode = graphNodes.get(i);
            graphNode.archiveTo(projectStorage, wrappedExternalStorage, archivingProgress.resolveAndLog(graphNode.getDisplayName(), i, graphNodes.size()), getProject().getWorkDirectory());
        }

        progressInfo.setProgress(2, 3);
        progressInfo.log("Writing project ...");
        Path fileSystemPath = projectStorage.getFileSystemPath();
        copyProject.setWorkDirectory(fileSystemPath);
        copyProject.saveProject(fileSystemPath.resolve("project.jip"));
        progressInfo.setProgress(3, 3);

        Files.delete(tempFile);
    }
}
