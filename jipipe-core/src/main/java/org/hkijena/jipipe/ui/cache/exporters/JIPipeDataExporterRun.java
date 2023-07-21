package org.hkijena.jipipe.ui.cache.exporters;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;

public class JIPipeDataExporterRun extends AbstractJIPipeRunnable {
    private final Path outputDirectory;
    private JIPipeData data;
    private String name;

    public JIPipeDataExporterRun(JIPipeData data, Path outputDirectory, String name) {
        this.data = data;
        this.outputDirectory = outputDirectory;
        this.name = name;
    }

    @Override
    public String getTaskLabel() {
        return "Export data";
    }

    @Override
    public void onInterrupted(InterruptedEvent event) {
        super.onInterrupted(event);
        data = null;
    }

    @Override
    public void run() {
        try {
            if (StringUtils.isNullOrEmpty(name)) {
                name = "untitled";
            }
            getProgressInfo().log("Exporting " + data + " to " + outputDirectory + " as " + name);
            data.exportData(new JIPipeFileSystemWriteDataStorage(getProgressInfo(), outputDirectory), name, true, getProgressInfo());
        } finally {
            data = null;
        }
    }
}
