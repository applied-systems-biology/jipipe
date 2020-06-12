package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.ui.ACAQWorkbench;

import java.nio.file.Path;

/**
 * Generates numbers from 0 to row count
 */
@ACAQDocumentation(name = "Row index table column", description = "A table column that generates each row based on the current row index")
public class RowIndexTableColumn implements TableColumn {
    @Override
    public String getLabel() {
        return "Generate: Row index";
    }

    @Override
    public String[] getDataAsString(int rows) {
        String[] data = new String[rows];
        for (int i = 0; i < rows; i++) {
            data[i] = "" + i;
        }
        return data;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        double[] data = new double[rows];
        for (int i = 0; i < rows; i++) {
            data[i] = i;
        }
        return data;
    }

    @Override
    public String getRowAsString(int row) {
        return "" + row;
    }

    @Override
    public double getRowAsDouble(int row) {
        return row;
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
        return new RowIndexTableColumn();
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {

    }
}
