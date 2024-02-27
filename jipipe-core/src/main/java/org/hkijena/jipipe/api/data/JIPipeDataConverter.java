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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * Interface that converts between {@link JIPipeData} instances
 */
public interface JIPipeDataConverter {

    /**
     * @return the data type that is the input of the conversion function
     */
    Class<? extends JIPipeData> getInputType();

    /**
     * @return the data type that is the output of the conversion function
     */
    Class<? extends JIPipeData> getOutputType();

    /**
     * Converts the supported input type to the output type
     *
     * @param input        the input data
     * @param progressInfo the progress info
     * @return the converted input data
     */
    JIPipeData convert(JIPipeData input, JIPipeProgressInfo progressInfo);
}
