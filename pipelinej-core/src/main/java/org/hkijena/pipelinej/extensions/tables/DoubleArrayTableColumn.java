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

package org.hkijena.pipelinej.extensions.tables;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.ui.ACAQWorkbench;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

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
    public String getRowAsString(int row) {
        return row < data.length ? "" + data[row] : "";
    }

    @Override
    public double getRowAsDouble(int row) {
        return row < data.length ? data[row] : 0;
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
    public void saveTo(Path storageFilePath, String name, boolean forceName) {
        ResultsTableData resultsTableData = new ResultsTableData(Collections.singletonList(this));
        resultsTableData.saveTo(storageFilePath, name, forceName);
    }

    @Override
    public ACAQData duplicate() {
        return new DoubleArrayTableColumn(Arrays.copyOf(data, data.length), label);
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {
        ResultsTableData data = new ResultsTableData(Collections.singleton(this));
        data.display(displayName, workbench);
    }
}
