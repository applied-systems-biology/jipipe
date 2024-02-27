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

package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.extensions.tables.compat.ResultsTableDataImageJExporter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * The fallback/default data adapter that can handle any data type.
 * Its operations are based on importing/exporting data tables
 */
@SetJIPipeDocumentation(name = "Data table export", description = "Exports a data table directory, provided via the name.")
public class DataTableImageJDataExporter implements ImageJDataExporter {
    public static final String ID = "default";

    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters properties, JIPipeProgressInfo progressInfo) {
        Path path = StringUtils.isNullOrEmpty(properties.getName()) ? RuntimeSettings.generateTempDirectory("data-table-export") : Paths.get(properties.getName());
        if (!path.isAbsolute()) {
            path = RuntimeSettings.generateTempDirectory("data-table-export").resolve(path);
        }
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dataTable.exportData(new JIPipeFileSystemWriteDataStorage(new JIPipeProgressInfo(), path), new JIPipeProgressInfo());
        if (properties.isActivate()) {
            ResultsTableData tableData = new ResultsTableData();
            tableData.addStringColumn("Path");
            tableData.addRow();
            tableData.setValueAt(path.toString(), 0, 0);
            (new ResultsTableDataImageJExporter()).exportData(tableData, properties, progressInfo);
        }
        return Collections.singletonList(path.toFile());
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return JIPipeData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return Object.class;
    }
}
