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

package org.hkijena.jipipe.plugins.tables.compat;

import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.context.JIPipeMutableDataContext;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.List;

@SetJIPipeDocumentation(name = "Import results table", description = "Imports a results table. To import a table other than the default 'Results' table, customize the name.")
public class ResultsTableDataImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(ResultsTableData.class);
        if (objects != null && !objects.isEmpty()) {
            for (Object object : objects) {
                dataTable.addData(new ResultsTableData((ResultsTable) object), new JIPipeMutableDataContext(), new JIPipeProgressInfo());
            }
        } else {
            String tableName = StringUtils.orElse(parameters.getName(), "Results");
            ResultsTable resultsTable = null;
            if ("Results".equals(tableName)) {
                resultsTable = ResultsTable.getResultsTable();
            } else {
                Frame frame = WindowManager.getFrame(tableName);
                if (frame instanceof TextWindow) {
                    resultsTable = ((TextWindow) frame).getResultsTable();
                }
            }
            dataTable.addData(new ResultsTableData((ResultsTable) resultsTable.clone()), new JIPipeMutableDataContext(), new JIPipeProgressInfo());
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
