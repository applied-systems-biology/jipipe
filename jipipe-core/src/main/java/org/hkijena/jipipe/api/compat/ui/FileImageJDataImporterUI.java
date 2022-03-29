package org.hkijena.jipipe.api.compat.ui;

import org.hkijena.jipipe.api.compat.ImageJDataImportOperation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathType;

public class FileImageJDataImporterUI extends AbstractPathImageJDataImporterUI {
    /**
     * @param workbench the workbench
     * @param importer  the importer
     */
    public FileImageJDataImporterUI(JIPipeWorkbench workbench, ImageJDataImportOperation importer) {
        super(workbench, importer);
    }

    @Override
    public PathType getPathType() {
        return PathType.FilesOnly;
    }
}
