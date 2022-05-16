package org.hkijena.jipipe.extensions.cellpose.compat;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "Export Cellpose size model", description = "Exports a Cellpose size model into a directory.")
public class CellPoseSizeModelImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        Path path = Paths.get(parameters.getName());
        if (!path.isAbsolute()) {
            path = RuntimeSettings.generateTempDirectory("cellpose-export").resolve(path);
        }
        if (Files.exists(path)) {
            if (Files.isRegularFile(path)) {
                throw new RuntimeException("You provided an existing file. This is not allowed for Cellpose size models.");
            }
        } else {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 0; i < dataTable.getRowCount(); i++) {
            CellPoseSizeModelData modelData = dataTable.getData(i, CellPoseSizeModelData.class, progressInfo);
            modelData.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, path), "data", false, progressInfo);
        }
        return Collections.emptyList();
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return CellPoseSizeModelData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return Object.class;
    }
}