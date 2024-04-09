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

package org.hkijena.jipipe.plugins.parameters.library.collections;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Sets the template class for {@link ParameterCollectionList}.
 * Required to preserve annotations when creating the UI.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ParameterCollectionListTemplate {
    Class<? extends JIPipeParameterCollection> value();
}
