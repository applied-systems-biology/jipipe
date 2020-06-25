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

package org.hkijena.acaq5.api.data;

/**
 * Interface that converts between {@link ACAQData} instances
 */
public interface ACAQDataConverter {

    /**
     * @return the data type that is the input of the conversion function
     */
    Class<? extends ACAQData> getInputType();

    /**
     * @return the data type that is the output of the conversion function
     */
    Class<? extends ACAQData> getOutputType();

    /**
     * Converts the supported input type to the output type
     *
     * @param input the input data
     * @return the converted input data
     */
    ACAQData convert(ACAQData input);
}
