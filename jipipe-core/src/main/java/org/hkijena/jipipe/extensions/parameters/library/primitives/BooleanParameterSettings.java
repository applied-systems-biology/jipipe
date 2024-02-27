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

package org.hkijena.jipipe.extensions.parameters.library.primitives;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BooleanParameterSettings {
    /**
     * If enabled, the boolean parameter is shown as combo box
     *
     * @return if the boolean parameter is shown as combo box
     */
    boolean comboBoxStyle() default false;

    /**
     * @return The combo box label for the "true" value
     */
    String trueLabel() default "Enabled";

    /**
     * @return The combo box label for the "false" value
     */
    String falseLabel() default "Disabled";
}
