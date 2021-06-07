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
import org.hkijena.jipipe.ui.cache.JIPipeImportCachedSlotOutputRun;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class LoadResultIntoCacheRun implements JIPipeRunnable {
    private final JIPipeWorkbench workbench;
    private final JIPipeProject project;
    private final Path resultPath;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public LoadResultIntoCacheRun(JIPipeWorkbench workbench, JIPipeProject project, Path resultPath) {
        this.workbench = workbench;
        this.project = project;
        this.resultPath = resultPath;
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
        return "Load result into cache";
    }

    @Override
    public void run() {
        ArrayList<JIPipeGraphNode> nodes = new ArrayList<>(project.getGraph().getGraphNodes());
        progressInfo.setProgress(0, nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            if (getProgressInfo().isCancelled().get())
                return;
            JIPipeGraphNode node = nodes.get(i);
            JIPipeProgressInfo nodeProgress = progressInfo.resolveAndLog(node.getDisplayName(), i, nodes.size());
            Path nodeDir = resultPath.resolve(node.getProjectCompartment().getAliasIdInGraph()).resolve(node.getAliasIdInGraph());

            if (Files.isDirectory(nodeDir)) {
                JIPipeImportCachedSlotOutputRun run = new JIPipeImportCachedSlotOutputRun(project, node, nodeDir);
                run.setProgressInfo(nodeProgress);
                run.run();
            }
        }
    }
}
