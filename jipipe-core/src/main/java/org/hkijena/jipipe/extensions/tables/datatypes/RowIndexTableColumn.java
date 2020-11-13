/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.tables.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Path;

/**
 * Generates numbers from 0 to row count
 */
@JIPipeDocumentation(name = "Row index table column", description = "A table column that generates each row based on the current row index")
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
    public void saveTo(Path storageFilePath, String name, boolean forceName) {

    }

    @Override
    public JIPipeData duplicate() {
        return new RowIndexTableColumn();
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    public static RowIndexTableColumn importFrom(Path storagePath) {
        return new RowIndexTableColumn();
    }
}
