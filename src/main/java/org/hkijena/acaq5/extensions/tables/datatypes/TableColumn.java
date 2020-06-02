package org.hkijena.acaq5.extensions.tables.datatypes;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.plots.datatypes.PlotDataSeries;

/**
 * An {@link ACAQData} type that represents a column in a {@link PlotDataSeries}.
 * This type allows data to be provided
 */
public interface TableColumn extends ACAQData {
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
     * Returns true if the parameter is a mutable table column.
     * Will return false if the data is not a {@link TableColumn}
     *
     * @param klass the class
     * @return if the parameter is a mutable table column
     */
    static boolean isMutableTableColumn(Class<? extends ACAQData> klass) {
        return MutableTableColumn.class.isAssignableFrom(klass);
    }

    /**
     * Returns true if the parameter is a generating table column.
     * Will return false if the data is not a {@link TableColumn}
     *
     * @param klass the class
     * @return if the parameter is a mutable table column
     */
    static boolean isGeneratingTableColumn(Class<? extends ACAQData> klass) {
        return TableColumn.class.isAssignableFrom(klass) && !MutableTableColumn.class.isAssignableFrom(klass);
    }
}
