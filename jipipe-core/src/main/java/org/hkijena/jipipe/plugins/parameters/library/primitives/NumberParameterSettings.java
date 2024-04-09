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

package org.hkijena.jipipe.plugins.parameters.library.primitives;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attached to the getter or setter of {@link Number} parameter to setup the GUI
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NumberParameterSettings {
    /**
     * Minimum value
     *
     * @return Minimum value
     */
    double min() default Double.NEGATIVE_INFINITY;

    /**
     * Maximum value
     *
     * @return Maximum value
     */
    double max() default Double.POSITIVE_INFINITY;

    /**
     * The step size when the user clicks increase/decrease
     *
     * @return step size when the user clicks increase/decrease
     */
    double step() default 1;
}
