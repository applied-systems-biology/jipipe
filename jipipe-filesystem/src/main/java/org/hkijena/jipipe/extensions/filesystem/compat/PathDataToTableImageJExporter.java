package org.hkijena.jipipe.extensions.filesystem.compat;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.tables.compat.ResultsTableDataImageJExporter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "Export paths as table", description = "Exports paths as ImageJ table")
public class PathDataToTableImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters) {
        ResultsTableData result = new ResultsTableData();
        result.addStringColumn("Path");

        for (int row = 0; row < dataTable.getRowCount(); row++) {
            result.addRow();
            result.setValueAt(dataTable.getData(row, PathData.class, new JIPipeProgressInfo()).getPath(), row, 0);
        }

        ResultsTableDataImageJExporter exporter = new ResultsTableDataImageJExporter();
        return exporter.exportData(result, parameters);
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
