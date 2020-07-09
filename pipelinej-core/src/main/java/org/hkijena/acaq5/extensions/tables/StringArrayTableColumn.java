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

package org.hkijena.acaq5.extensions.tables;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.ui.ACAQWorkbench;

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
    public void saveTo(Path storageFilePath, String name, boolean forceName) {
        ResultsTableData resultsTableData = new ResultsTableData(Collections.singletonList(this));
        resultsTableData.saveTo(storageFilePath, name, forceName);
    }

    @Override
    public ACAQData duplicate() {
        return new StringArrayTableColumn(Arrays.copyOf(data, data.length), label);
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {
        ResultsTableData data = new ResultsTableData(Collections.singleton(this));
        data.display(displayName, workbench);
    }
}
