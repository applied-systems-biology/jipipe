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

package org.hkijena.jipipe.plugins.parameters.api.collections;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Settings for a {@link ListParameter} or derivative
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ListParameterSettings {
    /**
     * If enabled, the contents are displayed in a scrollbar
     *
     * @return if contents should be scrollable
     */
    boolean withScrollBar() default false;

    /**
     * If the scrollbar is enabled, the height of the list control
     *
     * @return height of the list control if scrolling is enabled
     */
    int scrollableHeight() default 350;
}
