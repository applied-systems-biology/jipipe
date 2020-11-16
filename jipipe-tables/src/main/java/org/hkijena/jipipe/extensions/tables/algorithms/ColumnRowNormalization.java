package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

import java.util.ArrayList;
import java.util.List;

public enum ColumnRowNormalization {
    ZeroOrEmpty("Zero or empty"),
    RowIndex("Row index"),
    LastValue("Last value"),
    Cycling("Cycle");

    private final String name;

    ColumnRowNormalization(String name) {
        this.name = name;
    }

    /**
     * Generates columns that have the same number of true rows
     * @param inputColumns the input columns
     * @return List of {@link org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn} or {@link org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn}
     */
    public List<TableColumn> normalize(List<TableColumn> inputColumns) {
        List<TableColumn> result = new ArrayList<>();
        int nRow = 0;
        for (TableColumn column : inputColumns) {
            nRow = Math.max(column.getRows(), nRow);
        }
        for (TableColumn inputColumn : inputColumns) {
            if(inputColumn.isNumeric()) {
                double[] data = new double[nRow];
                for (int row = 0; row < nRow; row++) {
                    if(row < inputColumn.getRows())
                        data[row] = inputColumn.getRowAsDouble(row);
                    else {
                        switch (this) {
                            case RowIndex:
                                data[row] = row;
                                break;
                            case LastValue:
                                if(inputColumn.getRows() > 0)
                                    data[row] = inputColumn.getRowAsDouble(inputColumn.getRows() - 1);
                                else
                                    data[row] = 0;
                                break;
                            case Cycling:
                                if(inputColumn.getRows() > 0)
                                    data[row] = inputColumn.getRowAsDouble(row % inputColumn.getRows());
                                else
                                    data[row] = 0;
                                break;
                        }
                    }
                }
                result.add(new DoubleArrayTableColumn(data, inputColumn.getLabel()));
            }
            else {
                String[] data = new String[nRow];
                for (int row = 0; row < nRow; row++) {
                    if(row < inputColumn.getRows())
                        data[row] = inputColumn.getRowAsString(row);
                    else {
                        switch (this) {
                            case RowIndex:
                                data[row] = "" + row;
                                break;
                            case LastValue:
                                if(inputColumn.getRows() > 0)
                                    data[row] = inputColumn.getRowAsString(inputColumn.getRows() - 1);
                                else
                                    data[row] = "";
                                break;
                            case Cycling:
                                if(inputColumn.getRows() > 0)
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

    @Override
    public String toString() {
        return name;
    }
}
