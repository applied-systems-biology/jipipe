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

package org.hkijena.jipipe.plugins.tables;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.Collections;

/**
 * A table column that references a column within a {@link ResultsTableData}
 */
public class TableColumnDataReference implements TableColumnData {

    private ResultsTableData source;
    private int sourceColumn;
    private String label;

    public TableColumnDataReference(ResultsTableData source, int sourceColumn) {
        this.source = source;
        this.sourceColumn = sourceColumn;
        this.label = source.getColumnName(sourceColumn);
    }

    @Override
    public String[] getDataAsString(int rows) {
        String[] strings = new String[rows];
        for (int row = 0; row < rows; row++) {
            if (row < source.getRowCount())
                strings[row] = source.getValueAsString(row, sourceColumn);
            else
                strings[row] = "";
        }
        return strings;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        double[] doubles = new double[rows];
        for (int row = 0; row < rows; row++) {
            if (row < source.getRowCount())
                doubles[row] = source.getValueAsDouble(row, sourceColumn);
            else
                break;
        }
        return doubles;
    }

    @Override
    public String getRowAsString(int row) {
        return row < source.getRowCount() ? source.getValueAsString(row, sourceColumn) : "";
    }

    @Override
    public double getRowAsDouble(int row) {
        return row < source.getRowCount() ? source.getValueAsDouble(row, sourceColumn) : 0;
    }

    @Override
    public int getRows() {
        return source.getRowCount();
    }

    @Override
    public boolean isNumeric() {
        return source.isNumericColumn(sourceColumn);
    }

    @Override
    public boolean isUserRemovable() {
        return false;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        ResultsTableData resultsTableData = new ResultsTableData(Collections.singletonList(this));
        resultsTableData.exportData(storage, name, forceName, progressInfo);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return this;
    }

}
