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

package org.hkijena.jipipe.plugins.tables.parameters.collections;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;

/**
 * A list of {@link ResultsTableData}
 */
public class ResultsTableDataList extends ListParameter<ResultsTableData> {

    /**
     * Creates a new instance
     */
    public ResultsTableDataList() {
        super(ResultsTableData.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ResultsTableDataList(ResultsTableDataList other) {
        super(ResultsTableData.class);
        for (ResultsTableData data : other) {
            add(new ResultsTableData(data));
        }
    }
}
