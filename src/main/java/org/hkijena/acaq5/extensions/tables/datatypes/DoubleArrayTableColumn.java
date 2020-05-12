package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * {@link TableColumn} that contains doubles.
 */
@ACAQDocumentation(name = "Numeric table column", description = "A table column that contains numbers (64bit floating point)")
public class DoubleArrayTableColumn implements MutableTableColumn {

    private double[] data;
    private String label;

    /**
     * Creates a new instance
     *
     * @param data  the data. Can have any size
     * @param label non-empty name
     */
    public DoubleArrayTableColumn(double[] data, String label) {
        this.data = data;
        this.label = label;
    }

    @Override
    public String[] getDataAsString(int rows) {
        double[] arr = getDataAsDouble(rows);
        String[] result = new String[rows];
        for (int i = 0; i < arr.length; ++i) {
            result[i] = "" + arr[i];
        }
        return result;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        return Arrays.copyOf(data, rows);
    }

    @Override
    public String getLabel() {
        return label;
    }

    public double[] getData() {
        return data;
    }

    @Override
    public int getRows() {
        return data.length;
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public boolean isUserRemovable() {
        return true;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    @Override
    public ACAQData duplicate() {
        return new DoubleArrayTableColumn(Arrays.copyOf(data, data.length), label);
    }
}
