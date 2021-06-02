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
import org.hkijena.jipipe.api.JIPipeProjectCacheQuery;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCachedSlotToOutputExporterRun;
import org.hkijena.jipipe.ui.running.JIPipeRunExecuterUI;
import org.hkijena.jipipe.utils.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

public class SaveProjectAndCacheRun implements JIPipeRunnable {
    private final JIPipeWorkbench workbench;
    private final JIPipeProject project;
    private final Path outputPath;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public SaveProjectAndCacheRun(JIPipeWorkbench workbench, JIPipeProject project, Path outputPath) {
        this.workbench = workbench;
        this.project = project;
        this.outputPath = outputPath;
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
        return "Save project and cache";
    }

    @Override
    public void run() {
        try {
            Files.createDirectories(outputPath);
            project.saveProject(outputPath.resolve("project.jip"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<JIPipeGraphNode> nodes = new ArrayList<>(project.getGraph().getGraphNodes());
        progressInfo.setProgress(0, nodes.size());
        JIPipeProjectCacheQuery query = new JIPipeProjectCacheQuery(project);
        for (int i = 0; i < nodes.size(); i++) {
            if(getProgressInfo().isCancelled().get())
                return;
            JIPipeGraphNode node = nodes.get(i);
            JIPipeProgressInfo nodeProgress = progressInfo.resolveAndLog(node.getDisplayName(), i, nodes.size());

            Map<String, JIPipeDataSlot> cache = query.getCachedCache(node);
            if(cache == null || cache.isEmpty())
                continue;

            Path nodeDir = outputPath.resolve(node.getProjectCompartment().getAliasIdInGraph()).resolve(node.getAliasIdInGraph());
            try {
                Files.createDirectories(nodeDir);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            JIPipeCachedSlotToOutputExporterRun run = new JIPipeCachedSlotToOutputExporterRun(workbench, nodeDir,
                    new ArrayList<>(cache.values()), true);
            run.setProgressInfo(nodeProgress);
            run.run();
        }
    }
}
