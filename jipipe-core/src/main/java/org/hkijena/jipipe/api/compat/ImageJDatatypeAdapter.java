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

package org.hkijena.jipipe.api.compat;

import org.hkijena.jipipe.api.data.JIPipeData;

import java.util.List;

/**
 * Interface that contains functions that allow conversion between JIPipe and ImageJ data type
 */
public interface ImageJDatatypeAdapter {

    /**
     * Returns true if this adapter can convert to the specified JIPipe data type
     *
     * @param imageJData ImageJ data
     * @return if this adapter can convert to the specified JIPipe data type
     */
    boolean canConvertImageJToJIPipe(Object imageJData);

    /**
     * Returns true if this adapter can convert to the specified ImageJ data type
     *
     * @param jipipeData JIPipe data
     * @return if this adapter can convert to the specified ImageJ data type
     */
    boolean canConvertJIPipeToImageJ(JIPipeData jipipeData);

    /**
     * Gets the corresponding ImageJ data type
     *
     * @return the data type
     */
    Class<?> getImageJDatatype();

    /**
     * Gets the corresponding JIPipe data type
     *
     * @return the data type
     */
    Class<? extends JIPipeData> getJIPipeDatatype();

    /**
     * Converts an ImageJ data type to its corresponding JIPipe type
     *
     * @param imageJData ImageJ data
     * @return converted data
     */
    JIPipeData convertImageJToJIPipe(Object imageJData);

    /**
     * Converts an JIPipe data type to its corresponding ImageJ data type
     *
     * @param jipipeData   JIPipe data
     * @param activate   If true, the data should be made visible in ImageJ
     * @param noWindow   If true, the conversion should not create GUI windows
     * @param windowName Window name of the ImageJ data. Might be ignored or used otherwise to identify data.
     * @return Converted object
     */
    Object convertJIPipeToImageJ(JIPipeData jipipeData, boolean activate, boolean noWindow, String windowName);

    /**
     * Converts multiple JIPipe data to their corresponding ImageJ data type.
     * This method is actually called by the single algorithm run, as some data might need to be condensed
     *
     * @param jipipeData   JIPipe data
     * @param activate   If true, the data should be made visible in ImageJ
     * @param noWindow   If true, the conversion should not create GUI windows
     * @param windowName Window name of the ImageJ data. Might be ignored or used otherwise to identify data.
     * @return Converted object
     */
    List<Object> convertMultipleJIPipeToImageJ(List<JIPipeData> jipipeData, boolean activate, boolean noWindow, String windowName);


    /**
     * Imports JIPipe data from an ImageJ window
     *
     * @param parameters Parameters of the adapter. Depends entirely on the adapter
     * @return Imported JIPipe data
     */
    JIPipeData importFromImageJ(String parameters);

}
