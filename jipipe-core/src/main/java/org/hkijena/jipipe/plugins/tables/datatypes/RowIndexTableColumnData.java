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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;

/**
 * Generates numbers from 0 to row count
 */
@SetJIPipeDocumentation(name = "Row index table column", description = "A table column that generates each row based on the current row index")
@JIPipeDataStorageDocumentation(humanReadableDescription = "This is a structural data type. The storage folder is empty.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
public class RowIndexTableColumnData implements TableColumnData {
    public static RowIndexTableColumnData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new RowIndexTableColumnData();
    }

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
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new RowIndexTableColumnData();
    }

}
