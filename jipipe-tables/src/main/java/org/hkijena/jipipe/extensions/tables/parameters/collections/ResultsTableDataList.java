package org.hkijena.jipipe.extensions.tables.parameters.collections;

import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

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
