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

package org.hkijena.jipipe.extensions.cellpose.compat;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellposeSizeModelData;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@SetJIPipeDocumentation(name = "Export Cellpose size model", description = "Exports a Cellpose size model into a directory.")
public class CellposeSizeModelImageJExporter implements ImageJDataExporter {
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
            CellposeSizeModelData modelData = dataTable.getData(i, CellposeSizeModelData.class, progressInfo);
            modelData.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, path), "data", false, progressInfo);
        }
        return Collections.emptyList();
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return CellposeSizeModelData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return Object.class;
    }
}
