package org.hkijena.jipipe.api.compat.ui;

import org.hkijena.jipipe.api.compat.ImageJDataExportOperation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathType;

public class PathImageJDataExporterUI extends AbstractPathImageJDataExporterUI {
    /**
     * @param workbench the workbench
     * @param exporter  the importer
     */
    public PathImageJDataExporterUI(JIPipeWorkbench workbench, ImageJDataExportOperation exporter) {
        super(workbench, exporter);
    }

    @Override
    public PathType getPathType() {
        return PathType.FilesAndDirectories;
    }
}