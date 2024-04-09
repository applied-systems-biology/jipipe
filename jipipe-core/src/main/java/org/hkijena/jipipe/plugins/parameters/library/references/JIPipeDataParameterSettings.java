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
import org.hkijena.jipipe.utils.classfilters.AnyClassFilter;
import org.hkijena.jipipe.utils.classfilters.ClassFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Settings for {@link JIPipeData} parameters
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JIPipeDataParameterSettings {
    /**
     * Control which data types are available
     *
     * @return the data base class
     */
    Class<? extends JIPipeData> dataBaseClass() default JIPipeData.class;

    /**
     * Allows to implement filters for classes
     *
     * @return class filter instance
     */
    Class<? extends ClassFilter> dataClassFilter() default AnyClassFilter.class;

    /**
     * If true, users can pick hidden data types
     *
     * @return if users can also pick hidden data types
     */
    boolean showHidden() default false;
}
