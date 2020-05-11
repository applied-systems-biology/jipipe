package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * {@link TableColumn} that contains doubles.
 */
public class StringArrayTableColumn implements TableColumn {

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

    }

    @Override
    public ACAQData duplicate() {
        return new StringArrayTableColumn(Arrays.copyOf(data, data.length), label);
    }
}
