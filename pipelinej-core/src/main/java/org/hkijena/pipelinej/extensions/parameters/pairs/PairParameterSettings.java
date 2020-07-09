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

package org.hkijena.pipelinej.extensions.parameters.pairs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Allows to control the behavior of {@link Pair}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PairParameterSettings {
    /**
     * @return Optional label for the key
     */
    String keyLabel() default "";

    /**
     * @return Optional label for the value
     */
    String valueLabel() default "";

    /**
     * @return If the parameters are shown in one row - separated with an arrow. Otherwise display them in two rows
     */
    boolean singleRow() default true;

    /**
     * @return If a chevron/arrow is shown in the single row mode
     */
    boolean singleRowWithChevron() default true;
}
