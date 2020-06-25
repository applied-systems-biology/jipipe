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

package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataConverter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

import java.util.Collections;

/**
 * Converts a column to a table
 */
public class DoubleArrayColumnToTableConverter implements ACAQDataConverter {
    @Override
    public Class<? extends ACAQData> getInputType() {
        return DoubleArrayTableColumn.class;
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
