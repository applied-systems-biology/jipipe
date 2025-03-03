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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;

public class RelabeledTableColumnData implements TableColumnData {
    private final TableColumnData tableColumn;
    private String label;

    public RelabeledTableColumnData(TableColumnData tableColumn, String label) {
        this.tableColumn = tableColumn;
        this.label = label;
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        tableColumn.exportData(storage, name, forceName, progressInfo);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new RelabeledTableColumnData((TableColumnData) tableColumn.duplicate(progressInfo), label);
    }

    @Override
    public String[] getDataAsString(int rows) {
        return tableColumn.getDataAsString(rows);
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        return tableColumn.getDataAsDouble(rows);
    }

    @Override
    public String getRowAsString(int row) {
        return tableColumn.getRowAsString(row);
    }

    @Override
    public double getRowAsDouble(int row) {
        return tableColumn.getRowAsDouble(row);
    }

    @Override
    public int getRows() {
        return tableColumn.getRows();
    }

    @Override
    public boolean isNumeric() {
        return tableColumn.isNumeric();
    }

    @Override
    public boolean isUserRemovable() {
        return tableColumn.isUserRemovable();
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
