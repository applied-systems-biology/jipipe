package org.hkijena.acaq5.extensions.annotation.algorithms;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataConverter;
import org.hkijena.acaq5.extensions.annotation.datatypes.AnnotationTableData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

/**
 * Converts {@link ResultsTableData} to {@link AnnotationTableData}
 */
public class ImplicitResultsTableDataConverter implements ACAQDataConverter {
    @Override
    public Class<? extends ACAQData> getInputType() {
        return ResultsTableData.class;
    }

    @Override
    public Class<? extends ACAQData> getOutputType() {
        return AnnotationTableData.class;
    }

    @Override
    public ACAQData convert(ACAQData input) {
        return new AnnotationTableData((ResultsTableData) input);
    }
}
