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
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.importers.JIPipeImportCachedSlotOutputRun;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class LoadResultDirectoryIntoCacheRun implements JIPipeRunnable {
    private final JIPipeWorkbench workbench;
    private final JIPipeProject project;
    private final Path resultPath;

    private final boolean clearBefore;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public LoadResultDirectoryIntoCacheRun(JIPipeWorkbench workbench, JIPipeProject project, Path resultPath, boolean clearBefore) {
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
        ArrayList<JIPipeGraphNode> nodes = new ArrayList<>(project.getGraph().getGraphNodes());
        progressInfo.setProgress(0, nodes.size());
        if(clearBefore) {
            progressInfo.log("Clearing existing cache ...");
            project.getCache().clear();
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (getProgressInfo().isCancelled())
                return;
            JIPipeGraphNode node = nodes.get(i);
            progressInfo.setProgress(i, nodes.size());
            JIPipeProgressInfo nodeProgress = progressInfo.resolveAndLog(node.getDisplayName(), i, nodes.size());
            Path nodeDir = resultPath.resolve(node.getProjectCompartment().getAliasIdInParentGraph()).resolve(node.getAliasIdInParentGraph());

            if (Files.isDirectory(nodeDir)) {
                JIPipeImportCachedSlotOutputRun run = new JIPipeImportCachedSlotOutputRun(project, node, nodeDir);
                run.setProgressInfo(nodeProgress);
                run.run();
            }
        }
    }
}
