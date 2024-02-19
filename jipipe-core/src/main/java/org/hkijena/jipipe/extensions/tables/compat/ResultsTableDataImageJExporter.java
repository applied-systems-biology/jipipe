package org.hkijena.jipipe.extensions.tables.compat;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Export to results table", description = "Exports tables into an ImageJ table. Leave the name empty or set it to 'Results' to export it to the standard 'Results' table.")
public class ResultsTableDataImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        List<Object> result = new ArrayList<>();
        if (parameters.isActivate()) {
            String tableName = StringUtils.orElse(parameters.getName(), "Results");
            ResultsTable resultsTable = "Results".equals(tableName) ? ResultsTable.getResultsTable() : new ResultsTable();
            if (!parameters.isAppend()) {
                resultsTable.reset();
            }
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                ResultsTableData data = new ResultsTableData(dataTable.getData(i, ResultsTableData.class, new JIPipeProgressInfo()));
                result.add(data.getTable());
                data.addToTable(resultsTable);
            }
            if (!parameters.isNoWindows()) {
                resultsTable.show(tableName);
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
