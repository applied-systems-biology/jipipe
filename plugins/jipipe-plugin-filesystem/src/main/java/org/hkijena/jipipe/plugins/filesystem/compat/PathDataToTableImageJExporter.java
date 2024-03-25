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

package org.hkijena.jipipe.plugins.filesystem.compat;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.plugins.tables.compat.ResultsTableDataImageJExporter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

import java.util.List;

@SetJIPipeDocumentation(name = "Export paths as table", description = "Exports paths as ImageJ table")
public class PathDataToTableImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        ResultsTableData result = new ResultsTableData();
        result.addStringColumn("Path");

        for (int row = 0; row < dataTable.getRowCount(); row++) {
            result.addRow();
            result.setValueAt(dataTable.getData(row, PathData.class, new JIPipeProgressInfo()).getPath(), row, 0);
        }

        ResultsTableDataImageJExporter exporter = new ResultsTableDataImageJExporter();
        return exporter.exportData(result, parameters, progressInfo);
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return PathData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return ResultsTable.class;
    }
}
