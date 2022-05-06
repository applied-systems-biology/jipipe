package org.hkijena.jipipe.extensions.tables.compat;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.Frame;
import java.util.List;

@JIPipeDocumentation(name = "Import results table", description = "Imports a results table. To import a table other than the default 'Results' table, customize the name.")
public class ResultsTableDataImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(ResultsTableData.class);
        if (objects != null && !objects.isEmpty()) {
            for (Object object : objects) {
                dataTable.addData(new ResultsTableData((ResultsTable) object), new JIPipeProgressInfo());
            }
        } else {
            String tableName = StringUtils.orElse(parameters.getName(), "Results");
            ResultsTable resultsTable = null;
            if("Results".equals(tableName)) {
                resultsTable = ResultsTable.getResultsTable();
            }
            else {
                Frame frame = WindowManager.getFrame(tableName);
                if(frame instanceof TextWindow) {
                    resultsTable = ((TextWindow) frame).getResultsTable();
                }
            }
            dataTable.addData(new ResultsTableData((ResultsTable) resultsTable.clone()), new JIPipeProgressInfo());
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
