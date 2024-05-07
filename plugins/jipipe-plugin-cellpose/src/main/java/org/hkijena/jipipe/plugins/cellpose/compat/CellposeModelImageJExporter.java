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

package org.hkijena.jipipe.plugins.cellpose.compat;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.plugins.cellpose.datatypes.CellposeModelData;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@SetJIPipeDocumentation(name = "Export Cellpose model", description = "Exports a Cellpose model into a directory.")
public class CellposeModelImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        Path path = Paths.get(parameters.getName());
        if (!path.isAbsolute()) {
            path = JIPipeRuntimeApplicationSettings.generateTempDirectory("cellpose-export").resolve(path);
        }
        if (Files.exists(path)) {
            if (Files.isRegularFile(path)) {
                throw new RuntimeException("You provided an existing file. This is not allowed for Cellpose models.");
            }
        } else {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 0; i < dataTable.getRowCount(); i++) {
            CellposeModelData modelData = dataTable.getData(i, CellposeModelData.class, progressInfo);
            modelData.exportData(new JIPipeFileSystemWriteDataStorage(progressInfo, path), "data", false, progressInfo);
        }
        return Collections.emptyList();
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return CellposeModelData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return Object.class;
    }
}
