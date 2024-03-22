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
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.utils.ArchiveUtils;

import java.io.IOException;
import java.nio.file.Path;

public class JIPipeExtractTemplateZipFileRun implements JIPipeRunnable {

    private final JIPipeProjectTemplate template;
    private final Path targetDirectory;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public JIPipeExtractTemplateZipFileRun(JIPipeProjectTemplate template, Path targetDirectory) {
        this.template = template;
        this.targetDirectory = targetDirectory;
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
        return "Extract template";
    }

    @Override
    public void run() {
        try {
            ArchiveUtils.decompressZipFile(template.getZipFile(), targetDirectory, getProgressInfo());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
