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

package org.hkijena.jipipe.plugins.parameters.api.enums;

import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeAllowedValueInfo;

import java.util.List;

/**
 * Base class for all dynamic enum-like parameters.
 * Used by JIPipe's auto-doc method to obtain allowed values
 */
public interface EnumParameter {
    /**
     * Gets the allowed values of this parameter
     * @return the allowed values
     */
    List<JIPipeParameterTypeAllowedValueInfo> getAllowedValueInfos();
}
