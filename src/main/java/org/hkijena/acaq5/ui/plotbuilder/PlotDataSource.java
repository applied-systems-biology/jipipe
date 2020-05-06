package org.hkijena.acaq5.ui.plotbuilder;

/**
 * Interface for any data source for plot data.
 * All methods must always return valid outputs.
 */
public interface PlotDataSource {

    /**
     * Name of this data source
     *
     * @return the name
     */
    String getName();

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
}
