/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.tables.ResultsTableData;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter between {@link ResultsTableData} and {@link ResultsTable}
 */
public class ResultsTableDataImageJAdapter implements ImageJDatatypeAdapter {
    @Override
    public boolean canConvertImageJToJIPipe(Object imageJData) {
        return imageJData instanceof ResultsTable;
    }

    @Override
    public boolean canConvertJIPipeToImageJ(JIPipeData jipipeData) {
        return jipipeData instanceof ResultsTableData;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return ResultsTable.class;
    }

    @Override
    public Class<? extends JIPipeData> getJIPipeDatatype() {
        return ResultsTableData.class;
    }

    /**
     * Converts {@link ResultsTable} to {@link ResultsTableData}.
     * If imageJData is null, the currently active {@link ResultsTable} is used.
     *
     * @param imageJData ImageJ data
     * @return converted data
     */
    @Override
    public JIPipeData convertImageJToJIPipe(Object imageJData) {
        if (imageJData instanceof ResultsTable)
            return new ResultsTableData((ResultsTable) imageJData);
        else
            return new ResultsTableData((ResultsTable) ResultsTable.getResultsTable().clone());
    }

    @Override
    public Object convertJIPipeToImageJ(JIPipeData jipipeData, boolean activate, boolean noWindow, String windowName) {
        ResultsTable resultsTable = ((ResultsTableData) jipipeData).getTable();
        if (activate) {
            ResultsTable.getResultsTable().reset();
            ((ResultsTableData) jipipeData).addToTable(ResultsTable.getResultsTable());
            if (!noWindow) {
                ResultsTable.getResultsTable().show("Results");
            }
        }
        return resultsTable;
    }

    @Override
    public List<Object> convertMultipleJIPipeToImageJ(List<JIPipeData> jipipeData, boolean activate, boolean noWindow, String windowName) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < jipipeData.size(); i++) {
            result.add(convertJIPipeToImageJ(jipipeData.get(i), activate, noWindow, windowName + "/" + i));
        }
        return result;
    }

    @Override
    public JIPipeData importFromImageJ(String parameters) {
        ResultsTable table = ResultsTable.getResultsTable();
        return new ResultsTableData((ResultsTable) table.clone());
    }
}
