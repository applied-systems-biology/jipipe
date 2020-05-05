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
}
