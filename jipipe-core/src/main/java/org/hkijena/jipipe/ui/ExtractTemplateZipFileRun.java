package org.hkijena.jipipe.ui;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.utils.ArchiveUtils;

import java.io.IOException;
import java.nio.file.Path;

public class ExtractTemplateZipFileRun implements JIPipeRunnable {

    private final JIPipeProjectTemplate template;
    private final Path targetDirectory;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    public ExtractTemplateZipFileRun(JIPipeProjectTemplate template, Path targetDirectory) {
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
