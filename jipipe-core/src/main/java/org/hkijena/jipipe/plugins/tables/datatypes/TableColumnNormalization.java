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

package org.hkijena.jipipe.plugins.tables.datatypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum TableColumnNormalization {
    ZeroOrEmpty("Zero or empty"),
    NaNOrEmpty("NaN or empty"),
    NegativeInfinityOrEmpty("-∞ or empty"),
    PositiveInfinityOrEmpty("+∞ or empty"),
    RowIndex("Row index"),
    LastValue("Last value"),
    Cycling("Cycle");

    private final String name;

    TableColumnNormalization(String name) {
        this.name = name;
    }


    /**
     * Generates columns that have the same number of true rows
     *
     * @param inputColumns the input columns
     * @return List of {@link org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumn} or {@link org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumn}
     */
    public List<TableColumn> normalize(List<TableColumn> inputColumns, int nRow) {
        List<TableColumn> result = new ArrayList<>();
        for (TableColumn inputColumn : inputColumns) {
            if (inputColumn.isNumeric()) {
                double[] data = new double[nRow];
                for (int row = 0; row < nRow; row++) {
                    if (row < inputColumn.getRows())
                        data[row] = inputColumn.getRowAsDouble(row);
                    else {
                        switch (this) {
                            case RowIndex:
                                data[row] = row;
                                break;
                            case NaNOrEmpty:
                                data[row] = Double.NaN;
                                break;
                            case NegativeInfinityOrEmpty:
                                data[row] = Double.NEGATIVE_INFINITY;
                                break;
                            case PositiveInfinityOrEmpty:
                                data[row] = Double.POSITIVE_INFINITY;
                                break;
                            case LastValue:
                                if (inputColumn.getRows() > 0)
                                    data[row] = inputColumn.getRowAsDouble(inputColumn.getRows() - 1);
                                else
                                    data[row] = 0;
                                break;
                            case Cycling:
                                if (inputColumn.getRows() > 0)
                                    data[row] = inputColumn.getRowAsDouble(row % inputColumn.getRows());
                                else
                                    data[row] = 0;
                                break;
                        }
                    }
                }
                result.add(new DoubleArrayTableColumn(data, inputColumn.getLabel()));
            } else {
                String[] data = new String[nRow];
                for (int row = 0; row < nRow; row++) {
                    if (row < inputColumn.getRows())
                        data[row] = inputColumn.getRowAsString(row);
                    else {
                        switch (this) {
                            case RowIndex:
                                data[row] = "" + row;
                                break;
                            case LastValue:
                                if (inputColumn.getRows() > 0)
                                    data[row] = inputColumn.getRowAsString(inputColumn.getRows() - 1);
                                else
                                    data[row] = "";
                                break;
                            case Cycling:
                                if (inputColumn.getRows() > 0)
                                    data[row] = inputColumn.getRowAsString(row % inputColumn.getRows());
                                else
                                    data[row] = "";
                                break;
                            case ZeroOrEmpty:
                                data[row] = "";
                                break;
                        }
                    }
                }
                result.add(new StringArrayTableColumn(data, inputColumn.getLabel()));
            }
        }
        return result;
    }

    /**
     * Generates columns that have the same number of true rows
     *
     * @param inputColumns the input columns
     * @return List of {@link org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumn} or {@link org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumn}
     */
    public List<TableColumn> normalize(List<TableColumn> inputColumns) {
        int nRow = 0;
        for (TableColumn column : inputColumns) {
            nRow = Math.max(column.getRows(), nRow);
        }
        return normalize(inputColumns, nRow);
    }

    /**
     * Normalizes a whole table
     *
     * @param tableData the table
     * @param nRow      number of rows
     * @return table with at least nRow rows
     */
    public ResultsTableData normalize(ResultsTableData tableData, int nRow) {
        ResultsTableData copy = new ResultsTableData();
        for (String columnName : tableData.getColumnNames()) {
            TableColumn columnReference = tableData.getColumnReference(tableData.getColumnIndex(columnName));
            TableColumn generatedColumn = normalize(Collections.singletonList(columnReference), nRow).get(0);
            copy.addColumn(columnName, generatedColumn, true);
        }
        return copy;
    }

    @Override
    public String toString() {
        return name;
    }
}
