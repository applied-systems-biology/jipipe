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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.desktop.app.cache.importers.JIPipeDesktopImportCachedSlotOutputRun;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class JIPipeDesktopLoadResultDirectoryIntoCacheRun implements JIPipeRunnable {
    private final JIPipeWorkbench workbench;
    private final JIPipeProject project;
    private final Path resultPath;

    private final boolean clearBefore;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public JIPipeDesktopLoadResultDirectoryIntoCacheRun(JIPipeWorkbench workbench, JIPipeProject project, Path resultPath, boolean clearBefore) {
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
        if (clearBefore) {
            progressInfo.log("Clearing existing cache ...");
            project.getCache().clearAll(progressInfo.resolve("Clear cache"));
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (getProgressInfo().isCancelled())
                return;
            JIPipeGraphNode node = nodes.get(i);
            progressInfo.setProgress(i, nodes.size());
            JIPipeProgressInfo nodeProgress = progressInfo.resolveAndLog(node.getDisplayName(), i, nodes.size());
            Path nodeDir = resultPath.resolve(node.getProjectCompartment().getAliasIdInParentGraph()).resolve(node.getAliasIdInParentGraph());

            if (Files.isDirectory(nodeDir)) {
                JIPipeDesktopImportCachedSlotOutputRun run = new JIPipeDesktopImportCachedSlotOutputRun(project, node, nodeDir);
                run.setProgressInfo(nodeProgress);
                run.run();
            }
        }
    }
}
