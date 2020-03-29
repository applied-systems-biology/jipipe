package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ROIData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

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
     * @param imageJData
     * @return
     */
    @Override
    public ACAQData convertImageJToACAQ(Object imageJData) {
        if(imageJData instanceof ResultsTable)
            return new ResultsTableData((ResultsTable) imageJData);
        else
            return new ResultsTableData((ResultsTable) ResultsTable.getResultsTable().clone());
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate) {
        ResultsTable resultsTable = ((ResultsTableData) acaqData).getTable();
        if (activate) {
            ResultsTable.getResultsTable().reset();
            ((ResultsTableData) acaqData).addToTable(ResultsTable.getResultsTable());
        }
        return resultsTable;
    }
}
