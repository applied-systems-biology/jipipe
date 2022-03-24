package org.hkijena.jipipe.extensions.tables.compat;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.List;

@JIPipeDocumentation(name = "Import results table", description = "Imports a results table")
public class ResultsTableDataImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters) {
        JIPipeDataTable dataTable = new JIPipeDataTable(ResultsTableData.class);
        if(objects != null && !objects.isEmpty()) {
            for (Object object : objects) {
                dataTable.addData(new ResultsTableData((ResultsTable) object), new JIPipeProgressInfo());
            }
        }
        else {
            dataTable.addData(new ResultsTableData((ResultsTable) ResultsTable.getResultsTable().clone()), new JIPipeProgressInfo());
        }
        return dataTable;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return ResultsTableData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return ResultsTable.class;
    }
}
