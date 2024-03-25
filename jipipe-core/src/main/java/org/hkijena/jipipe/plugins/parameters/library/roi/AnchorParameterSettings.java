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

package org.hkijena.jipipe.plugins.parameters.library.roi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to modify the behavior of the {@link Anchor} parameter editor UI
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AnchorParameterSettings {
    boolean allowTopLeft() default true;

    boolean allowTopCenter() default true;

    boolean allowTopRight() default true;

    boolean allowBottomLeft() default true;

    boolean allowBottomCenter() default true;

    boolean allowBottomRight() default true;

    boolean allowCenterLeft() default true;

    boolean allowCenterRight() default true;

    boolean allowCenterCenter() default true;
}
