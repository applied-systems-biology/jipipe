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

package org.hkijena.jipipe.ui.project;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.cache.JIPipeLocalProjectMemoryCache;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.ProjectsSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.exporters.JIPipeDataTableToOutputExporterRun;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

public class SaveProjectAndCacheToDirectoryRun implements JIPipeRunnable {
    private final JIPipeWorkbench workbench;
    private final JIPipeProject project;
    private final Path outputPath;
    private final boolean addAsRecentProject;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public SaveProjectAndCacheToDirectoryRun(JIPipeWorkbench workbench, JIPipeProject project, Path outputPath, boolean addAsRecentProject) {
        this.workbench = workbench;
        this.project = project;
        this.outputPath = outputPath;
        this.addAsRecentProject = addAsRecentProject;
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
        return "Save project and cache (directory)";
    }

    @Override
    public void run() {
        if (Files.isDirectory(outputPath)) {
            PathUtils.deleteDirectoryRecursively(outputPath, getProgressInfo().resolve("Delete existing files"));
        }
        try {
            Files.createDirectories(outputPath);
            project.saveProject(outputPath.resolve("project.jip"));
            if (addAsRecentProject)
                ProjectsSettings.getInstance().addRecentProject(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<JIPipeGraphNode> nodes = new ArrayList<>(project.getGraph().getGraphNodes());
        progressInfo.setProgress(0, nodes.size());
        JIPipeLocalProjectMemoryCache cache = project.getCache();
        for (int i = 0; i < nodes.size(); i++) {
            if (getProgressInfo().isCancelled())
                return;
            progressInfo.setProgress(i + 1, nodes.size());
            JIPipeGraphNode node = nodes.get(i);
            JIPipeProgressInfo nodeProgress = progressInfo.resolveAndLog(node.getDisplayName(), i, nodes.size());

            Map<String, JIPipeDataTable> slotMap = cache.query(node, node.getUUIDInParentGraph(), nodeProgress);
            if (slotMap == null || slotMap.isEmpty())
                continue;

            Path nodeDir = outputPath.resolve(node.getProjectCompartment().getAliasIdInParentGraph()).resolve(node.getAliasIdInParentGraph());
            try {
                Files.createDirectories(nodeDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JIPipeDataTableToOutputExporterRun run = new JIPipeDataTableToOutputExporterRun(workbench, nodeDir,
                    new ArrayList<>(slotMap.values()), true, false);
            run.setProgressInfo(nodeProgress);
            run.run();
        }
    }
}
