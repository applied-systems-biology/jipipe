package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

/**
 * {@link TableColumn} that contains doubles.
 */
@ACAQDocumentation(name = "String table column", description = "A table column that contains text values")
public class StringArrayTableColumn implements MutableTableColumn {

    private String[] data;
    private String label;

    /**
     * Creates a new instance
     *
     * @param data  the data. Can have any size
     * @param label non-empty name
     */
    public StringArrayTableColumn(String[] data, String label) {
        this.data = data;
        this.label = label;
    }

    @Override
    public String[] getDataAsString(int rows) {
        if (rows == data.length)
            return data;
        String[] result = new String[rows];
        for (int i = 0; i < rows; ++i) {
            result[i] = i < data.length ? data[i] : "";
        }
        return result;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        String[] arr = getDataAsString(rows);
        double[] result = new double[rows];
        for (int i = 0; i < arr.length; ++i) {
            try {
                result[i] = Double.parseDouble(arr[i]);
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }

    @Override
    public String getRowAsString(int row) {
        return row < data.length ? data[row] : "";
    }

    @Override
    public double getRowAsDouble(int row) {
        if (row < data.length) {
            try {
                return Double.parseDouble(data[row]);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    @Override
    public int getRows() {
        return data.length;
    }

    @Override
    public boolean isNumeric() {
        return false;
    }

    @Override
    public boolean isUserRemovable() {
        return true;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public String[] getData() {
        return data;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        ResultsTableData resultsTableData = new ResultsTableData(Collections.singletonList(this));
        resultsTableData.saveTo(storageFilePath, name);
    }

    @Override
    public ACAQData duplicate() {
        return new StringArrayTableColumn(Arrays.copyOf(data, data.length), label);
    }
}
