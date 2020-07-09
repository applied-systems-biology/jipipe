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

package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.tables.ResultsTableData;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter between {@link ResultsTableData} and {@link ResultsTable}
 */
public class ResultsTableDataImageJAdapter implements ImageJDatatypeAdapter {
    @Override
    public boolean canConvertImageJToACAQ(Object imageJData) {
        return imageJData instanceof ResultsTable;
    }

    @Override
    public boolean canConvertACAQToImageJ(ACAQData acaqData) {
        return acaqData instanceof ResultsTableData;
    }

    @Override
    public Class<?> getImageJDatatype() {
        return ResultsTable.class;
    }

    @Override
    public Class<? extends ACAQData> getACAQDatatype() {
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
    public ACAQData convertImageJToACAQ(Object imageJData) {
        if (imageJData instanceof ResultsTable)
            return new ResultsTableData((ResultsTable) imageJData);
        else
            return new ResultsTableData((ResultsTable) ResultsTable.getResultsTable().clone());
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate, boolean noWindow, String windowName) {
        ResultsTable resultsTable = ((ResultsTableData) acaqData).getTable();
        if (activate) {
            ResultsTable.getResultsTable().reset();
            ((ResultsTableData) acaqData).addToTable(ResultsTable.getResultsTable());
            if (!noWindow) {
                ResultsTable.getResultsTable().show("Results");
            }
        }
        return resultsTable;
    }

    @Override
    public List<Object> convertMultipleACAQToImageJ(List<ACAQData> acaqData, boolean activate, boolean noWindow, String windowName) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < acaqData.size(); i++) {
            result.add(convertACAQToImageJ(acaqData.get(i), activate, noWindow, windowName + "/" + i));
        }
        return result;
    }

    @Override
    public ACAQData importFromImageJ(String parameters) {
        ResultsTable table = ResultsTable.getResultsTable();
        return new ResultsTableData((ResultsTable) table.clone());
    }
}
