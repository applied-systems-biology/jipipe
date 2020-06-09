package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataConverter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

import java.util.Collections;

/**
 * Converts a column to a table
 */
public class StringArrayColumnToTableConverter implements ACAQDataConverter {
    @Override
    public Class<? extends ACAQData> getInputType() {
        return StringArrayTableColumn.class;
    }

    @Override
    public Class<? extends ACAQData> getOutputType() {
        return ResultsTableData.class;
    }

    @Override
    public ACAQData convert(ACAQData input) {
        return new ResultsTableData(Collections.singletonList((TableColumn) input));
    }
}
