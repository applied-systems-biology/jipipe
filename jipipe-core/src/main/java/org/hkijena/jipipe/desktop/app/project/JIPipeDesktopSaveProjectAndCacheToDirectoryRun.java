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
import org.hkijena.jipipe.api.cache.JIPipeLocalProjectMemoryCache;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.cache.exporters.JIPipeDesktopDataTableToOutputExporterRun;
import org.hkijena.jipipe.plugins.settings.JIPipeProjectDefaultsApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

public class JIPipeDesktopSaveProjectAndCacheToDirectoryRun extends AbstractJIPipeRunnable {
    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeProject project;
    private final Path outputPath;
    private final boolean addAsRecentProject;

    public JIPipeDesktopSaveProjectAndCacheToDirectoryRun(JIPipeDesktopWorkbench workbench, JIPipeProject project, Path outputPath, boolean addAsRecentProject) {
        this.workbench = workbench;
        this.project = project;
        this.outputPath = outputPath;
        this.addAsRecentProject = addAsRecentProject;
    }

    @Override
    public String getTaskLabel() {
        return "Save project and cache (directory)";
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        if (Files.isDirectory(outputPath)) {
            PathUtils.deleteDirectoryRecursively(outputPath, getProgressInfo().resolve("Delete existing files"));
        }
        try {
            Files.createDirectories(outputPath);
            project.saveProject(outputPath.resolve("project.jip"));
            if (addAsRecentProject)
                JIPipeProjectDefaultsApplicationSettings.getInstance().addRecentProject(outputPath);
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

            JIPipeDesktopDataTableToOutputExporterRun run = new JIPipeDesktopDataTableToOutputExporterRun(workbench, nodeDir,
                    new ArrayList<>(slotMap.values()), true, false);
            run.setProgressInfo(nodeProgress);
            run.run();
        }
    }
}
