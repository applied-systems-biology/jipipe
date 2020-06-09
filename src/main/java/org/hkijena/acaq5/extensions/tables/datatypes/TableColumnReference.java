package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

import java.nio.file.Path;

/**
 * A table column that references a column within a {@link org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData}
 */
public class TableColumnReference implements TableColumn {

    private ResultsTableData source;
    private int sourceColumn;

    public TableColumnReference(ResultsTableData source, int sourceColumn) {
        this.source = source;
        this.sourceColumn = sourceColumn;
    }

    @Override
    public String[] getDataAsString(int rows) {
        String[] strings = new String[rows];
        for (int row = 0; row < rows; row++) {
            if(row < source.getRowCount())
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
            if(row < source.getRowCount())
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
        return false;
    }

    @Override
    public boolean isUserRemovable() {
        return false;
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    @Override
    public ACAQData duplicate() {
        return null;
    }
}
