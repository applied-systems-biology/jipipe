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

package org.hkijena.jipipe.api.parameters;

import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

/**
 * An {@link JIPipeParameterCollection} that comes with a predefined name
 */
public interface JIPipeNamedParameterCollection {
    /**
     * A default name. Can still be overridden by external settings
     *
     * @return A default name. Can still be overridden by external settings
     */
    String getDefaultParameterCollectionName();

    /**
     * A default description. Can still be overridden by external settings
     *
     * @return A default description. Can still be overridden by external settings
     */
    HTMLText getDefaultParameterCollectionDescription();
}
