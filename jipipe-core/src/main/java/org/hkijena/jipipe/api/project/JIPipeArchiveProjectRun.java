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
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.ProjectArchiveUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class JIPipeArchiveProjectRun extends DefaultJIPipeRunnable {

    private final JIPipeProject project;

    protected JIPipeArchiveProjectRun(JIPipeProject project) {
        this.project = project;
    }

    public JIPipeProject getProject() {
        return project;
    }

    protected void archive(JIPipeWriteDataStorage projectStorage, JIPipeWriteDataStorage wrappedExternalStorage, Path inputsSubPath) throws IOException {

        if (project.getProjectFile() == null) {
            throw new RuntimeException("The project must be saved at least once!");
        }

        final Path targetPath = projectStorage.getFileSystemPath();

        JIPipeProgressInfo progressInfo = getProgressInfo();
        progressInfo.setProgress(0, 3);
        progressInfo.log("Copying project ...");
        project.saveProject();

        // Load the project again
        JIPipeProject copyProject = JIPipeProject.loadProject(getProject().getProjectFile(), new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox(), progressInfo);
        copyProject.setWorkDirectory(project.getWorkDirectory());
        ImmutableList<JIPipeGraphNode> graphNodes = ImmutableList.copyOf(copyProject.getGraph().getGraphNodes());
        progressInfo.setProgress(0, graphNodes.size());

        // Find all absolute paths that will need to be archived - based on that we then can find the root path
        JIPipeProgressInfo discoveryProgress = progressInfo.resolve("Discovering archived data");
        Set<Path> toArchive = new HashSet<>();
        for (int i = 0; i < graphNodes.size(); i++) {
            JIPipeGraphNode graphNode = graphNodes.get(i);
            toArchive.addAll(graphNode.archiveDiscoverExternalPaths(discoveryProgress.resolveAndLog(graphNode.getDisplayName(), i, graphNodes.size())));
        }
        discoveryProgress.log("-> Discovered " + toArchive.size() + " paths");

        // Ensure absolute paths
        toArchive = PathUtils.ensureAbsoluteNormalized(toArchive);

        // Create the mapping for the files to be archived
        Map<Path, Path> updateMap = ProjectArchiveUtils.shortestRelativeMapping(toArchive);

        // Make mapping relative to inputsSubPath
        for (Path key : ImmutableList.copyOf(updateMap.keySet())) {
            updateMap.put(key, inputsSubPath.resolve(updateMap.get(key)));
        }

        // Print to log
        progressInfo.log("Mapping results: ");
        for (Map.Entry<Path, Path> entry : updateMap.entrySet()) {
            progressInfo.log("- " + entry.getKey() + " -> " + entry.getValue());
        }

        // Archive
        ProjectArchiveUtils.materializeMapping(updateMap, targetPath, false, progressInfo.resolve("Copy files"));

        JIPipeProgressInfo updatingProgress = progressInfo.resolve("Updating nodes");
        for (int i = 0; i < graphNodes.size(); i++) {
            JIPipeGraphNode graphNode = graphNodes.get(i);
            graphNode.archiveUpdateExternalPaths(updateMap, updatingProgress.resolveAndLog(graphNode.getDisplayName(), i, graphNodes.size()));
        }

        progressInfo.setProgress(2, 3);
        progressInfo.log("Writing project ...");

        copyProject.setWorkDirectory(targetPath);
        copyProject.saveProject(targetPath.resolve("project.jip"), true);
        progressInfo.setProgress(3, 3);
    }
}
