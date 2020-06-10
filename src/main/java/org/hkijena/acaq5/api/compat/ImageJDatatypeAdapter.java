package org.hkijena.acaq5.api.compat;

import org.hkijena.acaq5.api.data.ACAQData;

import java.util.List;

/**
 * Interface that contains functions that allow conversion between ACAQ5 and ImageJ data type
 */
public interface ImageJDatatypeAdapter {

    /**
     * Returns true if this adapter can convert to the specified ACAQ data type
     *
     * @param imageJData ImageJ data
     * @return if this adapter can convert to the specified ACAQ data type
     */
    boolean canConvertImageJToACAQ(Object imageJData);

    /**
     * Returns true if this adapter can convert to the specified ImageJ data type
     *
     * @param acaqData ACAQ data
     * @return if this adapter can convert to the specified ImageJ data type
     */
    boolean canConvertACAQToImageJ(ACAQData acaqData);

    /**
     * Gets the corresponding ImageJ data type
     *
     * @return the data type
     */
    Class<?> getImageJDatatype();

    /**
     * Gets the corresponding ACAQ5 data type
     *
     * @return the data type
     */
    Class<? extends ACAQData> getACAQDatatype();

    /**
     * Converts an ImageJ data type to its corresponding ACAQ5 type
     *
     * @param imageJData ImageJ data
     * @return converted data
     */
    ACAQData convertImageJToACAQ(Object imageJData);

    /**
     * Converts an ACAQ5 data type to its corresponding ImageJ data type
     *
     * @param acaqData   ACAQ data
     * @param activate   If true, the data should be made visible in ImageJ
     * @param noWindow   If true, the conversion should not create GUI windows
     * @param windowName Window name of the ImageJ data. Might be ignored or used otherwise to identify data.
     * @return Converted object
     */
    Object convertACAQToImageJ(ACAQData acaqData, boolean activate, boolean noWindow, String windowName);

    /**
     * Converts multiple ACAQ5 data to their corresponding ImageJ data type.
     * This method is actually called by the single algorithm run, as some data might need to be condensed
     *
     * @param acaqData   ACAQ data
     * @param activate   If true, the data should be made visible in ImageJ
     * @param noWindow   If true, the conversion should not create GUI windows
     * @param windowName Window name of the ImageJ data. Might be ignored or used otherwise to identify data.
     * @return Converted object
     */
    List<Object> convertMultipleACAQToImageJ(List<ACAQData> acaqData, boolean activate, boolean noWindow, String windowName);


    /**
     * Imports ACAQ data from an ImageJ window
     *
     * @param parameters Parameters of the adapter. Depends entirely on the adapter
     * @return Imported ACAQ data
     */
    ACAQData importFromImageJ(String parameters);

}
