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

package org.hkijena.jipipe.ui.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;

public class AllCacheToNonVirtualRun implements JIPipeRunnable {

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "Move cache from HDD to RAM";
    }

    @Override
    public void run() {
        progressInfo.setMaxProgress(JIPipeProjectWindow.getOpenWindows().size());
        for (JIPipeProjectWindow window : JIPipeProjectWindow.getOpenWindows()) {
            JIPipeProject project = window.getProject();
            progressInfo.incrementProgress();
            JIPipeProgressInfo subProgress = this.progressInfo.resolveAndLog("Project '" + (window.getProjectSavePath() == null ? "New project" : window.getProjectSavePath().getFileName()) + "'");
            project.getCache().makeNonVirtual(subProgress);
        }
    }
}
