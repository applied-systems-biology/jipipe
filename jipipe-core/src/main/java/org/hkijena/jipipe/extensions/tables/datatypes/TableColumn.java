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
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.extensions.tables.MutableTableColumn;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An {@link JIPipeData} type that represents a column.
 * This type allows data to be provided
 */
@JIPipeDocumentation(name = "Table column", description = "A table column")
@JIPipeDataStorageDocumentation(humanReadableDescription = "This is a generic data type. The storage folder is empty.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
public interface TableColumn extends JIPipeData {
    /**
     * Returns true if the parameter is a mutable table column.
     * Will return false if the data is not a column
     *
     * @param klass the class
     * @return if the parameter is a mutable table column
     */
    static boolean isMutableTableColumn(Class<? extends JIPipeData> klass) {
        return MutableTableColumn.class.isAssignableFrom(klass);
    }

    /**
     * Returns true if the parameter is a generating table column.
     * Will return false if the data is not a column
     *
     * @param klass the class
     * @return if the parameter is a mutable table column
     */
    static boolean isGeneratingTableColumn(Class<? extends JIPipeData> klass) {
        return !klass.isInterface() && TableColumn.class.isAssignableFrom(klass) && !MutableTableColumn.class.isAssignableFrom(klass);
    }

    /**
     * Returns a new table column that contains the selected rows in the provided order
     *
     * @param input the input column
     * @param rows  the rows
     * @return a new table column that contains the selected rows in the provided order
     */
    static TableColumn getSlice(TableColumn input, List<Integer> rows) {
        if (input.isNumeric()) {
            double[] values = new double[rows.size()];
            for (int row = 0; row < rows.size(); row++) {
                int inputRow = rows.get(row);
                values[row] = input.getRowAsDouble(inputRow);
            }
            return new DoubleArrayTableColumn(values, input.getLabel());
        } else {
            String[] values = new String[rows.size()];
            for (int row = 0; row < rows.size(); row++) {
                int inputRow = rows.get(row);
                values[row] = input.getRowAsString(inputRow);
            }
            return new StringArrayTableColumn(values, input.getLabel());
        }
    }

    /**
     * Creates a new column from a collection.
     * Returns a {@link DoubleArrayTableColumn} if the list only contains numbers.
     * Otherwise, returns a {@link StringArrayTableColumn}.
     *
     * @param rows  the row vales
     * @param label the label
     * @return the column
     */
    static TableColumn fromList(Collection<?> rows, String label) {
        if (rows.stream().allMatch(o -> o instanceof Number)) {
            double[] arr = new double[rows.size()];
            int i = 0;
            for (Object row : rows) {
                arr[i++] = ((Number) row).doubleValue();
            }
            return new DoubleArrayTableColumn(arr, label);
        } else {
            String[] arr = new String[rows.size()];
            int i = 0;
            for (Object row : rows) {
                arr[i++] = StringUtils.nullToEmpty(row);
            }
            return new StringArrayTableColumn(arr, label);
        }
    }

    /**
     * Returns as many data entries as rows
     *
     * @param rows the number of rows
     * @return Array with same length as rows
     */
    String[] getDataAsString(int rows);

    /**
     * Returns as many data entries as rows
     *
     * @param rows the number of rows
     * @return Array with same length as rows
     */
    double[] getDataAsDouble(int rows);

    /**
     * Returns the data entry as string
     *
     * @param row the row
     * @return the data
     */
    String getRowAsString(int row);

    /**
     * Returns the data entry as string
     *
     * @param row the row
     * @return the data
     */
    double getRowAsDouble(int row);

    /**
     * Returns the number of rows that are existing (not generated)
     *
     * @return the number of rows that are existing (not generated)
     */
    int getRows();

    /**
     * Returns true if this column contains double entries.
     * If false, it contains {@link String} entries.
     *
     * @return true if this column contains double entries.
     */
    boolean isNumeric();

    /**
     * Returns if users can remove this source
     *
     * @return if users can remove this source
     */
    boolean isUserRemovable();

    /**
     * Label of this column
     *
     * @return the label
     */
    String getLabel();

    /**
     * Gets the data at the specified row as object
     *
     * @param row the row
     * @return the object
     */
    default Object getRowAsObject(int row) {
        return isNumeric() ? getRowAsDouble(row) : getRowAsString(row);
    }

    /**
     * Returns the data as list of objects
     * The objects are strings or numbers depending on the column type
     *
     * @return the list of objects
     */
    default List<Object> getDataAsObjectList() {
        List<Object> result = new ArrayList<>(getRows());
        for (int i = 0; i < getRows(); i++) {
            result.add(getRowAsObject(i));
        }
        return result;
    }
}
