package org.hkijena.jipipe.extensions.tables.compat;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Export to results table", description = "Exports tables into the 'Results' table")
public class ResultsTableDataImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        List<Object> result = new ArrayList<>();
        if (parameters.isActivate()) {
            ResultsTable resultsTable = ResultsTable.getResultsTable();
            if (!parameters.isAppend()) {
                resultsTable.reset();
            }
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                ResultsTableData data = new ResultsTableData(dataTable.getData(i, ResultsTableData.class, new JIPipeProgressInfo()));
                result.add(data.getTable());
                data.addToTable(resultsTable);
            }
            if (!parameters.isNoWindows()) {
                ResultsTable.getResultsTable().show("Results");
            }
        } else {
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                ResultsTableData data = new ResultsTableData(dataTable.getData(i, ResultsTableData.class, new JIPipeProgressInfo()));
                result.add(data.getTable());
            }
        }
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return ResultsTableData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return ResultsTable.class;
    }
}
