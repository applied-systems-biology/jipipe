package org.hkijena.acaq5.api.compat;

import org.hkijena.acaq5.api.data.ACAQData;

public interface ImageJDatatypeAdapter {

    /**
     * Returns true if this adapter can convert to the specified ACAQ data type
     *
     * @param imageJData
     * @return
     */
    boolean canConvertImageJToACAQ(Object imageJData);

    /**
     * @param acaqData
     * @return
     */
    boolean canConvertACAQToImageJ(ACAQData acaqData);

    /**
     * Gets the corresponding ImageJ data type
     *
     * @return
     */
    Class<?> getImageJDatatype();

    /**
     * Gets the corresponding ACAQ5 data type
     *
     * @return
     */
    Class<? extends ACAQData> getACAQDatatype();

    /**
     * Converts an ImageJ data type to its corresponding ACAQ5 type
     *
     * @param imageJData
     * @return
     */
    ACAQData convertImageJToACAQ(Object imageJData);

    /**
     * Converts an ACAQ5 data type to its corresponding ImageJ data type
     *
     * @param acaqData
     * @param activate If true, the data should be made visible in ImageJ
     * @return
     */
    Object convertACAQToImageJ(ACAQData acaqData, boolean activate);
}
