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

package org.hkijena.jipipe.plugins.parameters.library.references;

import org.hkijena.jipipe.api.data.JIPipeData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ImageJImporterParameterSettings {
    /**
     * The data type that is targeted
     *
     * @return the base class
     */
    Class<? extends JIPipeData> baseClass() default JIPipeData.class;

    /**
     * If true, conversions are considered
     *
     * @return if convertible importers are considered
     */
    boolean includeConvertible() default true;
}
