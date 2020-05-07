package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;

/**
 * Generates numbers from 0 to row count
 */
public class ZeroTableColumn implements TableColumn {
    @Override
    public String getLabel() {
        return "Generate: Zeros";
    }

    @Override
    public String[] getDataAsString(int rows) {
        String[] data = new String[rows];
        for (int i = 0; i < rows; i++) {
            data[i] = "0";
        }
        return data;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        return new double[rows];
    }

    @Override
    public int getRows() {
        return 0;
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public boolean isUserRemovable() {
        return false;
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    @Override
    public ACAQData duplicate() {
        return new ZeroTableColumn();
    }
}
