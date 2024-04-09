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

package org.hkijena.jipipe.desktop.app.cache.exporters;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;

public class JIPipeDesktopDataExporterRun extends AbstractJIPipeRunnable {
    private final Path outputDirectory;
    private JIPipeData data;
    private String name;

    public JIPipeDesktopDataExporterRun(JIPipeData data, Path outputDirectory, String name) {
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
