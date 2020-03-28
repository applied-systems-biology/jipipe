package org.hkijena.acaq5.extensions.imagejdatatypes.compat;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
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

    @Override
    public ACAQData convertImageJToACAQ(Object imageJData) {
        return new ResultsTableData((ResultsTable) imageJData);
    }

    @Override
    public Object convertACAQToImageJ(ACAQData acaqData, boolean activate) {
        ResultsTable resultsTable = ((ResultsTableData) acaqData).getTable();
        if (activate) {
            resultsTable.show("Results");
        }
        return resultsTable;
    }
}
