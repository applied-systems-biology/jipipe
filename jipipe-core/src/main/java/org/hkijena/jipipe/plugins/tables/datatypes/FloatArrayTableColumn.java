/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.tables.datatypes;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.tables.MutableTableColumn;

import java.util.Arrays;
import java.util.Collections;

/**
 * {@link TableColumn} that contains doubles.
 */
@SetJIPipeDocumentation(name = "Numeric table column (float)", description = "A table column that contains numbers (64bit floating point)")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a single *.csv file that contains the table data.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/results-table.schema.json")
public class FloatArrayTableColumn implements MutableTableColumn {

    private float[] data;
    private String label;

    /**
     * Creates a new instance
     *
     * @param data  the data. Can have any size
     * @param label non-empty name
     */
    public FloatArrayTableColumn(float[] data, String label) {
        this.data = data;
        this.label = label;
    }

    public static FloatArrayTableColumn importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        ResultsTableData resultsTableData = ResultsTableData.importData(storage, progressInfo);
        TableColumn source = resultsTableData.getColumnReference(0);
        double[] dataAsDouble = source.getDataAsDouble(source.getRows());
        float[] arr = new float[dataAsDouble.length];
        for (int i = 0; i < dataAsDouble.length; i++) {
            arr[i] = (float) dataAsDouble[i];
        }
        return new FloatArrayTableColumn(arr, source.getLabel());
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
        double[] o = new double[rows];
        int ncopy = Math.min(rows, data.length);
        for (int i = 0; i < ncopy; i++) {
            o[i] = data[i];
        }
        return o;
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

    public float[] getData() {
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
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        ResultsTableData resultsTableData = new ResultsTableData(Collections.singletonList(this));
        resultsTableData.exportData(storage, name, forceName, progressInfo);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new FloatArrayTableColumn(Arrays.copyOf(data, data.length), label);
    }

    @Override
    public void display(String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        ResultsTableData data = new ResultsTableData(Collections.singleton(this));
        data.display(displayName, desktopWorkbench, source);
    }
}
