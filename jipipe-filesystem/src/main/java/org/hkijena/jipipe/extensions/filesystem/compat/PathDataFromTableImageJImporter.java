package org.hkijena.jipipe.extensions.filesystem.compat;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.tables.compat.ResultsTableDataImageJImporter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.List;

@JIPipeDocumentation(description = "Imports a path from an opened table. The paths must be located in the first column.")
public class PathDataFromTableImageJImporter implements ImageJDataImporter {

    private final Class<? extends PathData> dataClass;

    public PathDataFromTableImageJImporter(Class<? extends PathData> dataClass) {
        this.dataClass = dataClass;
    }

    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        ResultsTableDataImageJImporter importer = new ResultsTableDataImageJImporter();
        JIPipeDataTable tables = importer.importData(objects, parameters, progressInfo);
        JIPipeDataTable result = new JIPipeDataTable(getImportedJIPipeDataType());
        for (int row = 0; row < tables.getRowCount(); row++) {
            ResultsTableData tableData = result.getData(row, ResultsTableData.class, new JIPipeProgressInfo());
            for (int i = 0; i < tableData.getRowCount(); i++) {
                String value = tableData.getValueAsString(i, 0);
                result.addData(new PathData(value), new JIPipeProgressInfo());
            }
        }
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return dataClass;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return ResultsTable.class;
    }

    @Override
    public String getName() {
        return "Import " + JIPipeDataInfo.getInstance(dataClass).getName() + " from table";
    }
}
