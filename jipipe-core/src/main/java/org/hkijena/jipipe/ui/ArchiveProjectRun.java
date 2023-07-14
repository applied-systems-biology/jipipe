package org.hkijena.jipipe.ui;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.causes.UnspecifiedReportEntryCause;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class ArchiveProjectRun implements JIPipeRunnable {

    private final JIPipeProject project;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    protected ArchiveProjectRun(JIPipeProject project) {
        this.project = project;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    public JIPipeProject getProject() {
        return project;
    }

    protected void archive(JIPipeWriteDataStorage projectStorage, JIPipeWriteDataStorage wrappedExternalStorage) throws IOException {
        progressInfo.setProgress(0, 3);
        progressInfo.log("Copying project ...");
        // Store the project into a temporary file
        Path tempFile = Files.createTempFile(getProject().getWorkDirectory(), "archive-project", ".jip");
        project.saveProject(tempFile);

        // Load the project again
        JIPipeProject copyProject = JIPipeProject.loadProject(tempFile, new UnspecifiedReportEntryCause(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
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
